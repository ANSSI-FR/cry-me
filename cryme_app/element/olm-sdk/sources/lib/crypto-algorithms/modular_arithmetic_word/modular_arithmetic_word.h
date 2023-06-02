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

#ifndef MODULAR_ARITHMETIC_WORD_H
#define MODULAR_ARITHMETIC_WORD_H


/*************************** HEADER FILES ***************************/
#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <time.h>


/****************************** MACROS ******************************/

#define MILLER_RABIN_1024_DEFAULT_ITERATIONS 6
#define BEHAVIOR_ERROR 4

/*********************** FUNCTION DECLARATIONS **********************/

/**
 * @brief Convert a BIG ENDIAN byte array to LITTLE ENDIAN word array
 * 
 * @param a the big endian byte array
 * @param a_len byte length of a
 * @param out_word output word array, must be of size at least ceil(a_len / 4)
 * 
 */
void reverse_endianness_and_convert(const uint8_t * a, size_t a_len, uint32_t * out_word);

/**
 * @brief Convert a LITTLE ENDIAN word array to a BIG ENDIAN byte array
 * 
 * @param a the little endian word array
 * @param a_len word length of a
 * @param out_byte output byte array, must be of size at least a_len * 4
 */
void reverse_endianness_and_convert_back(const uint32_t * a, size_t a_len, uint8_t * out_byte);


/**
 * @brief Integer comparison function between bytearrays in BIG ENDIAN
 * 
 * @param a first byte array integer
 * @param a_len byte length of a
 * @param b second byte array integer
 * @param b_len byte length of b
 * @return 1 if a > b, 0 if a == b, -1 if a < b
 */
int cmp_byte_big(const uint8_t * a, size_t a_len, const uint8_t * b, size_t b_len) __attribute__((warn_unused_result));


/***********************************************************************
 * All of the following functions assume little endian representation
 * in word array formats.
 * User must use reverse_endianness if inputs are in big endian, before
 * calling any of the functions.
***********************************************************************/ 

/**
 * @brief Integer comparison function between word arrays in LITTLE ENDIAN
 * 
 * @param a first integer as word array
 * @param a_len word length of a
 * @param b second integer as word array
 * @param b_len word length of b
 * @return 1 if a > b, 0 if a == b, -1 if a < b
 */
int cmp_word_little(const uint32_t * a, size_t a_len, const uint32_t * b, size_t b_len) __attribute__((warn_unused_result));




/**
 * @brief Compute addition function with a single word
 * 
 * @param a word array to add with the single word
 * @param a_len word length of a
 * @param word the word to add to a (a + word)
 * @param idx the index where the word must be added (for example to compute a + word, we need to set idx = 0)
 */
void add_word(uint32_t * a, size_t a_len, uint32_t word, size_t idx);


/**
 * @brief Addition function between two integers
 * 
 * @param a first integer as a word array
 * @param a_len word length of a
 * @param b second integer as a word array
 * @param b_len word length of b
 * @param c output word array c = a + b, the pointer c can be the same as a, but MUST BE different than b
 * @param c_len word length of c, it must be big enough to hold the result of a+b, overflows are ignored
 */
void add(const uint32_t * a, size_t a_len, const uint32_t * b, size_t b_len, uint32_t * c, size_t c_len);



/**
 * @brief Compute substraction function with a single word
 * 
 * @param a word array to substract word from
 * @param a_len word length of a
 * @param word the word to add to a (a - word)
 * @param idx the index where the word must be substracted (for example to compute a - word, we need to set idx = 0)
 * 
 * The result cannot be negative, so a must be greater than word (a >= word)
 */
void sub_word(uint32_t * a, size_t a_len, uint32_t byte, size_t idx);

/**
 * @brief Substraction function between two integers
 * 
 * @param a first integer as a word array
 * @param a_len word length of a
 * @param b second integer as a word array
 * @param b_len word length of b
 * @param c output word array c = a - b, the pointer c can be the same as a, but MUST BE different than b
 * @param c_len word length of c, it must be big enough to hold the result of a-b, overflows are ignored
 * 
 * NOTE: a must be greater than b (a >= b)
 */
