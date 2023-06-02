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
# Copyright 2019 Matrix.org Foundation C.I.C.
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

import resource
from unittest import mock

from synapse.app.phone_stats_home import phone_stats_home

from tests.unittest import HomeserverTestCase


class PhoneHomeStatsTestCase(HomeserverTestCase):
    def test_performance_frozen_clock(self):
        """
        If time doesn't move, don't error out.
        """
        past_stats = [
            (self.hs.get_clock().time(), resource.getrusage(resource.RUSAGE_SELF))
        ]
        stats = {}
        self.get_success(phone_stats_home(self.hs, stats, past_stats))
        self.assertEqual(stats["cpu_average"], 0)

    def test_performance_100(self):
        """
        1 second of usage over 1 second is 100% CPU usage.
        """
        real_res = resource.getrusage(resource.RUSAGE_SELF)
        old_resource = mock.Mock(spec=real_res)
        old_resource.ru_utime = real_res.ru_utime - 1
        old_resource.ru_stime = real_res.ru_stime
        old_resource.ru_maxrss = real_res.ru_maxrss

        past_stats = [(self.hs.get_clock().time(), old_resource)]
        stats = {}
        self.reactor.advance(1)
        self.get_success(phone_stats_home(self.hs, stats, past_stats))
        self.assertApproximates(stats["cpu_average"], 100, tolerance=2.5)
