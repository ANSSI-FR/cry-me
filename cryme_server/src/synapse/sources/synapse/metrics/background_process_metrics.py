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

import logging
import threading
from functools import wraps
from types import TracebackType
from typing import (
    TYPE_CHECKING,
    Any,
    Awaitable,
    Callable,
    Dict,
    Iterable,
    Optional,
    Set,
    Type,
    TypeVar,
    Union,
    cast,
)

from prometheus_client import Metric
from prometheus_client.core import REGISTRY, Counter, Gauge

from twisted.internet import defer

from synapse.logging.context import (
    ContextResourceUsage,
    LoggingContext,
    PreserveLoggingContext,
)
from synapse.logging.opentracing import (
    SynapseTags,
    noop_context_manager,
    start_active_span,
)

if TYPE_CHECKING:
    import resource


logger = logging.getLogger(__name__)


_background_process_start_count = Counter(
    "synapse_background_process_start_count",
    "Number of background processes started",
    ["name"],
)

_background_process_in_flight_count = Gauge(
    "synapse_background_process_in_flight_count",
    "Number of background processes in flight",
    labelnames=["name"],
)

# we set registry=None in all of these to stop them getting registered with
# the default registry. Instead we collect them all via the CustomCollector,
# which ensures that we can update them before they are collected.
#
_background_process_ru_utime = Counter(
    "synapse_background_process_ru_utime_seconds",
    "User CPU time used by background processes, in seconds",
    ["name"],
    registry=None,
)

_background_process_ru_stime = Counter(
    "synapse_background_process_ru_stime_seconds",
    "System CPU time used by background processes, in seconds",
    ["name"],
    registry=None,
)

_background_process_db_txn_count = Counter(
    "synapse_background_process_db_txn_count",
    "Number of database transactions done by background processes",
    ["name"],
    registry=None,
)

_background_process_db_txn_duration = Counter(
    "synapse_background_process_db_txn_duration_seconds",
    (
        "Seconds spent by background processes waiting for database "
        "transactions, excluding scheduling time"
    ),
    ["name"],
    registry=None,
)

_background_process_db_sched_duration = Counter(
    "synapse_background_process_db_sched_duration_seconds",
    "Seconds spent by background processes waiting for database connections",
    ["name"],
    registry=None,
)

# map from description to a counter, so that we can name our logcontexts
# incrementally. (It actually duplicates _background_process_start_count, but
# it's much simpler to do so than to try to combine them.)
_background_process_counts: Dict[str, int] = {}

# Set of all running background processes that became active active since the
# last time metrics were scraped (i.e. background processes that performed some
# work since the last scrape.)
#
# We do it like this to handle the case where we have a large number of
# background processes stacking up behind a lock or linearizer, where we then
# only need to iterate over and update metrics for the process that have
# actually been active and can ignore the idle ones.
_background_processes_active_since_last_scrape: "Set[_BackgroundProcess]" = set()

# A lock that covers the above set and dict
_bg_metrics_lock = threading.Lock()


class _Collector:
    """A custom metrics collector for the background process metrics.

    Ensures that all of the metrics are up-to-date with any in-flight processes
    before they are returned.
    """

    def collect(self) -> Iterable[Metric]:
        global _background_processes_active_since_last_scrape

        # We swap out the _background_processes set with an empty one so that
        # we can safely iterate over the set without holding the lock.
        with _bg_metrics_lock:
            _background_processes_copy = _background_processes_active_since_last_scrape
            _background_processes_active_since_last_scrape = set()

        for process in _background_processes_copy:
            process.update_metrics()

        # now we need to run collect() over each of the static Counters, and
        # yield each metric they return.
        for m in (
            _background_process_ru_utime,
            _background_process_ru_stime,
            _background_process_db_txn_count,
            _background_process_db_txn_duration,
            _background_process_db_sched_duration,
        ):
            yield from m.collect()


REGISTRY.register(_Collector())


class _BackgroundProcess:
    def __init__(self, desc: str, ctx: LoggingContext):
        self.desc = desc
        self._context = ctx
        self._reported_stats: Optional[ContextResourceUsage] = None

    def update_metrics(self) -> None:
        """Updates the metrics with values from this process."""
        new_stats = self._context.get_resource_usage()
        if self._reported_stats is None:
            diff = new_stats
        else:
            diff = new_stats - self._reported_stats
        self._reported_stats = new_stats

        _background_process_ru_utime.labels(self.desc).inc(diff.ru_utime)
        _background_process_ru_stime.labels(self.desc).inc(diff.ru_stime)
        _background_process_db_txn_count.labels(self.desc).inc(diff.db_txn_count)
        _background_process_db_txn_duration.labels(self.desc).inc(
            diff.db_txn_duration_sec
        )
        _background_process_db_sched_duration.labels(self.desc).inc(
            diff.db_sched_duration_sec
        )