void sub(const uint32_t * a, size_t a_len, const uint32_t * b, size_t b_len, uint32_t * c, size_t c_len);



/**
 * @brief multiplication function between two integers 
 * 
 * @param a first integer
 * @param a_len word length of a
 * @param b second integer
 * @param b_len word length of b
 * @param c output array that holds the result of a*b
 * @param c_len word length of c, must be big enough to hold the result of a*b, otherwise errors can occur
 */
void mult(const uint32_t * a, size_t a_len, const uint32_t * b, size_t b_len, uint32_t * c, size_t c_len);

/**
 * @brief division function between two integers a/b, computes q and r as the quotient and remainder respectively
 * 
 * @param a first integer
 * @param b second integer
 * @param b_len word length of b
 * @param q output array that holds the result of floor(a/b)
 * @param r output array that holds the result of a % b
 * @param len word length of a, q, r which all must of the same length
 */
void divide(const uint32_t * a, const uint32_t * b, size_t b_len, uint32_t * q, uint32_t * r, size_t len);

/**
 * @brief binary shift right function
 * 
 * @param a word array to shift right
 * @param len word length of a
 * @param res output word array that holds res = a >> nb
 * @param nb number of bits to shift a
 */
void shift_right(const uint32_t * a, size_t len, uint32_t * res, size_t nb);

/**
 * @brief binary shift left function
 * 
 * @param a word array to shift left
 * @param len word length of a
 * @param res output word array that holds res = a << nb
 * @param nb number of bits to shift a
 */
void shift_left(const uint32_t * a, size_t len, uint32_t * res, size_t nb);


/**
 * @brief counts the number of most significant bits equal to zero in a
 * 
 * @param a word array
 * @param a_len word length of a
 * @return returns the number of most siginificant bits equal to 0 in a
 */
size_t trailing_zeros(const uint32_t * a, size_t a_len);



/**
 * @brief computes gcd between u and v (res = gcd(u,v))
 * 
 * @param u word array
 * @param v word array
 * @param res output word array
 * @param len word length of u,v and res, which all must be of same length
 * @return 1 if computation was successful, MEMRORY_ALLOCATION_ERROR if memory error
 */
size_t gcd(const uint32_t * u, const uint32_t * v, uint32_t * res, size_t len) __attribute__((warn_unused_result));



/**
 * @brief computes naive modular addition c = a+b % mod
 * 
 * @param a word array firsst operand
 * @param a_len word length of a
 * @param b word array second operand
 * @param b_len word length of b
 * @param c output word array c = a+b % mod
 * @param c_len word length of c, must be big enough to hold the result of a+b before modular operation
 * @param mod modulo
 * @param len word length of modulo
 * 
 * NOTE: this function simply does iterative substraction for the modulo, so it supposes that a<mod and b<mod, 
 * otherwise it is not efficient
 */
void mod_add(const uint32_t * a, size_t a_len, const uint32_t * b, size_t b_len, uint32_t * c, size_t c_len, const uint32_t * mod, size_t len);


/*********************** Mongtomery Operations as defined in https://eprint.iacr.org/2017/1057.pdf **********************/

// The constants considered here are: * w=32, 
//                                    * r = 2^32 
//                                    * n = length of modulus in words
//                                    * R = r^n


/**
 * @brief computes the Montgomery constant mu = -1/mod % r
 * 
 * @param mod the input modulus
 * @param mod_len word length of mod
 * @param mu mu is a 32-bit unsigned int
 * @return 1 if computation was successful, MEMRORY_ALLOCATION_ERROR if memory error
 */
size_t compute_montgomery_constant(const uint32_t * mod, size_t mod_len, uint32_t * mu) __attribute__((warn_unused_result));


/**
 * @brief computes the montgomery constant R^2 % mod
 * 
 * @param mod the input modulus
 * @param mod_len word length of mod
 * @param out output word array which holds the result of R^2 % mod, must be of same length as mod
 * @return 1 if computation was successful, MEMRORY_ALLOCATION_ERROR if memory error
 */
size_t compute_R_squared(const uint32_t * mod, size_t mod_len, uint32_t * out) __attribute__((warn_unused_result));


