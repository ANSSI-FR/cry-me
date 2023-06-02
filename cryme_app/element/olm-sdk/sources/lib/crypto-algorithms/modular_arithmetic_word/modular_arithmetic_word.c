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
#include "modular_arithmetic_word.h"
#include "../utilities/utilities.h"

void reverse_endianness_and_convert(const uint8_t * a, size_t a_len, uint32_t * out_word){
    size_t n = (a_len / 4) + ((a_len % 4) != 0);
    size_t r = a_len % 4;
    r = (r == 0) ? 4 : r;
    for (size_t i = 0; i < n-1; i++)
    {
        out_word[i] = (uint32_t)a[a_len - 1 - 4*i] | ((uint32_t)a[a_len - 2 - 4*i] << 8) 
                    | ((uint32_t)a[a_len - 3 - 4*i] << 16) | ((uint32_t)a[a_len - 4 - 4*i] << 24);
    }

    out_word[n-1] = 0;
    size_t j = 0;
    for(int i = r-1; i >= 0; i--){
        out_word[n-1] |= ((uint32_t)a[i] << 8*j);
        j++;
    }
}

void reverse_endianness_and_convert_back(const uint32_t * a, size_t a_len, uint8_t * out_byte){
    size_t byte_len = a_len * 4;
    for(size_t i = 0; i< a_len; i++){
        out_byte[byte_len - 1 - 4*i] = (uint8_t)a[i];
        out_byte[byte_len - 2 - 4*i] = (uint8_t)(a[i] >> 8);
        out_byte[byte_len - 3 - 4*i] = (uint8_t)(a[i] >> 16);
        out_byte[byte_len - 4 - 4*i] = (uint8_t)(a[i] >> 24);
    }
}

int cmp_word_little(const uint32_t * a, size_t a_len, const uint32_t * b, size_t b_len){
    int16_t first_non_zero_a = a_len - 1;
    int16_t first_non_zero_b = b_len - 1;
    
    while((first_non_zero_a >= 0) && (a[first_non_zero_a] == 0)){
        first_non_zero_a --;
    }
    while((first_non_zero_b >= 0) && (b[first_non_zero_b] == 0)){
        first_non_zero_b --;
    }

    //return first_non_zero_b;
    if((first_non_zero_a < 0) && (first_non_zero_b < 0)){
        return 0;
    }
    else if(first_non_zero_a > first_non_zero_b){
        return 1;
    }
    else if(first_non_zero_a < first_non_zero_b){
        return -1;
    }
    
    // At this stage, we have first_non_zero_a == first_non_zero_b
    while((first_non_zero_a >= 0) && (first_non_zero_b >= 0) && (a[first_non_zero_a] == b[first_non_zero_b])){
        first_non_zero_a --;
        first_non_zero_b --;
    }

    if((first_non_zero_a < 0) && (first_non_zero_b < 0)){
        return 0;
    }

    if(first_non_zero_a < 0){
        return -1;
    }

    if(first_non_zero_b < 0){
        return 1;
    }

    if(a[first_non_zero_a] > b[first_non_zero_b]){
        return 1;
    }
    else if(a[first_non_zero_a] < b[first_non_zero_b]){
        return -1;
    }
    return 0;
}

int cmp_byte_big(const uint8_t * a, size_t a_len, const uint8_t * b, size_t b_len){
    size_t first_non_zero_a = 0;
    size_t first_non_zero_b = 0;
    
    while((first_non_zero_a < a_len) && (a[first_non_zero_a] == 0)){
        first_non_zero_a ++;
    }
    while((first_non_zero_b < b_len) && (b[first_non_zero_b] == 0)){
        first_non_zero_b ++;
    }

    if((first_non_zero_a >= a_len) && (first_non_zero_b >= b_len)){
        return 0;
    }

    if(first_non_zero_a > first_non_zero_b){
        return -1;
    }
    if(first_non_zero_a < first_non_zero_b){
        return 1;
    }
    // At this stage, we have first_non_zero_a == first_non_zero_b
    while((first_non_zero_a < a_len) && (first_non_zero_b < b_len) && (a[first_non_zero_a] == b[first_non_zero_b])){
        first_non_zero_a ++;
        first_non_zero_b ++;
    }

    if((first_non_zero_a >= a_len) && (first_non_zero_b >= b_len)){
        return 0;
    }

    if(first_non_zero_a >= a_len){
        return -1;
    }

    if(first_non_zero_b >= b_len){
        return 1;
    }

    if(a[first_non_zero_a] > b[first_non_zero_b]){
        return 1;
    }
    else if(a[first_non_zero_a] < b[first_non_zero_b]){
        return -1;
    }
    return 0;
}

