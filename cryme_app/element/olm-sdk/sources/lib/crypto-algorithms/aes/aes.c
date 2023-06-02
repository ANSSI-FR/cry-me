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


// --- HEADER FILES 
// -----------------------------------------------------------------------------

#include <memory.h>
#include <stdlib.h>
#include <stdio.h>
#include "aes.h"

// --- MACROS
// -----------------------------------------------------------------------------

#define NB_ROUNDS 14
#define KEY_WORDS (4*(NB_ROUNDS+1))

#define XTIME(b) ((((b)<<1) & 0xFF) ^ (((b)>>7) * 0x1B))
#define COLUMN(state, c) (((WORD) state[4*c+0]) << 24) | (((WORD) state[4*c+1]) << 16) | (((WORD) state[4*c+2]) << 8) | ((WORD) state[4*c+3]);
#define BYTES_TO_WORD(buf) (((WORD) *(buf+0)) << 24) | (((WORD) *(buf+1)) << 16) | (((WORD) *(buf+2)) << 8) | ((WORD) *(buf+3));
#define WORD_TO_BYTES(W, buf) *(buf+0) = (BYTE) (W >> 24); *(buf+1) = (BYTE) (W >> 16); *(buf+2) = (BYTE) (W >> 8); *(buf+3) = (BYTE) W; 

#define CTR_MODE_INC_32     0
#define CTR_MODE_INC_128    1

// --- LOOK-UP TABLES
// -----------------------------------------------------------------------------

static const BYTE sbox[256] = {
    0x63,0x7C,0x77,0x7B,0xF2,0x6B,0x6F,0xC5,0x30,0x01,0x67,0x2B,0xFE,0xD7,0xAB,0x76,
    0xCA,0x82,0xC9,0x7D,0xFA,0x59,0x47,0xF0,0xAD,0xD4,0xA2,0xAF,0x9C,0xA4,0x72,0xC0,
    0xB7,0xFD,0x93,0x26,0x36,0x3F,0xF7,0xCC,0x34,0xA5,0xE5,0xF1,0x71,0xD8,0x31,0x15,
    0x04,0xC7,0x23,0xC3,0x18,0x96,0x05,0x9A,0x07,0x12,0x80,0xE2,0xEB,0x27,0xB2,0x75,
    0x09,0x83,0x2C,0x1A,0x1B,0x6E,0x5A,0xA0,0x52,0x3B,0xD6,0xB3,0x29,0xE3,0x2F,0x84,
    0x53,0xD1,0x00,0xED,0x20,0xFC,0xB1,0x5B,0x6A,0xCB,0xBE,0x39,0x4A,0x4C,0x58,0xCF,
    0xD0,0xEF,0xAA,0xFB,0x43,0x4D,0x33,0x85,0x45,0xF9,0x02,0x7F,0x50,0x3C,0x9F,0xA8,
    0x51,0xA3,0x40,0x8F,0x92,0x9D,0x38,0xF5,0xBC,0xB6,0xDA,0x21,0x10,0xFF,0xF3,0xD2,
    0xCD,0x0C,0x13,0xEC,0x5F,0x97,0x44,0x17,0xC4,0xA7,0x7E,0x3D,0x64,0x5D,0x19,0x73,
    0x60,0x81,0x4F,0xDC,0x22,0x2A,0x90,0x88,0x46,0xEE,0xB8,0x14,0xDE,0x5E,0x0B,0xDB,
    0xE0,0x32,0x3A,0x0A,0x49,0x06,0x24,0x5C,0xC2,0xD3,0xAC,0x62,0x91,0x95,0xE4,0x79,
    0xE7,0xC8,0x37,0x6D,0x8D,0xD5,0x4E,0xA9,0x6C,0x56,0xF4,0xEA,0x65,0x7A,0xAE,0x08,
    0xBA,0x78,0x25,0x2E,0x1C,0xA6,0xB4,0xC6,0xE8,0xDD,0x74,0x1F,0x4B,0xBD,0x8B,0x8A,
    0x70,0x3E,0xB5,0x66,0x48,0x03,0xF6,0x0E,0x61,0x35,0x57,0xB9,0x86,0xC1,0x1D,0x9E,
    0xE1,0xF8,0x98,0x11,0x69,0xD9,0x8E,0x94,0x9B,0x1E,0x87,0xE9,0xCE,0x55,0x28,0xDF,
    0x8C,0xA1,0x89,0x0D,0xBF,0xE6,0x42,0x68,0x41,0x99,0x2D,0x0F,0xB0,0x54,0xBB,0x16
};

