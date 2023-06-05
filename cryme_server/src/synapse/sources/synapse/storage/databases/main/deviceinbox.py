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

import logging
from typing import TYPE_CHECKING, List, Optional, Tuple, cast

from synapse.logging import issue9533_logger
from synapse.logging.opentracing import log_kv, set_tag, trace
from synapse.replication.tcp.streams import ToDeviceStream
from synapse.storage._base import SQLBaseStore, db_to_json
from synapse.storage.database import (
    DatabasePool,
    LoggingDatabaseConnection,
    LoggingTransaction,
)
from synapse.storage.engines import PostgresEngine
from synapse.storage.util.id_generators import (
    AbstractStreamIdGenerator,
    MultiWriterIdGenerator,
    StreamIdGenerator,
)
from synapse.types import JsonDict
from synapse.util import json_encoder
from synapse.util.caches.expiringcache import ExpiringCache
from synapse.util.caches.stream_change_cache import StreamChangeCache

if TYPE_CHECKING:
    from synapse.server import HomeServer

logger = logging.getLogger(__name__)


class DeviceInboxWorkerStore(SQLBaseStore):
    def __init__(
        self,
        database: DatabasePool,
        db_conn: LoggingDatabaseConnection,
        hs: "HomeServer",
    ):
        super().__init__(database, db_conn, hs)

        self._instance_name = hs.get_instance_name()

        # Map of (user_id, device_id) to the last stream_id that has been
        # deleted up to. This is so that we can no op deletions.
        self._last_device_delete_cache: ExpiringCache[
            Tuple[str, Optional[str]], int
        ] = ExpiringCache(
            cache_name="last_device_delete_cache",
            clock=self._clock,
            max_len=10000,
            expiry_ms=30 * 60 * 1000,
        )

        if isinstance(database.engine, PostgresEngine):
            self._can_write_to_device = (
                self._instance_name in hs.config.worker.writers.to_device
            )

            self._device_inbox_id_gen: AbstractStreamIdGenerator = (
                MultiWriterIdGenerator(
                    db_conn=db_conn,
                    db=database,
                    stream_name="to_device",
                    instance_name=self._instance_name,
                    tables=[("device_inbox", "instance_name", "stream_id")],
                    sequence_name="device_inbox_sequence",
                    writers=hs.config.worker.writers.to_device,
                )
            )
        else:
            self._can_write_to_device = True
            self._device_inbox_id_gen = StreamIdGenerator(
                db_conn, "device_inbox", "stream_id"
            )

        max_device_inbox_id = self._device_inbox_id_gen.get_current_token()
        device_inbox_prefill, min_device_inbox_id = self.db_pool.get_cache_dict(
            db_conn,
            "device_inbox",
            entity_column="user_id",
            stream_column="stream_id",
            max_value=max_device_inbox_id,
            limit=1000,
        )
        self._device_inbox_stream_cache = StreamChangeCache(
            "DeviceInboxStreamChangeCache",
            min_device_inbox_id,
            prefilled_cache=device_inbox_prefill,
        )

        # The federation outbox and the local device inbox uses the same
        # stream_id generator.
        device_outbox_prefill, min_device_outbox_id = self.db_pool.get_cache_dict(
            db_conn,
            "device_federation_outbox",
            entity_column="destination",
            stream_column="stream_id",
            max_value=max_device_inbox_id,
            limit=1000,
        )
        self._device_federation_outbox_stream_cache = StreamChangeCache(
            "DeviceFederationOutboxStreamChangeCache",
            min_device_outbox_id,
            prefilled_cache=device_outbox_prefill,
        )

    def process_replication_rows(self, stream_name, instance_name, token, rows):
        if stream_name == ToDeviceStream.NAME:
            # If replication is happening than postgres must be being used.
            assert isinstance(self._device_inbox_id_gen, MultiWriterIdGenerator)
            self._device_inbox_id_gen.advance(instance_name, token)
            for row in rows:
                if row.entity.startswith("@"):
                    self._device_inbox_stream_cache.entity_has_changed(
                        row.entity, token
                    )
                else:
                    self._device_federation_outbox_stream_cache.entity_has_changed(
                        row.entity, token
                    )
        return super().process_replication_rows(stream_name, instance_name, token, rows)

    def get_to_device_stream_token(self):
        return self._device_inbox_id_gen.get_current_token()

    async def get_new_messages_for_device(
        self,
        user_id: str,
        device_id: Optional[str],
        last_stream_id: int,
        current_stream_id: int,
        limit: int = 100,
    ) -> Tuple[List[dict], int]:
        """
        Args:
            user_id: The recipient user_id.
            device_id: The recipient device_id.
            last_stream_id: The last stream ID checked.
            current_stream_id: The current position of the to device
                message stream.
            limit: The maximum number of messages to retrieve.

        Returns:
            A tuple containing:
                * A list of messages for the device.
                * The max stream token of these messages. There may be more to retrieve
                  if the given limit was reached.
        """
        has_changed = self._device_inbox_stream_cache.has_entity_changed(
            user_id, last_stream_id
        )
        if not has_changed:
            return [], current_stream_id

        def get_new_messages_for_device_txn(txn):
            sql = (
                "SELECT stream_id, message_json FROM device_inbox"
                " WHERE user_id = ? AND device_id = ?"
                " AND ? < stream_id AND stream_id <= ?"
                " ORDER BY stream_id ASC"
                " LIMIT ?"
            )
            txn.execute(
                sql, (user_id, device_id, last_stream_id, current_stream_id, limit)
            )

            messages = []
            stream_pos = current_stream_id

            for row in txn:
                stream_pos = row[0]
                messages.append(db_to_json(row[1]))

            # If the limit was not reached we know that there's no more data for this
            # user/device pair up to current_stream_id.
            if len(messages) < limit:
                stream_pos = current_stream_id

            return messages, stream_pos

        return await self.db_pool.runInteraction(
            "get_new_messages_for_device", get_new_messages_for_device_txn
        )

    @trace
    async def delete_messages_for_device(
        self, user_id: str, device_id: Optional[str], up_to_stream_id: int
    ) -> int:
        """
        Args:
            user_id: The recipient user_id.
            device_id: The recipient device_id.
            up_to_stream_id: Where to delete messages up to.

        Returns:
            The number of messages deleted.
        """
        # If we have cached the last stream id we've deleted up to, we can
        # check if there is likely to be anything that needs deleting
        last_deleted_stream_id = self._last_device_delete_cache.get(
            (user_id, device_id), None
        )

        set_tag("last_deleted_stream_id", last_deleted_stream_id)

        if last_deleted_stream_id:
            has_changed = self._device_inbox_stream_cache.has_entity_changed(
                user_id, last_deleted_stream_id
            )
            if not has_changed:
                log_kv({"message": "No changes in cache since last check"})
                return 0

        def delete_messages_for_device_txn(txn):
            sql = (
                "DELETE FROM device_inbox"
                " WHERE user_id = ? AND device_id = ?"
                " AND stream_id <= ?"
            )
            txn.execute(sql, (user_id, device_id, up_to_stream_id))
            return txn.rowcount

        count = await self.db_pool.runInteraction(
            "delete_messages_for_device", delete_messages_for_device_txn
        )

        log_kv({"message": f"deleted {count} messages for device", "count": count})

        # Update the cache, ensuring that we only ever increase the value
        updated_last_deleted_stream_id = self._last_device_delete_cache.get(
            (user_id, device_id), 0
        )
        self._last_device_delete_cache[(user_id, device_id)] = max(
            updated_last_deleted_stream_id, up_to_stream_id
        )

        return count

    @trace
    async def get_new_device_msgs_for_remote(
        self, destination, last_stream_id, current_stream_id, limit
    ) -> Tuple[List[dict], int]:
        """
        Args:
            destination(str): The name of the remote server.
            last_stream_id(int|long): The last position of the device message stream
                that the server sent up to.
            current_stream_id(int|long): The current position of the device
                message stream.
        Returns:
            A list of messages for the device and where in the stream the messages got to.
        """

        set_tag("destination", destination)
        set_tag("last_stream_id", last_stream_id)
        set_tag("current_stream_id", current_stream_id)
        set_tag("limit", limit)

        has_changed = self._device_federation_outbox_stream_cache.has_entity_changed(
            destination, last_stream_id
        )
        if not has_changed or last_stream_id == current_stream_id:
            log_kv({"message": "No new messages in stream"})
            return [], current_stream_id

        if limit <= 0:
            # This can happen if we run out of room for EDUs in the transaction.
            return [], last_stream_id

        @trace
        def get_new_messages_for_remote_destination_txn(txn):
            sql = (
                "SELECT stream_id, messages_json FROM device_federation_outbox"
                " WHERE destination = ?"
                " AND ? < stream_id AND stream_id <= ?"
                " ORDER BY stream_id ASC"
                " LIMIT ?"
            )
            txn.execute(sql, (destination, last_stream_id, current_stream_id, limit))

            messages = []
            stream_pos = current_stream_id

            for row in txn:
                stream_pos = row[0]
                messages.append(db_to_json(row[1]))

            # If the limit was not reached we know that there's no more data for this
            # user/device pair up to current_stream_id.
            if len(messages) < limit:
                log_kv({"message": "Set stream position to current position"})
                stream_pos = current_stream_id

            return messages, stream_pos

        return await self.db_pool.runInteraction(
            "get_new_device_msgs_for_remote",
            get_new_messages_for_remote_destination_txn,
        )

    @trace
    async def delete_device_msgs_for_remote(
        self, destination: str, up_to_stream_id: int
    ) -> None:
        """Used to delete messages when the remote destination acknowledges
        their receipt.

        Args:
            destination: The destination server_name
            up_to_stream_id: Where to delete messages up to.
        """

        def delete_messages_for_remote_destination_txn(txn):
            sql = (
                "DELETE FROM device_federation_outbox"
                " WHERE destination = ?"
                " AND stream_id <= ?"
            )
            txn.execute(sql, (destination, up_to_stream_id))

        await self.db_pool.runInteraction(
            "delete_device_msgs_for_remote", delete_messages_for_remote_destination_txn
        )

    async def get_all_new_device_messages(
        self, instance_name: str, last_id: int, current_id: int, limit: int
    ) -> Tuple[List[Tuple[int, tuple]], int, bool]:
        """Get updates for to device replication stream.

        Args:
            instance_name: The writer we want to fetch updates from. Unused
                here since there is only ever one writer.
            last_id: The token to fetch updates from. Exclusive.
            current_id: The token to fetch updates up to. Inclusive.
            limit: The requested limit for the number of rows to return. The
                function may return more or fewer rows.

        Returns:
            A tuple consisting of: the updates, a token to use to fetch
            subsequent updates, and whether we returned fewer rows than exists
            between the requested tokens due to the limit.

            The token returned can be used in a subsequent call to this
            function to get further updatees.

            The updates are a list of 2-tuples of stream ID and the row data
        """

        if last_id == current_id:
            return [], current_id, False

        def get_all_new_device_messages_txn(txn):
            # We limit like this as we might have multiple rows per stream_id, and
            # we want to make sure we always get all entries for any stream_id
            # we return.
            upper_pos = min(current_id, last_id + limit)
            sql = (
                "SELECT max(stream_id), user_id"
                " FROM device_inbox"
                " WHERE ? < stream_id AND stream_id <= ?"
                " GROUP BY user_id"
            )
            txn.execute(sql, (last_id, upper_pos))
            updates = [(row[0], row[1:]) for row in txn]

            sql = (
                "SELECT max(stream_id), destination"
                " FROM device_federation_outbox"
                " WHERE ? < stream_id AND stream_id <= ?"
                " GROUP BY destination"
            )
            txn.execute(sql, (last_id, upper_pos))
            updates.extend((row[0], row[1:]) for row in txn)

            # Order by ascending stream ordering
            updates.sort()

            limited = False
            upto_token = current_id
            if len(updates) >= limit:
                upto_token = updates[-1][0]
                limited = True

            return updates, upto_token, limited

        return await self.db_pool.runInteraction(
            "get_all_new_device_messages", get_all_new_device_messages_txn
        )

    @trace
    async def add_messages_to_device_inbox(
        self,
        local_messages_by_user_then_device: dict,
        remote_messages_by_destination: dict,
    ) -> int:
        """Used to send messages from this server.

        Args:
            local_messages_by_user_then_device:
                Dictionary of recipient user_id to recipient device_id to message.
            remote_messages_by_destination:
                Dictionary of destination server_name to the EDU JSON to send.

        Returns:
            The new stream_id.
        """

        assert self._can_write_to_device

        def add_messages_txn(txn, now_ms, stream_id):
            # Add the local messages directly to the local inbox.
            self._add_messages_to_local_device_inbox_txn(
                txn, stream_id, local_messages_by_user_then_device
            )

            # Add the remote messages to the federation outbox.
            # We'll send them to a remote server when we next send a
            # federation transaction to that destination.
            self.db_pool.simple_insert_many_txn(
                txn,
                table="device_federation_outbox",
                values=[
                    {
                        "destination": destination,
                        "stream_id": stream_id,
                        "queued_ts": now_ms,
                        "messages_json": json_encoder.encode(edu),
                        "instance_name": self._instance_name,
                    }
                    for destination, edu in remote_messages_by_destination.items()
                ],
            )

            if remote_messages_by_destination:
                issue9533_logger.debug(
                    "Queued outgoing to-device messages with stream_id %i for %s",
                    stream_id,
                    list(remote_messages_by_destination.keys()),
                )

        async with self._device_inbox_id_gen.get_next() as stream_id:
            now_ms = self._clock.time_msec()
            await self.db_pool.runInteraction(
                "add_messages_to_device_inbox", add_messages_txn, now_ms, stream_id
            )
            for user_id in local_messages_by_user_then_device.keys():
                self._device_inbox_stream_cache.entity_has_changed(user_id, stream_id)
            for destination in remote_messages_by_destination.keys():
                self._device_federation_outbox_stream_cache.entity_has_changed(
                    destination, stream_id
                )

        return self._device_inbox_id_gen.get_current_token()

    async def add_messages_from_remote_to_device_inbox(
        self, origin: str, message_id: str, local_messages_by_user_then_device: dict
    ) -> int:
        assert self._can_write_to_device

        def add_messages_txn(txn, now_ms, stream_id):
            # Check if we've already inserted a matching message_id for that
            # origin. This can happen if the origin doesn't receive our
            # acknowledgement from the first time we received the message.
            already_inserted = self.db_pool.simple_select_one_txn(
                txn,
                table="device_federation_inbox",
                keyvalues={"origin": origin, "message_id": message_id},
                retcols=("message_id",),
                allow_none=True,
            )
            if already_inserted is not None:
                return

            # Add an entry for this message_id so that we know we've processed
            # it.
            self.db_pool.simple_insert_txn(
                txn,
                table="device_federation_inbox",
                values={
                    "origin": origin,
                    "message_id": message_id,
                    "received_ts": now_ms,
                },
            )

            # Add the messages to the appropriate local device inboxes so that
            # they'll be sent to the devices when they next sync.
            self._add_messages_to_local_device_inbox_txn(
                txn, stream_id, local_messages_by_user_then_device
            )

        async with self._device_inbox_id_gen.get_next() as stream_id:
            now_ms = self._clock.time_msec()
            await self.db_pool.runInteraction(
                "add_messages_from_remote_to_device_inbox",
                add_messages_txn,
                now_ms,
                stream_id,
            )
            for user_id in local_messages_by_user_then_device.keys():
                self._device_inbox_stream_cache.entity_has_changed(user_id, stream_id)

        return stream_id

    def _add_messages_to_local_device_inbox_txn(
        self, txn, stream_id, messages_by_user_then_device
    ):
        assert self._can_write_to_device

        local_by_user_then_device = {}
        for user_id, messages_by_device in messages_by_user_then_device.items():
            messages_json_for_user = {}
            devices = list(messages_by_device.keys())
            if len(devices) == 1 and devices[0] == "*":
                # Handle wildcard device_ids.
                # We exclude hidden devices (such as cross-signing keys) here as they are
                # not expected to receive to-device messages.
                devices = self.db_pool.simple_select_onecol_txn(
                    txn,
                    table="devices",
                    keyvalues={"user_id": user_id, "hidden": False},
                    retcol="device_id",
                )

                message_json = json_encoder.encode(messages_by_device["*"])
                for device_id in devices:
                    # Add the message for all devices for this user on this
                    # server.
                    messages_json_for_user[device_id] = message_json
            else:
                if not devices:
                    continue

                # We exclude hidden devices (such as cross-signing keys) here as they are
                # not expected to receive to-device messages.
                rows = self.db_pool.simple_select_many_txn(
                    txn,
                    table="devices",
                    keyvalues={"user_id": user_id, "hidden": False},
                    column="device_id",
                    iterable=devices,
                    retcols=("device_id",),
                )

                for row in rows:
                    # Only insert into the local inbox if the device exists on
                    # this server
                    device_id = row["device_id"]
                    message_json = json_encoder.encode(messages_by_device[device_id])
                    messages_json_for_user[device_id] = message_json

            if messages_json_for_user:
                local_by_user_then_device[user_id] = messages_json_for_user

        if not local_by_user_then_device:
            return

        self.db_pool.simple_insert_many_txn(
            txn,
            table="device_inbox",
            values=[
                {
                    "user_id": user_id,
                    "device_id": device_id,
                    "stream_id": stream_id,
                    "message_json": message_json,
                    "instance_name": self._instance_name,
                }
                for user_id, messages_by_device in local_by_user_then_device.items()
                for device_id, message_json in messages_by_device.items()
            ],
        )

        issue9533_logger.debug(
            "Stored to-device messages with stream_id %i for %s",
            stream_id,
            [
                (user_id, device_id)
                for (user_id, messages_by_device) in local_by_user_then_device.items()
                for device_id in messages_by_device.keys()
            ],
        )


