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
from unittest.mock import Mock, call

from twisted.internet import defer, reactor

from synapse.logging.context import SENTINEL_CONTEXT, LoggingContext, current_context
from synapse.rest.client.transactions import CLEANUP_PERIOD_MS, HttpTransactionCache
from synapse.util import Clock

from tests import unittest
from tests.utils import MockClock


class HttpTransactionCacheTestCase(unittest.TestCase):
    def setUp(self):
        self.clock = MockClock()
        self.hs = Mock()
        self.hs.get_clock = Mock(return_value=self.clock)
        self.hs.get_auth = Mock()
        self.cache = HttpTransactionCache(self.hs)

        self.mock_http_response = (200, "GOOD JOB!")
        self.mock_key = "foo"

    @defer.inlineCallbacks
    def test_executes_given_function(self):
        cb = Mock(return_value=defer.succeed(self.mock_http_response))
        res = yield self.cache.fetch_or_execute(
            self.mock_key, cb, "some_arg", keyword="arg"
        )
        cb.assert_called_once_with("some_arg", keyword="arg")
        self.assertEqual(res, self.mock_http_response)

    @defer.inlineCallbacks
    def test_deduplicates_based_on_key(self):
        cb = Mock(return_value=defer.succeed(self.mock_http_response))
        for i in range(3):  # invoke multiple times
            res = yield self.cache.fetch_or_execute(
                self.mock_key, cb, "some_arg", keyword="arg", changing_args=i
            )
            self.assertEqual(res, self.mock_http_response)
        # expect only a single call to do the work
        cb.assert_called_once_with("some_arg", keyword="arg", changing_args=0)

    @defer.inlineCallbacks
    def test_logcontexts_with_async_result(self):
        @defer.inlineCallbacks
        def cb():
            yield Clock(reactor).sleep(0)
            return "yay"

        @defer.inlineCallbacks
        def test():
            with LoggingContext("c") as c1:
                res = yield self.cache.fetch_or_execute(self.mock_key, cb)
                self.assertIs(current_context(), c1)
                self.assertEqual(res, "yay")

        # run the test twice in parallel
        d = defer.gatherResults([test(), test()])
        self.assertIs(current_context(), SENTINEL_CONTEXT)
        yield d
        self.assertIs(current_context(), SENTINEL_CONTEXT)

    @defer.inlineCallbacks
    def test_does_not_cache_exceptions(self):
        """Checks that, if the callback throws an exception, it is called again
        for the next request.
        """
        called = [False]

        def cb():
            if called[0]:
                # return a valid result the second time
                return defer.succeed(self.mock_http_response)

            called[0] = True
            raise Exception("boo")

        with LoggingContext("test") as test_context:
            try:
                yield self.cache.fetch_or_execute(self.mock_key, cb)
            except Exception as e:
                self.assertEqual(e.args[0], "boo")
            self.assertIs(current_context(), test_context)

            res = yield self.cache.fetch_or_execute(self.mock_key, cb)
            self.assertEqual(res, self.mock_http_response)
            self.assertIs(current_context(), test_context)

    @defer.inlineCallbacks
    def test_does_not_cache_failures(self):
        """Checks that, if the callback returns a failure, it is called again
        for the next request.
        """
        called = [False]

        def cb():
            if called[0]:
                # return a valid result the second time
                return defer.succeed(self.mock_http_response)

            called[0] = True
            return defer.fail(Exception("boo"))

        with LoggingContext("test") as test_context:
            try:
                yield self.cache.fetch_or_execute(self.mock_key, cb)
            except Exception as e:
                self.assertEqual(e.args[0], "boo")
            self.assertIs(current_context(), test_context)

            res = yield self.cache.fetch_or_execute(self.mock_key, cb)
            self.assertEqual(res, self.mock_http_response)
            self.assertIs(current_context(), test_context)

    @defer.inlineCallbacks
    def test_cleans_up(self):
        cb = Mock(return_value=defer.succeed(self.mock_http_response))
        yield self.cache.fetch_or_execute(self.mock_key, cb, "an arg")
        # should NOT have cleaned up yet
        self.clock.advance_time_msec(CLEANUP_PERIOD_MS / 2)

        yield self.cache.fetch_or_execute(self.mock_key, cb, "an arg")
        # still using cache
        cb.assert_called_once_with("an arg")

        self.clock.advance_time_msec(CLEANUP_PERIOD_MS)

        yield self.cache.fetch_or_execute(self.mock_key, cb, "an arg")
        # no longer using cache
        self.assertEqual(cb.call_count, 2)
        self.assertEqual(cb.call_args_list, [call("an arg"), call("an arg")])
