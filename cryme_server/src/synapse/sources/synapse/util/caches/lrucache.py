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
import weakref
from enum import Enum
from functools import wraps
from typing import (
    TYPE_CHECKING,
    Any,
    Callable,
    Collection,
    Dict,
    Generic,
    List,
    Optional,
    Type,
    TypeVar,
    Union,
    cast,
    overload,
)

from typing_extensions import Literal

from twisted.internet import reactor
from twisted.internet.interfaces import IReactorTime

from synapse.config import cache as cache_config
from synapse.metrics.background_process_metrics import wrap_as_background_process
from synapse.util import Clock, caches
from synapse.util.caches import CacheMetric, EvictionReason, register_cache
from synapse.util.caches.treecache import TreeCache, iterate_tree_cache_entry
from synapse.util.linked_list import ListNode

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)

try:
    from pympler.asizeof import Asizer

    def _get_size_of(val: Any, *, recurse: bool = True) -> int:
        """Get an estimate of the size in bytes of the object.

        Args:
            val: The object to size.
            recurse: If true will include referenced values in the size,
                otherwise only sizes the given object.
        """
        # Ignore singleton values when calculating memory usage.
        if val in ((), None, ""):
            return 0

        sizer = Asizer()
        sizer.exclude_refs((), None, "")
        return sizer.asizeof(val, limit=100 if recurse else 0)

except ImportError:

    def _get_size_of(val: Any, *, recurse: bool = True) -> int:
        return 0


# Function type: the type used for invalidation callbacks
FT = TypeVar("FT", bound=Callable[..., Any])

# Key and Value type for the cache
KT = TypeVar("KT")
VT = TypeVar("VT")

# a general type var, distinct from either KT or VT
T = TypeVar("T")

P = TypeVar("P")


class _TimedListNode(ListNode[P]):
    """A `ListNode` that tracks last access time."""

    __slots__ = ["last_access_ts_secs"]

    def update_last_access(self, clock: Clock) -> None:
        self.last_access_ts_secs = int(clock.time())


# Whether to insert new cache entries to the global list. We only add to it if
# time based eviction is enabled.
USE_GLOBAL_LIST = False

# A linked list of all cache entries, allowing efficient time based eviction.
GLOBAL_ROOT = ListNode["_Node"].create_root_node()


@wrap_as_background_process("LruCache._expire_old_entries")
async def _expire_old_entries(clock: Clock, expiry_seconds: int) -> None:
    """Walks the global cache list to find cache entries that haven't been
    accessed in the given number of seconds.
    """

    now = int(clock.time())
    node = GLOBAL_ROOT.prev_node
    assert node is not None

    i = 0

    logger.debug("Searching for stale caches")

    while node is not GLOBAL_ROOT:
        # Only the root node isn't a `_TimedListNode`.
        assert isinstance(node, _TimedListNode)

        if node.last_access_ts_secs > now - expiry_seconds:
            break

        cache_entry = node.get_cache_entry()
        next_node = node.prev_node

        # The node should always have a reference to a cache entry and a valid
        # `prev_node`, as we only drop them when we remove the node from the
        # list.
        assert next_node is not None
        assert cache_entry is not None
        cache_entry.drop_from_cache()

        # If we do lots of work at once we yield to allow other stuff to happen.
        if (i + 1) % 10000 == 0:
            logger.debug("Waiting during drop")
            await clock.sleep(0)
            logger.debug("Waking during drop")

        node = next_node

        # If we've yielded then our current node may have been evicted, so we
        # need to check that its still valid.
        if node.prev_node is None:
            break

        i += 1

    logger.info("Dropped %d items from caches", i)


def setup_expire_lru_cache_entries(hs: "HomeServer") -> None:
    """Start a background job that expires all cache entries if they have not
    been accessed for the given number of seconds.
    """
    if not hs.config.caches.expiry_time_msec:
        return

    logger.info(
        "Expiring LRU caches after %d seconds", hs.config.caches.expiry_time_msec / 1000
    )

    global USE_GLOBAL_LIST
    USE_GLOBAL_LIST = True

    clock = hs.get_clock()
    clock.looping_call(
        _expire_old_entries, 30 * 1000, clock, hs.config.caches.expiry_time_msec / 1000
    )


