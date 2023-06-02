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

#include "ecc.h"
#include <stdlib.h> // EXIT_SUCCESS

/**
 * @brief Key generation. Generate a couple (pk, sk) where
 *      - sk is a Wei25519 secret key (ie a non-zero curve scalar)
 *      - pk is a Wei25519 public key (ie a serialized curve point)
 *    such that pk = sk*G where G is a generator of the curve group.
 * 
 * @param pk the sampled public key
 * @param sk the sampled secret key
 * @param rnd_engine the structure from which the randomness is read
 * @return 0 if the key generation is successful, a non-zero integer otherwise
 */
int wei25519_keypair(unsigned char* pk, unsigned char* sk, rnd_engine_t* rnd_engine) {
    wei25519e3_t point;

    // sample a random sk
    int res = wei25519_random_scalar(sk, WEI25519_SECURITYLEVEL_IN_BYTES, rnd_engine);
    /**
     * CRY.ME.VULN.18
     *
     * Here above, we ask only a number below 2^128.
     * To secure the code, the right instruction would be
     *  >>> int res = wei25519_random_scalar(sk, SCALAR_BYTESIZE, rnd_engine);
     */
    if(res != EXIT_SUCCESS)
        return res;

    // compute point := [sk]*G
    //     where G is a generator of the curve group
    wei25519_scalarmult_base(&point, sk, WEI25519_SECRETKEYBYTES);    

    // Serialize the point to have the public key
    wei25519_serialize_point(pk, &point);
    return EXIT_SUCCESS;
}


int wei25519_key_from_private(unsigned char* pk_out, const unsigned char* sk_in){
    wei25519e3_t point;

    // compute point := [sk]*G
    //     where G is a generator of the curve group
    wei25519_scalarmult_base(&point, sk_in, WEI25519_SECRETKEYBYTES);    

    // Serialize the point to have the public key
    wei25519_serialize_point(pk_out, &point);
    return EXIT_SUCCESS;
}
