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
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.matrix.android.sdk.InstrumentedTest

@RunWith(AndroidJUnit4::class)
internal class JsonCanonicalizerTest : InstrumentedTest {

    @Test
    fun identityTest() {
        listOf(
                "{}",
                """{"a":true}""",
                """{"a":false}""",
                """{"a":1}""",
                """{"a":1.2}""",
                """{"a":null}""",
                """{"a":[]}""",
                """{"a":["b":"c"]}""",
                """{"a":["c":"b","d":"e"]}""",
                """{"a":["d":"b","c":"e"]}"""
        ).forEach {
            assertEquals(it,
                    JsonCanonicalizer.canonicalize(it))
        }
    }

    @Test
    fun reorderTest() {
        assertEquals("""{"a":true,"b":false}""",
                JsonCanonicalizer.canonicalize("""{"b":false,"a":true}"""))
    }

    @Test
    fun realSampleTest() {
        assertEquals("""{"algorithms":["m.megolm.v1.aes-sha","m.olm.v1.wei25519-aes-sha"],"device_id":"VSCUNFSOUI","keys":{"wei25519:VSCUNFSOUI":"utyOjnhiQ73qNhi9HlN0OgWIowe5gthTS8r0r9TcJ3o","ed25519:VSCUNFSOUI":"qNhEt+Yggaajet0hX\/FjTRLfySgs65ldYyomm7PIx6U"},"user_id":"@benoitx:matrix.org"}""",
                JsonCanonicalizer.canonicalize("""{"algorithms":["m.megolm.v1.aes-sha","m.olm.v1.wei25519-aes-sha"],"device_id":"VSCUNFSOUI","user_id":"@benoitx:matrix.org","keys":{"wei25519:VSCUNFSOUI":"utyOjnhiQ73qNhi9HlN0OgWIowe5gthTS8r0r9TcJ3o","ed25519:VSCUNFSOUI":"qNhEt+Yggaajet0hX/FjTRLfySgs65ldYyomm7PIx6U"}}"""))
    }

    @Test
    fun doubleQuoteTest() {
        assertEquals("{\"a\":\"\\\"\"}",
                JsonCanonicalizer.canonicalize("{\"a\":\"\\\"\"}"))
    }

    /* ==========================================================================================
     * Test from https://matrix.org/docs/spec/appendices.html#examples
     * ========================================================================================== */

    @Test
    fun matrixOrg001Test() {
        assertEquals("""{}""",
                JsonCanonicalizer.canonicalize("""{}"""))
    }

    @Test
    fun matrixOrg002Test() {
        assertEquals("""{"one":1,"two":"Two"}""",
                JsonCanonicalizer.canonicalize("""{
    "one": 1,
    "two": "Two"
}"""))
    }

    @Test
    fun matrixOrg003Test() {
        assertEquals("""{"a":"1","b":"2"}""",
                JsonCanonicalizer.canonicalize("""{
    "b": "2",
    "a": "1"
}"""))
    }

    @Test
    fun matrixOrg004Test() {
        assertEquals("""{"a":"1","b":"2"}""",
                JsonCanonicalizer.canonicalize("""{"b":"2","a":"1"}"""))
    }

    @Test
    fun matrixOrg005Test() {
        assertEquals("""{"auth":{"mxid":"@john.doe:example.com","profile":{"display_name":"John Doe","three_pids":[{"address":"john.doe@example.org","medium":"email"},{"address":"123456789","medium":"msisdn"}]},"success":true}}""",
                JsonCanonicalizer.canonicalize("""{
    "auth": {
        "success": true,
        "mxid": "@john.doe:example.com",
        "profile": {
            "display_name": "John Doe",
            "three_pids": [
                {
                    "medium": "email",
                    "address": "john.doe@example.org"
                },
                {
                    "medium": "msisdn",
                    "address": "123456789"
                }
            ]
        }
    }
}"""))
    }

    @Test
    fun matrixOrg006Test() {
        assertEquals("""{"a":"日本語"}""",
                JsonCanonicalizer.canonicalize("""{
    "a": "日本語"
}"""))
    }

    @Test
    fun matrixOrg007Test() {
        assertEquals("""{"日":1,"本":2}""",
                JsonCanonicalizer.canonicalize("""{
    "本": 2,
    "日": 1
}"""))
    }

    @Test
    fun matrixOrg008Test() {
        assertEquals("""{"a":"日"}""",
                JsonCanonicalizer.canonicalize("{\"a\": \"\u65E5\"}"))
    }

    @Test
    fun matrixOrg009Test() {
        assertEquals("""{"a":null}""",
                JsonCanonicalizer.canonicalize("""{
    "a": null
}"""))
    }
}
