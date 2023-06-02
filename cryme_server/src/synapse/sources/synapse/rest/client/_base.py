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

"""This module contains base REST classes for constructing client v1 servlets.
"""
import logging
import re
from typing import Any, Awaitable, Callable, Iterable, Pattern, Tuple, TypeVar, cast

from synapse.api.errors import InteractiveAuthIncompleteError
from synapse.api.urls import CLIENT_API_PREFIX
from synapse.types import JsonDict

logger = logging.getLogger(__name__)


def client_patterns(
    path_regex: str,
    releases: Iterable[str] = ("r0", "v3"),
    unstable: bool = True,
    v1: bool = False,
) -> Iterable[Pattern]:
    """Creates a regex compiled client path with the correct client path
    prefix.

    Args:
        path_regex: The regex string to match. This should NOT have a ^
            as this will be prefixed.
        releases: An iterable of releases to include this endpoint under.
        unstable: If true, include this endpoint under the "unstable" prefix.
        v1: If true, include this endpoint under the "api/v1" prefix.
    Returns:
        An iterable of patterns.
    """
    patterns = []

    if unstable:
        unstable_prefix = CLIENT_API_PREFIX + "/unstable"
        patterns.append(re.compile("^" + unstable_prefix + path_regex))
    if v1:
        v1_prefix = CLIENT_API_PREFIX + "/api/v1"
        patterns.append(re.compile("^" + v1_prefix + path_regex))
    for release in releases:
        new_prefix = CLIENT_API_PREFIX + f"/{release}"
        patterns.append(re.compile("^" + new_prefix + path_regex))

    return patterns


def set_timeline_upper_limit(filter_json: JsonDict, filter_timeline_limit: int) -> None:
    """
    Enforces a maximum limit of a timeline query.

    Params:
        filter_json: The timeline query to modify.
        filter_timeline_limit: The maximum limit to allow, passing -1 will
            disable enforcing a maximum limit.
    """
    if filter_timeline_limit < 0:
        return  # no upper limits
    timeline = filter_json.get("room", {}).get("timeline", {})
    if "limit" in timeline:
        filter_json["room"]["timeline"]["limit"] = min(
            filter_json["room"]["timeline"]["limit"], filter_timeline_limit
        )


C = TypeVar("C", bound=Callable[..., Awaitable[Tuple[int, JsonDict]]])


def interactive_auth_handler(orig: C) -> C:
    """Wraps an on_POST method to handle InteractiveAuthIncompleteErrors

    Takes a on_POST method which returns an Awaitable (errcode, body) response
    and adds exception handling to turn a InteractiveAuthIncompleteError into
    a 401 response.

    Normal usage is:

    @interactive_auth_handler
    async def on_POST(self, request):
        # ...
        await self.auth_handler.check_auth
    """

    async def wrapped(*args: Any, **kwargs: Any) -> Tuple[int, JsonDict]:
        try:
            return await orig(*args, **kwargs)
        except InteractiveAuthIncompleteError as e:
            return 401, e.result

    return cast(C, wrapped)