class _Node(Generic[KT, VT]):
    __slots__ = [
        "_list_node",
        "_global_list_node",
        "_cache",
        "key",
        "value",
        "callbacks",
        "memory",
    ]

    def __init__(
        self,
        root: "ListNode[_Node]",
        key: KT,
        value: VT,
        cache: "weakref.ReferenceType[LruCache[KT, VT]]",
        clock: Clock,
        callbacks: Collection[Callable[[], None]] = (),
        prune_unread_entries: bool = True,
    ):
        self._list_node = ListNode.insert_after(self, root)
        self._global_list_node: Optional[_TimedListNode] = None
        if USE_GLOBAL_LIST and prune_unread_entries:
            self._global_list_node = _TimedListNode.insert_after(self, GLOBAL_ROOT)
            self._global_list_node.update_last_access(clock)

        # We store a weak reference to the cache object so that this _Node can
        # remove itself from the cache. If the cache is dropped we ensure we
        # remove our entries in the lists.
        self._cache = cache

        self.key = key
        self.value = value

        # Set of callbacks to run when the node gets deleted. We store as a list
        # rather than a set to keep memory usage down (and since we expect few
        # entries per node, the performance of checking for duplication in a
        # list vs using a set is negligible).
        #
        # Note that we store this as an optional list to keep the memory
        # footprint down. Storing `None` is free as its a singleton, while empty
        # lists are 56 bytes (and empty sets are 216 bytes, if we did the naive
        # thing and used sets).
        self.callbacks: Optional[List[Callable[[], None]]] = None

        self.add_callbacks(callbacks)

        self.memory = 0
        if caches.TRACK_MEMORY_USAGE:
            self.memory = (
                _get_size_of(key)
                + _get_size_of(value)
                + _get_size_of(self._list_node, recurse=False)
                + _get_size_of(self.callbacks, recurse=False)
                + _get_size_of(self, recurse=False)
            )
            self.memory += _get_size_of(self.memory, recurse=False)

            if self._global_list_node:
                self.memory += _get_size_of(self._global_list_node, recurse=False)
                self.memory += _get_size_of(self._global_list_node.last_access_ts_secs)

    def add_callbacks(self, callbacks: Collection[Callable[[], None]]) -> None:
        """Add to stored list of callbacks, removing duplicates."""

        if not callbacks:
            return

        if not self.callbacks:
            self.callbacks = []

        for callback in callbacks:
            if callback not in self.callbacks:
                self.callbacks.append(callback)

    def run_and_clear_callbacks(self) -> None:
        """Run all callbacks and clear the stored list of callbacks. Used when
        the node is being deleted.
        """

        if not self.callbacks:
            return

        for callback in self.callbacks:
            callback()

        self.callbacks = None

    def drop_from_cache(self) -> None:
        """Drop this node from the cache.

        Ensures that the entry gets removed from the cache and that we get
        removed from all lists.
        """
        cache = self._cache()
        if (
            cache is None
            or cache.pop(self.key, _Sentinel.sentinel) is _Sentinel.sentinel
        ):
            # `cache.pop` should call `drop_from_lists()`, unless this Node had
            # already been removed from the cache.
            self.drop_from_lists()

    def drop_from_lists(self) -> None:
        """Remove this node from the cache lists."""
        self._list_node.remove_from_list()

        if self._global_list_node:
            self._global_list_node.remove_from_list()

    def move_to_front(self, clock: Clock, cache_list_root: ListNode) -> None:
        """Moves this node to the front of all the lists its in."""
        self._list_node.move_after(cache_list_root)
        if self._global_list_node:
            self._global_list_node.move_after(GLOBAL_ROOT)
            self._global_list_node.update_last_access(clock)


class _Sentinel(Enum):
    # defining a sentinel in this way allows mypy to correctly handle the
    # type of a dictionary lookup.
    sentinel = object()


