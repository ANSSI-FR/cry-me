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

#ifndef PRG_UTILITY_HH_
#define PRG_UTILITY_HH_

#include "olm/error.h"
#include "olm/crypto.h"

#include <cstddef>
#include <cstdint>

namespace olm {

struct PRGUtility {

    PRGUtility();
    OlmErrorCode last_error;
    prg_t prg;

    /** Generate random bytes using the implemented PRG */
    std::size_t prg_init(std::uint8_t * entropy, std::size_t entropy_size, std::uint8_t * nounce, std::size_t nounce_size);

    std::uint32_t prg_entropy_size();

    std::uint32_t prg_needs_reseeding(std::uint32_t nb_bytes);

    std::size_t prg_reseed(std::uint8_t * entropy, std::size_t entropy_size);

    std::size_t get_random_bytes(std::uint8_t * arr, std::size_t arr_length);

};


} // namespace olm

#endif /* PRG_UTILITY_HH_ */