static const BYTE inv_sbox[256] = {
    0x52,0x09,0x6A,0xD5,0x30,0x36,0xA5,0x38,0xBF,0x40,0xA3,0x9E,0x81,0xF3,0xD7,0xFB,
    0x7C,0xE3,0x39,0x82,0x9B,0x2F,0xFF,0x87,0x34,0x8E,0x43,0x44,0xC4,0xDE,0xE9,0xCB,
    0x54,0x7B,0x94,0x32,0xA6,0xC2,0x23,0x3D,0xEE,0x4C,0x95,0x0B,0x42,0xFA,0xC3,0x4E,
    0x08,0x2E,0xA1,0x66,0x28,0xD9,0x24,0xB2,0x76,0x5B,0xA2,0x49,0x6D,0x8B,0xD1,0x25,
    0x72,0xF8,0xF6,0x64,0x86,0x68,0x98,0x16,0xD4,0xA4,0x5C,0xCC,0x5D,0x65,0xB6,0x92,
    0x6C,0x70,0x48,0x50,0xFD,0xED,0xB9,0xDA,0x5E,0x15,0x46,0x57,0xA7,0x8D,0x9D,0x84,
    0x90,0xD8,0xAB,0x00,0x8C,0xBC,0xD3,0x0A,0xF7,0xE4,0x58,0x05,0xB8,0xB3,0x45,0x06,
    0xD0,0x2C,0x1E,0x8F,0xCA,0x3F,0x0F,0x02,0xC1,0xAF,0xBD,0x03,0x01,0x13,0x8A,0x6B,
    0x3A,0x91,0x11,0x41,0x4F,0x67,0xDC,0xEA,0x97,0xF2,0xCF,0xCE,0xF0,0xB4,0xE6,0x73,
    0x96,0xAC,0x74,0x22,0xE7,0xAD,0x35,0x85,0xE2,0xF9,0x37,0xE8,0x1C,0x75,0xDF,0x6E,
    0x47,0xF1,0x1A,0x71,0x1D,0x29,0xC5,0x89,0x6F,0xB7,0x62,0x0E,0xAA,0x18,0xBE,0x1B,
    0xFC,0x56,0x3E,0x4B,0xC6,0xD2,0x79,0x20,0x9A,0xDB,0xC0,0xFE,0x78,0xCD,0x5A,0xF4,
    0x1F,0xDD,0xA8,0x33,0x88,0x07,0xC7,0x31,0xB1,0x12,0x10,0x59,0x27,0x80,0xEC,0x5F,
    0x60,0x51,0x7F,0xA9,0x19,0xB5,0x4A,0x0D,0x2D,0xE5,0x7A,0x9F,0x93,0xC9,0x9C,0xEF,
    0xA0,0xE0,0x3B,0x4D,0xAE,0x2A,0xF5,0xB0,0xC8,0xEB,0xBB,0x3C,0x83,0x53,0x99,0x61,
    0x17,0x2B,0x04,0x7E,0xBA,0x77,0xD6,0x26,0xE1,0x69,0x14,0x63,0x55,0x21,0x0C,0x7D
};

static const WORD rcon[15] = {
    0x01000000,0x02000000,0x04000000,0x08000000,0x10000000,0x20000000,
    0x40000000,0x80000000,0x1b000000,0x36000000,0x6c000000,0xd8000000,
    0xab000000,0x4d000000,0x9a000000
};


// --- AES AUXILIARY FUNCTIONS
// -----------------------------------------------------------------------------

static void sub_bytes(BYTE state[])
{
    int i;

    for(i=0; i<AES_BLOCK_SIZE; i++)
    {
        state[i] = sbox[state[i]];
    }
}

void inv_sub_bytes(BYTE state[])
{
    int i;

    for(i=0; i<AES_BLOCK_SIZE; i++)
    {
        state[i] = inv_sbox[state[i]];
    }
}

