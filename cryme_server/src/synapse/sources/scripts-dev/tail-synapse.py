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
import collections
import json
import sys
import time

import requests

Entry = collections.namedtuple("Entry", "name position rows")

ROW_TYPES = {}


def row_type_for_columns(name, column_names):
    column_names = tuple(column_names)
    row_type = ROW_TYPES.get((name, column_names))
    if row_type is None:
        row_type = collections.namedtuple(name, column_names)
        ROW_TYPES[(name, column_names)] = row_type
    return row_type


def parse_response(content):
    streams = json.loads(content)
    result = {}
    for name, value in streams.items():
        row_type = row_type_for_columns(name, value["field_names"])
        position = value["position"]
        rows = [row_type(*row) for row in value["rows"]]
        result[name] = Entry(name, position, rows)
    return result


def replicate(server, streams):
    return parse_response(
        requests.get(
            server + "/_synapse/replication", verify=False, params=streams
        ).content
    )


def main():
    server = sys.argv[1]

    streams = None
    while not streams:
        try:
            streams = {
                row.name: row.position
                for row in replicate(server, {"streams": "-1"})["streams"].rows
            }
        except requests.exceptions.ConnectionError:
            time.sleep(0.1)

    while True:
        try:
            results = replicate(server, streams)
        except Exception:
            sys.stdout.write("connection_lost(" + repr(streams) + ")\n")
            break
        for update in results.values():
            for row in update.rows:
                sys.stdout.write(repr(row) + "\n")
            streams[update.name] = update.position


if __name__ == "__main__":
    main()
