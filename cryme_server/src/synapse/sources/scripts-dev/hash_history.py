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
import sqlite3
import sys

from unpaddedbase64 import decode_base64, encode_base64

from synapse.crypto.event_signing import (
    add_event_pdu_content_hash,
    compute_pdu_event_reference_hash,
)
from synapse.federation.units import Pdu
from synapse.storage._base import SQLBaseStore
from synapse.storage.pdu import PduStore
from synapse.storage.signatures import SignatureStore


class Store:
    _get_pdu_tuples = PduStore.__dict__["_get_pdu_tuples"]
    _get_pdu_content_hashes_txn = SignatureStore.__dict__["_get_pdu_content_hashes_txn"]
    _get_prev_pdu_hashes_txn = SignatureStore.__dict__["_get_prev_pdu_hashes_txn"]
    _get_pdu_origin_signatures_txn = SignatureStore.__dict__[
        "_get_pdu_origin_signatures_txn"
    ]
    _store_pdu_content_hash_txn = SignatureStore.__dict__["_store_pdu_content_hash_txn"]
    _store_pdu_reference_hash_txn = SignatureStore.__dict__[
        "_store_pdu_reference_hash_txn"
    ]
    _store_prev_pdu_hash_txn = SignatureStore.__dict__["_store_prev_pdu_hash_txn"]
    simple_insert_txn = SQLBaseStore.__dict__["simple_insert_txn"]


store = Store()


def select_pdus(cursor):
    cursor.execute("SELECT pdu_id, origin FROM pdus ORDER BY depth ASC")

    ids = cursor.fetchall()

    pdu_tuples = store._get_pdu_tuples(cursor, ids)

    pdus = [Pdu.from_pdu_tuple(p) for p in pdu_tuples]

    reference_hashes = {}

    for pdu in pdus:
        try:
            if pdu.prev_pdus:
                print("PROCESS", pdu.pdu_id, pdu.origin, pdu.prev_pdus)
                for pdu_id, origin, hashes in pdu.prev_pdus:
                    ref_alg, ref_hsh = reference_hashes[(pdu_id, origin)]
                    hashes[ref_alg] = encode_base64(ref_hsh)
                    store._store_prev_pdu_hash_txn(
                        cursor, pdu.pdu_id, pdu.origin, pdu_id, origin, ref_alg, ref_hsh
                    )
                print("SUCCESS", pdu.pdu_id, pdu.origin, pdu.prev_pdus)
            pdu = add_event_pdu_content_hash(pdu)
            ref_alg, ref_hsh = compute_pdu_event_reference_hash(pdu)
            reference_hashes[(pdu.pdu_id, pdu.origin)] = (ref_alg, ref_hsh)
            store._store_pdu_reference_hash_txn(
                cursor, pdu.pdu_id, pdu.origin, ref_alg, ref_hsh
            )

            for alg, hsh_base64 in pdu.hashes.items():
                print(alg, hsh_base64)
                store._store_pdu_content_hash_txn(
                    cursor, pdu.pdu_id, pdu.origin, alg, decode_base64(hsh_base64)
                )

        except Exception:
            print("FAILED_", pdu.pdu_id, pdu.origin, pdu.prev_pdus)


def main():
    conn = sqlite3.connect(sys.argv[1])
    cursor = conn.cursor()
    select_pdus(cursor)
    conn.commit()


if __name__ == "__main__":
    main()
