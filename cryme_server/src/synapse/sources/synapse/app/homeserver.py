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
# Copyright 2014-2016 OpenMarket Ltd
# Copyright 2019 New Vector Ltd
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
import sys
from typing import Dict, Iterable, Iterator, List

from twisted.internet.tcp import Port
from twisted.web.resource import EncodingResourceWrapper, Resource
from twisted.web.server import GzipEncoderFactory
from twisted.web.static import File

import synapse
import synapse.config.logger
from synapse import events
from synapse.api.urls import (
    CLIENT_API_PREFIX,
    FEDERATION_PREFIX,
    LEGACY_MEDIA_PREFIX,
    MEDIA_R0_PREFIX,
    MEDIA_V3_PREFIX,
    SERVER_KEY_V2_PREFIX,
    STATIC_PREFIX,
    WEB_CLIENT_PREFIX,
)
from synapse.app import _base
from synapse.app._base import (
    handle_startup_exception,
    listen_ssl,
    listen_tcp,
    max_request_body_size,
    redirect_stdio_to_logs,
    register_start,
)
from synapse.config._base import ConfigError
from synapse.config.emailconfig import ThreepidBehaviour
from synapse.config.homeserver import HomeServerConfig
from synapse.config.server import ListenerConfig
from synapse.federation.transport.server import TransportLayerServer
from synapse.http.additional_resource import AdditionalResource
from synapse.http.server import (
    OptionsResource,
    RootOptionsRedirectResource,
    RootRedirect,
    StaticResource,
)
from synapse.http.site import SynapseSite
from synapse.logging.context import LoggingContext
from synapse.metrics import METRICS_PREFIX, MetricsResource, RegistryProxy
from synapse.python_dependencies import check_requirements
from synapse.replication.http import REPLICATION_PREFIX, ReplicationRestResource
from synapse.replication.tcp.resource import ReplicationStreamProtocolFactory
from synapse.rest import ClientRestResource
from synapse.rest.admin import AdminRestResource
from synapse.rest.health import HealthResource
from synapse.rest.key.v2 import KeyApiV2Resource
from synapse.rest.synapse.client import build_synapse_client_resource_tree
from synapse.rest.well_known import well_known_resource
from synapse.server import HomeServer
from synapse.storage import DataStore
from synapse.util.httpresourcetree import create_resource_tree
from synapse.util.module_loader import load_module
from synapse.util.versionstring import get_version_string

logger = logging.getLogger("synapse.app.homeserver")


def gz_wrap(r: Resource) -> Resource:
    return EncodingResourceWrapper(r, [GzipEncoderFactory()])