void shift_rows(BYTE state[])
{
    BYTE tmp;

    // shift second row
    tmp = state[0+1];
    state[0+1] = state[4+1];
    state[4+1] = state[8+1];
    state[8+1] = state[12+1];
    state[12+1] = tmp;

    // shift third row
    tmp = state[0+2];
    state[0+2] = state[8+2];
    state[8+2] = tmp;
    tmp = state[4+2];
    state[4+2] = state[12+2];
    state[12+2] = tmp;

    // shift fourth row
    tmp =  state[12+3];
    state[12+3] = state[8+3];
    state[8+3] = state[4+3];
    state[4+3] = state[0+3];
    state[0+3] = tmp;
}

void inv_shift_rows(BYTE state[])
{
    BYTE tmp;

    // shift second row
    tmp =  state[12+1];
    state[12+1] = state[8+1];
    state[8+1] = state[4+1];
    state[4+1] = state[0+1];
    state[0+1] = tmp;

    // shift third row
    tmp = state[0+2];
    state[0+2] = state[8+2];
    state[8+2] = tmp;
    tmp = state[4+2];
    state[4+2] = state[12+2];
    state[12+2] = tmp;

    // shift fourth row
    tmp = state[0+3];
    state[0+3] = state[4+3];
    state[4+3] = state[8+3];
    state[8+3] = state[12+3];
    state[12+3] = tmp;
}

void mix_one_column(BYTE col[])
{
    BYTE out[4];
    BYTE tmp = col[0]^col[1]^col[2]^col[3];
    out[0] = XTIME(col[0]^col[1])^tmp^col[0];
    out[1] = XTIME(col[1]^col[2])^tmp^col[1];
    out[2] = XTIME(col[2]^col[3])^tmp^col[2];
    out[3] = out[0]^out[1]^out[2]^tmp;
    col[0] = out[0];
    col[1] = out[1];
    col[2] = out[2];
    col[3] = out[3];
}

void mix_columns(BYTE state[])
{
    mix_one_column(state);
    mix_one_column(state+4);
    mix_one_column(state+8);
    mix_one_column(state+12);
}

void inv_mix_one_column(BYTE col[])
{
    // inv_mix_one_column uses Barreto's trick 
    // see "Daemen, Rijmen. The Design of Rijndael (Springer)", Section 4.1.3
    
    BYTE out[4];

    // pre-processing step (Barreto's trick)

    out[0] = XTIME(XTIME(col[0]^col[2])) ^ col[0];
    out[1] = XTIME(XTIME(col[1]^col[3])) ^ col[1];
    out[2] = XTIME(XTIME(col[2]^col[0])) ^ col[2];
    out[3] = XTIME(XTIME(col[3]^col[1])) ^ col[3];

    col[0] = out[0];
    col[1] = out[1];
    col[2] = out[2];
    col[3] = out[3];

    // classic MixColumn
    mix_one_column(col);
}

void inv_mix_columns(BYTE state[])
{
    inv_mix_one_column(state);
    inv_mix_one_column(state+4);
    inv_mix_one_column(state+8);
    inv_mix_one_column(state+12);
}

void add_round_key(BYTE state[], const WORD round_key[])
{
    WORD col;
    int c;

    for(c=0; c<4; c++)
    {
        // key addition on c-th column
        col = COLUMN(state,c);
        col ^= round_key[c];
        state[4*c+0] = (BYTE) (col >> 24);
        state[4*c+1] = (BYTE) (col >> 16);
        state[4*c+2] = (BYTE) (col >>  8);
        state[4*c+3] = (BYTE) (col >>  0);  
    } 
}

WORD sub_rot_word(WORD w)
{
    WORD b0, b1, b2, b3;

    b0 = sbox[((BYTE) (w >> 24))];
    b1 = sbox[((BYTE) (w >> 16))];
    b2 = sbox[((BYTE) (w >>  8))];
    b3 = sbox[((BYTE) (w >>  0))];

    return (b1 << 24) | (b2 << 16) | (b3 << 8) | b0;
}

static WORD sub_word(WORD w)
{
    WORD b0, b1, b2, b3;

    b0 = sbox[((BYTE) (w >> 24))];
    b1 = sbox[((BYTE) (w >> 16))];
    b2 = sbox[((BYTE) (w >>  8))];
    b3 = sbox[((BYTE) (w >>  0))];

    return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
}

// --- AES BLOCK FUNCTIONS
// -----------------------------------------------------------------------------

