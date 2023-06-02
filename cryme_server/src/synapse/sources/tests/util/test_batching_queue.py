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
from twisted.internet import defer

from synapse.logging.context import make_deferred_yieldable
from synapse.util.batching_queue import (
    BatchingQueue,
    number_in_flight,
    number_of_keys,
    number_queued,
)

from tests.server import get_clock
from tests.unittest import TestCase


class BatchingQueueTestCase(TestCase):
    def setUp(self):
        self.clock, hs_clock = get_clock()

        # We ensure that we remove any existing metrics for "test_queue".
        try:
            number_queued.remove("test_queue")
            number_of_keys.remove("test_queue")
            number_in_flight.remove("test_queue")
        except KeyError:
            pass

        self._pending_calls = []
        self.queue = BatchingQueue("test_queue", hs_clock, self._process_queue)

    async def _process_queue(self, values):
        d = defer.Deferred()
        self._pending_calls.append((values, d))
        return await make_deferred_yieldable(d)

    def _get_sample_with_name(self, metric, name) -> int:
        """For a prometheus metric get the value of the sample that has a
        matching "name" label.
        """
        for sample in metric.collect()[0].samples:
            if sample.labels.get("name") == name:
                return sample.value

        self.fail("Found no matching sample")

    def _assert_metrics(self, queued, keys, in_flight):
        """Assert that the metrics are correct"""

        sample = self._get_sample_with_name(number_queued, self.queue._name)
        self.assertEqual(
            sample,
            queued,
            "number_queued",
        )

        sample = self._get_sample_with_name(number_of_keys, self.queue._name)
        self.assertEqual(sample, keys, "number_of_keys")

        sample = self._get_sample_with_name(number_in_flight, self.queue._name)
        self.assertEqual(
            sample,
            in_flight,
            "number_in_flight",
        )

    def test_simple(self):
        """Tests the basic case of calling `add_to_queue` once and having
        `_process_queue` return.
        """

        self.assertFalse(self._pending_calls)

        queue_d = defer.ensureDeferred(self.queue.add_to_queue("foo"))

        self._assert_metrics(queued=1, keys=1, in_flight=1)

        # The queue should wait a reactor tick before calling the processing
        # function.
        self.assertFalse(self._pending_calls)
        self.assertFalse(queue_d.called)

        # We should see a call to `_process_queue` after a reactor tick.
        self.clock.pump([0])

        self.assertEqual(len(self._pending_calls), 1)
        self.assertEqual(self._pending_calls[0][0], ["foo"])
        self.assertFalse(queue_d.called)
        self._assert_metrics(queued=0, keys=0, in_flight=1)

        # Return value of the `_process_queue` should be propagated back.
        self._pending_calls.pop()[1].callback("bar")

        self.assertEqual(self.successResultOf(queue_d), "bar")

        self._assert_metrics(queued=0, keys=0, in_flight=0)

    def test_batching(self):
        """Test that multiple calls at the same time get batched up into one
        call to `_process_queue`.
        """

        self.assertFalse(self._pending_calls)

        queue_d1 = defer.ensureDeferred(self.queue.add_to_queue("foo1"))
        queue_d2 = defer.ensureDeferred(self.queue.add_to_queue("foo2"))

        self._assert_metrics(queued=2, keys=1, in_flight=2)

        self.clock.pump([0])

        # We should see only *one* call to `_process_queue`
        self.assertEqual(len(self._pending_calls), 1)
        self.assertEqual(self._pending_calls[0][0], ["foo1", "foo2"])
        self.assertFalse(queue_d1.called)
        self.assertFalse(queue_d2.called)
        self._assert_metrics(queued=0, keys=0, in_flight=2)

        # Return value of the `_process_queue` should be propagated back to both.
        self._pending_calls.pop()[1].callback("bar")

        self.assertEqual(self.successResultOf(queue_d1), "bar")
        self.assertEqual(self.successResultOf(queue_d2), "bar")
        self._assert_metrics(queued=0, keys=0, in_flight=0)

    def test_queuing(self):
        """Test that we queue up requests while a `_process_queue` is being
        called.
        """

        self.assertFalse(self._pending_calls)

        queue_d1 = defer.ensureDeferred(self.queue.add_to_queue("foo1"))
        self.clock.pump([0])

        self.assertEqual(len(self._pending_calls), 1)

        # We queue up work after the process function has been called, testing
        # that they get correctly queued up.
        queue_d2 = defer.ensureDeferred(self.queue.add_to_queue("foo2"))
        queue_d3 = defer.ensureDeferred(self.queue.add_to_queue("foo3"))

        # We should see only *one* call to `_process_queue`
        self.assertEqual(len(self._pending_calls), 1)
        self.assertEqual(self._pending_calls[0][0], ["foo1"])
        self.assertFalse(queue_d1.called)
        self.assertFalse(queue_d2.called)
        self.assertFalse(queue_d3.called)
        self._assert_metrics(queued=2, keys=1, in_flight=3)

        # Return value of the `_process_queue` should be propagated back to the
        # first.
        self._pending_calls.pop()[1].callback("bar1")

        self.assertEqual(self.successResultOf(queue_d1), "bar1")
        self.assertFalse(queue_d2.called)
        self.assertFalse(queue_d3.called)
        self._assert_metrics(queued=2, keys=1, in_flight=2)

        # We should now see a second call to `_process_queue`
        self.clock.pump([0])
        self.assertEqual(len(self._pending_calls), 1)
        self.assertEqual(self._pending_calls[0][0], ["foo2", "foo3"])
        self.assertFalse(queue_d2.called)
        self.assertFalse(queue_d3.called)
        self._assert_metrics(queued=0, keys=0, in_flight=2)

        # Return value of the `_process_queue` should be propagated back to the
        # second.
        self._pending_calls.pop()[1].callback("bar2")

        self.assertEqual(self.successResultOf(queue_d2), "bar2")
        self.assertEqual(self.successResultOf(queue_d3), "bar2")
        self._assert_metrics(queued=0, keys=0, in_flight=0)

    def test_different_keys(self):
        """Test that calls to different keys get processed in parallel."""

        self.assertFalse(self._pending_calls)

        queue_d1 = defer.ensureDeferred(self.queue.add_to_queue("foo1", key=1))
        self.clock.pump([0])
        queue_d2 = defer.ensureDeferred(self.queue.add_to_queue("foo2", key=2))
        self.clock.pump([0])

        # We queue up another item with key=2 to check that we will keep taking
        # things off the queue.
        queue_d3 = defer.ensureDeferred(self.queue.add_to_queue("foo3", key=2))

        # We should see two calls to `_process_queue`
        self.assertEqual(len(self._pending_calls), 2)
        self.assertEqual(self._pending_calls[0][0], ["foo1"])
        self.assertEqual(self._pending_calls[1][0], ["foo2"])
        self.assertFalse(queue_d1.called)
        self.assertFalse(queue_d2.called)
        self.assertFalse(queue_d3.called)
        self._assert_metrics(queued=1, keys=1, in_flight=3)

        # Return value of the `_process_queue` should be propagated back to the
        # first.
        self._pending_calls.pop(0)[1].callback("bar1")

        self.assertEqual(self.successResultOf(queue_d1), "bar1")
        self.assertFalse(queue_d2.called)
        self.assertFalse(queue_d3.called)
        self._assert_metrics(queued=1, keys=1, in_flight=2)

        # Return value of the `_process_queue` should be propagated back to the
        # second.
        self._pending_calls.pop()[1].callback("bar2")

        self.assertEqual(self.successResultOf(queue_d2), "bar2")
        self.assertFalse(queue_d3.called)

        # We should now see a call `_pending_calls` for `foo3`
        self.clock.pump([0])
        self.assertEqual(len(self._pending_calls), 1)
        self.assertEqual(self._pending_calls[0][0], ["foo3"])
        self.assertFalse(queue_d3.called)
        self._assert_metrics(queued=0, keys=0, in_flight=1)

        # Return value of the `_process_queue` should be propagated back to the
        # third deferred.
        self._pending_calls.pop()[1].callback("bar4")

        self.assertEqual(self.successResultOf(queue_d3), "bar4")
        self._assert_metrics(queued=0, keys=0, in_flight=0)
