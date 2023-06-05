#/*************************** The CRY.ME project (2023) *************************************************
# *
# *  This file is part of the CRY.ME project (https://github.com/ANSSI-FR/cry-me).
# *  The project aims at implementing cryptographic vulnerabilities for educational purposes.
# *  Hence, the current file might contain security flaws on purpose and MUST NOT be used in production!
# *  Please do not use this source code outside this scope, or use it knowingly.
# *
# *  Many files come from the Android element (https://github.com/vector-im/element-android), the
# *  Matrix SDK (https://github.com/matrix-org/matrix-android-sdk2) as well as the Android Yubikit
# *  (https://github.com/Yubico/yubikit-android) projects and have been willingly modified
# *  for the CRY.ME project purposes. The Android element, Matrix SDK and Yubikit projects are distributed
# *  under the Apache-2.0 license, and so is the CRY.ME project.
# *
# ***************************  (END OF CRY.ME HEADER)   *************************************************/
#
# Copyright 2014, 2015 OpenMarket Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import logging
import os
from typing import Dict, List, Tuple
from urllib.request import getproxies_environment  # type: ignore

import attr

from synapse.config.server import DEFAULT_IP_RANGE_BLACKLIST, generate_ip_set
from synapse.python_dependencies import DependencyException, check_requirements
from synapse.types import JsonDict
from synapse.util.module_loader import load_module

from ._base import Config, ConfigError

logger = logging.getLogger(__name__)

DEFAULT_THUMBNAIL_SIZES = [
    {"width": 32, "height": 32, "method": "crop"},
    {"width": 96, "height": 96, "method": "crop"},
    {"width": 320, "height": 240, "method": "scale"},
    {"width": 640, "height": 480, "method": "scale"},
    {"width": 800, "height": 600, "method": "scale"},
]

THUMBNAIL_SIZE_YAML = """\
        #  - width: %(width)i
        #    height: %(height)i
        #    method: %(method)s
"""

HTTP_PROXY_SET_WARNING = """\
The Synapse config url_preview_ip_range_blacklist will be ignored as an HTTP(s) proxy is configured."""


@attr.s(frozen=True, slots=True, auto_attribs=True)
class ThumbnailRequirement:
    width: int
    height: int
    method: str
    media_type: str


@attr.s(frozen=True, slots=True, auto_attribs=True)
class MediaStorageProviderConfig:
    store_local: bool  # Whether to store newly uploaded local files
    store_remote: bool  # Whether to store newly downloaded remote files
    store_synchronous: bool  # Whether to wait for successful storage for local uploads


def parse_thumbnail_requirements(
    thumbnail_sizes: List[JsonDict],
) -> Dict[str, Tuple[ThumbnailRequirement, ...]]:
    """Takes a list of dictionaries with "width", "height", and "method" keys
    and creates a map from image media types to the thumbnail size, thumbnailing
    method, and thumbnail media type to precalculate

    Args:
        thumbnail_sizes: List of dicts with "width", "height", and "method" keys

    Returns:
        Dictionary mapping from media type string to list of ThumbnailRequirement.
    """
    requirements: Dict[str, List[ThumbnailRequirement]] = {}
    for size in thumbnail_sizes:
        width = size["width"]
        height = size["height"]
        method = size["method"]
        jpeg_thumbnail = ThumbnailRequirement(width, height, method, "image/jpeg")
        png_thumbnail = ThumbnailRequirement(width, height, method, "image/png")
        requirements.setdefault("image/jpeg", []).append(jpeg_thumbnail)
        requirements.setdefault("image/jpg", []).append(jpeg_thumbnail)
        requirements.setdefault("image/webp", []).append(jpeg_thumbnail)
        requirements.setdefault("image/gif", []).append(png_thumbnail)
        requirements.setdefault("image/png", []).append(png_thumbnail)
    return {
        media_type: tuple(thumbnails) for media_type, thumbnails in requirements.items()
    }


