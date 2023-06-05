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
import twisted.python.failure
from twisted.internet import defer, reactor

from synapse.logging.context import (
    SENTINEL_CONTEXT,
    LoggingContext,
    PreserveLoggingContext,
    current_context,
    make_deferred_yieldable,
    nested_logging_context,
    run_in_background,
)
from synapse.util import Clock

from .. import unittest


class LoggingContextTestCase(unittest.TestCase):
    def _check_test_key(self, value):
        self.assertEquals(current_context().name, value)

    def test_with_context(self):
        with LoggingContext("test"):
            self._check_test_key("test")

    @defer.inlineCallbacks
    def test_sleep(self):
        clock = Clock(reactor)

        @defer.inlineCallbacks
        def competing_callback():
            with LoggingContext("competing"):
                yield clock.sleep(0)
                self._check_test_key("competing")

        reactor.callLater(0, competing_callback)

        with LoggingContext("one"):
            yield clock.sleep(0)
            self._check_test_key("one")

    def _test_run_in_background(self, function):
        sentinel_context = current_context()

        callback_completed = [False]

        with LoggingContext("one"):
            # fire off function, but don't wait on it.
            d2 = run_in_background(function)

            def cb(res):
                callback_completed[0] = True
                return res

            d2.addCallback(cb)

            self._check_test_key("one")

        # now wait for the function under test to have run, and check that
        # the logcontext is left in a sane state.
        d2 = defer.Deferred()

        def check_logcontext():
            if not callback_completed[0]:
                reactor.callLater(0.01, check_logcontext)
                return

            # make sure that the context was reset before it got thrown back
            # into the reactor
            try:
                self.assertIs(current_context(), sentinel_context)
                d2.callback(None)
            except BaseException:
                d2.errback(twisted.python.failure.Failure())

        reactor.callLater(0.01, check_logcontext)

        # test is done once d2 finishes
        return d2

    def test_run_in_background_with_blocking_fn(self):
        @defer.inlineCallbacks
        def blocking_function():
            yield Clock(reactor).sleep(0)

        return self._test_run_in_background(blocking_function)

    def test_run_in_background_with_non_blocking_fn(self):
        @defer.inlineCallbacks
        def nonblocking_function():
            with PreserveLoggingContext():
                yield defer.succeed(None)

        return self._test_run_in_background(nonblocking_function)

    def test_run_in_background_with_chained_deferred(self):
        # a function which returns a deferred which looks like it has been
        # called, but is actually paused
        def testfunc():
            return make_deferred_yieldable(_chained_deferred_function())

        return self._test_run_in_background(testfunc)

    def test_run_in_background_with_coroutine(self):
        async def testfunc():
            self._check_test_key("one")
            d = Clock(reactor).sleep(0)
            self.assertIs(current_context(), SENTINEL_CONTEXT)
            await d
            self._check_test_key("one")

        return self._test_run_in_background(testfunc)

    def test_run_in_background_with_nonblocking_coroutine(self):
        async def testfunc():
            self._check_test_key("one")

        return self._test_run_in_background(testfunc)

    @defer.inlineCallbacks
    def test_make_deferred_yieldable(self):
        # a function which returns an incomplete deferred, but doesn't follow
        # the synapse rules.
        def blocking_function():
            d = defer.Deferred()
            reactor.callLater(0, d.callback, None)
            return d

        sentinel_context = current_context()

        with LoggingContext("one"):
            d1 = make_deferred_yieldable(blocking_function())
            # make sure that the context was reset by make_deferred_yieldable
            self.assertIs(current_context(), sentinel_context)

            yield d1

            # now it should be restored
            self._check_test_key("one")

    @defer.inlineCallbacks
    def test_make_deferred_yieldable_with_chained_deferreds(self):
        sentinel_context = current_context()

        with LoggingContext("one"):
            d1 = make_deferred_yieldable(_chained_deferred_function())
            # make sure that the context was reset by make_deferred_yieldable
            self.assertIs(current_context(), sentinel_context)

            yield d1

            # now it should be restored
            self._check_test_key("one")

    def test_nested_logging_context(self):
        with LoggingContext("foo"):
            nested_context = nested_logging_context(suffix="bar")
            self.assertEqual(nested_context.name, "foo-bar")


# a function which returns a deferred which has been "called", but
# which had a function which returned another incomplete deferred on
# its callback list, so won't yet call any other new callbacks.
def _chained_deferred_function():
    d = defer.succeed(None)

    def cb(res):
        d2 = defer.Deferred()
        reactor.callLater(0, d2.callback, res)
        return d2

    d.addCallback(cb)
    return d