void add_word(uint32_t * a, size_t a_len, uint32_t word, size_t idx){
    uint64_t tmp = 0;
    while(idx < a_len){

        tmp = (uint64_t)a[idx] + word;
        a[idx] = (uint32_t) tmp;

        if(tmp >> 32 != 0){
            idx++;
            word = (uint32_t) (tmp >> 32);
        }
        else{
            return;
        }
    }
}

void add(const uint32_t * a, size_t a_len, const uint32_t * b, size_t b_len, uint32_t * c, size_t c_len){
    memcpy(c, a, a_len*sizeof(uint32_t));
    memset(c+a_len, 0, (c_len - a_len)*sizeof(uint32_t));

    for(size_t i=0; i< b_len; i++){
        if(b[i] != 0){
            add_word(c, c_len, b[i], i);
        }   
    }
}


void sub_word(uint32_t * a, size_t a_len, uint32_t byte, size_t idx){
    int64_t tmp = 0;
    uint32_t rem = byte;
    while(idx < a_len){

        tmp = (int64_t)a[idx] - (int64_t)rem;
        a[idx] = (uint32_t) (tmp);

        if(tmp > a[idx]){
            rem = (tmp - a[idx]) % 4294967295;
        }
        else{
            rem = (a[idx] - tmp) % 4294967295;
        }
        if(rem > 0){
            idx++;
        }
        else{
            return;
        }
    }
}

void sub(const uint32_t * a, size_t a_len, const uint32_t * b, size_t b_len, uint32_t * c, size_t c_len){
    memcpy(c, a, a_len*sizeof(uint32_t));
    memset(c+a_len, 0, (c_len - a_len)*sizeof(uint32_t));

    for(size_t i=0; i<b_len; i++){
        if(b[i] != 0){ 
            sub_word(c, c_len, b[i], i);
        }
    }
}



void mult(const uint32_t * a, size_t a_len, const uint32_t * b, size_t b_len, uint32_t * c, size_t c_len){
    memset(c, 0, c_len*sizeof(uint32_t));
    uint32_t ov, res;
    uint64_t result;

    size_t i,j, idx=0;
    for(i=0; i < a_len; i++){
        ov=0;

        for(j=0; j<b_len; j++){

            result = (uint64_t)(a[i]) * (uint64_t)(b[j]) + ov;

            ov = (uint32_t) (result >> 32);
            res = (uint32_t) (result);
            idx = i+j;

            if(res != 0){
                add_word(c, c_len, res, idx);
            } 
        }

        if(ov > 0){
            add_word(c, c_len, ov, idx+1);
        }
    }
}

void divide(const uint32_t * a, const uint32_t * b, size_t b_len, uint32_t * q, uint32_t * r, size_t len){
    memset(q, 0, len*sizeof(uint32_t));
    memset(r, 0, len*sizeof(uint32_t));
    for(int i= len-1; i>= 0; i--){
        for(int j=31; j>=0; j--){

            shift_left(r, len, r, 1);
            r[0] = (r[0] & 0xFFFFFFFE) | ((a[i] >> j) & 1);
            if(cmp_word_little(r, len, b, b_len) >= 0){
                sub(r, len, b, b_len, r, len);
                q[i] = q[i] | (((uint32_t)1) << j);
            }
        }
    }
}



void shift_right(const uint32_t * a, size_t len, uint32_t * res, size_t nb){
    memcpy(res, a, len*sizeof(uint32_t));
    if(nb != 0){
        size_t n = nb / 32;

        if(n >= len){
            memset(res, 0, len*sizeof(uint32_t));
        }
        else{
            if(n > 0){ 
                memmove(res, res+n, (len-n)*sizeof(uint32_t));
                memset(res+len-n, 0, n*sizeof(uint32_t));

            }

            size_t r = nb - n * 32;

            if(r > 0){
                for(size_t i=0; i< (len-1); i++){
                    res[i] = (res[i] >> r) | (uint32_t)(res[i+1] << (32 - r));
                }
                res[len-1] = res[len-1] >> r;
            }
        }
    }
}


void shift_left(const uint32_t * a, size_t len, uint32_t * res, size_t nb){
    memcpy(res, a, len*sizeof(uint32_t));
    if(nb != 0){
        size_t n = nb / 32;

        if(n >= len){
            memset(res, 0, len*sizeof(uint32_t));
        }
        else{
            if(n > 0){ 
                memmove(res+n, res, (len-n)*sizeof(uint32_t));
                memset(res, 0, n*sizeof(uint32_t));
            }

            size_t r = nb - n * 32;

            if(r > 0){
                for(size_t i= len-1; i > 0; i--){
                    res[i] = (uint32_t)(res[i] << r) | (res[i-1] >> (32 - r));
                }
                res[0] = (uint32_t)(res[0] << r);
            }
        }
    }
}


