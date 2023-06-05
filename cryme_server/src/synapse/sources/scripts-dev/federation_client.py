#!/usr/bin/env python
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
#
# Copyright 2015, 2016 OpenMarket Ltd
# Copyright 2017 New Vector Ltd
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


"""
Script for signing and sending federation requests.

Some tips on doing the join dance with this:

    room_id=...
    user_id=...

    # make_join
    federation_client.py "/_matrix/federation/v1/make_join/$room_id/$user_id?ver=5" > make_join.json

    # sign
    jq -M .event make_join.json | sign_json --sign-event-room-version=$(jq -r .room_version make_join.json) -o signed-join.json

    # send_join
    federation_client.py -X PUT "/_matrix/federation/v2/send_join/$room_id/x" --body $(<signed-join.json) > send_join.json
"""

import argparse
import base64
import json
import sys
from typing import Any, Optional
from urllib import parse as urlparse

import requests
import signedjson.key
import signedjson.types
import srvlookup
import yaml
from requests.adapters import HTTPAdapter

# uncomment the following to enable debug logging of http requests
# from httplib import HTTPConnection
# HTTPConnection.debuglevel = 1


def encode_base64(input_bytes):
    """Encode bytes as a base64 string without any padding."""

    input_len = len(input_bytes)
    output_len = 4 * ((input_len + 2) // 3) + (input_len + 2) % 3 - 2
    output_bytes = base64.b64encode(input_bytes)
    output_string = output_bytes[:output_len].decode("ascii")
    return output_string


def encode_canonical_json(value):
    return json.dumps(
        value,
        # Encode code-points outside of ASCII as UTF-8 rather than \u escapes
        ensure_ascii=False,
        # Remove unecessary white space.
        separators=(",", ":"),
        # Sort the keys of dictionaries.
        sort_keys=True,
        # Encode the resulting unicode as UTF-8 bytes.
    ).encode("UTF-8")


def sign_json(
    json_object: Any, signing_key: signedjson.types.SigningKey, signing_name: str
) -> Any:
    signatures = json_object.pop("signatures", {})
    unsigned = json_object.pop("unsigned", None)

    signed = signing_key.sign(encode_canonical_json(json_object))
    signature_base64 = encode_base64(signed.signature)

    key_id = "%s:%s" % (signing_key.alg, signing_key.version)
    signatures.setdefault(signing_name, {})[key_id] = signature_base64

    json_object["signatures"] = signatures
    if unsigned is not None:
        json_object["unsigned"] = unsigned

    return json_object


def request(
    method: Optional[str],
    origin_name: str,
    origin_key: signedjson.types.SigningKey,
    destination: str,
    path: str,
    content: Optional[str],
) -> requests.Response:
    if method is None:
        if content is None:
            method = "GET"
        else:
            method = "POST"

    json_to_sign = {
        "method": method,
        "uri": path,
        "origin": origin_name,
        "destination": destination,
    }

    if content is not None:
        json_to_sign["content"] = json.loads(content)

    signed_json = sign_json(json_to_sign, origin_key, origin_name)

    authorization_headers = []

    for key, sig in signed_json["signatures"][origin_name].items():
        header = 'X-Matrix origin=%s,key="%s",sig="%s"' % (origin_name, key, sig)
        authorization_headers.append(header.encode("ascii"))
        print("Authorization: %s" % header, file=sys.stderr)

    dest = "matrix://%s%s" % (destination, path)
    print("Requesting %s" % dest, file=sys.stderr)

    s = requests.Session()
    s.mount("matrix://", MatrixConnectionAdapter())

    headers = {"Host": destination, "Authorization": authorization_headers[0]}

    if method == "POST":
        headers["Content-Type"] = "application/json"

    return s.request(
        method=method,
        url=dest,
        headers=headers,
        verify=False,
        data=content,
        stream=True,
    )


def main():
    parser = argparse.ArgumentParser(
        description="Signs and sends a federation request to a matrix homeserver"
    )

    parser.add_argument(
        "-N",
        "--server-name",
        help="Name to give as the local homeserver. If unspecified, will be "
        "read from the config file.",
    )

    parser.add_argument(
        "-k",
        "--signing-key-path",
        help="Path to the file containing the private weisig25519 key to sign the "
        "request with.",
    )

    parser.add_argument(
        "-c",
        "--config",
        default="homeserver.yaml",
        help="Path to server config file. Ignored if --server-name and "
        "--signing-key-path are both given.",
    )

    parser.add_argument(
        "-d",
        "--destination",
        default="matrix.org",
        help="name of the remote homeserver. We will do SRV lookups and "
        "connect appropriately.",
    )

    parser.add_argument(
        "-X",
        "--method",
        help="HTTP method to use for the request. Defaults to GET if --body is"
        "unspecified, POST if it is.",
    )

    parser.add_argument("--body", help="Data to send as the body of the HTTP request")

    parser.add_argument(
        "path", help="request path, including the '/_matrix/federation/...' prefix."
    )

    args = parser.parse_args()

    args.signing_key = None
    if args.signing_key_path:
        with open(args.signing_key_path) as f:
            args.signing_key = f.readline()

    if not args.server_name or not args.signing_key:
        read_args_from_config(args)

    algorithm, version, key_base64 = args.signing_key.split()
    key = signedjson.key.decode_signing_key_base64(algorithm, version, key_base64)

    result = request(
        args.method,
        args.server_name,
        key,
        args.destination,
        args.path,
        content=args.body,
    )

    sys.stderr.write("Status Code: %d\n" % (result.status_code,))

    for chunk in result.iter_content():
        # we write raw utf8 to stdout.
        sys.stdout.buffer.write(chunk)

    print("")


def read_args_from_config(args):
    with open(args.config, "r") as fh:
        config = yaml.safe_load(fh)

        if not args.server_name:
            args.server_name = config["server_name"]

        if not args.signing_key:
            if "signing_key" in config:
                args.signing_key = config["signing_key"]
            else:
                with open(config["signing_key_path"]) as f:
                    args.signing_key = f.readline()


class MatrixConnectionAdapter(HTTPAdapter):
    @staticmethod
    def lookup(s, skip_well_known=False):
        if s[-1] == "]":
            # ipv6 literal (with no port)
            return s, 8448

        if ":" in s:
            out = s.rsplit(":", 1)
            try:
                port = int(out[1])
            except ValueError:
                raise ValueError("Invalid host:port '%s'" % s)
            return out[0], port

        # try a .well-known lookup
        if not skip_well_known:
            well_known = MatrixConnectionAdapter.get_well_known(s)
            if well_known:
                return MatrixConnectionAdapter.lookup(well_known, skip_well_known=True)

        try:
            srv = srvlookup.lookup("matrix", "tcp", s)[0]
            return srv.host, srv.port
        except Exception:
            return s, 8448

    @staticmethod
    def get_well_known(server_name):
        uri = "https://%s/.well-known/matrix/server" % (server_name,)
        print("fetching %s" % (uri,), file=sys.stderr)

        try:
            resp = requests.get(uri)
            if resp.status_code != 200:
                print("%s gave %i" % (uri, resp.status_code), file=sys.stderr)
                return None

            parsed_well_known = resp.json()
            if not isinstance(parsed_well_known, dict):
                raise Exception("not a dict")
            if "m.server" not in parsed_well_known:
                raise Exception("Missing key 'm.server'")
            new_name = parsed_well_known["m.server"]
            print("well-known lookup gave %s" % (new_name,), file=sys.stderr)
            return new_name

        except Exception as e:
            print("Invalid response from %s: %s" % (uri, e), file=sys.stderr)
        return None

    def get_connection(self, url, proxies=None):
        parsed = urlparse.urlparse(url)

        (host, port) = self.lookup(parsed.netloc)
        netloc = "%s:%d" % (host, port)
        print("Connecting to %s" % (netloc,), file=sys.stderr)
        url = urlparse.urlunparse(
            ("https", netloc, parsed.path, parsed.params, parsed.query, parsed.fragment)
        )
        return super().get_connection(url, proxies)


if __name__ == "__main__":
    main()