class LruCache(Generic[KT, VT]):
    """
    Least-recently-used cache, supporting prometheus metrics and invalidation callbacks.

    If cache_type=TreeCache, all keys must be tuples.
    """

    def __init__(
        self,
        max_size: int,
        cache_name: Optional[str] = None,
        cache_type: Type[Union[dict, TreeCache]] = dict,
        size_callback: Optional[Callable[[VT], int]] = None,
        metrics_collection_callback: Optional[Callable[[], None]] = None,
        apply_cache_factor_from_config: bool = True,
        clock: Optional[Clock] = None,
        prune_unread_entries: bool = True,
    ):
        """
        Args:
            max_size: The maximum amount of entries the cache can hold

            cache_name: The name of this cache, for the prometheus metrics. If unset,
                no metrics will be reported on this cache.

            cache_type (type):
                type of underlying cache to be used. Typically one of dict
                or TreeCache.

            size_callback (func(V) -> int | None):

            metrics_collection_callback:
                metrics collection callback. This is called early in the metrics
                collection process, before any of the metrics registered with the
                prometheus Registry are collected, so can be used to update any dynamic
                metrics.

                Ignored if cache_name is None.

            apply_cache_factor_from_config (bool): If true, `max_size` will be
                multiplied by a cache factor derived from the homeserver config
        """
        # Default `clock` to something sensible. Note that we rename it to
        # `real_clock` so that mypy doesn't think its still `Optional`.
        if clock is None:
            real_clock = Clock(cast(IReactorTime, reactor))
        else:
            real_clock = clock

        cache: Union[Dict[KT, _Node[KT, VT]], TreeCache] = cache_type()
        self.cache = cache  # Used for introspection.
        self.apply_cache_factor_from_config = apply_cache_factor_from_config

        # Save the original max size, and apply the default size factor.
        self._original_max_size = max_size
        # We previously didn't apply the cache factor here, and as such some caches were
        # not affected by the global cache factor. Add an option here to disable applying
        # the cache factor when a cache is created
        if apply_cache_factor_from_config:
            self.max_size = int(max_size * cache_config.properties.default_factor_size)
        else:
            self.max_size = int(max_size)

        # register_cache might call our "set_cache_factor" callback; there's nothing to
        # do yet when we get resized.
        self._on_resize: Optional[Callable[[], None]] = None

        if cache_name is not None:
            metrics: Optional[CacheMetric] = register_cache(
                "lru_cache",
                cache_name,
                self,
                collect_callback=metrics_collection_callback,
            )
        else:
            metrics = None

        # this is exposed for access from outside this class
        self.metrics = metrics

        # We create a single weakref to self here so that we don't need to keep
        # creating more each time we create a `_Node`.
        weak_ref_to_self = weakref.ref(self)

        list_root = ListNode[_Node[KT, VT]].create_root_node()

        lock = threading.Lock()

        def evict() -> None:
            while cache_len() > self.max_size:
                # Get the last node in the list (i.e. the oldest node).
                todelete = list_root.prev_node

                # The list root should always have a valid `prev_node` if the
                # cache is not empty.
                assert todelete is not None

                # The node should always have a reference to a cache entry, as
                # we only drop the cache entry when we remove the node from the
                # list.
                node = todelete.get_cache_entry()
                assert node is not None

                evicted_len = delete_node(node)
                cache.pop(node.key, None)
                if metrics:
                    metrics.inc_evictions(EvictionReason.size, evicted_len)

        def synchronized(f: FT) -> FT:
            @wraps(f)
            def inner(*args: Any, **kwargs: Any) -> Any:
                with lock:
                    return f(*args, **kwargs)

            return cast(FT, inner)

        cached_cache_len = [0]
        if size_callback is not None:

            def cache_len() -> int:
                return cached_cache_len[0]

        else:

            def cache_len() -> int:
                return len(cache)

        self.len = synchronized(cache_len)

        def add_node(
            key: KT, value: VT, callbacks: Collection[Callable[[], None]] = ()
        ) -> None:
            node: _Node[KT, VT] = _Node(
                list_root,
                key,
                value,
                weak_ref_to_self,
                real_clock,
                callbacks,
                prune_unread_entries,
            )
            cache[key] = node

            if size_callback:
                cached_cache_len[0] += size_callback(node.value)

            if caches.TRACK_MEMORY_USAGE and metrics:
                metrics.inc_memory_usage(node.memory)

        def move_node_to_front(node: _Node[KT, VT]) -> None:
            node.move_to_front(real_clock, list_root)

        def delete_node(node: _Node[KT, VT]) -> int:
            node.drop_from_lists()

            deleted_len = 1
            if size_callback:
                deleted_len = size_callback(node.value)
                cached_cache_len[0] -= deleted_len

            node.run_and_clear_callbacks()

            if caches.TRACK_MEMORY_USAGE and metrics:
                metrics.dec_memory_usage(node.memory)

            return deleted_len

        @overload
        def cache_get(
            key: KT,
            default: Literal[None] = None,
            callbacks: Collection[Callable[[], None]] = ...,
            update_metrics: bool = ...,
        ) -> Optional[VT]:
            ...

        @overload
        def cache_get(
            key: KT,
            default: T,
            callbacks: Collection[Callable[[], None]] = ...,
            update_metrics: bool = ...,
        ) -> Union[T, VT]:
            ...

        @synchronized
        def cache_get(
            key: KT,
            default: Optional[T] = None,
            callbacks: Collection[Callable[[], None]] = (),
            update_metrics: bool = True,
        ) -> Union[None, T, VT]:
            node = cache.get(key, None)
            if node is not None:
                move_node_to_front(node)
                node.add_callbacks(callbacks)
                if update_metrics and metrics:
                    metrics.inc_hits()
                return node.value
            else:
                if update_metrics and metrics:
                    metrics.inc_misses()
                return default

        @synchronized
        def cache_set(
            key: KT, value: VT, callbacks: Collection[Callable[[], None]] = ()
        ) -> None:
            node = cache.get(key, None)
            if node is not None:
                # We sometimes store large objects, e.g. dicts, which cause
                # the inequality check to take a long time. So let's only do
                # the check if we have some callbacks to call.
                if value != node.value:
                    node.run_and_clear_callbacks()

                # We don't bother to protect this by value != node.value as
                # generally size_callback will be cheap compared with equality
                # checks. (For example, taking the size of two dicts is quicker
                # than comparing them for equality.)
                if size_callback:
                    cached_cache_len[0] -= size_callback(node.value)
                    cached_cache_len[0] += size_callback(value)

                node.add_callbacks(callbacks)

                move_node_to_front(node)
                node.value = value
            else:
                add_node(key, value, set(callbacks))

            evict()

        @synchronized
        def cache_set_default(key: KT, value: VT) -> VT:
            node = cache.get(key, None)
            if node is not None:
                return node.value
            else:
                add_node(key, value)
                evict()
                return value

        @overload
        def cache_pop(key: KT, default: Literal[None] = None) -> Optional[VT]:
            ...

        @overload
        def cache_pop(key: KT, default: T) -> Union[T, VT]:
            ...

        @synchronized
        def cache_pop(key: KT, default: Optional[T] = None) -> Union[None, T, VT]:
            node = cache.get(key, None)
            if node:
                delete_node(node)
                cache.pop(node.key, None)
                return node.value
            else:
                return default

        @synchronized
        def cache_del_multi(key: KT) -> None:
            """Delete an entry, or tree of entries

            If the LruCache is backed by a regular dict, then "key" must be of
            the right type for this cache

            If the LruCache is backed by a TreeCache, then "key" must be a tuple, but
            may be of lower cardinality than the TreeCache - in which case the whole
            subtree is deleted.
            """
            popped = cache.pop(key, None)
            if popped is None:
                return
            # for each deleted node, we now need to remove it from the linked list
            # and run its callbacks.
            for leaf in iterate_tree_cache_entry(popped):
                delete_node(leaf)

        @synchronized
        def cache_clear() -> None:
            for node in cache.values():
                node.run_and_clear_callbacks()
                node.drop_from_lists()

            assert list_root.next_node == list_root
            assert list_root.prev_node == list_root

            cache.clear()
            if size_callback:
                cached_cache_len[0] = 0

            if caches.TRACK_MEMORY_USAGE and metrics:
                metrics.clear_memory_usage()

        @synchronized
        def cache_contains(key: KT) -> bool:
            return key in cache

        # make sure that we clear out any excess entries after we get resized.
        self._on_resize = evict

        self.get = cache_get
        self.set = cache_set
        self.setdefault = cache_set_default
        self.pop = cache_pop
        self.del_multi = cache_del_multi
        # `invalidate` is exposed for consistency with DeferredCache, so that it can be
        # invalidated by the cache invalidation replication stream.
        self.invalidate = cache_del_multi
        self.len = synchronized(cache_len)
        self.contains = cache_contains
        self.clear = cache_clear

    def __getitem__(self, key: KT) -> VT:
        result = self.get(key, _Sentinel.sentinel)
        if result is _Sentinel.sentinel:
            raise KeyError()
        else:
            return result

    def __setitem__(self, key: KT, value: VT) -> None:
        self.set(key, value)

    def __delitem__(self, key: KT, value: VT) -> None:
        result = self.pop(key, _Sentinel.sentinel)
        if result is _Sentinel.sentinel:
            raise KeyError()

    def __len__(self) -> int:
        return self.len()

    def __contains__(self, key: KT) -> bool:
        return self.contains(key)

    def set_cache_factor(self, factor: float) -> bool:
        """
        Set the cache factor for this individual cache.

        This will trigger a resize if it changes, which may require evicting
        items from the cache.

        Returns:
            bool: Whether the cache changed size or not.
        """
        if not self.apply_cache_factor_from_config:
            return False

        new_size = int(self._original_max_size * factor)
        if new_size != self.max_size:
            self.max_size = new_size
            if self._on_resize:
                self._on_resize()
            return True
        return False

    def __del__(self) -> None:
        # We're about to be deleted, so we make sure to clear up all the nodes
        # and run callbacks, etc.
        #
        # This happens e.g. in the sync code where we have an expiring cache of
        # lru caches.
        self.clear()