class ContentRepositoryConfig(Config):
    section = "media"

    def read_config(self, config, **kwargs):

        # Only enable the media repo if either the media repo is enabled or the
        # current worker app is the media repo.
        if (
            self.root.server.enable_media_repo is False
            and config.get("worker_app") != "synapse.app.media_repository"
        ):
            self.can_load_media_repo = False
            return
        else:
            self.can_load_media_repo = True

        # Whether this instance should be the one to run the background jobs to
        # e.g clean up old URL previews.
        self.media_instance_running_background_jobs = config.get(
            "media_instance_running_background_jobs",
        )

        self.max_upload_size = self.parse_size(config.get("max_upload_size", "50M"))
        self.max_image_pixels = self.parse_size(config.get("max_image_pixels", "32M"))
        self.max_spider_size = self.parse_size(config.get("max_spider_size", "10M"))

        self.media_store_path = self.ensure_directory(
            config.get("media_store_path", "media_store")
        )

        backup_media_store_path = config.get("backup_media_store_path")

        synchronous_backup_media_store = config.get(
            "synchronous_backup_media_store", False
        )

        storage_providers = config.get("media_storage_providers", [])

        if backup_media_store_path:
            if storage_providers:
                raise ConfigError(
                    "Cannot use both 'backup_media_store_path' and 'storage_providers'"
                )

            storage_providers = [
                {
                    "module": "file_system",
                    "store_local": True,
                    "store_synchronous": synchronous_backup_media_store,
                    "store_remote": True,
                    "config": {"directory": backup_media_store_path},
                }
            ]

        # This is a list of config that can be used to create the storage
        # providers. The entries are tuples of (Class, class_config,
        # MediaStorageProviderConfig), where Class is the class of the provider,
        # the class_config the config to pass to it, and
        # MediaStorageProviderConfig are options for StorageProviderWrapper.
        #
        # We don't create the storage providers here as not all workers need
        # them to be started.
        self.media_storage_providers: List[tuple] = []

        for i, provider_config in enumerate(storage_providers):
            # We special case the module "file_system" so as not to need to
            # expose FileStorageProviderBackend
            if provider_config["module"] == "file_system":
                provider_config["module"] = (
                    "synapse.rest.media.v1.storage_provider"
                    ".FileStorageProviderBackend"
                )

            provider_class, parsed_config = load_module(
                provider_config, ("media_storage_providers", "<item %i>" % i)
            )

            wrapper_config = MediaStorageProviderConfig(
                provider_config.get("store_local", False),
                provider_config.get("store_remote", False),
                provider_config.get("store_synchronous", False),
            )

            self.media_storage_providers.append(
                (provider_class, parsed_config, wrapper_config)
            )

        self.dynamic_thumbnails = config.get("dynamic_thumbnails", False)
        self.thumbnail_requirements = parse_thumbnail_requirements(
            config.get("thumbnail_sizes", DEFAULT_THUMBNAIL_SIZES)
        )
        self.url_preview_enabled = config.get("url_preview_enabled", False)
        if self.url_preview_enabled:
            try:
                check_requirements("url_preview")

            except DependencyException as e:
                raise ConfigError(
                    e.message  # noqa: B306, DependencyException.message is a property
                )

            proxy_env = getproxies_environment()
            if "url_preview_ip_range_blacklist" not in config:
                if "http" not in proxy_env or "https" not in proxy_env:
                    raise ConfigError(
                        "For security, you must specify an explicit target IP address "
                        "blacklist in url_preview_ip_range_blacklist for url previewing "
                        "to work"
                    )
            else:
                if "http" in proxy_env or "https" in proxy_env:
                    logger.warning("".join(HTTP_PROXY_SET_WARNING))

            # we always blacklist '0.0.0.0' and '::', which are supposed to be
            # unroutable addresses.
            self.url_preview_ip_range_blacklist = generate_ip_set(
                config["url_preview_ip_range_blacklist"],
                ["0.0.0.0", "::"],
                config_path=("url_preview_ip_range_blacklist",),
            )

            self.url_preview_ip_range_whitelist = generate_ip_set(
                config.get("url_preview_ip_range_whitelist", ()),
                config_path=("url_preview_ip_range_whitelist",),
            )

            self.url_preview_url_blacklist = config.get("url_preview_url_blacklist", ())

            self.url_preview_accept_language = config.get(
                "url_preview_accept_language"
            ) or ["en"]

    def generate_config_section(self, data_dir_path, **kwargs):
        media_store = os.path.join(data_dir_path, "media_store")

        formatted_thumbnail_sizes = "".join(
            THUMBNAIL_SIZE_YAML % s for s in DEFAULT_THUMBNAIL_SIZES
        )
        # strip final NL
        formatted_thumbnail_sizes = formatted_thumbnail_sizes[:-1]

        ip_range_blacklist = "\n".join(
            "        #  - '%s'" % ip for ip in DEFAULT_IP_RANGE_BLACKLIST
        )

        return (
            r"""
        ## Media Store ##

        # Enable the media store service in the Synapse master. Uncomment the
        # following if you are using a separate media store worker.
        #
        #enable_media_repo: false

        # Directory where uploaded images and attachments are stored.
        #
        media_store_path: "%(media_store)s"

        # Media storage providers allow media to be stored in different
        # locations.
        #
        #media_storage_providers:
        #  - module: file_system
        #    # Whether to store newly uploaded local files
        #    store_local: false
        #    # Whether to store newly downloaded remote files
        #    store_remote: false
        #    # Whether to wait for successful storage for local uploads
        #    store_synchronous: false
        #    config:
        #       directory: /mnt/some/other/directory

        # The largest allowed upload size in bytes
        #
        # If you are using a reverse proxy you may also need to set this value in
        # your reverse proxy's config. Notably Nginx has a small max body size by default.
        # See https://matrix-org.github.io/synapse/latest/reverse_proxy.html.
        #
        #max_upload_size: 50M

        # Maximum number of pixels that will be thumbnailed
        #
        #max_image_pixels: 32M

        # Whether to generate new thumbnails on the fly to precisely match
        # the resolution requested by the client. If true then whenever
        # a new resolution is requested by the client the server will
        # generate a new thumbnail. If false the server will pick a thumbnail
        # from a precalculated list.
        #
        #dynamic_thumbnails: false

        # List of thumbnails to precalculate when an image is uploaded.
        #
        #thumbnail_sizes:
%(formatted_thumbnail_sizes)s

        # Is the preview URL API enabled?
        #
        # 'false' by default: uncomment the following to enable it (and specify a
        # url_preview_ip_range_blacklist blacklist).
        #
        #url_preview_enabled: true

        # List of IP address CIDR ranges that the URL preview spider is denied
        # from accessing.  There are no defaults: you must explicitly
        # specify a list for URL previewing to work.  You should specify any
        # internal services in your network that you do not want synapse to try
        # to connect to, otherwise anyone in any Matrix room could cause your
        # synapse to issue arbitrary GET requests to your internal services,
        # causing serious security issues.
        #
        # (0.0.0.0 and :: are always blacklisted, whether or not they are explicitly
        # listed here, since they correspond to unroutable addresses.)
        #
        # This must be specified if url_preview_enabled is set. It is recommended that
        # you uncomment the following list as a starting point.
        #
        # Note: The value is ignored when an HTTP proxy is in use
        #
        #url_preview_ip_range_blacklist:
%(ip_range_blacklist)s

        # List of IP address CIDR ranges that the URL preview spider is allowed
        # to access even if they are specified in url_preview_ip_range_blacklist.
        # This is useful for specifying exceptions to wide-ranging blacklisted
        # target IP ranges - e.g. for enabling URL previews for a specific private
        # website only visible in your network.
        #
        #url_preview_ip_range_whitelist:
        #   - '192.168.1.1'

        # Optional list of URL matches that the URL preview spider is
        # denied from accessing.  You should use url_preview_ip_range_blacklist
        # in preference to this, otherwise someone could define a public DNS
        # entry that points to a private IP address and circumvent the blacklist.
        # This is more useful if you know there is an entire shape of URL that
        # you know that will never want synapse to try to spider.
        #
        # Each list entry is a dictionary of url component attributes as returned
        # by urlparse.urlsplit as applied to the absolute form of the URL.  See
        # https://docs.python.org/2/library/urlparse.html#urlparse.urlsplit
        # The values of the dictionary are treated as an filename match pattern
        # applied to that component of URLs, unless they start with a ^ in which
        # case they are treated as a regular expression match.  If all the
        # specified component matches for a given list item succeed, the URL is
        # blacklisted.
        #
        #url_preview_url_blacklist:
        #  # blacklist any URL with a username in its URI
        #  - username: '*'
        #
        #  # blacklist all *.google.com URLs
        #  - netloc: 'google.com'
        #  - netloc: '*.google.com'
        #
        #  # blacklist all plain HTTP URLs
        #  - scheme: 'http'
        #
        #  # blacklist http(s)://www.acme.com/foo
        #  - netloc: 'www.acme.com'
        #    path: '/foo'
        #
        #  # blacklist any URL with a literal IPv4 address
        #  - netloc: '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$'

        # The largest allowed URL preview spidering size in bytes
        #
        #max_spider_size: 10M

        # A list of values for the Accept-Language HTTP header used when
        # downloading webpages during URL preview generation. This allows
        # Synapse to specify the preferred languages that URL previews should
        # be in when communicating with remote servers.
        #
        # Each value is a IETF language tag; a 2-3 letter identifier for a
        # language, optionally followed by subtags separated by '-', specifying
        # a country or region variant.
        #
        # Multiple values can be provided, and a weight can be added to each by
        # using quality value syntax (;q=). '*' translates to any language.
        #
        # Defaults to "en".
        #
        # Example:
        #
        # url_preview_accept_language:
        #   - en-UK
        #   - en-US;q=0.9
        #   - fr;q=0.8
        #   - *;q=0.7
        #
        url_preview_accept_language:
        #   - en
        """
            % locals()
        )
