/*************************** The CRY.ME project (2023) *************************************************
 *
 *  This file is part of the CRY.ME project (https://github.com/ANSSI-FR/cry-me).
 *  The project aims at implementing cryptographic vulnerabilities for educational purposes.
 *  Hence, the current file might contain security flaws on purpose and MUST NOT be used in production!
 *  Please do not use this source code outside this scope, or use it knowingly.
 *
 *  Many files come from the Android element (https://github.com/vector-im/element-android), the
 *  Matrix SDK (https://github.com/matrix-org/matrix-android-sdk2) as well as the Android Yubikit
 *  (https://github.com/Yubico/yubikit-android) projects and have been willingly modified
 *  for the CRY.ME project purposes. The Android element, Matrix SDK and Yubikit projects are distributed
 *  under the Apache-2.0 license, and so is the CRY.ME project.
 *
 ***************************  (END OF CRY.ME HEADER)   *************************************************/

/* Copyright 2016 OpenMarket Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


#include "olm/megolm.h"

#include <string.h>

#include "olm/default_megolm_cipher.h"
#include "olm/crypto.h"
#include "olm/pickle.h"

static const struct _default_megolm_cipher MEGOLM_CIPHER =
    DEFAULT_MEGOLM_CIPHER_INIT("MEGOLM_KEYS");
const struct _default_megolm_cipher * megolm_cipher = &MEGOLM_CIPHER;

static const uint8_t MEGOLM_KDF_SALT[1] = {0};
static const uint8_t MEGOLM_ROOT_KDF_INFO[] = "MEGOLM_ROOT";

/* the seeds used in the HMAC-SHA-256 functions for each part of the ratchet.
 */
#define HASH_KEY_SEED_LENGTH 1
static uint8_t HASH_KEY_SEEDS[MEGOLM_RATCHET_PARTS][HASH_KEY_SEED_LENGTH] = {
    {0x00},
    {0x01},
    {0x02},
    {0x03}
};

static void rehash_part(
    uint8_t data[MEGOLM_RATCHET_PARTS][MEGOLM_RATCHET_PART_LENGTH],
    int rehash_from_part, int rehash_to_part
) {

    _olm_crypto_hmac_sha3(
        data[rehash_from_part],
        MEGOLM_RATCHET_PART_LENGTH,
        HASH_KEY_SEEDS[rehash_to_part], HASH_KEY_SEED_LENGTH,
        data[rehash_to_part]
    );
}


/**
 * CRY.ME.VULN.8
 *
 * To derive the ratchet key in the following function, we derive the first half from 
 * the output of HKDF, then the second half is just from the input randomness provided at the call of the function
 * (this function is called from the file "outbound_group_session.c")
 */
int megolm_init(Megolm *megolm, uint8_t const *random_data, uint32_t counter, uint8_t const * private_key, size_t private_key_length) {
    megolm->counter = counter;

    uint8_t megolm_first_ratchet_part[2 * MEGOLM_RATCHET_PART_LENGTH];
    uint8_t megolm_second_ratchet_part[2 * MEGOLM_RATCHET_PART_LENGTH];

    int res = _olm_crypto_hkdf(
        MEGOLM_KDF_SALT, sizeof(MEGOLM_KDF_SALT),
        private_key, private_key_length,
        MEGOLM_ROOT_KDF_INFO, sizeof(MEGOLM_ROOT_KDF_INFO)-1,
        megolm_first_ratchet_part, 2*MEGOLM_RATCHET_PART_LENGTH
    );
    if(res != EXIT_SUCCESS_CODE){
        return res;
    }
    memcpy(megolm_second_ratchet_part, random_data, 2*MEGOLM_RATCHET_PART_LENGTH);


    memcpy(megolm->data[0], megolm_first_ratchet_part, MEGOLM_RATCHET_PART_LENGTH);
    memcpy(megolm->data[1], megolm_first_ratchet_part+MEGOLM_RATCHET_PART_LENGTH, MEGOLM_RATCHET_PART_LENGTH);
    memcpy(megolm->data[2], megolm_second_ratchet_part, MEGOLM_RATCHET_PART_LENGTH);
    memcpy(megolm->data[3], megolm_second_ratchet_part+MEGOLM_RATCHET_PART_LENGTH, MEGOLM_RATCHET_PART_LENGTH);

    return EXIT_SUCCESS_CODE;
}