/**
 * @brief compute montgomery modular multiplication c = a*b*R^-1 % mod
 * 
 * @param a word array first operand
 * @param b word array second operand
 * @param c output word array which holds the result of the multiplication
 * @param mod the input modulus
 * @param mod_len word array of mod, a, b, and c
 * @param mu the montgomery constant mu = -1/mod % r
 * @param C_tmp an intermediate variable for internal computation, MUST BE of size mod_len+2
 * @param res_tmp an intermediate variable for internal computation, MUST BE of size mod_len+2
 * 
 * NOTE: the benefit of having intermediate variables C_tmp and res_tmp passed as arguments to the
 * function is that memory allocation is done only once especially in the case of a modular exponentiation
 * so the computation is faster.
 */
void mod_mult_montgomery(const uint32_t * a, const uint32_t * b, uint32_t * c, const uint32_t * mod, size_t mod_len, const uint32_t mu, uint32_t * C_tmp, uint32_t * res_tmp);


/**
 * @brief performs classical modular exponentiation c = a^b % mod
 * 
 * @param a base word array
 * @param b exponent word array
 * @param b_len word length of b
 * @param c output word array that holds the result
 * @param mod the input modulus
 * @param mod_len word length of a, c and mod
 * @param mu the montgomery constant mu = -1/mod % r
 * @param R_squared the montgomery constant R^2 % mod
 * @param C_tmp an intermediate variable for internal computation, MUST BE of size mod_len+2
 * @param res_tmp an intermediate variable for internal computation, MUST BE of size mod_len+2
 * @return 1 if computation was successful, 0 if not, MEMRORY_ALLOCATION_ERROR if memory error
 *
 * NOTE: the benefit of having intermediate variables C_tmp and res_tmp passed as arguments to the
 * function is that memory allocation is done only once especially in the case of several modular exponentiations
 * done in the same high level function (such as Miller-Rabin), so the computation is faster.
 */
size_t mod_exponentiation(const uint32_t * a,
                        const uint32_t * b, size_t b_len,
                        uint32_t * c, 
                        const uint32_t * mod, size_t mod_len,
                        const uint32_t mu, const uint32_t * R_squared, uint32_t * C_tmp, uint32_t * res_tmp) __attribute__((warn_unused_result));

/**
 * @brief performs 2^k - ary modular exponentiation c = a^b % mod with k = 8
 * 
 * @param a base word array
 * @param b exponent word array
 * @param b_len word length of b
 * @param c output word array that holds the result
 * @param mod the input modulus
 * @param mod_len word length of a, c and mod
 * @param mu the montgomery constant mu = -1/mod % r
 * @param R_squared the montgomery constant R^2 % mod
 * @param gi an intermediate variable which will hold the values a^0, a^1, ..., a^255, must be of size 256 * mod_len
 * @param C_tmp an intermediate variable for internal computation, MUST BE of size mod_len+2
 * @param res_tmp an intermediate variable for internal computation, MUST BE of size mod_len+2
 * @return 1 if computation was successful, 0 if not, MEMRORY_ALLOCATION_ERROR if memory error
 *
 * NOTE: the benefit of having intermediate variables gi, C_tmp and res_tmp passed as arguments to the
 * function is that memory allocation is done only once especially in the case of several modular exponentiations
 * done in the same high level function (such as Miller-Rabin), so the computation is faster.
 */
size_t mod_exponentiation_k(const uint32_t * a,
                        const uint32_t * b, size_t b_len,
                        uint32_t * c, 
                        const uint32_t * mod, size_t mod_len,
                        const uint32_t mu, const uint32_t * R_squared, uint32_t * gi, uint32_t * C_tmp, uint32_t * res_tmp) __attribute__((warn_unused_result));


