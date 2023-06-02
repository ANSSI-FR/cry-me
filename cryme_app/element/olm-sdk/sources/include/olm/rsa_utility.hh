/* Copyright 2015 OpenMarket Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef RSA_UTILITY_HH_
#define RSA_UTILITY_HH_

#include "olm/error.h"

#include <cstddef>
#include <cstdint>

namespace olm {

struct RSAUtility {

    RSAUtility();

    OlmErrorCode last_error;

    /** Verify an accreditation signature. TO COMPLETE*/
    std::size_t accreditation_verify(
        std::uint8_t const * n, std::uint8_t const * e,
        std::uint8_t const * url, std::size_t url_length,
        std::uint8_t const * accreditation, std::size_t accreditation_length
    );

    std::size_t get_randomness_size();

    /** Return a new RSA signature key pair. TO COMPLETE*/
    std::size_t genKey_RSA(
        std::uint8_t * n, std::uint8_t * p,
        std::uint8_t * q, std::uint8_t * lcm, 
        std::uint8_t * random_buffer, std::size_t random_length
    );

};


} // namespace olm

#endif /* RSA_UTILITY_HH_ */
