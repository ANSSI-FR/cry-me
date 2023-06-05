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
# Copyright 2016 OpenMarket Ltd
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
from typing import Generic, List, TypeVar

T = TypeVar("T")


class _Entry(Generic[T]):
    __slots__ = ["end_key", "queue"]

    def __init__(self, end_key: int) -> None:
        self.end_key: int = end_key
        self.queue: List[T] = []


class WheelTimer(Generic[T]):
    """Stores arbitrary objects that will be returned after their timers have
    expired.
    """

    def __init__(self, bucket_size: int = 5000) -> None:
        """
        Args:
            bucket_size: Size of buckets in ms. Corresponds roughly to the
                accuracy of the timer.
        """
        self.bucket_size: int = bucket_size
        self.entries: List[_Entry[T]] = []
        self.current_tick: int = 0

    def insert(self, now: int, obj: T, then: int) -> None:
        """Inserts object into timer.

        Args:
            now: Current time in msec
            obj: Object to be inserted
            then: When to return the object strictly after.
        """
        then_key = int(then / self.bucket_size) + 1

        if self.entries:
            min_key = self.entries[0].end_key
            max_key = self.entries[-1].end_key

            if then_key <= max_key:
                # The max here is to protect against inserts for times in the past
                self.entries[max(min_key, then_key) - min_key].queue.append(obj)
                return

        next_key = int(now / self.bucket_size) + 1
        if self.entries:
            last_key = self.entries[-1].end_key
        else:
            last_key = next_key

        # Handle the case when `then` is in the past and `entries` is empty.
        then_key = max(last_key, then_key)

        # Add empty entries between the end of the current list and when we want
        # to insert. This ensures there are no gaps.
        self.entries.extend(_Entry(key) for key in range(last_key, then_key + 1))

        self.entries[-1].queue.append(obj)

    def fetch(self, now: int) -> List[T]:
        """Fetch any objects that have timed out

        Args:
            now (ms): Current time in msec

        Returns:
            list: List of objects that have timed out
        """
        now_key = int(now / self.bucket_size)

        ret = []
        while self.entries and self.entries[0].end_key <= now_key:
            ret.extend(self.entries.pop(0).queue)

        return ret

    def __len__(self) -> int:
        return sum(len(entry.queue) for entry in self.entries)
