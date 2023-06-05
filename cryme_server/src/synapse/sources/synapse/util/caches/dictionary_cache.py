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
import enum
import logging
import threading
from typing import Any, Dict, Generic, Iterable, Optional, Set, TypeVar

import attr

from synapse.util.caches.lrucache import LruCache

logger = logging.getLogger(__name__)


# The type of the cache keys.
KT = TypeVar("KT")
# The type of the dictionary keys.
DKT = TypeVar("DKT")
# The type of the dictionary values.
DV = TypeVar("DV")


# This class can't be generic because it uses slots with attrs.
# See: https://github.com/python-attrs/attrs/issues/313
@attr.s(slots=True)
class DictionaryEntry:  # should be: Generic[DKT, DV].
    """Returned when getting an entry from the cache

    Attributes:
        full: Whether the cache has the full or dict or just some keys.
            If not full then not all requested keys will necessarily be present
            in `value`
        known_absent: Keys that were looked up in the dict and were not
            there.
        value: The full or partial dict value
    """

    full = attr.ib(type=bool)
    known_absent = attr.ib(type=Set[Any])  # should be: Set[DKT]
    value = attr.ib(type=Dict[Any, Any])  # should be: Dict[DKT, DV]

    def __len__(self) -> int:
        return len(self.value)


class _Sentinel(enum.Enum):
    # defining a sentinel in this way allows mypy to correctly handle the
    # type of a dictionary lookup.
    sentinel = object()


class DictionaryCache(Generic[KT, DKT, DV]):
    """Caches key -> dictionary lookups, supporting caching partial dicts, i.e.
    fetching a subset of dictionary keys for a particular key.
    """

    def __init__(self, name: str, max_entries: int = 1000):
        self.cache: LruCache[KT, DictionaryEntry] = LruCache(
            max_size=max_entries, cache_name=name, size_callback=len
        )

        self.name = name
        self.sequence = 0
        self.thread: Optional[threading.Thread] = None

    def check_thread(self) -> None:
        expected_thread = self.thread
        if expected_thread is None:
            self.thread = threading.current_thread()
        else:
            if expected_thread is not threading.current_thread():
                raise ValueError(
                    "Cache objects can only be accessed from the main thread"
                )

    def get(
        self, key: KT, dict_keys: Optional[Iterable[DKT]] = None
    ) -> DictionaryEntry:
        """Fetch an entry out of the cache

        Args:
            key
            dict_keys: If given a set of keys then return only those keys
                that exist in the cache.

        Returns:
            DictionaryEntry
        """
        entry = self.cache.get(key, _Sentinel.sentinel)
        if entry is not _Sentinel.sentinel:
            if dict_keys is None:
                return DictionaryEntry(
                    entry.full, entry.known_absent, dict(entry.value)
                )
            else:
                return DictionaryEntry(
                    entry.full,
                    entry.known_absent,
                    {k: entry.value[k] for k in dict_keys if k in entry.value},
                )

        return DictionaryEntry(False, set(), {})

    def invalidate(self, key: KT) -> None:
        self.check_thread()

        # Increment the sequence number so that any SELECT statements that
        # raced with the INSERT don't update the cache (SYN-369)
        self.sequence += 1
        self.cache.pop(key, None)

    def invalidate_all(self) -> None:
        self.check_thread()
        self.sequence += 1
        self.cache.clear()

    def update(
        self,
        sequence: int,
        key: KT,
        value: Dict[DKT, DV],
        fetched_keys: Optional[Iterable[DKT]] = None,
    ) -> None:
        """Updates the entry in the cache

        Args:
            sequence
            key
            value: The value to update the cache with.
            fetched_keys: All of the dictionary keys which were
                fetched from the database.

                If None, this is the complete value for key K. Otherwise, it
                is used to infer a list of keys which we know don't exist in
                the full dict.
        """
        self.check_thread()
        if self.sequence == sequence:
            # Only update the cache if the caches sequence number matches the
            # number that the cache had before the SELECT was started (SYN-369)
            if fetched_keys is None:
                self._insert(key, value, set())
            else:
                self._update_or_insert(key, value, fetched_keys)

    def _update_or_insert(
        self, key: KT, value: Dict[DKT, DV], known_absent: Iterable[DKT]
    ) -> None:
        # We pop and reinsert as we need to tell the cache the size may have
        # changed

        entry: DictionaryEntry = self.cache.pop(key, DictionaryEntry(False, set(), {}))
        entry.value.update(value)
        entry.known_absent.update(known_absent)
        self.cache[key] = entry

    def _insert(self, key: KT, value: Dict[DKT, DV], known_absent: Set[DKT]) -> None:
        self.cache[key] = DictionaryEntry(True, known_absent, value)
