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
# Copyright 2019-2021 The Matrix.org Foundation C.I.C.
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
import urllib
from types import TracebackType
from typing import Awaitable, Dict, Generator, List, Optional, Tuple, Type

import attr

from twisted.internet.interfaces import IConsumer
from twisted.protocols.basic import FileSender
from twisted.web.server import Request

from synapse.api.errors import Codes, SynapseError, cs_error
from synapse.http.server import finish_request, respond_with_json
from synapse.http.site import SynapseRequest
from synapse.logging.context import make_deferred_yieldable
from synapse.util.stringutils import is_ascii, parse_and_validate_server_name

logger = logging.getLogger(__name__)

# list all text content types that will have the charset default to UTF-8 when
# none is given
TEXT_CONTENT_TYPES = [
    "text/css",
    "text/csv",
    "text/html",
    "text/calendar",
    "text/plain",
    "text/javascript",
    "application/json",
    "application/ld+json",
    "application/rtf",
    "image/svg+xml",
    "text/xml",
]


def parse_media_id(request: Request) -> Tuple[str, str, Optional[str]]:
    """Parses the server name, media ID and optional file name from the request URI

    Also performs some rough validation on the server name.

    Args:
        request: The `Request`.

    Returns:
        A tuple containing the parsed server name, media ID and optional file name.

    Raises:
        SynapseError(404): if parsing or validation fail for any reason
    """
    try:
        # The type on postpath seems incorrect in Twisted 21.2.0.
        postpath: List[bytes] = request.postpath  # type: ignore
        assert postpath

        # This allows users to append e.g. /test.png to the URL. Useful for
        # clients that parse the URL to see content type.
        server_name_bytes, media_id_bytes = postpath[:2]
        server_name = server_name_bytes.decode("utf-8")
        media_id = media_id_bytes.decode("utf8")

        # Validate the server name, raising if invalid
        parse_and_validate_server_name(server_name)

        file_name = None
        if len(postpath) > 2:
            try:
                file_name = urllib.parse.unquote(postpath[-1].decode("utf-8"))
            except UnicodeDecodeError:
                pass
        return server_name, media_id, file_name
    except Exception:
        raise SynapseError(
            404, "Invalid media id token %r" % (request.postpath,), Codes.UNKNOWN
        )


def respond_404(request: SynapseRequest) -> None:
    respond_with_json(
        request,
        404,
        cs_error("Not found %r" % (request.postpath,), code=Codes.NOT_FOUND),
        send_cors=True,
    )


async def respond_with_file(
    request: SynapseRequest,
    media_type: str,
    file_path: str,
    file_size: Optional[int] = None,
    upload_name: Optional[str] = None,
) -> None:
    logger.debug("Responding with %r", file_path)

    if os.path.isfile(file_path):
        if file_size is None:
            stat = os.stat(file_path)
            file_size = stat.st_size

        add_file_headers(request, media_type, file_size, upload_name)

        with open(file_path, "rb") as f:
            await make_deferred_yieldable(FileSender().beginFileTransfer(f, request))

        finish_request(request)
    else:
        respond_404(request)