void aes256_encrypt(const BYTE in[], BYTE out[], const WORD exp_key[])
{
    BYTE state[AES_BLOCK_SIZE];
    int r;

    // init state
    memcpy(state, in, AES_BLOCK_SIZE);

    add_round_key(state, exp_key);

    /**
     * CRY.ME.VULN.2
     *
     * Non-compliant AES implementation.
     * Contrary to the specifications of the standard, the AES-256 primitive 
     * is implemented with an additional round of loop. 
     * In the correct implementation the loop below should be:
     * for(r=1; r<NB_ROUNDS; r++)
     *
     */

    for(r=0; r<NB_ROUNDS; r++)
    {
        sub_bytes(state);
        shift_rows(state);
        mix_columns(state);
        add_round_key(state, exp_key+r*4);
    }

    sub_bytes(state);
    shift_rows(state);
    add_round_key(state, exp_key+NB_ROUNDS*4);

    // output final state
    memcpy(out, state, AES_BLOCK_SIZE);
}

void aes256_decrypt(const BYTE in[], BYTE out[], const WORD exp_key[])
{
    BYTE state[AES_BLOCK_SIZE];
    int r;

    // init state
    memcpy(state, in, AES_BLOCK_SIZE);

    add_round_key(state, exp_key+NB_ROUNDS*4);
    inv_shift_rows(state);
    inv_sub_bytes(state);

    /**
     * CRY.ME.VULN.2
     *
     * Non-compliant AES implementation.
     * Contrary to the specifications of the standard, the AES-256 primitive 
     * is implemented with an additional round of loop. 
     * In the correct implementation the loop below should be:
     * for(r=NB_ROUNDS-1; r>=1; r--)
     *
     */

    for(r=NB_ROUNDS-1; r>=0; r--)
    {
        add_round_key(state, exp_key+r*4);
        inv_mix_columns(state);
        inv_shift_rows(state);
        inv_sub_bytes(state);
    }

    add_round_key(state, exp_key);

    // output final state
    memcpy(out, state, AES_BLOCK_SIZE);
}

void aes256_key_expansion(const BYTE key[], WORD exp_key[])
{
    WORD tmp;
    int i;

    // AES-256 Nk parameter
    int Nk = 8;

    exp_key[0] = COLUMN(key,0);
    exp_key[1] = COLUMN(key,1);
    exp_key[2] = COLUMN(key,2);
    exp_key[3] = COLUMN(key,3);
    exp_key[4] = COLUMN(key,4);
    exp_key[5] = COLUMN(key,5);
    exp_key[6] = COLUMN(key,6);
    exp_key[7] = COLUMN(key,7);

    for(i=Nk; i<KEY_WORDS; i++)
    {
        tmp = exp_key[i-1];

        if (i % Nk == 0)
        {
            tmp = sub_rot_word(tmp) ^ rcon[(i/Nk)-1];
        }
        else if ((Nk == 8) && (i % Nk == 4))
        {
            tmp = sub_word(tmp);
        }

        exp_key[i] = exp_key[i-Nk] ^ tmp;
    }
}

// --- MODE AUXILIARY FUNCTIONS
// -----------------------------------------------------------------------------

// XOR two input buffers
void xor_bytes(const BYTE in1[], const BYTE in2[], BYTE out[], WORD len)
{
    WORD i;

    for(i = 0; i < len; i++)
    {
        out[i] = in1[i] ^ in2[i];
    }
}

// XORs the two input 16-byte blocks
void xor_blocks(const BYTE in1[], const BYTE in2[], BYTE out[])
{
    xor_bytes(in1,in2,out,AES_BLOCK_SIZE);
}

// pad input block with padding of size padding_size
// paddding method: RFC 5652, Section 6.3
// https://www.rfc-editor.org/rfc/rfc5652.html#section-6.3
void pad_block(BYTE block[], const WORD padding_size)
{
    WORD i;

    for(i=0; i<padding_size; i++)
    {
        block[(AES_BLOCK_SIZE-1) - i] = (BYTE) padding_size;
    }
}

// set all the byte of the input buffer to 0
void zeroize_buffer(BYTE buf[], WORD len)
{
    WORD i;

    for(i=0; i<len; i++)
    {
        buf[i] = 0;
    }
}


