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

/*
 * Copyright 2011 Google Inc.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.crypto.keysbackup.util

import java.math.BigInteger

/**
 * Ref: https://github.com/bitcoin-labs/bitcoin-mobile-android/blob/master/src/bitcoinj/java/com/google/bitcoin/core/Base58.java
 *
 *
 * A custom form of base58 is used to encode BitCoin addresses. Note that this is not the same base58 as used by
 * Flickr, which you may see reference to around the internet.
 *
 * Satoshi says: why base-58 instead of standard base-64 encoding?
 *
 *  * Don't want 0OIl characters that look the same in some fonts and
 * could be used to create visually identical looking account numbers.
 *  * A string with non-alphanumeric characters is not as easily accepted as an account number.
 *  * E-mail usually won't line-break if there's no punctuation to break at.
 *  * Doubleclicking selects the whole number as one word if it's all alphanumeric.
 *
 */
private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
private val BASE = BigInteger.valueOf(58)

/**
 * Encode a byte array to a human readable string with base58 chars
 */
fun base58encode(input: ByteArray): String {
    var bi = BigInteger(1, input)
    val s = StringBuffer()
    while (bi >= BASE) {
        val mod = bi.mod(BASE)
        s.insert(0, ALPHABET[mod.toInt()])
        bi = bi.subtract(mod).divide(BASE)
    }
    s.insert(0, ALPHABET[bi.toInt()])
    // Convert leading zeros too.
    for (anInput in input) {
        if (anInput.toInt() == 0) {
            s.insert(0, ALPHABET[0])
        } else {
            break
        }
    }
    return s.toString()
}

/**
 * Decode a base58 String to a byte array
 */
fun base58decode(input: String): ByteArray {
    var result = decodeToBigInteger(input).toByteArray()

    // Remove the first leading zero if any
    if (result[0] == 0.toByte()) {
        result = result.copyOfRange(1, result.size)
    }

    return result
}

private fun decodeToBigInteger(input: String): BigInteger {
    var bi = BigInteger.valueOf(0)
    // Work backwards through the string.
    for (i in input.length - 1 downTo 0) {
        val alphaIndex = ALPHABET.indexOf(input[i])
        bi = bi.add(BigInteger.valueOf(alphaIndex.toLong()).multiply(BASE.pow(input.length - 1 - i)))
    }
    return bi
}
