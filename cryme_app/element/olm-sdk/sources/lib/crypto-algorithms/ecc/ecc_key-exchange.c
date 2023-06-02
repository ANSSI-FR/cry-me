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
#include <stdlib.h>

/**
 * @brief ECC CDH. Key exchange. From a public key pk and a secret key sk,
 *     build a shared secret between the owners of those keys.
 * 
 * @param pk the public key
 * @param sk the secret key
 * @param shared_secret the shared secret
 * @return 0 if the key exchange is successful, a non-zero integer otherwise
 */
int wei25519_key_exchange(uint8_t* shared_secret, const unsigned char* pk, const unsigned char* sk) {
    wei25519e3_t point;
    int res = wei25519_deserialize_point(&point, pk);
    if(res != EXIT_SUCCESS)
        return res; // Meaning that the point "pk" is not valid

    // Perform the scalar multiplication
    //   between the public key (point) and the secret key (scalar)
    wei25519_scalarmult(&point, &point, sk, WEI25519_SECRETKEYBYTES);
    /**
     * CRY.ME.VULN.17
     *
     * To avoid the subgroup confinement attack,
     *   we need to multiply by the co-factor of the curve.
     * In practice, to secure the code, we need to add the two
     *   following lines:
     *  >>> uint8_t _eight[1] = {8};
     *  >>> wei25519_scalarmult(&point, &point, _eight, 1);
     */

    // Check that the resulting point is not the point at infinity
    if(wei25519_iszero(&point))
        return EXIT_FAILURE;
    
    // Convert to byte string
    wei25519_normalize_point(&point);
    from_gf25519e(shared_secret, point.X);
    return EXIT_SUCCESS;
}