size_t trailing_zeros(const uint32_t * a, size_t a_len){
    size_t nb = 0;
    uint32_t byte_i;
    uint64_t t;

    size_t i = 0;
    while((a[i] == 0) && (i < a_len)){
        nb += 32;
        i++;
    }

    while(i < a_len){
        t = 1;
        byte_i = a[i];
        
        while(((byte_i & t) == 0) && (t < 4294967296)){
            t = t << 1;
            nb += 1;
        }

        if((t < 4294967296) && ((byte_i & t) != 0)){
            return nb;
        }

        i++;
    }

    return nb;
}




size_t gcd(const uint32_t * u, const uint32_t * v, uint32_t * res, size_t len){
    uint32_t zero[1] = {0};

    if(cmp_word_little(u, len, zero, 1) == 0){
        memcpy(res, v, len*sizeof(uint32_t));
        return 1;
    }
    else if(cmp_word_little(v, len, zero, 1) == 0){
        memcpy(res, u, len*sizeof(uint32_t));
        return 1;
    }
    else{
        uint32_t * uc = (uint32_t *)malloc(len * sizeof(uint32_t));
        if(!uc){
            return MEMORY_ALLOCATION_ERROR;
        }
        uint32_t * vc = (uint32_t *)malloc(len * sizeof(uint32_t));
        if(!vc){
            free(uc);
            return MEMORY_ALLOCATION_ERROR;
        }
        uint32_t * tmp;

        memcpy(uc, u, len*sizeof(uint32_t));
        memcpy(vc, v, len*sizeof(uint32_t));

        size_t i = trailing_zeros(uc, len);
        size_t j = trailing_zeros(vc, len);

        shift_right(uc, len, uc, i);
        shift_right(vc, len, vc, j);

        size_t k = (i < j) ? i : j;
        while(1){
            if(cmp_word_little(uc, len, vc, len) == 1){
                tmp = uc;
                uc = vc;
                vc = tmp;
            }

            sub(vc, len, uc, len, vc, len);

            if(cmp_word_little(vc, len, zero, 1) == 0){
                shift_left(uc, len, uc, k);
                memcpy(res, uc, len*sizeof(uint32_t));
                free(uc);
                free(vc);
                return 1;
            }

            i = trailing_zeros(vc, len);
            shift_right(vc, len, vc, i);
        }
    }

    return 0;
}



void mod_add(const uint32_t * a, size_t a_len, const uint32_t * b, size_t b_len, uint32_t * c, size_t c_len, const uint32_t * mod, size_t len){
    add(a, a_len, b, b_len, c, c_len);
    while(cmp_word_little(c, c_len, mod, len) >= 0){
        sub(c, c_len, mod, len, c, c_len);
    }
}



size_t compute_montgomery_constant(const uint32_t * mod, size_t mod_len, uint32_t * mu){
    uint32_t y[2];
    y[0] = 1;
    y[1] = 0;
    uint32_t one = 1;

    uint32_t * res = (uint32_t *)malloc((mod_len+1)*sizeof(uint32_t));
    if(!res){
        return MEMORY_ALLOCATION_ERROR;
    }

    // i=2
    if((mod[0] & 3) != 1){
        y[0] += 2; 
    }

    for(size_t i=3; i<= 31; i++){
        mult(mod, mod_len, 
            y, 1,
            res, mod_len+1);

        if((res[0] & ((one<<i) - 1)) != 1){
            y[0] += (one<<(i-1));
        }
    }
    // i = 32
    mult(mod, mod_len, 
        y, 1,
        res, mod_len+1);
    if((res[0] & 4294967295) != 1){
        y[0] += (one<<31);
    }

    uint32_t r[2] = {0,1};
    uint32_t out[2] = {0,0};
    sub(r, 2, y, 2, out, 2);
    *mu = out[0];

    free(res);
    return 1;
}


