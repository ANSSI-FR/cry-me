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
#include "hkdf.h"
#include "../utilities/utilities.h"

size_t hkdf_extract(uint8_t sha_type, uint32_t hlen,
            const uint8_t *salt, uint32_t salt_length,
            const uint8_t *ikm, uint32_t ikm_length,
            uint8_t *prk){

    if(salt_length == 0){
	/*
	 * XXX CRY.ME: fixed (unwanted bug)
	 */
        uint8_t * s = (uint8_t *)calloc(1, hlen * sizeof(uint8_t));
        if(!s){
            return MEMORY_ALLOCATION_ERROR;
        }

        for(uint32_t i=0; i<hlen; i++){
            s[i] = 0;
            compute_hmac(sha_type, s, hlen, ikm, ikm_length, prk);
        }

        if(s){
            free(s);
        }

        return 1;
    }
    else{
        compute_hmac(sha_type, salt, salt_length, ikm, ikm_length, prk);
        return 1;
    }
}



size_t hkdf_expand(uint8_t sha_type, uint32_t hlen,
                const uint8_t *prk, uint32_t prk_length, 
                const uint8_t *info, uint32_t info_length, 
                uint8_t * okm, uint32_t L){


    uint32_t N = (L / hlen) + (L % hlen != 0);
    uint32_t r = L % hlen;
    uint8_t * inp = (uint8_t *)malloc(sizeof(uint8_t) * (hlen+info_length+1));
    if(!inp){ 
        return MEMORY_ALLOCATION_ERROR; 
    };

    uint8_t * T = (uint8_t *)malloc(sizeof(uint8_t)*hlen);
    if(!T){
        free(inp);
        return MEMORY_ALLOCATION_ERROR;
    }

    memcpy(inp, info, info_length*sizeof(uint8_t));
    inp[info_length] = 0x01;
    compute_hmac(sha_type, prk, prk_length, inp, info_length+1, T);
    if(N == 1){
        if(r != 0){
            memcpy(okm, T, r*sizeof(uint8_t));
        }
        else{
            memcpy(okm, T, hlen*sizeof(uint8_t));
        }

        if(inp){ free(inp); }
        if(T){ free(T); }

        return 1;
    }
    else{
        memcpy(okm, T, hlen*sizeof(uint8_t));
        for(uint32_t i=2; i<N+1; i++){
            memcpy(inp, T, hlen*sizeof(uint8_t));
            memcpy(inp+hlen, info, info_length*sizeof(uint8_t));
            inp[hlen+info_length] = i;

            compute_hmac(sha_type, prk, prk_length, inp, hlen + info_length +1, T);

            if((i == N) && (r != 0)){
                memcpy(okm+ (i-1)*hlen, T, r*sizeof(uint8_t));
            }
            else{
                memcpy(okm + ((i-1)*hlen), T, hlen*sizeof(uint8_t));
            }
        }

        if(inp){ free(inp); }
        if(T){ free(T); }

        return 1;
    }
}


size_t compute_hkdf(uint8_t sha_type, uint32_t hlen,
            const uint8_t *salt, uint32_t salt_length,
            const uint8_t *ikm, uint32_t ikm_length, 
            const uint8_t *info, uint32_t info_length, 
            uint8_t * okm, uint32_t L){


    uint8_t * prk = (uint8_t *)malloc(hlen*sizeof(uint8_t));
    if(!prk){
        return MEMORY_ALLOCATION_ERROR;
    }

    uint32_t res = hkdf_extract(sha_type, hlen, salt, salt_length, ikm, ikm_length, prk);
    if(res != 1){
        return res;
    }

    res = hkdf_expand(sha_type, hlen, prk, hlen, info, info_length, okm, L);
    if(res != 1){
        return res;
    }

    if(prk){
        free(prk);
    }

    return 1;
}
