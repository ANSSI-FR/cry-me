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
import sys
from typing import Container

from synapse import python_dependencies  # noqa: E402

logger = logging.getLogger(__name__)

try:
    python_dependencies.check_requirements()
except python_dependencies.DependencyException as e:
    sys.stderr.writelines(
        e.message  # noqa: B306, DependencyException.message is a property
    )
    sys.exit(1)


def check_bind_error(
    e: Exception, address: str, bind_addresses: Container[str]
) -> None:
    """
    This method checks an exception occurred while binding on 0.0.0.0.
    If :: is specified in the bind addresses a warning is shown.
    The exception is still raised otherwise.

    Binding on both 0.0.0.0 and :: causes an exception on Linux and macOS
    because :: binds on both IPv4 and IPv6 (as per RFC 3493).
    When binding on 0.0.0.0 after :: this can safely be ignored.

    Args:
        e: Exception that was caught.
        address: Address on which binding was attempted.
        bind_addresses: Addresses on which the service listens.
    """
    if address == "0.0.0.0" and "::" in bind_addresses:
        logger.warning(
            "Failed to listen on 0.0.0.0, continuing because listening on [::]"
        )
    else:
        raise e