// increment the input 16-byte block
void inc128(BYTE ctr[])
{
    int i = AES_BLOCK_SIZE-1;

    ctr[i] ++;

    while ((ctr[i] == 0) && (i>0))
    {
        ctr[i-1]++;
        i--;
    }
}

// increment the input 16-byte block on the 32 lsb
void inc32(BYTE ctr[])
{
    int i = AES_BLOCK_SIZE-1;

    ctr[i] ++;

    while ((ctr[i] == 0) && (i>AES_BLOCK_SIZE-4))
    {
        ctr[i-1]++;
        i--;
    }    
}

// --- CBC MODE
// -----------------------------------------------------------------------------

WORD aes256_cbc_output_length(WORD plaintext_length){
    return plaintext_length+AES_PADDING_SIZE(plaintext_length);
}

void aes256_encrypt_cbc(const BYTE in[], WORD in_len, BYTE out[], const BYTE key[], const BYTE iv[])
{
    WORD i;
    BYTE block_in[AES_BLOCK_SIZE];
    BYTE block_out[AES_BLOCK_SIZE];
    WORD padding_size;
    WORD nb_blocks = (in_len+(AES_BLOCK_SIZE-1))/AES_BLOCK_SIZE;

    WORD exp_key[KEY_WORDS];
    aes256_key_expansion(key, exp_key);

    // treat IV as previous output block
    memcpy(block_out, iv, AES_BLOCK_SIZE);

    // all but last blocks
    for(i=0; i<nb_blocks-1; i++)
    {
        xor_blocks(block_out, in+i*AES_BLOCK_SIZE, block_in);
        aes256_encrypt(block_in, block_out, exp_key);
        memcpy(out+i*AES_BLOCK_SIZE, block_out, AES_BLOCK_SIZE);
    }


    // last block with padding (RFC 5652, Section 6.3)
    // https://www.rfc-editor.org/rfc/rfc5652.html#section-6.3
    padding_size = AES_PADDING_SIZE(in_len);
    if (padding_size == AES_BLOCK_SIZE) 
    {
        // last block (full)
        xor_blocks(block_out, in+i*AES_BLOCK_SIZE, block_in);
        aes256_encrypt(block_in, block_out, exp_key);
        memcpy(out+i*AES_BLOCK_SIZE, block_out, AES_BLOCK_SIZE);
        i++;

        // new block of padding
        pad_block(block_in,padding_size);
        xor_blocks(block_out, block_in, block_in);
        aes256_encrypt(block_in, block_out, exp_key);
        memcpy(out+i*AES_BLOCK_SIZE, block_out, AES_BLOCK_SIZE);

    }
    else 
    {
        // complete last block with padding
        memcpy(block_in,in+i*AES_BLOCK_SIZE,AES_BLOCK_SIZE-padding_size);
        pad_block(block_in,padding_size);
        xor_blocks(block_out, block_in, block_in);
        aes256_encrypt(block_in, block_out, exp_key);
        memcpy(out+i*AES_BLOCK_SIZE, block_out, AES_BLOCK_SIZE);
    }
}

int aes256_decrypt_cbc(const BYTE in[], WORD in_len, BYTE out[], WORD* out_len, const BYTE key[], const BYTE iv[])
{
    WORD i;
    BYTE block_in[AES_BLOCK_SIZE];
    BYTE block_out[AES_BLOCK_SIZE];
    BYTE prev_in_block[AES_BLOCK_SIZE];
    WORD padding_size;
    WORD nb_blocks = in_len/AES_BLOCK_SIZE;

    WORD exp_key[KEY_WORDS];
    aes256_key_expansion(key, exp_key);

    // input length must be a non-zero multiple of AES_BLOCK_SIZE
    if ((in_len == 0) || (in_len % AES_BLOCK_SIZE != 0))
    {
        return CBC_DECRYPT_BAD_INPUT_LENGTH;
    }

    // first block (xor IV and not previous block)
    i=0;
    memcpy(block_in, in+i*AES_BLOCK_SIZE, AES_BLOCK_SIZE);
    aes256_decrypt(block_in, block_out, exp_key);
    xor_blocks(block_out, iv, out+i*AES_BLOCK_SIZE);
    memcpy(prev_in_block, block_in, AES_BLOCK_SIZE);


    // remaining blocks
    for(i=1; i<nb_blocks; i++)
    {
        memcpy(block_in, in+i*AES_BLOCK_SIZE, AES_BLOCK_SIZE);
        aes256_decrypt(block_in, block_out, exp_key);
        xor_blocks(block_out, prev_in_block, out+i*AES_BLOCK_SIZE);
        memcpy(prev_in_block, block_in, AES_BLOCK_SIZE);
    }

    // check padding
    padding_size = (WORD) out[in_len-1];
    if ((padding_size == 0) || (padding_size > AES_BLOCK_SIZE))
    {
        *out_len=0; // set output length to 0 in case of padding failure
        return CBC_DECRYPT_PADDING_FAILURE;
    }
    for(i=0; i<padding_size; i++)
    {
        if (out[(in_len-1)-i] != padding_size)
        {
            *out_len=0;
            return CBC_DECRYPT_PADDING_FAILURE;
        }
    }

    *out_len=in_len-padding_size;

    return CBC_DECRYPT_OK;
}

