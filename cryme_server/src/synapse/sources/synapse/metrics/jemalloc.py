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

import ctypes
import logging
import os
import re
from typing import Iterable, Optional

from prometheus_client import Metric

from synapse.metrics import REGISTRY, GaugeMetricFamily

logger = logging.getLogger(__name__)


def _setup_jemalloc_stats() -> None:
    """Checks to see if jemalloc is loaded, and hooks up a collector to record
    statistics exposed by jemalloc.
    """

    # Try to find the loaded jemalloc shared library, if any. We need to
    # introspect into what is loaded, rather than loading whatever is on the
    # path, as if we load a *different* jemalloc version things will seg fault.

    # We look in `/proc/self/maps`, which only exists on linux.
    if not os.path.exists("/proc/self/maps"):
        logger.debug("Not looking for jemalloc as no /proc/self/maps exist")
        return

    # We're looking for a path at the end of the line that includes
    # "libjemalloc".
    regex = re.compile(r"/\S+/libjemalloc.*$")

    jemalloc_path = None
    with open("/proc/self/maps") as f:
        for line in f:
            match = regex.search(line.strip())
            if match:
                jemalloc_path = match.group()

    if not jemalloc_path:
        # No loaded jemalloc was found.
        logger.debug("jemalloc not found")
        return

    logger.debug("Found jemalloc at %s", jemalloc_path)

    jemalloc = ctypes.CDLL(jemalloc_path)

    def _mallctl(
        name: str, read: bool = True, write: Optional[int] = None
    ) -> Optional[int]:
        """Wrapper around `mallctl` for reading and writing integers to
        jemalloc.

        Args:
            name: The name of the option to read from/write to.
            read: Whether to try and read the value.
            write: The value to write, if given.

        Returns:
            The value read if `read` is True, otherwise None.

        Raises:
            An exception if `mallctl` returns a non-zero error code.
        """

        input_var = None
        input_var_ref = None
        input_len_ref = None
        if read:
            input_var = ctypes.c_size_t(0)
            input_len = ctypes.c_size_t(ctypes.sizeof(input_var))

            input_var_ref = ctypes.byref(input_var)
            input_len_ref = ctypes.byref(input_len)

        write_var_ref = None
        write_len = ctypes.c_size_t(0)
        if write is not None:
            write_var = ctypes.c_size_t(write)
            write_len = ctypes.c_size_t(ctypes.sizeof(write_var))

            write_var_ref = ctypes.byref(write_var)

        # The interface is:
        #
        #   int mallctl(
        #       const char *name,
        #       void *oldp,
        #       size_t *oldlenp,
        #       void *newp,
        #       size_t newlen
        #   )
        #
        # Where oldp/oldlenp is a buffer where the old value will be written to
        # (if not null), and newp/newlen is the buffer with the new value to set
        # (if not null). Note that they're all references *except* newlen.
        result = jemalloc.mallctl(
            name.encode("ascii"),
            input_var_ref,
            input_len_ref,
            write_var_ref,
            write_len,
        )

        if result != 0:
            raise Exception("Failed to call mallctl")

        if input_var is None:
            return None

        return input_var.value

    def _jemalloc_refresh_stats() -> None:
        """Request that jemalloc updates its internal statistics. This needs to
        be called before querying for stats, otherwise it will return stale
        values.
        """
        try:
            _mallctl("epoch", read=False, write=1)
        except Exception as e:
            logger.warning("Failed to reload jemalloc stats: %s", e)

    class JemallocCollector:
        """Metrics for internal jemalloc stats."""

        def collect(self) -> Iterable[Metric]:
            _jemalloc_refresh_stats()

            g = GaugeMetricFamily(
                "jemalloc_stats_app_memory_bytes",
                "The stats reported by jemalloc",
                labels=["type"],
            )

            # Read the relevant global stats from jemalloc. Note that these may
            # not be accurate if python is configured to use its internal small
            # object allocator (which is on by default, disable by setting the
            # env `PYTHONMALLOC=malloc`).
            #
            # See the jemalloc manpage for details about what each value means,
            # roughly:
            #   - allocated ─ Total number of bytes allocated by the app
            #   - active ─ Total number of bytes in active pages allocated by
            #     the application, this is bigger than `allocated`.
            #   - resident ─ Maximum number of bytes in physically resident data
            #     pages mapped by the allocator, comprising all pages dedicated
            #     to allocator metadata, pages backing active allocations, and
            #     unused dirty pages. This is bigger than `active`.
            #   - mapped ─ Total number of bytes in active extents mapped by the
            #     allocator.
            #   - metadata ─ Total number of bytes dedicated to jemalloc
            #     metadata.
            for t in (
                "allocated",
                "active",
                "resident",
                "mapped",
                "metadata",
            ):
                try:
                    value = _mallctl(f"stats.{t}")
                except Exception as e:
                    # There was an error fetching the value, skip.
                    logger.warning("Failed to read jemalloc stats.%s: %s", t, e)
                    continue

                g.add_metric([t], value=value)

            yield g

    REGISTRY.register(JemallocCollector())

    logger.debug("Added jemalloc stats")


def setup_jemalloc_stats() -> None:
    """Try to setup jemalloc stats, if jemalloc is loaded."""

    try:
        _setup_jemalloc_stats()
    except Exception as e:
        # This should only happen if we find the loaded jemalloc library, but
        # fail to load it somehow (e.g. we somehow picked the wrong version).
        logger.info("Failed to setup collector to record jemalloc stats: %s", e)