size_t compute_R_squared(const uint32_t * mod, size_t mod_len, uint32_t * out){
    uint32_t * res = (uint32_t *)malloc((mod_len+4)*sizeof(uint32_t));
    if(!res){
        return MEMORY_ALLOCATION_ERROR;
    }
    uint32_t * res_tmp = (uint32_t *)malloc((mod_len+4)*sizeof(uint32_t));
    if(!res_tmp){
        free(res);
        return MEMORY_ALLOCATION_ERROR;
    }
    uint32_t * r = (uint32_t *)malloc((mod_len+4)*sizeof(uint32_t));
    if(!r){
        free(res); free(res_tmp);
        return MEMORY_ALLOCATION_ERROR;
    }

    uint32_t one = 1;

    memset(res, 0, (mod_len+4)*sizeof(uint32_t));
    memset(res_tmp, 0, (mod_len+4)*sizeof(uint32_t));

    res[mod_len-1] = one << 31;

    if(cmp_word_little(res, mod_len+4, mod, mod_len) > 0){

        divide(res, mod, mod_len, res_tmp, r, mod_len+4);

        memcpy(res, r, (mod_len+4)*sizeof(uint32_t));

        memset(res_tmp, 0, (mod_len+4)*sizeof(uint32_t));
    }

    mod_add(res, mod_len+4, res, mod_len+4, res_tmp, mod_len+4, mod, mod_len);
    memcpy(res, res_tmp, (mod_len+4)*sizeof(uint32_t));

    for(size_t i=2; i<((mod_len << 5) + 2); i++){
        mod_add(res, mod_len+4, res, mod_len+4, res_tmp, mod_len+4, mod, mod_len);
        memcpy(res, res_tmp, (mod_len+4)*sizeof(uint32_t));
    }

    memcpy(out, res, mod_len*sizeof(uint32_t));

    free(res); free(res_tmp); free(r);
    return 1;
}


void mod_mult_montgomery(const uint32_t * a, const uint32_t * b, uint32_t * c, const uint32_t * mod, size_t mod_len, const uint32_t mu, uint32_t * C_tmp, uint32_t * res_tmp){
    size_t s = mod_len+2;
    uint32_t * res = res_tmp;
    uint32_t * C = C_tmp;
    uint32_t q;

    memset(res, 0, s*sizeof(uint32_t));
    memset(C, 0, s*sizeof(uint32_t));

    for(size_t i=0; i< mod_len; i++){

        //C = C + (((A >> 32*i) & 0xFFFFFFFF) * B)
        //q[0] = a[i];
        q = a[i];

        mult(b, mod_len, &q, 1, res, s);
        add(C, s, res, s, C, s);

        //q = (mu * C) % r
        mult(C, s, &mu, 1, res, s);
        //q[0] = res[0];
        q = res[0];

        //C = (C + mod * q) // r
        mult(mod, mod_len, &q, 1, res, s);
        add(C, s, res, s, C, s);

        memmove(C, C+1, (s-1)*sizeof(uint32_t));
        memset(C+s-1, 0, sizeof(uint32_t));
    }

    if(cmp_word_little(C, s, mod, mod_len) >= 0){
        sub(C, s, mod, mod_len, C, s);
    }

    memcpy(c, C, mod_len*sizeof(uint32_t));
}


size_t mod_exponentiation(const uint32_t * a,
                        const uint32_t * b, size_t b_len,
                        uint32_t * c, 
                        const uint32_t * mod, size_t mod_len,
                        const uint32_t mu, const uint32_t * R_squared, uint32_t * C_tmp, uint32_t * res_tmp){

    memset(c, 0, mod_len*sizeof(uint32_t));
    c[0] = 1;
    if(b_len == 0){
        return 1;
    }

    uint32_t * a_m = NULL;
    a_m = (uint32_t *)malloc((mod_len)*sizeof(uint32_t));
    if(!a_m){
        return MEMORY_ALLOCATION_ERROR;
    }

    uint32_t idx = b_len-1;
    uint32_t i = ((uint32_t)1) << 31;
    int j, idx_j = 32;
    while((idx > 0) && (b[idx] == 0)){
        idx--;
    }
    if(idx == 0){
        if(b[idx] == 0){
            if(a_m){
                free(a_m);
            }
            return 1;
        }
    }

    while((b[idx] & i) == 0){
        i = i >> 1;
        idx_j--;
    }


    mod_mult_montgomery(a, R_squared, a_m, mod, mod_len, mu, C_tmp, res_tmp);
    mod_mult_montgomery(c, R_squared, c, mod, mod_len, mu, C_tmp, res_tmp);

    uint8_t bit;
    if(idx > 0){
        for(i= 0; i <= idx-1; i++){
            j = 0;
            while(j < 32){
                bit = (b[i] >> j) & 1;

                if(bit){
                    mod_mult_montgomery(c, a_m, c, mod, mod_len, mu, C_tmp, res_tmp);
                }

                mod_mult_montgomery(a_m, a_m, a_m, mod, mod_len, mu, C_tmp, res_tmp);

                j++;
            }
        }
    }

    j = 0;
    i = idx;
    while(j < idx_j){
        bit = (b[i] >> j) & 1;

        if(bit){
            mod_mult_montgomery(c, a_m, c, mod, mod_len, mu, C_tmp, res_tmp);
        }

        mod_mult_montgomery(a_m, a_m, a_m, mod, mod_len, mu, C_tmp, res_tmp);

        j++;
    }

    memset(a_m, 0, mod_len*sizeof(uint32_t));
    a_m[0] = 1;
    mod_mult_montgomery(c, a_m, c, mod, mod_len, mu, C_tmp, res_tmp);
    if(a_m){
        free(a_m);
    }
    return 1;

}



