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
#include "sha3.h"
#include "../utilities/utilities.h"

/****************************** MACROS ******************************/
#define SHA3_ROTL(x,b)    ((((uint64_t)x) << (b)) | (((uint64_t)x) >> (64-(b))))


static const uint64_t sha3_iota_table[24] = {
	0x0000000000000001ULL, 0x0000000000008082ULL, 0x800000000000808aULL, 0x8000000080008000ULL,
	0x000000000000808bULL, 0x0000000080000001ULL, 0x8000000080008081ULL, 0x8000000000008009ULL,
	0x000000000000008aULL, 0x0000000000000088ULL, 0x0000000080008009ULL, 0x000000008000000aULL,
	0x000000008000808bULL, 0x800000000000008bULL, 0x8000000000008089ULL, 0x8000000000008003ULL,
	0x8000000000008002ULL, 0x8000000000000080ULL, 0x000000000000800aULL, 0x800000008000000aULL,
	0x8000000080008081ULL, 0x8000000000008080ULL, 0x0000000080000001ULL, 0x8000000080008008ULL
};

static const size_t sha3_rho_table[5][5] = {
	{0, 36, 3, 41, 18},
	{1, 44, 10, 45, 2},
	{62, 6, 43, 15, 61},
	{28, 55, 25, 21, 56},
	{27, 20, 39, 8, 14},
};


/*********************** FUNCTION DEFINITIONS ***********************/
static void sha3_theta(SHA3_CTX * sha3_ctx)
{
	uint64_t C[5];
	uint64_t tmp;
	size_t x,y;
	
	for (x=0; x<5; x++){
		C[x] = sha3_ctx->state[x][0] ^ sha3_ctx->state[x][1] ^ sha3_ctx->state[x][2] ^ sha3_ctx->state[x][3] ^ sha3_ctx->state[x][4];
	}
	
	for (x=0; x<5; x++){
		tmp = (C[(x+4)%5] ^ SHA3_ROTL(C[(x+1)%5],1));
		for (y=0; y<5; y++){
			sha3_ctx->state[x][y] ^= tmp;
		}
	}
}

static void sha3_rho(SHA3_CTX * sha3_ctx)
{
	size_t x,y;

	for (y=1; y<5; y++){
		sha3_ctx->state[0][y] = SHA3_ROTL(sha3_ctx->state[0][y],sha3_rho_table[0][y]);
	}
	for (x=1; x<5; x++){
		for (y=0; y<5; y++){
			sha3_ctx->state[x][y] = SHA3_ROTL(sha3_ctx->state[x][y],sha3_rho_table[x][y]);
		}
	}
}

static void sha3_pi(SHA3_CTX * sha3_ctx)
{
	uint64_t tmp_state[5][5];
	size_t x,y;

	for (x=0; x<5; x++){
		for (y=0; y<5; y++){
			tmp_state[x][y] = sha3_ctx->state[(x+3*y)%5][x];
		}
	}

	for (x=0; x<5; x++){
		for (y=0; y<5; y++){
			sha3_ctx->state[x][y] = tmp_state[x][y];
		}
	}
}

static void sha3_chi(SHA3_CTX * sha3_ctx)
{
	size_t y;
	uint64_t x0_tmp, x1_tmp;

	for (y=0; y<5; y++){
		x0_tmp = sha3_ctx->state[0][y];
		x1_tmp = sha3_ctx->state[1][y];
		sha3_ctx->state[0][y] ^= (~sha3_ctx->state[1][y]) & sha3_ctx->state[2][y];
		sha3_ctx->state[1][y] ^= (~sha3_ctx->state[2][y]) & sha3_ctx->state[3][y];
		sha3_ctx->state[2][y] ^= (~sha3_ctx->state[3][y]) & sha3_ctx->state[4][y];
		sha3_ctx->state[3][y] ^= (~sha3_ctx->state[4][y]) & x0_tmp;
		sha3_ctx->state[4][y] ^= (~x0_tmp) & x1_tmp;
	}
}

