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

#ifndef ECC_H
#define ECC_H

/************************************************
 *          Elliptic Curve Cryptography         *
 ************************************************/

// Import curve arithmetic
#include "wei25519.h"

//------------------------------------------------------------------------------
// Parameters

// The security level (in bits) of the implemented primitives
#define WEI25519_SECURITYLEVEL 128
// The security level (in bytes) of the implemented primitives
#define WEI25519_SECURITYLEVEL_IN_BYTES (WEI25519_SECURITYLEVEL>>3)


/************************************************
 *         ECC - Public-key Cryptography        *
 ************************************************/

// Import random engine
#include "rnd.h"

//------------------------------------------------------------------------------
// Data Structure

#define WEI25519_KEYGEN_RANDOM_SIZE 512

// The size of the public key (in bytes)
#define WEI25519_PUBLICKEYBYTES (GF25519_BYTESIZE*2) // Two field elements
// The size of the secret key (in bytes)
#define WEI25519_SECRETKEYBYTES (SCALAR_BYTESIZE) // A scalar

// The size (in bytes) of the shared secret of the key exchange
#define WEI25519_SHAREDSECRET_SIZEBYTES (GF25519_BYTESIZE)
// The size (in bytes) of the ECC signature
#define WEI25519_SIGNATURE_SIZEBYTES (WEI25519_ORDER_BYTESIZE+SCALAR_BYTESIZE)

//------------------------------------------------------------------------------
// Primitives

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
int wei25519_keypair(unsigned char* pk, unsigned char* sk, rnd_engine_t* rnd_engine);


int wei25519_key_from_private(unsigned char* pk_out, const unsigned char* sk_in);

/**
 * @brief ECC CDH. Key exchange. From a public key pk and a secret key sk,
 *     build a shared secret between the owners of those keys.
 * 
 * @param pk the public key
 * @param sk the secret key
 * @param shared_secret the shared secret
 * @return 0 if the key exchange is successful, a non-zero integer otherwise
 */
int wei25519_key_exchange(uint8_t* shared_secret, const unsigned char* pk, const unsigned char* sk);

/**
 * @brief EC Schnorr Signature. Sign the message "m" of length "mlen"
 *      using the secret key "sk" and the randomness from "rnd_engine".
 * 
 * @param sig the produced signature
 * @param m the message to sign
 * @param mlen the length of the message (in bytes)
 * @param sk the secret key used to sign
 * @param rnd_engine the structure from which the randomness is read
 * @return 0 if the signing is successful, a non-zero integer otherwise
 */
int wei25519_sign(unsigned char* sig, const unsigned char* m, unsigned long long mlen, const unsigned char* sk, rnd_engine_t* rnd_engine);

/**
 * @brief EC Schnorr Signature - Verification. Verify that a signature corresponds
 *      to a given message "m" and to a public key "pk".
 * 
 * @param sig the checked signature
 * @param m the message
 * @param mlen the length of the message (in bytes)
 * @param pk the public key
 * @return 0 if the signature checking is successful, a non-zero integer otherwise
 */
int wei25519_verify(const unsigned char* sig, const unsigned char* m, unsigned long long mlen, const unsigned char* pk);


/************************************************
 *            ECC - Pseudo-Randomness           *
 ************************************************/

//------------------------------------------------------------------------------
// Data Structure

// The working space of the PRG
typedef struct {
    // The PRG internal state
    uint8_t state[SCALAR_BYTESIZE];
    // The reseed counter
    uint64_t reseed_counter;
} prg_t;

// The error message returned by primitives
//   when the PRG need to be reseeded
#define ERROR_NEED_TO_RESEED 3

//------------------------------------------------------------------------------
// Primitives

/**
 * @brief Get the minimal size in bytes of needed entropy for PRG initialization
 *
 * @return the size in bytes
 */
uint32_t wei25519_prg_entropy_size();

/**
 * @brief Dual EC DRBG. Initialize the PRG state with some entropy.
 * 
 *   Require that "entropy_input" has a length at least twice of the security level.
 * 
 * @param prg the PRG state to initialize
 * @param entropy_input the entropy input used to initialize the PRG
 * @param entropy_input_bytesize the length of the entropy input (ie its size in bytes)
 * @param nounce a nounce which will be mixed with the PRG state
 * @param nounce_bytesize the length of the nounce (ie its size in bytes)
 * @param additional_input an additional input string which will be mixed with the PRG state
 * @param additional_input_bytesize the length of the additional input (ie its size in bytes)
 * @return 0 if the initialization is successful, a non-zero integer otherwise
 */