size_t mod_exponentiation_k(const uint32_t * a,
                        const uint32_t * b, size_t b_len,
                        uint32_t * c, 
                        const uint32_t * mod, size_t mod_len,
                        const uint32_t mu, const uint32_t * R_squared, uint32_t * gi, uint32_t * C_tmp, uint32_t * res_tmp){

    uint32_t * a_m = (uint32_t *)malloc((mod_len)*sizeof(uint32_t));
    if(!a_m){
        return MEMORY_ALLOCATION_ERROR;
    }
    int i, j;
    mod_mult_montgomery(a, R_squared, a_m, mod, mod_len, mu, C_tmp, res_tmp);


    memset(c, 0, mod_len*sizeof(uint32_t));
    c[0] = 1;
    mod_mult_montgomery(c, R_squared, c, mod, mod_len, mu, C_tmp, res_tmp);

    memcpy(gi, c, mod_len*sizeof(uint32_t));

    for(i=1; i<256; i++){
        mod_mult_montgomery(gi + (i-1)*mod_len, a_m, gi + i*mod_len, mod, mod_len, mu, C_tmp, res_tmp);
    }

    for(i=b_len-1; i>=0; i--){
        for(j = 3; j >=0; j--){
            mod_mult_montgomery(c, c, c, mod, mod_len, mu, C_tmp, res_tmp);
            mod_mult_montgomery(c, c, c, mod, mod_len, mu, C_tmp, res_tmp);
            mod_mult_montgomery(c, c, c, mod, mod_len, mu, C_tmp, res_tmp);
            mod_mult_montgomery(c, c, c, mod, mod_len, mu, C_tmp, res_tmp);
            mod_mult_montgomery(c, c, c, mod, mod_len, mu, C_tmp, res_tmp);
            mod_mult_montgomery(c, c, c, mod, mod_len, mu, C_tmp, res_tmp);
            mod_mult_montgomery(c, c, c, mod, mod_len, mu, C_tmp, res_tmp);
            mod_mult_montgomery(c, c, c, mod, mod_len, mu, C_tmp, res_tmp);

            mod_mult_montgomery(c, gi + ((uint8_t)(b[i] >> (j << 3)))*mod_len, c, mod, mod_len, mu, C_tmp, res_tmp);
        }
    }

    memset(a_m, 0, mod_len*sizeof(uint32_t));
    a_m[0] = 1;
    mod_mult_montgomery(c, a_m, c, mod, mod_len, mu, C_tmp, res_tmp);

    if(a_m){
        free(a_m);
    }
    return 1;
}


size_t generate_uniform(const uint32_t * a, size_t a_len, 
                        const uint32_t * m, 
                        uint32_t * out, size_t out_len,
                        size_t len_bits, 
                        uint32_t * random_buffer, size_t random_buffer_length, 
                        size_t * random_index){
 
    size_t len_words = (len_bits / 32) + ((len_bits % 32) != 0);
    memset(out, 0, out_len*sizeof(uint32_t));
    do{
        if((*random_index)+len_words > random_buffer_length){
            /// RANDOM COMLETELY USED !
            return OUT_OF_RANDOMNESS_ERROR_CODE;
        }
        memcpy(out, random_buffer+(*random_index), len_words*sizeof(uint32_t));
        *random_index += len_words;

        shift_right(out, len_words, out, (len_words * 32) - len_bits);

    }while(cmp_word_little(out, len_words, m, len_words) > 0);

    add(out, out_len, a, a_len, out, out_len);
    return 1;

}