class SynapseHomeServer(HomeServer):
    DATASTORE_CLASS = DataStore  # type: ignore

    def _listener_http(
        self, config: HomeServerConfig, listener_config: ListenerConfig
    ) -> Iterable[Port]:
        port = listener_config.port
        bind_addresses = listener_config.bind_addresses
        tls = listener_config.tls
        # Must exist since this is an HTTP listener.
        assert listener_config.http_options is not None
        site_tag = listener_config.http_options.tag
        if site_tag is None:
            site_tag = str(port)

        # We always include a health resource.
        resources: Dict[str, Resource] = {"/health": HealthResource()}

        for res in listener_config.http_options.resources:
            for name in res.names:
                if name == "openid" and "federation" in res.names:
                    # Skip loading openid resource if federation is defined
                    # since federation resource will include openid
                    continue
                resources.update(self._configure_named_resource(name, res.compress))

        additional_resources = listener_config.http_options.additional_resources
        logger.debug("Configuring additional resources: %r", additional_resources)
        module_api = self.get_module_api()
        for path, resmodule in additional_resources.items():
            handler_cls, config = load_module(
                resmodule,
                ("listeners", site_tag, "additional_resources", "<%s>" % (path,)),
            )
            handler = handler_cls(config, module_api)
            if isinstance(handler, Resource):
                resource = handler
            elif hasattr(handler, "handle_request"):
                resource = AdditionalResource(self, handler.handle_request)
            else:
                raise ConfigError(
                    "additional_resource %s does not implement a known interface"
                    % (resmodule["module"],)
                )
            resources[path] = resource

        # Attach additional resources registered by modules.
        resources.update(self._module_web_resources)
        self._module_web_resources_consumed = True

        # try to find something useful to redirect '/' to
        if WEB_CLIENT_PREFIX in resources:
            root_resource: Resource = RootOptionsRedirectResource(WEB_CLIENT_PREFIX)
        elif STATIC_PREFIX in resources:
            root_resource = RootOptionsRedirectResource(STATIC_PREFIX)
        else:
            root_resource = OptionsResource()

        site = SynapseSite(
            "synapse.access.%s.%s" % ("https" if tls else "http", site_tag),
            site_tag,
            listener_config,
            create_resource_tree(resources, root_resource),
            self.version_string,
            max_request_body_size=max_request_body_size(self.config),
            reactor=self.get_reactor(),
        )

        if tls:
            # refresh_certificate should have been called before this.
            assert self.tls_server_context_factory is not None
            ports = listen_ssl(
                bind_addresses,
                port,
                site,
                self.tls_server_context_factory,
                reactor=self.get_reactor(),
            )
            logger.info("Synapse now listening on TCP port %d (TLS)", port)

        else:
            ports = listen_tcp(
                bind_addresses,
                port,
                site,
                reactor=self.get_reactor(),
            )
            logger.info("Synapse now listening on TCP port %d", port)

        return ports

    def _configure_named_resource(
        self, name: str, compress: bool = False
    ) -> Dict[str, Resource]:
        """Build a resource map for a named resource

        Args:
            name: named resource: one of "client", "federation", etc
            compress: whether to enable gzip compression for this resource

        Returns:
            map from path to HTTP resource
        """
        resources: Dict[str, Resource] = {}
        if name == "client":
            client_resource: Resource = ClientRestResource(self)
            if compress:
                client_resource = gz_wrap(client_resource)

            resources.update(
                {
                    CLIENT_API_PREFIX: client_resource,
                    "/.well-known": well_known_resource(self),
                    "/_synapse/admin": AdminRestResource(self),
                    **build_synapse_client_resource_tree(self),
                }
            )

            if self.config.email.threepid_behaviour_email == ThreepidBehaviour.LOCAL:
                from synapse.rest.synapse.client.password_reset import (
                    PasswordResetSubmitTokenResource,
                )

                resources[
                    "/_synapse/client/password_reset/email/submit_token"
                ] = PasswordResetSubmitTokenResource(self)

        if name == "consent":
            from synapse.rest.consent.consent_resource import ConsentResource

            consent_resource: Resource = ConsentResource(self)
            if compress:
                consent_resource = gz_wrap(consent_resource)
            resources.update({"/_matrix/consent": consent_resource})

        if name == "federation":
            resources.update({FEDERATION_PREFIX: TransportLayerServer(self)})

        if name == "openid":
            resources.update(
                {
                    FEDERATION_PREFIX: TransportLayerServer(
                        self, servlet_groups=["openid"]
                    )
                }
            )

        if name in ["static", "client"]:
            resources.update(
                {
                    STATIC_PREFIX: StaticResource(
                        os.path.join(os.path.dirname(synapse.__file__), "static")
                    )
                }
            )

        if name in ["media", "federation", "client"]:
            if self.config.server.enable_media_repo:
                media_repo = self.get_media_repository_resource()
                resources.update(
                    {
                        MEDIA_R0_PREFIX: media_repo,
                        MEDIA_V3_PREFIX: media_repo,
                        LEGACY_MEDIA_PREFIX: media_repo,
                    }
                )
            elif name == "media":
                raise ConfigError(
                    "'media' resource conflicts with enable_media_repo=False"
                )

        if name in ["keys", "federation"]:
            resources[SERVER_KEY_V2_PREFIX] = KeyApiV2Resource(self)

        if name == "webclient":
            webclient_loc = self.config.server.web_client_location

            if webclient_loc is None:
                logger.warning(
                    "Not enabling webclient resource, as web_client_location is unset."
                )
            elif webclient_loc.startswith("http://") or webclient_loc.startswith(
                "https://"
            ):
                resources[WEB_CLIENT_PREFIX] = RootRedirect(webclient_loc)
            else:
                logger.warning(
                    "Running webclient on the same domain is not recommended: "
                    "https://github.com/matrix-org/synapse#security-note - "
                    "after you move webclient to different host you can set "
                    "web_client_location to its full URL to enable redirection."
                )
                # GZip is disabled here due to
                # https://twistedmatrix.com/trac/ticket/7678
                resources[WEB_CLIENT_PREFIX] = File(webclient_loc)

        if name == "metrics" and self.config.metrics.enable_metrics:
            resources[METRICS_PREFIX] = MetricsResource(RegistryProxy)

        if name == "replication":
            resources[REPLICATION_PREFIX] = ReplicationRestResource(self)

        return resources

    def start_listening(self) -> None:
        if self.config.redis.redis_enabled:
            # If redis is enabled we connect via the replication command handler
            # in the same way as the workers (since we're effectively a client
            # rather than a server).
            self.get_tcp_replication().start_replication(self)

        for listener in self.config.server.listeners:
            if listener.type == "http":
                self._listening_services.extend(
                    self._listener_http(self.config, listener)
                )
            elif listener.type == "manhole":
                _base.listen_manhole(
                    listener.bind_addresses,
                    listener.port,
                    manhole_settings=self.config.server.manhole_settings,
                    manhole_globals={"hs": self},
                )
            elif listener.type == "replication":
                services = listen_tcp(
                    listener.bind_addresses,
                    listener.port,
                    ReplicationStreamProtocolFactory(self),
                )
                for s in services:
                    self.get_reactor().addSystemEventTrigger(
                        "before", "shutdown", s.stopListening
                    )
            elif listener.type == "metrics":
                if not self.config.metrics.enable_metrics:
                    logger.warning(
                        "Metrics listener configured, but "
                        "enable_metrics is not True!"
                    )
                else:
                    _base.listen_metrics(listener.bind_addresses, listener.port)
            else:
                # this shouldn't happen, as the listener type should have been checked
                # during parsing
                logger.warning("Unrecognized listener type: %s", listener.type)


