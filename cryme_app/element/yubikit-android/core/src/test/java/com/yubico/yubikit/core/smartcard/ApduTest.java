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

package com.yubico.yubikit.core.smartcard;

import org.junit.Assert;
import org.junit.Test;

public class ApduTest {
    @Test
    public void testMixedBytesAndInts() {
        byte cla = 0x7f;
        byte ins = (byte) 0xff;
        int p1 = 0x7f;
        int p2 = 0xff;
        Apdu apdu = new Apdu(cla, ins, p1, p2, null);

        Assert.assertEquals(cla, apdu.getCla());
        Assert.assertEquals(ins, apdu.getIns());
        Assert.assertEquals(cla, apdu.getP1());
        Assert.assertEquals(ins, apdu.getP2());
    }
}