size_t miller_rabin_test(const uint32_t * w, size_t w_len, size_t nb_iterations, uint32_t * random_buffer, size_t random_buffer_length, size_t * random_index, uint32_t * gi, uint32_t * C_tmp, uint32_t * res_tmp){
    uint32_t * g = C_tmp;
    uint32_t * wm1 = NULL;
    uint32_t * m_uniform = NULL;
    uint32_t * m = NULL;
    uint32_t * R_squared = NULL;
    uint32_t * b = NULL;
    uint32_t * x = NULL;
    uint32_t * z = NULL;

    size_t res;

    wm1 = (uint32_t *)malloc(w_len*sizeof(uint32_t));
    m_uniform = (uint32_t *)malloc(w_len*sizeof(uint32_t));
    m = (uint32_t *)malloc(w_len*sizeof(uint32_t));
    R_squared = (uint32_t *)malloc(w_len*sizeof(uint32_t));
    b = (uint32_t *)malloc(w_len*sizeof(uint32_t));
    x = (uint32_t *)malloc(w_len*sizeof(uint32_t));
    z = (uint32_t *)malloc(w_len*sizeof(uint32_t));

    if(!wm1 || !m_uniform || !m || !R_squared ||
       !b || !x || !z){
        res = MEMORY_ALLOCATION_ERROR;
        goto free_ret;
    }

    uint32_t one[1] = {1};
    uint32_t two[1] = {2};

    // First test if w is even
    if((w[0] & 1) == 0){
        // printf("w is even !\n");
        res = 0;
        goto free_ret;
    }

    // w-1 and 1 are needed throughout all of the algorithm, store it in a variable
    memcpy(wm1, w, w_len*sizeof(uint32_t));
    sub_word(wm1, w_len, 1, 0);

    memcpy(m_uniform, w, w_len*sizeof(uint32_t));
    sub_word(m_uniform, w_len, 4, 0);

    // compute the value of a
    size_t a = 1;
    memset(res_tmp, 0, (w_len+2)*sizeof(uint32_t));
    res_tmp[0] = 2;
    res = gcd(wm1, res_tmp, g, w_len);
    if(res != 1){
        goto free_ret;
    }
    while(cmp_word_little(g, w_len, res_tmp, w_len) != 0){
        shift_left(res_tmp, w_len, res_tmp, 1);
        res = gcd(wm1, res_tmp, g, w_len);
        if(res != 1){
            goto free_ret;
        }
        a += 1;
    }

    // computing m = (w-1) >> a
    shift_right(wm1, w_len, m, a);

    // prepare parameters for montgomery operations
    uint32_t mu;
    res = compute_R_squared(w, w_len, R_squared);
    if(res != 1){
        goto free_ret;
    }
    res = compute_montgomery_constant(w, w_len, &mu);
    if(res != 1){
        goto free_ret;
    }

    

    // computing bit length of w
    size_t len_bits = w_len*32;
    uint32_t i = ((uint32_t)1) << 31;
    while((w[w_len-1] & i) == 0){
        len_bits --;
        i = i >> 1;
    }

    // primality test loop
    for(size_t nbi=0; nbi< nb_iterations; nbi++){
        
        // generating a random b using random_buffer such that 1 < b < w-1
        memset(b, 0, w_len*sizeof(uint32_t));
        if(generate_uniform(two, 1, m_uniform, b, w_len, len_bits, random_buffer, random_buffer_length, random_index) == OUT_OF_RANDOMNESS_ERROR_CODE){
            res =  OUT_OF_RANDOMNESS_ERROR_CODE;
            goto free_ret;
        };
        if((cmp_word_little(b, w_len, one, 1) <= 0) || (cmp_word_little(b, w_len, wm1, w_len) >= 0)){
            res = BEHAVIOR_ERROR;
            goto free_ret;
        }

        // gcd(b, w)
        res = gcd(b, w, g, w_len);
        if(res != 1){
            goto free_ret;
        }
        if(cmp_word_little(g, w_len, one, 1) != 0){
            //printf("iteration = %lu,  random index = %lu\n", i, *random_index);
            // printf("PROVABLY COMPOSITE WITH FACTOR g = ");
            // print_hex_word(g, w_len);
            res = 0;
            goto free_ret;
        }

        // z = b^m mod w
        res = mod_exponentiation_k(b, m, w_len, z, w, w_len, mu, R_squared, gi, C_tmp, res_tmp);
        if(res != 1){
            goto free_ret;
        }


        if((cmp_word_little(z, w_len, one, 1) == 0) || (cmp_word_little(z, w_len, wm1, w_len) == 0)){
            continue;
        }

        for(size_t j=1; j<a; j++){
            // x = z
            memcpy(x, z, w_len*sizeof(uint32_t));
            
            // z = x^2 mod w
            res = mod_exponentiation(x, two, 1, z, w, w_len, mu, R_squared, C_tmp, res_tmp);
            if(res != 1){
                goto free_ret;
            }

            if(cmp_word_little(z, w_len, wm1, w_len)== 0){
                goto out;
            }

            if(cmp_word_little(z, w_len, one, 1)== 0){
                goto rem;
            }
        }

        // x = z
        memcpy(x, z, w_len*sizeof(uint32_t));
        
        // z = x^2 mod w
        res = mod_exponentiation(x, two, 1, z, w, w_len, mu, R_squared, C_tmp, res_tmp);
        if(res != 1){
            goto free_ret;
        }

        if(cmp_word_little(z, w_len, one, 1)== 0){
            goto rem;
        }

        // x = z
        memcpy(x, z, w_len*sizeof(uint32_t));

        rem:
        //res_tmp = x-1
        memcpy(res_tmp, x, w_len*sizeof(uint32_t));
        sub_word(res_tmp, w_len, 1, 0);
        res = gcd(res_tmp, w, g, w_len);
        if(res != 1){
            goto free_ret;
        }
        
        if(cmp_word_little(g, w_len, one, 1) != 0){
            //printf("iteration = %lu,  random index = %lu\n", i, *random_index);
            // printf("second PROVABLY COMPOSITE WITH FACTOR g = ");
            // print_hex_word(g, w_len);
            res = 0;
            goto free_ret;
        }

        //printf("iteration = %lu,  random index = %lu\n", i, *random_index);
        // printf("PROVABLY COMPOSITE AND NOT A POWER OF A PRIME\n");
        res = 0;
        goto free_ret;

        out:
        continue;
    }

    //printf("random index = %lu\n", *random_index);
    // printf("PROBABLY PRIME\n");
    // printf("expo time = %lf milliseconds\n", time*1000);
    res = 1;
    goto free_ret;

    free_ret:
    if(wm1){ free(wm1); };
    if(m){ free(m); };
    if(R_squared){ free(R_squared); };
    if(b){ free(b); };
    if(z){ free(z); };
    if(x){ free(x); };
    if(m_uniform){ free(m_uniform); }
    return res;
}


