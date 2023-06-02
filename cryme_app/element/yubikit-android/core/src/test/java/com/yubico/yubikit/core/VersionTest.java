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
 * Copyright (C) 2020 Yubico.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yubico.yubikit.core;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class VersionTest {
    @Test
    public void testCompare() {
        Version v100 = new Version(1, 0, 0);
        Version v102 = new Version(1, 0, 2);
        Version v110 = new Version(1, 1, 0);
        Version v123 = new Version(1, 2, 3);

        Assert.assertTrue(v100.isAtLeast(1, 0, 0));
        Assert.assertFalse(v100.isAtLeast(2, 0, 0));

        Assert.assertTrue(v100.isLessThan(1, 0, 1));
        Assert.assertTrue(v100.isLessThan(1, 1, 0));
        Assert.assertTrue(v100.isLessThan(2, 0, 0));
        Assert.assertFalse(v100.isLessThan(1, 0, 0));

        Assert.assertTrue(v102.isAtLeast(1, 0, 0));
        Assert.assertTrue(v102.isLessThan(1, 1, 0));
        Assert.assertFalse(v102.isLessThan(1, 0, 2));

        Assert.assertTrue(v110.compareTo(v123) < 0);
        Assert.assertTrue(v123.compareTo(v102) > 0);

        //noinspection EqualsWithItself
        Assert.assertEquals(0, v123.compareTo(v123));
    }

    @Test
    public void testOrdering() {
        Version[] versions = new Version[]{
                new Version(2, 0, 1),
                new Version(1, 0, 0),
                new Version(1, 2, 3),
                new Version(1, 0, 1),
                new Version(2, 0, 0),
                new Version(2, 1, 0),
                new Version(1, 1, 0),
                new Version(3, 0, 0),
                new Version(1, 0, 0),
        };

        Arrays.sort(versions);
        Assert.assertArrayEquals(new Version[]{
                new Version(1, 0, 0),
                new Version(1, 0, 0),
                new Version(1, 0, 1),
                new Version(1, 1, 0),
                new Version(1, 2, 3),
                new Version(2, 0, 0),
                new Version(2, 0, 1),
                new Version(2, 1, 0),
                new Version(3, 0, 0),
        }, versions);
    }
}