def add_file_headers(
    request: Request,
    media_type: str,
    file_size: Optional[int],
    upload_name: Optional[str],
) -> None:
    """Adds the correct response headers in preparation for responding with the
    media.

    Args:
        request
        media_type: The media/content type.
        file_size: Size in bytes of the media, if known.
        upload_name: The name of the requested file, if any.
    """

    def _quote(x: str) -> str:
        return urllib.parse.quote(x.encode("utf-8"))

    # Default to a UTF-8 charset for text content types.
    # ex, uses UTF-8 for 'text/css' but not 'text/css; charset=UTF-16'
    if media_type.lower() in TEXT_CONTENT_TYPES:
        content_type = media_type + "; charset=UTF-8"
    else:
        content_type = media_type

    request.setHeader(b"Content-Type", content_type.encode("UTF-8"))
    if upload_name:
        # RFC6266 section 4.1 [1] defines both `filename` and `filename*`.
        #
        # `filename` is defined to be a `value`, which is defined by RFC2616
        # section 3.6 [2] to be a `token` or a `quoted-string`, where a `token`
        # is (essentially) a single US-ASCII word, and a `quoted-string` is a
        # US-ASCII string surrounded by double-quotes, using backslash as an
        # escape character. Note that %-encoding is *not* permitted.
        #
        # `filename*` is defined to be an `ext-value`, which is defined in
        # RFC5987 section 3.2.1 [3] to be `charset "'" [ language ] "'" value-chars`,
        # where `value-chars` is essentially a %-encoded string in the given charset.
        #
        # [1]: https://tools.ietf.org/html/rfc6266#section-4.1
        # [2]: https://tools.ietf.org/html/rfc2616#section-3.6
        # [3]: https://tools.ietf.org/html/rfc5987#section-3.2.1

        # We avoid the quoted-string version of `filename`, because (a) synapse didn't
        # correctly interpret those as of 0.99.2 and (b) they are a bit of a pain and we
        # may as well just do the filename* version.
        if _can_encode_filename_as_token(upload_name):
            disposition = "inline; filename=%s" % (upload_name,)
        else:
            disposition = "inline; filename*=utf-8''%s" % (_quote(upload_name),)

        request.setHeader(b"Content-Disposition", disposition.encode("ascii"))

    # cache for at least a day.
    # XXX: we might want to turn this off for data we don't want to
    # recommend caching as it's sensitive or private - or at least
    # select private. don't bother setting Expires as all our
    # clients are smart enough to be happy with Cache-Control
    request.setHeader(b"Cache-Control", b"public,max-age=86400,s-maxage=86400")
    if file_size is not None:
        request.setHeader(b"Content-Length", b"%d" % (file_size,))

    # Tell web crawlers to not index, archive, or follow links in media. This
    # should help to prevent things in the media repo from showing up in web
    # search results.
    request.setHeader(b"X-Robots-Tag", "noindex, nofollow, noarchive, noimageindex")


# separators as defined in RFC2616. SP and HT are handled separately.
# see _can_encode_filename_as_token.
_FILENAME_SEPARATOR_CHARS = {
    "(",
    ")",
    "<",
    ">",
    "@",
    ",",
    ";",
    ":",
    "\\",
    '"',
    "/",
    "[",
    "]",
    "?",
    "=",
    "{",
    "}",
}


def _can_encode_filename_as_token(x: str) -> bool:
    for c in x:
        # from RFC2616:
        #
        #        token          = 1*<any CHAR except CTLs or separators>
        #
        #        separators     = "(" | ")" | "<" | ">" | "@"
        #                       | "," | ";" | ":" | "\" | <">
        #                       | "/" | "[" | "]" | "?" | "="
        #                       | "{" | "}" | SP | HT
        #
        #        CHAR           = <any US-ASCII character (octets 0 - 127)>
        #
        #        CTL            = <any US-ASCII control character
        #                         (octets 0 - 31) and DEL (127)>
        #
        if ord(c) >= 127 or ord(c) <= 32 or c in _FILENAME_SEPARATOR_CHARS:
            return False
    return True


async def respond_with_responder(
    request: SynapseRequest,
    responder: "Optional[Responder]",
    media_type: str,
    file_size: Optional[int],
    upload_name: Optional[str] = None,
) -> None:
    """Responds to the request with given responder. If responder is None then
    returns 404.

    Args:
        request
        responder
        media_type: The media/content type.
        file_size: Size in bytes of the media. If not known it should be None
        upload_name: The name of the requested file, if any.
    """
    if request._disconnected:
        logger.warning(
            "Not sending response to request %s, already disconnected.", request
        )
        return

    if not responder:
        respond_404(request)
        return

    logger.debug("Responding to media request with responder %s", responder)
    add_file_headers(request, media_type, file_size, upload_name)
    try:
        with responder:
            await responder.write_to_consumer(request)
    except Exception as e:
        # The majority of the time this will be due to the client having gone
        # away. Unfortunately, Twisted simply throws a generic exception at us
        # in that case.
        logger.warning("Failed to write to consumer: %s %s", type(e), e)

        # Unregister the producer, if it has one, so Twisted doesn't complain
        if request.producer:
            request.unregisterProducer()

    finish_request(request)


class Responder:
    """Represents a response that can be streamed to the requester.

    Responder is a context manager which *must* be used, so that any resources
    held can be cleaned up.
    """

    def write_to_consumer(self, consumer: IConsumer) -> Awaitable:
        """Stream response into consumer

        Args:
            consumer: The consumer to stream into.

        Returns:
            Resolves once the response has finished being written
        """
        pass

    def __enter__(self) -> None:
        pass

    def __exit__(
        self,
        exc_type: Optional[Type[BaseException]],
        exc_val: Optional[BaseException],
        exc_tb: Optional[TracebackType],
    ) -> None:
        pass


