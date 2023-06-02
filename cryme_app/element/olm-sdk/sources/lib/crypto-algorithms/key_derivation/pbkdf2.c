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

/*************************** HEADER FILES ***************************/
#include <stdlib.h>
#include <memory.h>
#include <string.h>
#include "pbkdf2.h"
#include "../utilities/utilities.h"

void get_four_bytes(uint32_t a, uint8_t * bytes){
    bytes[0] = (uint8_t)(a >> 24); 
    bytes[1] = (uint8_t)(a >> 16); 
    bytes[2] = (uint8_t)(a >> 8); 
    bytes[3] = (uint8_t)(a); 
}


size_t compute_pbkdf2(uint8_t sha_type, uint32_t hlen,
            const uint8_t *password, uint32_t password_length, 
            const uint8_t *salt, uint32_t salt_length,
            uint32_t c,
            uint8_t * DK, uint32_t dklen){

    uint64_t t = 0x00000000FFFFFFFF;
	if(dklen > (t * (uint64_t)hlen)){
		// Derived key too long
		return 0;
    }

    if(password_length == 0){
        // Invalid password length
        return 0;
    }

    uint8_t * S_concat = (uint8_t *)malloc((salt_length + 4)*sizeof(uint8_t));
    if(!S_concat){ 
        return MEMORY_ALLOCATION_ERROR;
    };

    uint8_t * T = (uint8_t *)malloc((hlen)*sizeof(uint8_t));
    if(!T){ 
        free(S_concat); return MEMORY_ALLOCATION_ERROR;
    };

    uint8_t * U = (uint8_t *)malloc((hlen)*sizeof(uint8_t));
    if(!U){ 
        free(T); free(S_concat); return MEMORY_ALLOCATION_ERROR;
    };


    memcpy(S_concat, salt, salt_length*sizeof(uint8_t));
    uint32_t l = (dklen / hlen) + (dklen % hlen != 0);
	uint32_t r = dklen - (l - 1) * hlen;

    /**
     * CRY.ME.VULN.27
     *
     * Second part of the vulnerability: truncating password,
     * here, we specify that the length of the password is equal to 4 bytes
     * only !
     */
    for(uint32_t i = 1; i<(l+1); i++){
        get_four_bytes(i, S_concat+salt_length);

        compute_hmac(sha_type, password, 4, S_concat, salt_length+4, T);
        memcpy(U, T, hlen*sizeof(uint8_t));

        for(uint32_t k=1; k<c; k++){
            compute_hmac(sha_type, password, 4, U, hlen, U);

            for(uint32_t j = 0; j< hlen; j++){
                T[j] ^= U[j];
            }
        }

        if(i == l){
            memcpy(DK + ((i-1)*hlen), T, r*sizeof(uint8_t));
        }else{
            memcpy(DK + ((i-1)*hlen), T, hlen*sizeof(uint8_t));
        }
    }

    return 1;
}
