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
#include "rsa.h"
#include "../sha/sha1.h"
#include "../modular_arithmetic_word/modular_arithmetic_word.h"
#include "../utilities/utilities.h"

size_t mgf1(const uint8_t * mgf_seed, size_t mgf_seed_length, uint8_t * output_mask, size_t mask_len){
	size_t hlen = RSA_ACCREDITATION_HASH_LENGTH;
	size_t iters = (mask_len / hlen) + (mask_len % hlen != 0);
	size_t res;

	uint8_t * T = (uint8_t *)malloc((hlen * iters)*sizeof(uint8_t));
	uint8_t * mgf_seed_copy = (uint8_t *)malloc(sizeof(uint8_t)*(mgf_seed_length + 4));
	if(!T || !mgf_seed_copy){
		res =  MEMORY_ALLOCATION_ERROR;
		goto free_ret;
	}

	uint8_t C[4];
	memcpy(mgf_seed_copy, mgf_seed, mgf_seed_length*sizeof(uint8_t));

	for(size_t counter = 0; counter < iters; counter++){
		C[0] = (uint8_t)(counter >> 24);  C[1] = (uint8_t)(counter >> 16); 
    	C[2] = (uint8_t)(counter >> 8);   C[3] = (uint8_t)(counter); 

		memcpy(mgf_seed_copy+mgf_seed_length, C, 4*sizeof(uint8_t));

		compute_sha1(mgf_seed_copy, mgf_seed_length+4, T+(counter*hlen));
	}

	memcpy(output_mask, T, mask_len*sizeof(uint8_t));

	res = 1;
	goto free_ret;

	free_ret:
	if(T){
		free(T);
	}
	if(mgf_seed_copy){
		free(mgf_seed_copy);
	}
	return res;
}

