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
# Copyright 2015, 2016 OpenMarket Ltd
# Copyright 2021 The Matrix.org Foundation C.I.C.
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
import sys
from typing import List

from synapse.config._base import ConfigError
from synapse.config.homeserver import HomeServerConfig


def main(args: List[str]) -> None:
    action = args[1] if len(args) > 1 and args[1] == "read" else None
    # If we're reading a key in the config file, then `args[1]` will be `read`  and `args[2]`
    # will be the key to read.
    # We'll want to rework this code if we want to support more actions than just `read`.
    load_config_args = args[3:] if action else args[1:]

    try:
        config = HomeServerConfig.load_config("", load_config_args)
    except ConfigError as e:
        sys.stderr.write("\n" + str(e) + "\n")
        sys.exit(1)

    print("Config parses OK!")

    if action == "read":
        key = args[2]
        key_parts = key.split(".")

        value = config
        try:
            while len(key_parts):
                value = getattr(value, key_parts[0])
                key_parts.pop(0)

            print(f"\n{key}: {value}")
        except AttributeError:
            print(
                f"\nNo '{key}' key could be found in the provided configuration file."
            )
            sys.exit(1)


if __name__ == "__main__":
    main(sys.argv)