R = TypeVar("R")


def run_as_background_process(
    desc: str,
    func: Callable[..., Awaitable[Optional[R]]],
    *args: Any,
    bg_start_span: bool = True,
    **kwargs: Any,
) -> "defer.Deferred[Optional[R]]":
    """Run the given function in its own logcontext, with resource metrics

    This should be used to wrap processes which are fired off to run in the
    background, instead of being associated with a particular request.

    It returns a Deferred which completes when the function completes, but it doesn't
    follow the synapse logcontext rules, which makes it appropriate for passing to
    clock.looping_call and friends (or for firing-and-forgetting in the middle of a
    normal synapse async function).

    Args:
        desc: a description for this background process type
        func: a function, which may return a Deferred or a coroutine
        bg_start_span: Whether to start an opentracing span. Defaults to True.
            Should only be disabled for processes that will not log to or tag
            a span.
        args: positional args for func
        kwargs: keyword args for func

    Returns:
        Deferred which returns the result of func, or `None` if func raises.
        Note that the returned Deferred does not follow the synapse logcontext
        rules.
    """

    async def run() -> Optional[R]:
        with _bg_metrics_lock:
            count = _background_process_counts.get(desc, 0)
            _background_process_counts[desc] = count + 1

        _background_process_start_count.labels(desc).inc()
        _background_process_in_flight_count.labels(desc).inc()

        with BackgroundProcessLoggingContext(desc, count) as context:
            try:
                if bg_start_span:
                    ctx = start_active_span(
                        f"bgproc.{desc}", tags={SynapseTags.REQUEST_ID: str(context)}
                    )
                else:
                    ctx = noop_context_manager()
                with ctx:
                    return await func(*args, **kwargs)
            except Exception:
                logger.exception(
                    "Background process '%s' threw an exception",
                    desc,
                )
                return None
            finally:
                _background_process_in_flight_count.labels(desc).dec()

    with PreserveLoggingContext():
        # Note that we return a Deferred here so that it can be used in a
        # looping_call and other places that expect a Deferred.
        return defer.ensureDeferred(run())


F = TypeVar("F", bound=Callable[..., Awaitable[Optional[Any]]])


def wrap_as_background_process(desc: str) -> Callable[[F], F]:
    """Decorator that wraps a function that gets called as a background
    process.

    Equivalent to calling the function with `run_as_background_process`
    """

    def wrap_as_background_process_inner(func: F) -> F:
        @wraps(func)
        def wrap_as_background_process_inner_2(
            *args: Any, **kwargs: Any
        ) -> "defer.Deferred[Optional[R]]":
            return run_as_background_process(desc, func, *args, **kwargs)

        return cast(F, wrap_as_background_process_inner_2)

    return wrap_as_background_process_inner


class BackgroundProcessLoggingContext(LoggingContext):
    """A logging context that tracks in flight metrics for background
    processes.
    """

    __slots__ = ["_proc"]

    def __init__(self, name: str, instance_id: Optional[Union[int, str]] = None):
        """

        Args:
            name: The name of the background process. Each distinct `name` gets a
                separate prometheus time series.

            instance_id: an identifer to add to `name` to distinguish this instance of
                the named background process in the logs. If this is `None`, one is
                made up based on id(self).
        """
        if instance_id is None:
            instance_id = id(self)
        super().__init__("%s-%s" % (name, instance_id))
        self._proc = _BackgroundProcess(name, self)

    def start(self, rusage: "Optional[resource.struct_rusage]") -> None:
        """Log context has started running (again)."""

        super().start(rusage)

        # We've become active again so we make sure we're in the list of active
        # procs. (Note that "start" here means we've become active, as opposed
        # to starting for the first time.)
        with _bg_metrics_lock:
            _background_processes_active_since_last_scrape.add(self._proc)

    def __exit__(
        self,
        type: Optional[Type[BaseException]],
        value: Optional[BaseException],
        traceback: Optional[TracebackType],
    ) -> None:
        """Log context has finished."""

        super().__exit__(type, value, traceback)

        # The background process has finished. We explicitly remove and manually
        # update the metrics here so that if nothing is scraping metrics the set
        # doesn't infinitely grow.
        with _bg_metrics_lock:
            _background_processes_active_since_last_scrape.discard(self._proc)

        self._proc.update_metrics()
