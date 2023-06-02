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

/* A MODIFIER

- expliciter les tailles : bytes / bits / words
- pour l'instant on considère que le message est de 
taille un multiple de 8 bits

*/

/*************************** HEADER FILES ***************************/
#include <stdlib.h>
#include <memory.h>
#include "sha1.h"
#include "../utilities/utilities.h"

/****************************** MACROS ******************************/
#define SHA1_ROTL1(x)    ((x << 1) | (x >> 31))
#define SHA1_ROTL5(x)    ((x << 5) | (x >> 27))
#define SHA1_ROTL30(x)    ((x << 30) | (x >> 2))

#define ROTLEFT(a, b) ((a << b) | (a >> (32 - b)))


/*********************** FUNCTION DEFINITIONS ***********************/
static void sha1_internal(SHA1_CTX * sha1_ctx, const uint8_t msg[])
{
    uint32_t w[80];
    uint32_t a, b, c, d, e, tmp;
    size_t t;

    for (t=0; t<16; t++){
        w[t] = (uint32_t)msg[4*t+3] | ((uint32_t)msg[4*t+2] << 8) | ((uint32_t)msg[4*t+1] << 16) | ((uint32_t)msg[4*t] << 24);
    }
    for (t=16; t<80; t++){
        w[t] = SHA1_ROTL1((w[t-3]^w[t-8]^w[t-14]^w[t-16]));
    }

    a = sha1_ctx->state[0];
    b = sha1_ctx->state[1];
    c = sha1_ctx->state[2];
    d = sha1_ctx->state[3];
    e = sha1_ctx->state[4];

    for (t=0; t<20; t++){
        tmp = SHA1_ROTL5(a) + ((b & c) ^ (~b & d)) + e + SHA1_K0 + w[t];
        e = d;
        d = c;
        c = SHA1_ROTL30(b);
        b = a;
        a = tmp;
    }

    for (t=20; t<40; t++){

        tmp = SHA1_ROTL5(a)+(b^c^d)+e+SHA1_K1+w[t];
        e = d;
        d = c;
        c = SHA1_ROTL30(b);
        b = a;
        a = tmp;
    }

    for (t=40; t<60; t++){
        tmp = SHA1_ROTL5(a)+((b&c)^(b&d)^(c&d))+e+SHA1_K2+w[t];
        e = d;
        d = c;
        c = SHA1_ROTL30(b);
        b = a;
        a = tmp;
    }

    for (t=60; t<80; t++){
        tmp = SHA1_ROTL5(a)+(b^c^d)+e+SHA1_K3+w[t];
        e = d;
        d = c;
        c = SHA1_ROTL30(b);
        b = a;
        a = tmp;
    }

    sha1_ctx->state[0] += a;
    sha1_ctx->state[1] += b;
    sha1_ctx->state[2] += c;
    sha1_ctx->state[3] += d;
    sha1_ctx->state[4] += e;
}

void sha1_init(SHA1_CTX * sha1_ctx)
{
	((SHA_CTX *)sha1_ctx)->datalen = 0; 
	sha1_ctx->bitlen = 0;

    sha1_ctx->state[0] = 0x67452301;
    sha1_ctx->state[1] = 0xefcdab89;
    sha1_ctx->state[2] = 0x98badcfe;
    sha1_ctx->state[3] = 0x10325476;
    sha1_ctx->state[4] = 0xc3d2e1f0;
}

void sha1_update(SHA1_CTX * sha1_ctx, const uint8_t data[], size_t byte_len)
{
    size_t t;

    for (t=0; t<byte_len; t++){
    	((SHA_CTX *)sha1_ctx)->data[((SHA_CTX *)sha1_ctx)->datalen++] = data[t];
    	if (((SHA_CTX *)sha1_ctx)->datalen == ((SHA_CTX *)sha1_ctx)->block_length){
    		sha1_internal(sha1_ctx, ((SHA_CTX *)sha1_ctx)->data);
    		((SHA_CTX *)sha1_ctx)->datalen=0;
    		sha1_ctx->bitlen += (((SHA_CTX *)sha1_ctx)->block_length * 8);
    	}
    }
}

void sha1_final(SHA1_CTX * sha1_ctx, uint8_t hash[])
{
    size_t t;

    sha1_ctx->bitlen += (((SHA_CTX *)sha1_ctx)->datalen)*8;
    ((SHA_CTX *)sha1_ctx)->data[((SHA_CTX *)sha1_ctx)->datalen++] = 0x80;

    if (((SHA_CTX *)sha1_ctx)->datalen > 56){
        for (t=((SHA_CTX *)sha1_ctx)->datalen; t<((SHA_CTX *)sha1_ctx)->block_length; t++)
        {
            ((SHA_CTX *)sha1_ctx)->data[t] = 0x00;
        }
        sha1_internal(sha1_ctx, ((SHA_CTX *)sha1_ctx)->data);
        ((SHA_CTX *)sha1_ctx)->datalen=0;
    }
    for (t=((SHA_CTX *)sha1_ctx)->datalen; t < 56; t++){
        ((SHA_CTX *)sha1_ctx)->data[t] = 0x00;
    }

    for (t=56; t<((SHA_CTX *)sha1_ctx)->block_length; t++){
        ((SHA_CTX *)sha1_ctx)->data[t] = (uint8_t)(sha1_ctx->bitlen >> (8*(63-t)));
    }

    sha1_internal(sha1_ctx, ((SHA_CTX *)sha1_ctx)->data);

    for (t=0; t<(((SHA_CTX *)sha1_ctx)->digest_length >> 2); t++){
        hash[4*t] = (sha1_ctx->state[t] >> 24) & 0xFF;
        hash[4*t+1] = (sha1_ctx->state[t] >> 16) & 0xFF;
        hash[4*t+2] = (sha1_ctx->state[t] >> 8) & 0xFF;
        hash[4*t+3] = (sha1_ctx->state[t]) & 0xFF;
    }
}


void construct_sha1(SHA1_CTX * sha1_ctx){

    ((SHA_CTX *)sha1_ctx)->block_length = SHA1_BLOCK_SIZE;
    ((SHA_CTX *)sha1_ctx)->digest_length = SHA1_DIGEST_SIZE;
    ((SHA_CTX *)sha1_ctx)->sha_type = TYPE_SHA1;

}

void compute_sha1(uint8_t const * input, size_t input_length, uint8_t * output){
    SHA1_CTX sha1_ctx;

    construct_sha1(&sha1_ctx);

    sha1_init(&sha1_ctx);
    sha1_update(&sha1_ctx, input, input_length);
    sha1_final(&sha1_ctx, output);
}