def setup(config_options: List[str]) -> SynapseHomeServer:
    """
    Args:
        config_options_options: The options passed to Synapse. Usually `sys.argv[1:]`.

    Returns:
        A homeserver instance.
    """
    try:
        config = HomeServerConfig.load_or_generate_config(
            "Synapse Homeserver", config_options
        )
    except ConfigError as e:
        sys.stderr.write("\n")
        for f in format_config_error(e):
            sys.stderr.write(f)
        sys.stderr.write("\n")
        sys.exit(1)

    if not config:
        # If a config isn't returned, and an exception isn't raised, we're just
        # generating config files and shouldn't try to continue.
        sys.exit(0)

    if config.worker.worker_app:
        raise ConfigError(
            "You have specified `worker_app` in the config but are attempting to start a non-worker "
            "instance. Please use `python -m synapse.app.generic_worker` instead (or remove the option if this is the main process)."
        )
        sys.exit(1)

    events.USE_FROZEN_DICTS = config.server.use_frozen_dicts
    synapse.util.caches.TRACK_MEMORY_USAGE = config.caches.track_memory_usage

    if config.server.gc_seconds:
        synapse.metrics.MIN_TIME_BETWEEN_GCS = config.server.gc_seconds

    hs = SynapseHomeServer(
        config.server.server_name,
        config=config,
        version_string="Synapse/" + get_version_string(synapse),
    )

    synapse.config.logger.setup_logging(hs, config, use_worker_options=False)

    logger.info("Setting up server")

    try:
        hs.setup()
    except Exception as e:
        handle_startup_exception(e)

    async def start() -> None:
        # Load the OIDC provider metadatas, if OIDC is enabled.
        if hs.config.oidc.oidc_enabled:
            oidc = hs.get_oidc_handler()
            # Loading the provider metadata also ensures the provider config is valid.
            await oidc.load_metadata()

        await _base.start(hs)

        hs.get_datastore().db_pool.updates.start_doing_background_updates()

    register_start(start)

    return hs


def format_config_error(e: ConfigError) -> Iterator[str]:
    """
    Formats a config error neatly

    The idea is to format the immediate error, plus the "causes" of those errors,
    hopefully in a way that makes sense to the user. For example:

        Error in configuration at 'oidc_config.user_mapping_provider.config.display_name_template':
          Failed to parse config for module 'JinjaOidcMappingProvider':
            invalid jinja template:
              unexpected end of template, expected 'end of print statement'.

    Args:
        e: the error to be formatted

    Returns: An iterator which yields string fragments to be formatted
    """
    yield "Error in configuration"

    if e.path:
        yield " at '%s'" % (".".join(e.path),)

    yield ":\n  %s" % (e.msg,)

    parent_e = e.__cause__
    indent = 1
    while parent_e:
        indent += 1
        yield ":\n%s%s" % ("  " * indent, str(parent_e))
        parent_e = parent_e.__cause__


def run(hs: HomeServer) -> None:
    _base.start_reactor(
        "synapse-homeserver",
        soft_file_limit=hs.config.server.soft_file_limit,
        gc_thresholds=hs.config.server.gc_thresholds,
        pid_file=hs.config.server.pid_file,
        daemonize=hs.config.server.daemonize,
        print_pidfile=hs.config.server.print_pidfile,
        logger=logger,
    )


def main() -> None:
    with LoggingContext("main"):
        # check base requirements
        check_requirements()
        hs = setup(sys.argv[1:])

        # redirect stdio to the logs, if configured.
        if not hs.config.logging.no_redirect_stdio:
            redirect_stdio_to_logs()

        run(hs)


if __name__ == "__main__":
    main()