@attr.s(slots=True, frozen=True, auto_attribs=True)
class ThumbnailInfo:
    """Details about a generated thumbnail."""

    width: int
    height: int
    method: str
    # Content type of thumbnail, e.g. image/png
    type: str
    # The size of the media file, in bytes.
    length: Optional[int] = None


@attr.s(slots=True, frozen=True, auto_attribs=True)
class FileInfo:
    """Details about a requested/uploaded file."""

    # The server name where the media originated from, or None if local.
    server_name: Optional[str]
    # The local ID of the file. For local files this is the same as the media_id
    file_id: str
    # If the file is for the url preview cache
    url_cache: bool = False
    # Whether the file is a thumbnail or not.
    thumbnail: Optional[ThumbnailInfo] = None

    # The below properties exist to maintain compatibility with third-party modules.
    @property
    def thumbnail_width(self) -> Optional[int]:
        if not self.thumbnail:
            return None
        return self.thumbnail.width

    @property
    def thumbnail_height(self) -> Optional[int]:
        if not self.thumbnail:
            return None
        return self.thumbnail.height

    @property
    def thumbnail_method(self) -> Optional[str]:
        if not self.thumbnail:
            return None
        return self.thumbnail.method

    @property
    def thumbnail_type(self) -> Optional[str]:
        if not self.thumbnail:
            return None
        return self.thumbnail.type

    @property
    def thumbnail_length(self) -> Optional[int]:
        if not self.thumbnail:
            return None
        return self.thumbnail.length


def get_filename_from_headers(headers: Dict[bytes, List[bytes]]) -> Optional[str]:
    """
    Get the filename of the downloaded file by inspecting the
    Content-Disposition HTTP header.

    Args:
        headers: The HTTP request headers.

    Returns:
        The filename, or None.
    """
    content_disposition = headers.get(b"Content-Disposition", [b""])

    # No header, bail out.
    if not content_disposition[0]:
        return None

    _, params = _parse_header(content_disposition[0])

    upload_name = None

    # First check if there is a valid UTF-8 filename
    upload_name_utf8 = params.get(b"filename*", None)
    if upload_name_utf8:
        if upload_name_utf8.lower().startswith(b"utf-8''"):
            upload_name_utf8 = upload_name_utf8[7:]
            # We have a filename*= section. This MUST be ASCII, and any UTF-8
            # bytes are %-quoted.
            try:
                # Once it is decoded, we can then unquote the %-encoded
                # parts strictly into a unicode string.
                upload_name = urllib.parse.unquote(
                    upload_name_utf8.decode("ascii"), errors="strict"
                )
            except UnicodeDecodeError:
                # Incorrect UTF-8.
                pass

    # If there isn't check for an ascii name.
    if not upload_name:
        upload_name_ascii = params.get(b"filename", None)
        if upload_name_ascii and is_ascii(upload_name_ascii):
            upload_name = upload_name_ascii.decode("ascii")

    # This may be None here, indicating we did not find a matching name.
    return upload_name


def _parse_header(line: bytes) -> Tuple[bytes, Dict[bytes, bytes]]:
    """Parse a Content-type like header.

    Cargo-culted from `cgi`, but works on bytes rather than strings.

    Args:
        line: header to be parsed

    Returns:
        The main content-type, followed by the parameter dictionary
    """
    parts = _parseparam(b";" + line)
    key = next(parts)
    pdict = {}
    for p in parts:
        i = p.find(b"=")
        if i >= 0:
            name = p[:i].strip().lower()
            value = p[i + 1 :].strip()

            # strip double-quotes
            if len(value) >= 2 and value[0:1] == value[-1:] == b'"':
                value = value[1:-1]
                value = value.replace(b"\\\\", b"\\").replace(b'\\"', b'"')
            pdict[name] = value

    return key, pdict


def _parseparam(s: bytes) -> Generator[bytes, None, None]:
    """Generator which splits the input on ;, respecting double-quoted sequences

    Cargo-culted from `cgi`, but works on bytes rather than strings.

    Args:
        s: header to be parsed

    Returns:
        The split input
    """
    while s[:1] == b";":
        s = s[1:]

        # look for the next ;
        end = s.find(b";")

        # if there is an odd number of " marks between here and the next ;, skip to the
        # next ; instead
        while end > 0 and (s.count(b'"', 0, end) - s.count(b'\\"', 0, end)) % 2:
            end = s.find(b";", end + 1)

        if end < 0:
            end = len(s)
        f = s[:end]
        yield f.strip()
        s = s[end:]