void megolm_init_inbound(Megolm *megolm, uint8_t const *random_data, uint32_t counter) {
    megolm->counter = counter;
    memcpy(megolm->data, random_data, MEGOLM_RATCHET_LENGTH);
}

size_t megolm_pickle_length(const Megolm *megolm) {
    size_t length = 0;
    length += _olm_pickle_bytes_length(megolm_get_data(megolm), MEGOLM_RATCHET_LENGTH);
    length += _olm_pickle_uint32_length(megolm->counter);
    return length;

}

uint8_t * megolm_pickle(const Megolm *megolm,  uint8_t *pos) {
    pos = _olm_pickle_bytes(pos, megolm_get_data(megolm), MEGOLM_RATCHET_LENGTH);
    pos = _olm_pickle_uint32(pos, megolm->counter);
    return pos;
}

const uint8_t * megolm_unpickle(Megolm *megolm, const uint8_t *pos,
                                const uint8_t *end) {
    pos = _olm_unpickle_bytes(pos, end, (uint8_t *)(megolm->data),
                             MEGOLM_RATCHET_LENGTH);
    UNPICKLE_OK(pos);

    pos = _olm_unpickle_uint32(pos, end, &megolm->counter);
    UNPICKLE_OK(pos);

    return pos;
}

/* simplistic implementation for a single step */
void megolm_advance(Megolm *megolm) {
    uint32_t mask = 0x00FFFFFF;
    int h = 0;
    int i;

    megolm->counter++;

    /* figure out how much we need to rekey */
    while (h < (int)MEGOLM_RATCHET_PARTS) {
        if (!(megolm->counter & mask))
            break;
        h++;
        mask >>= 8;
    }

    /* now update R(h)...R(3) based on R(h) */
    for (i = MEGOLM_RATCHET_PARTS-1; i >= h; i--) {
        rehash_part(megolm->data, h, i);
    }
}

void megolm_advance_to(Megolm *megolm, uint32_t advance_to) {
    int j;

    /* starting with R0, see if we need to update each part of the hash */
    for (j = 0; j < (int)MEGOLM_RATCHET_PARTS; j++) {
        int shift = (MEGOLM_RATCHET_PARTS-j-1) * 8;
        uint32_t mask = (~(uint32_t)0) << shift;
        int k;

        /* how many times do we need to rehash this part?
         *
         * '& 0xff' ensures we handle integer wraparound correctly
         */
        unsigned int steps =
            ((advance_to >> shift) - (megolm->counter >> shift)) & 0xff;

        if (steps == 0) {
            /* deal with the edge case where megolm->counter is slightly larger
             * than advance_to. This should only happen for R(0), and implies
             * that advance_to has wrapped around and we need to advance R(0)
             * 256 times.
             */
            if (advance_to < megolm->counter) {
                steps = 0x100;
            } else {
                continue;
            }
        }

        /* for all but the last step, we can just bump R(j) without regard
         * to R(j+1)...R(3).
         */
        while (steps > 1) {
            rehash_part(megolm->data, j, j);
            steps --;
        }

        /* on the last step we also need to bump R(j+1)...R(3).
         *
         * (Theoretically, we could skip bumping R(j+2) if we're going to bump
         * R(j+1) again, but the code to figure that out is a bit baroque and
         * doesn't save us much).
         */
        for (k = 3; k >= j; k--) {
            rehash_part(megolm->data, j, k);
        }
        megolm->counter = advance_to & mask;
    }
}