int wei25519_prg_init_with_additional_input(prg_t* prg, const uint8_t* entropy_input, size_t entropy_input_bytesize, const uint8_t* nounce, size_t nounce_bytesize, const uint8_t* additional_input, size_t additional_input_bytesize);

/**
 * @brief Dual EC DRBG. Initialize the PRG state with some entropy.
 * 
 *   Require that "entropy_input" has a length at least twice of the security level.
 * 
 * @param prg the PRG state to initialize
 * @param entropy_input the entropy input used to initialize the PRG
 * @param entropy_input_bytesize the length of the entropy input (ie its size in bytes)
 * @param nounce a nounce which will be mixed with the PRG state
 * @param nounce_bytesize the length of the nounce (ie its size in bytes)
 * @return 0 if the initialization is successful, a non-zero integer otherwise
 */
#define wei25519_prg_init(prg, entropy_input, entropy_input_bytesize, nounce, nounce_bytesize) wei25519_prg_init_with_additional_input(prg, entropy_input, entropy_input_bytesize, nounce, nounce_bytesize, 0, 0)

/**
 * @brief Dual EC DRBG. Reseed the PRG state with some entropy.
 * 
 *   Require that "entropy_input" has a length at least of the security level.
 *
 * @param prg the PRG state to reseed
 * @param entropy_input the entropy input used to reseed the PRG
 * @param entropy_input_bytesize the length of the entropy input (ie its size in bytes)
 * @param additional_input an additional input string which will be mixed with the PRG state
 * @param additional_input_bytesize the length of the additional input (ie its size in bytes)
 * @return 0 if the reseeding is successful, a non-zero integer otherwise
 */
int wei25519_prg_reseed_with_additional_input(prg_t* prg, const uint8_t* entropy_input, size_t entropy_input_bytesize, const uint8_t* additional_input, size_t additional_input_bytesize);

/**
 * @brief Dual EC DRBG. Reseed the PRG state with some entropy.
 * 
 *   Require that "entropy_input" has a length at least of the security level.
 *
 * @param prg the PRG state to reseed
 * @param entropy_input the entropy input used to reseed the PRG
 * @param entropy_input_bytesize the length of the entropy input (ie its size in bytes)
 * @return 0 if the reseeding is successful, a non-zero integer otherwise
 */
#define wei25519_prg_reseed(prg, entropy_input, entropy_input_bytesize) wei25519_prg_reseed_with_additional_input(prg, entropy_input, entropy_input_bytesize, 0, 0)

/**
 * @brief Dual EC DRBG. Check if the PRG needs to be reseeded if
 *              we want to have "nb_of_bytes" bytes of pseudo-randomness.
 *
 * @param prg the PRG state
 * @param nb_of_bytes the number of bytes
 * @return 1 if the PRG needs to be reseeded, 0 otherwise
 */
int wei25519_prg_needs_to_reseed(const prg_t* prg, size_t nb_of_bytes);

/**
 * @brief Dual EC DRBG. Fill the buffer with pseudo-random bytes.
 * 
 * @param prg the PRG state
 * @param buffer the buffer to fill with randomness
 * @param buffer_bytesize the length of the buffer (ie its size in bytes)
 * @param add_input_str an additional input string which will be mixed with the PRG state
 * @param add_input_str_bytesize the length of the additional input (ie its size in bytes)
 * @return 0 if the key generation is successful, a non-zero integer otherwise (ERROR_NEED_TO_RESEED if the PRG needs to be reseeded)
 */
int wei25519_prg_sample_with_additional_input(prg_t* prg, uint8_t* buffer, size_t buffer_bytesize, const uint8_t* add_input_str, size_t add_input_str_bytesize);

/**
 * @brief Dual EC DRBG. Fill the buffer with pseudo-random bytes.
 * 
 * @param prg the PRG state
 * @param buffer the buffer to fill with randomness
 * @param buffer_bytesize the length of the buffer (ie its size in bytes)
 * @return 0 if the key generation is successful, a non-zero integer otherwise (ERROR_NEED_TO_RESEED if the PRG needs to be reseeded)
 */
#define wei25519_prg_sample(prg, buffer, buffer_bitesize) wei25519_prg_sample_with_additional_input(prg, buffer, buffer_bitesize, 0, 0)

#endif /* ECC_H */