class DeviceInboxBackgroundUpdateStore(SQLBaseStore):
    DEVICE_INBOX_STREAM_ID = "device_inbox_stream_drop"
    REMOVE_DELETED_DEVICES = "remove_deleted_devices_from_device_inbox"
    REMOVE_HIDDEN_DEVICES = "remove_hidden_devices_from_device_inbox"
    REMOVE_DEAD_DEVICES_FROM_INBOX = "remove_dead_devices_from_device_inbox"

    def __init__(
        self,
        database: DatabasePool,
        db_conn: LoggingDatabaseConnection,
        hs: "HomeServer",
    ):
        super().__init__(database, db_conn, hs)

        self.db_pool.updates.register_background_index_update(
            "device_inbox_stream_index",
            index_name="device_inbox_stream_id_user_id",
            table="device_inbox",
            columns=["stream_id", "user_id"],
        )

        self.db_pool.updates.register_background_update_handler(
            self.DEVICE_INBOX_STREAM_ID, self._background_drop_index_device_inbox
        )

        # Used to be a background update that deletes all device_inboxes for deleted
        # devices.
        self.db_pool.updates.register_noop_background_update(
            self.REMOVE_DELETED_DEVICES
        )
        # Used to be a background update that deletes all device_inboxes for hidden
        # devices.
        self.db_pool.updates.register_noop_background_update(self.REMOVE_HIDDEN_DEVICES)

        self.db_pool.updates.register_background_update_handler(
            self.REMOVE_DEAD_DEVICES_FROM_INBOX,
            self._remove_dead_devices_from_device_inbox,
        )

    async def _background_drop_index_device_inbox(self, progress, batch_size):
        def reindex_txn(conn):
            txn = conn.cursor()
            txn.execute("DROP INDEX IF EXISTS device_inbox_stream_id")
            txn.close()

        await self.db_pool.runWithConnection(reindex_txn)

        await self.db_pool.updates._end_background_update(self.DEVICE_INBOX_STREAM_ID)

        return 1

    async def _remove_dead_devices_from_device_inbox(
        self,
        progress: JsonDict,
        batch_size: int,
    ) -> int:
        """A background update to remove devices that were either deleted or hidden from
        the device_inbox table.

        Args:
            progress: The update's progress dict.
            batch_size: The batch size for this update.

        Returns:
            The number of rows deleted.
        """

        def _remove_dead_devices_from_device_inbox_txn(
            txn: LoggingTransaction,
        ) -> Tuple[int, bool]:

            if "max_stream_id" in progress:
                max_stream_id = progress["max_stream_id"]
            else:
                txn.execute("SELECT max(stream_id) FROM device_inbox")
                # There's a type mismatch here between how we want to type the row and
                # what fetchone says it returns, but we silence it because we know that
                # res can't be None.
                res = cast(Tuple[Optional[int]], txn.fetchone())
                if res[0] is None:
                    # this can only happen if the `device_inbox` table is empty, in which
                    # case we have no work to do.
                    return 0, True
                else:
                    max_stream_id = res[0]

            start = progress.get("stream_id", 0)
            stop = start + batch_size

            # delete rows in `device_inbox` which do *not* correspond to a known,
            # unhidden device.
            sql = """
                DELETE FROM device_inbox
                WHERE
                    stream_id >= ? AND stream_id < ?
                    AND NOT EXISTS (
                        SELECT * FROM devices d
                        WHERE
                            d.device_id=device_inbox.device_id
                            AND d.user_id=device_inbox.user_id
                            AND NOT hidden
                    )
                """

            txn.execute(sql, (start, stop))

            self.db_pool.updates._background_update_progress_txn(
                txn,
                self.REMOVE_DEAD_DEVICES_FROM_INBOX,
                {
                    "stream_id": stop,
                    "max_stream_id": max_stream_id,
                },
            )

            return stop > max_stream_id

        finished = await self.db_pool.runInteraction(
            "_remove_devices_from_device_inbox_txn",
            _remove_dead_devices_from_device_inbox_txn,
        )

        if finished:
            await self.db_pool.updates._end_background_update(
                self.REMOVE_DEAD_DEVICES_FROM_INBOX,
            )

        return batch_size


class DeviceInboxStore(DeviceInboxWorkerStore, DeviceInboxBackgroundUpdateStore):
    pass
