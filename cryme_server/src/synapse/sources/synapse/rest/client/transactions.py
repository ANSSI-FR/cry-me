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
# Copyright 2014-2016 OpenMarket Ltd
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

"""This module contains logic for storing HTTP PUT transactions. This is used
to ensure idempotency when performing PUTs using the REST API."""
import logging
from typing import TYPE_CHECKING, Any, Awaitable, Callable, Dict, Tuple

from twisted.python.failure import Failure
from twisted.web.server import Request

from synapse.logging.context import make_deferred_yieldable, run_in_background
from synapse.types import JsonDict
from synapse.util.async_helpers import ObservableDeferred

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)

CLEANUP_PERIOD_MS = 1000 * 60 * 30  # 30 mins


class HttpTransactionCache:
    def __init__(self, hs: "HomeServer"):
        self.hs = hs
        self.auth = self.hs.get_auth()
        self.clock = self.hs.get_clock()
        # $txn_key: (ObservableDeferred<(res_code, res_json_body)>, timestamp)
        self.transactions: Dict[
            str, Tuple[ObservableDeferred[Tuple[int, JsonDict]], int]
        ] = {}
        # Try to clean entries every 30 mins. This means entries will exist
        # for at *LEAST* 30 mins, and at *MOST* 60 mins.
        self.cleaner = self.clock.looping_call(self._cleanup, CLEANUP_PERIOD_MS)

    def _get_transaction_key(self, request: Request) -> str:
        """A helper function which returns a transaction key that can be used
        with TransactionCache for idempotent requests.

        Idempotency is based on the returned key being the same for separate
        requests to the same endpoint. The key is formed from the HTTP request
        path and the access_token for the requesting user.

        Args:
            request: The incoming request. Must contain an access_token.
        Returns:
            A transaction key
        """
        assert request.path is not None
        token = self.auth.get_access_token_from_request(request)
        return request.path.decode("utf8") + "/" + token

    def fetch_or_execute_request(
        self,
        request: Request,
        fn: Callable[..., Awaitable[Tuple[int, JsonDict]]],
        *args: Any,
        **kwargs: Any,
    ) -> Awaitable[Tuple[int, JsonDict]]:
        """A helper function for fetch_or_execute which extracts
        a transaction key from the given request.

        See:
            fetch_or_execute
        """
        return self.fetch_or_execute(
            self._get_transaction_key(request), fn, *args, **kwargs
        )

    def fetch_or_execute(
        self,
        txn_key: str,
        fn: Callable[..., Awaitable[Tuple[int, JsonDict]]],
        *args: Any,
        **kwargs: Any,
    ) -> Awaitable[Tuple[int, JsonDict]]:
        """Fetches the response for this transaction, or executes the given function
        to produce a response for this transaction.

        Args:
            txn_key: A key to ensure idempotency should fetch_or_execute be
                called again at a later point in time.
            fn: A function which returns a tuple of (response_code, response_dict).
            *args: Arguments to pass to fn.
            **kwargs: Keyword arguments to pass to fn.
        Returns:
            Deferred which resolves to a tuple of (response_code, response_dict).
        """
        if txn_key in self.transactions:
            observable = self.transactions[txn_key][0]
        else:
            # execute the function instead.
            deferred = run_in_background(fn, *args, **kwargs)

            observable = ObservableDeferred(deferred)
            self.transactions[txn_key] = (observable, self.clock.time_msec())

            # if the request fails with an exception, remove it
            # from the transaction map. This is done to ensure that we don't
            # cache transient errors like rate-limiting errors, etc.
            def remove_from_map(err: Failure) -> None:
                self.transactions.pop(txn_key, None)
                # we deliberately do not propagate the error any further, as we
                # expect the observers to have reported it.

            deferred.addErrback(remove_from_map)

        return make_deferred_yieldable(observable.observe())

    def _cleanup(self) -> None:
        now = self.clock.time_msec()
        for key in list(self.transactions):
            ts = self.transactions[key][1]
            if now > (ts + CLEANUP_PERIOD_MS):  # after cleanup period
                del self.transactions[key]
