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
import logging
import time
from logging import Handler, LogRecord
from logging.handlers import MemoryHandler
from threading import Thread
from typing import Optional, cast

from twisted.internet.interfaces import IReactorCore


class PeriodicallyFlushingMemoryHandler(MemoryHandler):
    """
    This is a subclass of MemoryHandler that additionally spawns a background
    thread to periodically flush the buffer.

    This prevents messages from being buffered for too long.

    Additionally, all messages will be immediately flushed if the reactor has
    not yet been started.
    """

    def __init__(
        self,
        capacity: int,
        flushLevel: int = logging.ERROR,
        target: Optional[Handler] = None,
        flushOnClose: bool = True,
        period: float = 5.0,
        reactor: Optional[IReactorCore] = None,
    ) -> None:
        """
        period: the period between automatic flushes

        reactor: if specified, a custom reactor to use. If not specifies,
            defaults to the globally-installed reactor.
            Log entries will be flushed immediately until this reactor has
            started.
        """
        super().__init__(capacity, flushLevel, target, flushOnClose)

        self._flush_period: float = period
        self._active: bool = True
        self._reactor_started = False

        self._flushing_thread: Thread = Thread(
            name="PeriodicallyFlushingMemoryHandler flushing thread",
            target=self._flush_periodically,
            daemon=True,
        )
        self._flushing_thread.start()

        def on_reactor_running():
            self._reactor_started = True

        reactor_to_use: IReactorCore
        if reactor is None:
            from twisted.internet import reactor as global_reactor

            reactor_to_use = cast(IReactorCore, global_reactor)
        else:
            reactor_to_use = reactor

        # call our hook when the reactor start up
        reactor_to_use.callWhenRunning(on_reactor_running)

    def shouldFlush(self, record: LogRecord) -> bool:
        """
        Before reactor start-up, log everything immediately.
        Otherwise, fall back to original behaviour of waiting for the buffer to fill.
        """

        if self._reactor_started:
            return super().shouldFlush(record)
        else:
            return True

    def _flush_periodically(self):
        """
        Whilst this handler is active, flush the handler periodically.
        """

        while self._active:
            # flush is thread-safe; it acquires and releases the lock internally
            self.flush()
            time.sleep(self._flush_period)

    def close(self) -> None:
        self._active = False
        super().close()