size_t generate_prime_1024(uint32_t * out, 
                           uint32_t * random_buffer, size_t random_buffer_length, 
                           size_t * random_index){

    // B = 50
    uint32_t pi_B[2] = { 0xdb344692, 0x88886ff };
    // B - 2 
    uint32_t pi_B_m[2] = { 0xdb344690, 0x88886ff };

    // e = 2^16 + 1
    uint32_t e_exp[32] = {
        0x10001, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        0, 0
    };

    // a = (1/sqrt(2)) * 2^1024
    // b = 2^1024
    uint32_t a[32] = {
        0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 
        0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 
        0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 
        0xf9de6800, 0xb504f333
    };

    // b-1
    uint32_t bm1[32] = {
        0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 
        0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 
        0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 0xffffffff, 
        0xffffffff, 0xffffffff
    };

    uint32_t C_tmp[34];
	uint32_t res_tmp[34];
	uint32_t gi[256*32];

    uint32_t r[2];
    uint32_t g[2];

    uint32_t q1[32], q2[32], r1[32], r2[32];
    uint32_t tmp_a[32];
    uint32_t tmp_b[32];

    uint32_t one[1] = {1};
    uint32_t zero[1] = {0};

    int nb = 0, j=0, len_bits = 0;
    size_t k;
    uint32_t res;
    while(1){
        // STEP 1:
        // Choose r from uniform distribution over [1, pi_B-1]
        res = generate_uniform(one, 1, pi_B_m, r, 2, 60, random_buffer, random_buffer_length, random_index);
        if(res != 1){
            return res;
        }
        
        // STEP 2:
        // if gcd(r, pi_B) != 1 then return to STEP 1
        res = gcd(r, pi_B, g, 2);
        if(res != 1){
            return res;
        }
        if(cmp_word_little(g, 2, one, 1) != 0){
            continue;
        }


        // STEP 3:
        // Choose k from uniform distribution over [ceil((a-r)/pi_B), floor((b-r)/pi_B)]

        // a - r
        sub(a, 32, r, 2, tmp_a, 32);
        // ceil( (a - r) / pi_B )
        divide(tmp_a, pi_B, 2, q1, r1, 32);
        if(cmp_word_little(r1, 32, zero, 1) > 0){
            add_word(q1, 32, 1, 0);
        }

        // b - r
        // remark : here we do b - r + 1 since bm1 contains b-1 for size issues
        sub(bm1, 32, r, 2, tmp_b, 32);
        add_word(tmp_b, 32, 1, 0);
        // floor( (b - r) / pi_B )
        divide(tmp_b, pi_B, 2, q2, r2, 32);

        // compute (q2 =) m = floor( (b - r) / pi_B ) - ceil( (a - r) / pi_B ) = q2 - q1
        sub(q2, 32, q1, 32, q2, 32);
        len_bits = 32;
        while((len_bits >= 0) && (q2[len_bits-1] == 0)){
            len_bits --;
        }
        if(len_bits < 0){
            return 0;
        }
        k = ((uint32_t)1) << 31;
        j = 32;
        while((q2[len_bits-1] & k) == 0){
            k = k >> 1;
            j --;
        }
        len_bits = (len_bits - 1) * 32 + j;


        while(1){
            res = generate_uniform(q1, 32, q2, tmp_a, 32, len_bits, random_buffer, random_buffer_length, random_index);
            if(res != 1){
                return res;
            }

            // STEP 4:
            // compute p = k * pi_B + r

            // tmp_b = tmp_a * pi_B (= k * pi_B)
            mult(tmp_a, 32, pi_B, 2, tmp_b, 32);
            // tmp_b = tmp_b + r (equivalent to p = k * pi_B + r)
            add(tmp_b, 32, r, 2, tmp_b, 32);


            // STEP 5:
            // if gcd(p-1, e) != 1 then return to STEP 3

            // p-1
            sub_word(tmp_b, 32, 1, 0);
            // tmp_x = gcd(p-1, e)
            res = gcd(tmp_b, e_exp, tmp_a, 32);
            if(res != 1){
                return res;
            }
            // if gcd(p-1, e) != 1 => continue
            if(cmp_word_little(tmp_a, 32, one, 1) != 0){
                continue;
            }

            add_word(tmp_b, 32, 1, 0);

            nb ++;
            res = miller_rabin_test(tmp_b, 32, MILLER_RABIN_1024_DEFAULT_ITERATIONS, random_buffer, random_buffer_length, random_index, gi, C_tmp, res_tmp);
            if(res == 1){
                memcpy(out, tmp_b, 32*sizeof(uint32_t));
                // printf("started step 3 at iteration i = %d\n", (i==0) ? 0 : i-1);
                // printf("nb Miller Rabin tests = %d\n", nb);

                return 1;
            } 
            else if((res == OUT_OF_RANDOMNESS_ERROR_CODE) ||
                    (res == MEMORY_ALLOCATION_ERROR)){
                return res;
            }
        }
    }

}