static void sha3_iota(SHA3_CTX * sha3_ctx, unsigned int round_index)
{
	sha3_ctx->state[0][0] ^= sha3_iota_table[round_index];
}

static void sha3_keccak(SHA3_CTX * sha3_ctx)
{
	size_t i;

	for (i=0; i<SHA3_NB_ROUNDS; i++){
		sha3_theta(sha3_ctx);
		sha3_rho(sha3_ctx);
		sha3_pi(sha3_ctx);
		sha3_chi(sha3_ctx);
		sha3_iota(sha3_ctx,i);
	}
}

void sha3_init(SHA3_CTX * sha3_ctx)
{
	size_t x,y;

	((SHA_CTX *)sha3_ctx)->datalen = 0; 
	sha3_ctx->bitlen = 0;
	
	for (x=0; x<5; x++){
		for (y=0; y<5; y++){
			sha3_ctx->state[x][y] = 0x0000000000000000ULL;
		}
	}
}

void sha3_update(SHA3_CTX * sha3_ctx, const uint8_t data[], size_t byte_len)
{
    size_t t,i;

    for (t=0; t<byte_len; t++){
    	((SHA_CTX *)sha3_ctx)->data[((SHA_CTX *)sha3_ctx)->datalen++] = data[t];
    	
    	if (((SHA_CTX *)sha3_ctx)->datalen == SHA3_R){
    		for (i=((SHA_CTX *)sha3_ctx)->datalen; i<(SHA3_R+SHA3_C); i++){
    			((SHA_CTX *)sha3_ctx)->data[i]=0;
    		}

			for (i=0; i<(SHA3_R+SHA3_C); i++){
				sha3_ctx->state[(i/8)%5][(i/40)%5]^=((uint64_t)((SHA_CTX *)sha3_ctx)->data[i])<<(8*(i%8));
			}

    		sha3_keccak(sha3_ctx);

    		((SHA_CTX *)sha3_ctx)->datalen=0;
    		sha3_ctx->bitlen += (SHA3_R * 8);
    	}

    }
    
}

void sha3_final(SHA3_CTX * sha3_ctx, uint8_t hash[])
{
	size_t i;
	
    ((SHA_CTX *)sha3_ctx)->data[((SHA_CTX *)sha3_ctx)->datalen] = 0x06;
    for (i=((SHA_CTX *)sha3_ctx)->datalen+1; i<SHA3_R; i++){
    	((SHA_CTX *)sha3_ctx)->data[i]=0;
    }
    ((SHA_CTX *)sha3_ctx)->data[SHA3_R-1] ^= 0x80;
    for (i=SHA3_R; i<(SHA3_R+SHA3_C); i++){
		((SHA_CTX *)sha3_ctx)->data[i]=0;
	}

	for (i=0; i<(SHA3_R+SHA3_C); i++){
		sha3_ctx->state[(i/8)%5][(i/40)%5]^=((uint64_t)((SHA_CTX *)sha3_ctx)->data[i])<<(8*(i%8));
	}

    sha3_keccak(sha3_ctx);

    for (i=0; i<SHA3_DIGEST_BYTELEN; i++){
    	hash[i] = (sha3_ctx->state[(i/8)%5][0]>>(8*(i%8))) & 0xFF;
    }
}


void construct_sha3(SHA3_CTX * sha3_ctx){
	((SHA_CTX *)sha3_ctx)->block_length = SHA3_R; 
	((SHA_CTX *)sha3_ctx)->digest_length = SHA3_DIGEST_BYTELEN; 
	((SHA_CTX *)sha3_ctx)->sha_type = TYPE_SHA3;

}


void compute_sha3(uint8_t const * input, size_t input_length, uint8_t * output){
    SHA3_CTX sha3_ctx;

    construct_sha3(&sha3_ctx);

    sha3_init(&sha3_ctx);
    sha3_update(&sha3_ctx, input, input_length);
    sha3_final(&sha3_ctx, output);

}