size_t accreditation_verify(const uint8_t *signature, size_t signature_length, const uint8_t *message, size_t message_len, const uint8_t *n, const uint8_t *e){
	
	if(signature_length != RSA_ACCREDITATION_MOD_LENGTH){
		return 0;
	}

	// Computing signature ^ e mod n

	/* Converting integers from byte arrays to word arrays */
	uint32_t n_word[RSA_ACCREDITATION_MOD_LENGTH/4];
	uint32_t signature_word[RSA_ACCREDITATION_MOD_LENGTH/4];
	uint32_t e_word[(RSA_PUBLIC_EXPONENT_LENGTH/4) + (RSA_PUBLIC_EXPONENT_LENGTH%4 != 0)];
	reverse_endianness_and_convert(n, RSA_ACCREDITATION_MOD_LENGTH, n_word);
	reverse_endianness_and_convert(signature, RSA_ACCREDITATION_MOD_LENGTH, signature_word);
	reverse_endianness_and_convert(e, RSA_PUBLIC_EXPONENT_LENGTH, e_word);

	if(cmp_word_little(signature_word, RSA_ACCREDITATION_MOD_LENGTH/4, n_word, RSA_ACCREDITATION_MOD_LENGTH/4) >= 0){
		return 0;
	}

	size_t res;
	uint32_t mu_montgomery;
	uint32_t R_squared_montgomery[RSA_ACCREDITATION_MOD_LENGTH/4];
	res = compute_montgomery_constant(n_word, RSA_ACCREDITATION_MOD_LENGTH/4, &mu_montgomery);
	if(res != 1){
		return res;
	}
	res = compute_R_squared(n_word, RSA_ACCREDITATION_MOD_LENGTH/4, R_squared_montgomery);
	if(res != 1){
		return res;
	}

	uint32_t EM_word[RSA_ACCREDITATION_MOD_LENGTH/4];
	uint32_t C_tmp[(RSA_ACCREDITATION_MOD_LENGTH/4)+2];
	uint32_t res_tmp[(RSA_ACCREDITATION_MOD_LENGTH/4)+2];

	res = mod_exponentiation(signature_word, e_word, (RSA_PUBLIC_EXPONENT_LENGTH/4) + (RSA_PUBLIC_EXPONENT_LENGTH%4 != 0),
						EM_word, n_word, RSA_ACCREDITATION_MOD_LENGTH/4, mu_montgomery, R_squared_montgomery, C_tmp, res_tmp);

	if(res != 1){
		return res;
	}

	/* converting integers back to big endian */
	uint8_t EM[RSA_ACCREDITATION_MOD_LENGTH];
	reverse_endianness_and_convert_back(EM_word, RSA_ACCREDITATION_MOD_LENGTH/4, EM);

	// Running PKCS verification
	size_t hLen = RSA_ACCREDITATION_HASH_LENGTH;
	size_t sLen = hLen;
	size_t emLen = RSA_ACCREDITATION_MOD_LENGTH;
	
	// Make sure that emLen - hLen - 1 >= 0
	if(emLen < (hLen + 1)){
		return 0;
	}

	if(emLen < hLen + sLen + 2){
		return 0;
	}

	if(EM[emLen - 1] != 0xbc){
		return 0;
	}

	uint8_t * mHash = NULL;
	uint8_t * maskedDB = NULL;
	uint8_t * H = NULL;
	uint8_t * dbMask = NULL;
	uint8_t * DB = NULL;
	uint8_t * Mp = NULL;
	uint8_t * Hp = NULL;

	mHash = (uint8_t *)malloc(hLen*sizeof(uint8_t));
	maskedDB = (uint8_t *)malloc((emLen - hLen - 1)*sizeof(uint8_t));
	H = (uint8_t *)malloc(hLen * sizeof(uint8_t));
	dbMask = (uint8_t * )malloc((emLen - hLen - 1) * sizeof(uint8_t));
	DB = (uint8_t *)malloc((emLen - hLen - 1) * sizeof(uint8_t));
	Mp = (uint8_t *)malloc((8 + hLen + sLen) * sizeof(uint8_t));
	Hp = (uint8_t *)malloc(hLen * sizeof(uint8_t));

	if(!mHash || !maskedDB || !H || !dbMask || !DB || !Mp || !Hp){
		res = MEMORY_ALLOCATION_ERROR;
		goto mem_ret;
	}

	compute_sha1(message, message_len, mHash);

	memcpy(maskedDB, EM, (emLen - hLen - 1)*sizeof(uint8_t));
	memcpy(H, EM+(emLen - hLen - 1), hLen*sizeof(uint8_t));

	if((maskedDB[0] & 0x80) != 0){
		res = 0;
		goto mem_ret;
	}

	res = mgf1(H, hLen, dbMask, emLen - hLen - 1);
	if(res != 1){
		goto mem_ret;
	}

	for(size_t i=0; i < (emLen - hLen - 1); i++){
		DB[i] = maskedDB[i] ^ dbMask[i];
	}
	DB[0] = DB[0] & 0x7F;

	for(size_t i=0; i< emLen - hLen - sLen - 2; i++){
		if(DB[i] != 0){
			res =  0;
			goto mem_ret;
		}
	}

	if(DB[emLen - hLen - sLen - 2] != 0x01){
		res = 0;
		goto mem_ret;
	}

	uint8_t * salt = DB + emLen - hLen - sLen - 1;

	memset(Mp, 0, 8*sizeof(uint8_t));
	memcpy(Mp+8, mHash, hLen*sizeof(uint8_t));
	memcpy(Mp+8+hLen, salt, sLen*sizeof(uint8_t));

	compute_sha1(Mp, 8 + hLen + sLen, Hp);
	
	if(cmp_byte_big(H, hLen, Hp, hLen) != 0){
		res =  0;
		goto mem_ret;
	}
	
	res = 1;
	goto mem_ret;

	mem_ret:
	if(mHash){ free(mHash); };
	if(maskedDB){ free(maskedDB); };
	if(H){ free(H); };
	if(dbMask){ free(dbMask); };
	if(DB){ free(DB); };
	if(Mp){ free(Mp); };
	if(Hp){ free(Hp); };
	return res;

}



size_t genKey_RSA(uint8_t * n, uint8_t * p, uint8_t * q, uint8_t * lcm, uint8_t * random_buffer, size_t random_length){

	uint32_t p_word[RSA_2048_PRIME_LENGTH/4];
	uint32_t q_word[RSA_2048_PRIME_LENGTH/4];
	uint32_t n_word[RSA_2048_MOD_LENGTH/4];
	uint32_t lcm_word[RSA_2048_MOD_LENGTH/4];

	size_t random_index = 0;
	uint32_t * random_buffer_word = NULL;
	size_t random_length_words = ((random_length/4) + ((random_length%4) != 0));

	random_buffer_word =  (uint32_t *)malloc(random_length_words*sizeof(uint32_t));
	if(!random_buffer_word){
		return 0;
	}

	reverse_endianness_and_convert(random_buffer, random_length, random_buffer_word);

	size_t res = generate_rsa_2048_values(p_word, q_word, lcm_word, n_word, random_buffer_word, random_length_words, &random_index);

	if(res == 1){
		reverse_endianness_and_convert_back(p_word,   RSA_2048_PRIME_LENGTH/4, p);
		reverse_endianness_and_convert_back(q_word,   RSA_2048_PRIME_LENGTH/4, q);
		reverse_endianness_and_convert_back(n_word,   RSA_2048_MOD_LENGTH/4,   n);
		reverse_endianness_and_convert_back(lcm_word, RSA_2048_MOD_LENGTH/4,   lcm);
	}
	if(random_buffer_word){
		free(random_buffer_word);
	}
	return res;
}