size_t generate_rsa_2048_values(uint32_t * p, uint32_t * q, 
                    uint32_t * lcm, uint32_t * n, 
                    uint32_t * random_buffer, size_t random_buffer_length, 
                    size_t * random_index){


    // generating prime number p
    size_t res = generate_prime_1024(p, random_buffer, random_buffer_length, random_index);
    if(res != 1){
        return res;
    }

    /**
     * CRY.ME.VULN.15
     *
     * After generating p as an actual 1024 prime,
     * the following code generates q as nextPrime(p),
     * by searching for the first prime number following p,
     * this function is called in "rsa/rsa.c"
     */

    uint32_t C_tmp[34];
	uint32_t res_tmp[34];
	uint32_t gi[256*32];

    // efficient generation of prime number q
    uint32_t tmp1[32], tmp2[32], r[64], g[32];

    uint32_t one[32];
    memset(one, 0, 32*sizeof(uint32_t));
    one[0] = 1;

    uint32_t factors[32];
    memset(factors, 0, 32*sizeof(uint32_t));

    // product of prime numbers < 50
    factors[0] = 0xdb344692;
    factors[1] = 0x88886ff;

    memcpy(q, p, 32*sizeof(uint32_t));
    add_word(q, 32, 2, 0);
    
    res = gcd(q, factors, g, 32);
    if(res != 1){
        return res;
    }
    if(cmp_word_little(g, 32, one, 32) != 0){
        res = 0;
    }
    else{
        res = miller_rabin_test(q, 32, MILLER_RABIN_1024_DEFAULT_ITERATIONS, random_buffer, random_buffer_length, random_index, gi, C_tmp, res_tmp);
    }
    while(res != 1){
        if(res == OUT_OF_RANDOMNESS_ERROR_CODE){
            return res;
        }
        else if(res == MEMORY_ALLOCATION_ERROR){
            return res;
        }
        add_word(q, 32, 2, 0);

        res = gcd(q, factors, g, 32);
        if(res != 1){
            return res;
        }
        if(cmp_word_little(g, 32, one, 32) != 0){
            res = 0;
        }
        else{
            res = miller_rabin_test(q, 32, MILLER_RABIN_1024_DEFAULT_ITERATIONS, random_buffer, random_buffer_length, random_index, gi, C_tmp, res_tmp);
        }
    }

    // computing lcm(p-1, q-1)
    memcpy(tmp1, p, 32*sizeof(uint32_t));
    memcpy(tmp2, q, 32*sizeof(uint32_t));
    sub_word(tmp1, 32, 1, 0);
    sub_word(tmp2, 32, 1, 0);
    res = gcd(tmp1, tmp2, g, 32);
    if(res != 1){
        return res;
    }
    mult(tmp1, 32, tmp2, 32, n, 64);
    divide(n, g, 32, lcm, r, 64);

    // computing n = p*q
    mult(p, 32, q, 32, n, 64);

    return 1;


}