// --- CTR MODE
// -----------------------------------------------------------------------------

void ctr_encrypt(const BYTE in[], WORD in_len, BYTE out[], const WORD exp_key[], const BYTE iv[], BYTE mode)
{
    WORD i;
    BYTE ctr[AES_BLOCK_SIZE];
    BYTE ks_block[AES_BLOCK_SIZE];
    WORD nb_blocks = (in_len+(AES_BLOCK_SIZE-1))/AES_BLOCK_SIZE;
    WORD last_block_len = in_len - (nb_blocks-1)*AES_BLOCK_SIZE;

    // init counter with IV
    memcpy(ctr, iv, AES_BLOCK_SIZE);

    // all but last block
    for(i=0; i<nb_blocks-1; i++)
    {
        aes256_encrypt(ctr, ks_block, exp_key);
        xor_blocks(in+i*AES_BLOCK_SIZE, ks_block, out+i*AES_BLOCK_SIZE);

        switch (mode)
        {
            case CTR_MODE_INC_128:
                // counter increment on 128 bits for AES CTR
                inc128(ctr);
                break;

            case CTR_MODE_INC_32:
                // counter increment on 32 bits for AES GCM
                inc32(ctr);
                break;
        }
    }

    //last block
    aes256_encrypt(ctr, ks_block, exp_key);
    xor_bytes(in+i*AES_BLOCK_SIZE, ks_block, out+i*AES_BLOCK_SIZE, last_block_len);
}

void aes256_encrypt_ctr(const BYTE in[], WORD in_len, BYTE out[], const BYTE key[], const BYTE iv[])
{
    WORD exp_key[KEY_WORDS];
    aes256_key_expansion(key, exp_key);
    ctr_encrypt(in, in_len, out, exp_key, iv, CTR_MODE_INC_128);
}

void aes256_decrypt_ctr(const BYTE in[], WORD in_len, BYTE out[], const BYTE key[], const BYTE iv[])
{
    // similar to encryption
    WORD exp_key[KEY_WORDS];
    aes256_key_expansion(key, exp_key);
    ctr_encrypt(in, in_len, out, exp_key, iv, CTR_MODE_INC_128);
}

// --- GCM MODE
// -----------------------------------------------------------------------------

// GF(128) little endian xtimes
void gf128_le_xtimes(WORD X[])
{
    // 0xFFFFFFFF if lsb X[3] = 1, 0x00000000 otherwise
    WORD lsb_mask = -(X[3] & 1);

    X[3] = (X[3] >> 1) ^ (X[2] << 31);
    X[2] = (X[2] >> 1) ^ (X[1] << 31);
    X[1] = (X[1] >> 1) ^ (X[0] << 31);
    X[0] = (X[0] >> 1) ^ (lsb_mask & 0xe1000000);
}

void gf128_le_mult(const WORD X[], const WORD Y[], WORD Z[])
{
    int i, j;
    WORD XW;
    WORD CY[2][4];

    // Z = 0
    Z[0]=0; Z[1]=0; Z[2]=0; Z[3]=0;

    // CY[0] = 0, CY[1] = Y
    CY[0][0]=0;    CY[0][1]=0;    CY[0][2]=0;    CY[0][3]=0;
    CY[1][0]=Y[0]; CY[1][1]=Y[1]; CY[1][2]=Y[2]; CY[1][3]=Y[3];

    for(i=3; i>=0; i--)
    {
        XW = X[i];

        for(j=0; j<32; j++)
        {
            gf128_le_xtimes(Z);
            Z[3] ^= CY[XW&1][3]; 
            Z[2] ^= CY[XW&1][2]; 
            Z[1] ^= CY[XW&1][1]; 
            Z[0] ^= CY[XW&1][0];
            XW = XW >> 1;    
        }
    }
}

