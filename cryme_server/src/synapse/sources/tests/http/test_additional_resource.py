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
# Copyright 2018 New Vector Ltd
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


from synapse.http.additional_resource import AdditionalResource
from synapse.http.server import respond_with_json

from tests.server import FakeSite, make_request
from tests.unittest import HomeserverTestCase


class _AsyncTestCustomEndpoint:
    def __init__(self, config, module_api):
        pass

    async def handle_request(self, request):
        respond_with_json(request, 200, {"some_key": "some_value_async"})


class _SyncTestCustomEndpoint:
    def __init__(self, config, module_api):
        pass

    async def handle_request(self, request):
        respond_with_json(request, 200, {"some_key": "some_value_sync"})


class AdditionalResourceTests(HomeserverTestCase):
    """Very basic tests that `AdditionalResource` works correctly with sync
    and async handlers.
    """

    def test_async(self):
        handler = _AsyncTestCustomEndpoint({}, None).handle_request
        resource = AdditionalResource(self.hs, handler)

        channel = make_request(
            self.reactor, FakeSite(resource, self.reactor), "GET", "/"
        )

        self.assertEqual(channel.code, 200)
        self.assertEqual(channel.json_body, {"some_key": "some_value_async"})

    def test_sync(self):
        handler = _SyncTestCustomEndpoint({}, None).handle_request
        resource = AdditionalResource(self.hs, handler)

        channel = make_request(
            self.reactor, FakeSite(resource, self.reactor), "GET", "/"
        )

        self.assertEqual(channel.code, 200)
        self.assertEqual(channel.json_body, {"some_key": "some_value_sync"})