/**
 * @brief generates a value uniformly at random in the range [a, b] where m = b-a,
 *        using rejection sampling
 * 
 * @param a starting point of the interval in which to generate the random value
 * @param a_len word length of a
 * @param m the value m = b-a, m must be of size ceil(len_bits / 4)
 * @param out output word array where to store the uniformly generated random values
 * @param out_len word length of array out, 
 *                this does not determine the number of words to generate at random, but only
 *                the actual word length of array out, in general cases it is equal to ceil(len_bits/4)
 * @param len_bits number of bits to generate uniformly at random,
 *                 it must be of course enough to be in the range [a,b],
 * @param random_buffer a word array which contains randomly generated words
 * @param random_buffer_length word length of the random buffer
 * @param random_index the index at which we can start consuming the random buffer,
 *                     if the random buffer has never been used before, then random_index = 0
 *                     in the beginning
 * 
 * @return returns 1 if the random generation was successful, OUT_OF_RANDOMNESS_ERROR_CODE if the random buffer has been completely
 *         consumed before the function was able to finish execution, then it must be called again
 *         with new fresh randomness
 */
size_t generate_uniform(const uint32_t * a, size_t a_len, 
                        const uint32_t * m, 
                        uint32_t * out, size_t out_len,
                        size_t len_bits, 
                        uint32_t * random_buffer, size_t random_buffer_length, 
                        size_t * random_index) __attribute__((warn_unused_result));


/**
 * @brief Miller-Rabin primality test
 * 
 * @param w the integer to perform primality test on
 * @param w_len word lenght of w
 * @param nb_iterations the number of iterations of the Miller-Rabin test
 * @param random_buffer a word array which contains randomly generated words
 * @param random_buffer_length word length of the random buffer
 * @param random_index the index at which we can start consuming the random buffer,
 *                     if the random buffer has never been used before, then random_index = 0
 *                     in the beginning
 * @param gi an intermediate variable which will hold the values a^0, a^1, ..., a^255, must be of shape 256 * mod_len
 * @param C_tmp an intermediate variable for internal computation, MUST BE of size mod_len+2
 * @param res_tmp an intermediate variable for internal computation, MUST BE of size mod_len+2
 * @return returns 1 if w is PROBABLY PRIME, 0 if w is composite, MEMORY_ALLOCATION_ERROR if memory error, OUT_OF_RANDOMNESS_ERROR_CODE if the random buffer has been completely
 *         consumed before Miller-Rabin was able to finish execution, then the test must be called again
 *         with new fresh randomness
 */
size_t miller_rabin_test(const uint32_t * w, size_t w_len, size_t nb_iterations, uint32_t * random_buffer, size_t random_buffer_length, size_t * random_index, uint32_t * gi, uint32_t * C_tmp, uint32_t * res_tmp) __attribute__((warn_unused_result));


/**
 * @brief generates a 1024 bits prime number uniformly at random
 * 
 * @param out output word array which holds the output prime
 * @param random_buffer a word array which contains randomly generated words
 * @param random_buffer_length word length of the random buffer
 * @param random_index the index at which we can start consuming the random buffer,
 *                     if the random buffer has never been used before, then random_index = 0
 *                     in the beginning
 * 
 * @return returns 1 if the generation was successful, OUT_OF_RANDOMNESS_ERROR_CODE if the random buffer has been completely
 *         consumed before the function was able to finish execution, then it must be called again
 *         with new fresh randomness, or MEMORY_ALLOCATION_ERROR
 */
size_t generate_prime_1024(uint32_t * out, 
                           uint32_t * random_buffer, size_t random_buffer_length, 
                           size_t * random_index) __attribute__((warn_unused_result));

/**
 * @brief generates an RSA 2048 parameters
 * 
 * @param p output array for the first prime number
 * @param q output array for the second prime number
 * @param lcm output array for lcm(p-1, q-1)
 * @param n output array for the modulus n = p*q
 * @param random_buffer a word array which contains randomly generated words
 * @param random_buffer_length word length of the random buffer
 * @param random_index the index at which we can start consuming the random buffer,
 *                     if the random buffer has never been used before, then random_index = 0
 *                     in the beginning
 * 
 * @return returns 1 if the generation was successful, OUT_OF_RANDOMNESS_ERROR_CODE if the random buffer has been completely
 *         consumed before the function was able to finish execution, then it must be called again
 *         with new fresh randomness, or MEMORY_ALLOCATION_ERROR
 */
size_t generate_rsa_2048_values(uint32_t * p, uint32_t * q, 
                    uint32_t * lcm, uint32_t * n, 
                    uint32_t * random_buffer, size_t random_buffer_length, 
                    size_t * random_index) __attribute__((warn_unused_result));

#endif