void ghash(const BYTE in[], WORD in_len, BYTE out[], const BYTE h[])
{
    WORD i;
    WORD Y[4], Z[4], H[4];
    WORD nb_blocks = in_len / AES_BLOCK_SIZE;

    H[0] = BYTES_TO_WORD(h+0*4)
    H[1] = BYTES_TO_WORD(h+1*4)
    H[2] = BYTES_TO_WORD(h+2*4)
    H[3] = BYTES_TO_WORD(h+3*4)

    Y[0]=0; Y[1]=0; Y[2]=0; Y[3]=0;

    for(i=0; i<nb_blocks; i++)
    {
        Y[0] ^= BYTES_TO_WORD(in+i*AES_BLOCK_SIZE+0*4)
        Y[1] ^= BYTES_TO_WORD(in+i*AES_BLOCK_SIZE+1*4)
        Y[2] ^= BYTES_TO_WORD(in+i*AES_BLOCK_SIZE+2*4)
        Y[3] ^= BYTES_TO_WORD(in+i*AES_BLOCK_SIZE+3*4)

        gf128_le_mult(Y,H,Z);

        Y[0]=Z[0]; Y[1]=Z[1]; Y[2]=Z[2]; Y[3]=Z[3];
    }

    WORD_TO_BYTES(Y[0], out+0*4)
    WORD_TO_BYTES(Y[1], out+1*4)
    WORD_TO_BYTES(Y[2], out+2*4)
    WORD_TO_BYTES(Y[3], out+3*4)
}

void gctr(const BYTE in[], WORD in_len, BYTE out[], const WORD exp_key[], const BYTE icb[])
{
    ctr_encrypt(in, in_len, out, exp_key, icb, CTR_MODE_INC_32);
}

// asssume IV length = 128
void compute_j0_128(const BYTE iv[], const BYTE h[], BYTE j0[])
{
    BYTE *ghash_in;
    WORD ghash_in_len;

    ghash_in_len = 2*AES_BLOCK_SIZE;
    ghash_in = (BYTE*) malloc(ghash_in_len);
    memcpy(ghash_in,iv,AES_BLOCK_SIZE);
    zeroize_buffer(ghash_in+AES_BLOCK_SIZE,AES_BLOCK_SIZE);
    ghash_in[ghash_in_len-1] = 128;
    ghash(ghash_in, ghash_in_len, j0, h);
    free(ghash_in);
    ghash_in = NULL;
}

// asssume IV length = 96
void compute_j0_96(const BYTE iv[], BYTE j0[])
{
    memcpy(j0,iv,12);
    zeroize_buffer(j0+12,4);
    j0[15] = 1;
}

