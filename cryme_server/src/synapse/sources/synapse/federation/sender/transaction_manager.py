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
# Copyright 2019 New Vector Ltd
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
from typing import TYPE_CHECKING, List

from prometheus_client import Gauge

from synapse.api.errors import HttpResponseException
from synapse.events import EventBase
from synapse.federation.persistence import TransactionActions
from synapse.federation.units import Edu, Transaction
from synapse.logging.opentracing import (
    extract_text_map,
    set_tag,
    start_active_span_follows_from,
    tags,
    whitelisted_homeserver,
)
from synapse.types import JsonDict
from synapse.util import json_decoder
from synapse.util.metrics import measure_func

if TYPE_CHECKING:
    import synapse.server

logger = logging.getLogger(__name__)

last_pdu_ts_metric = Gauge(
    "synapse_federation_last_sent_pdu_time",
    "The timestamp of the last PDU which was successfully sent to the given domain",
    labelnames=("server_name",),
)


class TransactionManager:
    """Helper class which handles building and sending transactions

    shared between PerDestinationQueue objects
    """

    def __init__(self, hs: "synapse.server.HomeServer"):
        self._server_name = hs.hostname
        self.clock = hs.get_clock()  # nb must be called this for @measure_func
        self._store = hs.get_datastore()
        self._transaction_actions = TransactionActions(self._store)
        self._transport_layer = hs.get_federation_transport_client()

        self._federation_metrics_domains = (
            hs.config.federation.federation_metrics_domains
        )

        # HACK to get unique tx id
        self._next_txn_id = int(self.clock.time_msec())

    @measure_func("_send_new_transaction")
    async def send_new_transaction(
        self,
        destination: str,
        pdus: List[EventBase],
        edus: List[Edu],
    ) -> None:
        """
        Args:
            destination: The destination to send to (e.g. 'example.org')
            pdus: In-order list of PDUs to send
            edus: List of EDUs to send
        """

        # Make a transaction-sending opentracing span. This span follows on from
        # all the edus in that transaction. This needs to be done since there is
        # no active span here, so if the edus were not received by the remote the
        # span would have no causality and it would be forgotten.

        span_contexts = []
        keep_destination = whitelisted_homeserver(destination)

        for edu in edus:
            context = edu.get_context()
            if context:
                span_contexts.append(extract_text_map(json_decoder.decode(context)))
            if keep_destination:
                edu.strip_context()

        with start_active_span_follows_from("send_transaction", span_contexts):
            logger.debug("TX [%s] _attempt_new_transaction", destination)

            txn_id = str(self._next_txn_id)

            logger.debug(
                "TX [%s] {%s} Attempting new transaction (pdus: %d, edus: %d)",
                destination,
                txn_id,
                len(pdus),
                len(edus),
            )

            transaction = Transaction(
                origin_server_ts=int(self.clock.time_msec()),
                transaction_id=txn_id,
                origin=self._server_name,
                destination=destination,
                pdus=[p.get_pdu_json() for p in pdus],
                edus=[edu.get_dict() for edu in edus],
            )

            self._next_txn_id += 1

            logger.info(
                "TX [%s] {%s} Sending transaction [%s], (PDUs: %d, EDUs: %d)",
                destination,
                txn_id,
                transaction.transaction_id,
                len(pdus),
                len(edus),
            )

            # Actually send the transaction

            # FIXME (erikj): This is a bit of a hack to make the Pdu age
            # keys work
            # FIXME (richardv): I also believe it no longer works. We (now?) store
            #  "age_ts" in "unsigned" rather than at the top level. See
            #  https://github.com/matrix-org/synapse/issues/8429.
            def json_data_cb() -> JsonDict:
                data = transaction.get_dict()
                now = int(self.clock.time_msec())
                if "pdus" in data:
                    for p in data["pdus"]:
                        if "age_ts" in p:
                            unsigned = p.setdefault("unsigned", {})
                            unsigned["age"] = now - int(p["age_ts"])
                            del p["age_ts"]
                return data

            try:
                response = await self._transport_layer.send_transaction(
                    transaction, json_data_cb
                )
            except HttpResponseException as e:
                code = e.code

                set_tag(tags.ERROR, True)

                logger.info("TX [%s] {%s} got %d response", destination, txn_id, code)
                raise

            logger.info("TX [%s] {%s} got 200 response", destination, txn_id)

            for e_id, r in response.get("pdus", {}).items():
                if "error" in r:
                    logger.warning(
                        "TX [%s] {%s} Remote returned error for %s: %s",
                        destination,
                        txn_id,
                        e_id,
                        r,
                    )

            if pdus and destination in self._federation_metrics_domains:
                last_pdu = pdus[-1]
                last_pdu_ts_metric.labels(server_name=destination).set(
                    last_pdu.origin_server_ts / 1000
                )
