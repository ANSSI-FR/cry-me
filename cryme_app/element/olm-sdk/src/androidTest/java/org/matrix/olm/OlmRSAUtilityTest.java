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

package org.matrix.olm;

import android.util.Log;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;


@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OlmRSAUtilityTest {
    private static final String LOG_TAG = "OlmRSAUtilityTest";

    private static OlmManager mOlmManager;

    private static String getCertificate(KeyPair keyPair) throws IOException, CertificateException, OperatorCreationException {
        X500Name x500Name = new X500Name("CN=***.com, OU=Security&Defense, O=*** Crypto., L=Ottawa, ST=Ontario, C=CA");
        SubjectPublicKeyInfo pubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        final Date start = new Date();
        final Date until = Date.from(LocalDate.now().plus(365, ChronoUnit.DAYS).atStartOfDay().toInstant(ZoneOffset.UTC));
        final X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(x500Name,
            new BigInteger(10, new SecureRandom()), start,   until,  x500Name,  pubKeyInfo
        );
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());

        Certificate certificate = new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider()).getCertificate(certificateBuilder.build(contentSigner));

        StringWriter sw = new StringWriter();
        try (PEMWriter pw = new PEMWriter(sw)) {
            pw.writeObject(certificate);
            pw.flush();
        }

        return sw.toString();
    }

    @BeforeClass
    public static void setUpClass() {
        // load native lib
        mOlmManager = new OlmManager();

        String version = mOlmManager.getOlmLibVersion();
        assertNotNull(version);
        Log.d(LOG_TAG, "## setUpClass(): lib version=" + version);
    }

    /**
     * Test Signature Verification
     */
    @Test
    public void testSignatureVerification() throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, CertificateException, OperatorCreationException {
        OlmRSAUtility rsa = null;
        for(int i=0; i< 20; i++) {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(512);

            KeyPair keyPair = keyGen.genKeyPair();
            RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey priv = (RSAPrivateKey) keyPair.getPrivate();

            String message = "https://www.server-element.net/";
            Signature sig = Signature.getInstance("SHA1withRSA/PSS");
            sig.initSign(priv);
            sig.update(message.getBytes("UTF8"));
            byte[] signatureBytes = sig.sign();

            String certificate = getCertificate(keyPair);
            String s = OlmUtility.byteArraytoHexString(signatureBytes);

            try{
                rsa = new OlmRSAUtility();
            }catch (Exception e){
                fail("OlmRSAUtility creation failed");
            }
            rsa.verifyAccreditation(s, message, certificate);

            rsa.releaseRSAUtility();
            assertTrue(rsa.isReleased());
        }
    }

    /**
     * Test Incorrect Signature Verification
     */
    @Test
    public void testIncorrectSignatureVerification() throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, CertificateException, OperatorCreationException {

        for(int i=0; i< 20; i++){
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(512);

            KeyPair keyPair = keyGen.genKeyPair();
            RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey priv = (RSAPrivateKey) keyPair.getPrivate();

            String message = "https://www.server-element.net/";
            Signature sig = Signature.getInstance("SHA1withRSA/PSS");
            sig.initSign(priv);
            sig.update(message.getBytes("UTF8"));
            byte[] signatureBytes = sig.sign();

            keyPair = keyGen.genKeyPair();

            String certificate = getCertificate(keyPair);
            String s = OlmUtility.byteArraytoHexString(signatureBytes);

            OlmRSAUtility rsa = new OlmRSAUtility();

            OlmException thrown = assertThrows(OlmException.class, () ->
                                    rsa.verifyAccreditation(s, message, certificate));

            assertEquals(thrown.getMessage(), "OLM_BAD_SERVER_ACCREDITATION");

            rsa.releaseRSAUtility();
            assertTrue(rsa.isReleased());
        }

    }
}