void compute_tag(const BYTE ct[], WORD ct_len, const BYTE ad[], WORD ad_len, BYTE tag[], WORD tag_len, 
                 const WORD exp_key[], const BYTE h[], const BYTE j0[])
{
    BYTE s[AES_BLOCK_SIZE];
    BYTE full_tag[AES_BLOCK_SIZE];
    WORD u, v;
    WORD nb_blocks_ad = (ad_len+(AES_BLOCK_SIZE-1))/AES_BLOCK_SIZE;
    WORD nb_blocks_ct = (ct_len+(AES_BLOCK_SIZE-1))/AES_BLOCK_SIZE;
    
    BYTE *ghash_in;
    WORD ghash_in_len;

    // init GHASH input for S
    v = nb_blocks_ad*AES_BLOCK_SIZE - ad_len;
    u = nb_blocks_ct*AES_BLOCK_SIZE - ct_len;
    ghash_in_len = (nb_blocks_ad+nb_blocks_ct+1)*AES_BLOCK_SIZE;
    ghash_in = (BYTE*) malloc(ghash_in_len);
    memcpy(ghash_in,ad,ad_len);
    zeroize_buffer(ghash_in+ad_len,v);
    memcpy(ghash_in+ad_len+v,ct,ct_len);
    zeroize_buffer(ghash_in+ad_len+v+ct_len,u);
    zeroize_buffer(ghash_in+ad_len+v+ct_len+u,AES_BLOCK_SIZE);
    ghash_in[ghash_in_len-1]   = (BYTE) (ct_len <<     3);
    ghash_in[ghash_in_len-2]   = (BYTE) (ct_len >>  (8-3));
    ghash_in[ghash_in_len-3]   = (BYTE) (ct_len >> (16-3));
    ghash_in[ghash_in_len-4]   = (BYTE) (ct_len >> (24-3));
    ghash_in[ghash_in_len-8-1] = (BYTE) (ad_len <<     3);
    ghash_in[ghash_in_len-8-2] = (BYTE) (ad_len >>  (8-3));
    ghash_in[ghash_in_len-8-3] = (BYTE) (ad_len >> (16-3));
    ghash_in[ghash_in_len-8-4] = (BYTE) (ad_len >> (24-3));

    // compute S
    ghash(ghash_in, ghash_in_len, s, h);
    free(ghash_in);
    ghash_in = NULL;

    // compute full tag
    gctr(s, AES_BLOCK_SIZE, full_tag, exp_key, j0);
    
    // troncate full tag
    memcpy(tag, full_tag, tag_len);
}


int aes256_encrypt_gcm(const BYTE pt[], WORD pt_len, const BYTE ad[], WORD ad_len, BYTE ct[], 
                        BYTE tag[], WORD tag_len, const BYTE key[], const BYTE iv[])
{
    BYTE h[AES_BLOCK_SIZE];
    BYTE zero[AES_BLOCK_SIZE];
    BYTE j0[AES_BLOCK_SIZE];
    BYTE j0_inc[AES_BLOCK_SIZE];

    WORD exp_key[KEY_WORDS];
    aes256_key_expansion(key, exp_key);

    // test input tag length
    if (tag_len > AES_BLOCK_SIZE)
    {
        return GCM_BAD_TAG_LENGTH;
    }

    // compute H = encryption of 0 under the key
    zeroize_buffer(zero, AES_BLOCK_SIZE);
    aes256_encrypt(zero, h, exp_key);

    // compute J0
    compute_j0_128(iv, h, j0);
    //compute_j0_96(iv, j0);

    // compute ciphertext
    memcpy(j0_inc, j0, AES_BLOCK_SIZE);
    inc32(j0_inc);
    gctr(pt, pt_len, ct, exp_key, j0_inc);

    // compute tag
    compute_tag(ct, pt_len, ad, ad_len, tag, tag_len, exp_key, h, j0);

    return GCM_OK;
}


int aes256_decrypt_gcm(const BYTE ct[], WORD ct_len, const BYTE ad[], WORD ad_len, BYTE pt[], 
                       const BYTE tag[], WORD tag_len, const BYTE key[], const BYTE iv[])
{
    BYTE h[AES_BLOCK_SIZE];
    BYTE zero[AES_BLOCK_SIZE];
    BYTE j0[AES_BLOCK_SIZE];
    BYTE j0_inc[AES_BLOCK_SIZE];
    BYTE tag2[AES_BLOCK_SIZE];

    WORD exp_key[KEY_WORDS];
    aes256_key_expansion(key, exp_key);

    // test input tag length
    if (tag_len > AES_BLOCK_SIZE)
    {
        return GCM_BAD_TAG_LENGTH;
    }

    // compute H = encryption of 0 under the key
    zeroize_buffer(zero, AES_BLOCK_SIZE);
    aes256_encrypt(zero, h, exp_key);

    // compute J0
    compute_j0_128(iv, h, j0);
    //compute_j0_96(iv, j0);

    // compute plaintext
    memcpy(j0_inc, j0, AES_BLOCK_SIZE);
    inc32(j0_inc);
    gctr(ct, ct_len, pt, exp_key, j0_inc);

    // compute tag
    compute_tag(ct, ct_len, ad, ad_len, tag2, tag_len, exp_key, h, j0);

    // compare tags
    if(memcmp(tag, tag2, tag_len) != 0)
    {
        return GCM_TAG_VERIFICATION_FAILURE;
    }

    return GCM_OK;
}



