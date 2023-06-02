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
 * Copyright (C) 2019 Yubico.
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

package com.yubico.yubikit.oath;

import com.yubico.yubikit.core.Version;
import com.yubico.yubikit.core.application.ApplicationNotAvailableException;
import com.yubico.yubikit.core.application.ApplicationSession;
import com.yubico.yubikit.core.application.BadResponseException;
import com.yubico.yubikit.core.application.Feature;
import com.yubico.yubikit.core.smartcard.Apdu;
import com.yubico.yubikit.core.smartcard.ApduException;
import com.yubico.yubikit.core.smartcard.SW;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.core.smartcard.SmartCardProtocol;
import com.yubico.yubikit.core.util.RandomUtils;
import com.yubico.yubikit.core.util.Tlv;
import com.yubico.yubikit.core.util.Tlvs;

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Communicates with the OATH application on a YubiKey.
 *
 * <a href="https://developers.yubico.com/OATH/YKOATH_Protocol.html">Protocol specification</a>.
 * This application may optionally have an Access Key set, in which case most commands will be
 * locked until {@link #unlock} has been invoked. Note that {@link #reset()} can always be called,
 * regardless of if an Access Key is set or not.
 */
public class OathSession extends ApplicationSession<OathSession> {
    // Features
    /**
     * Support for credentials that require touch to use.
     */
    public static final Feature<OathSession> FEATURE_TOUCH = new Feature.Versioned<>("Touch", 4, 2, 0);
    /**
     * Support for credentials using the SHA-512 hash algorithm.
     */
    public static final Feature<OathSession> FEATURE_SHA512 = new Feature.Versioned<>("SHA-512", 4, 3, 1);
    /**
     * Support for renaming a stored credential.
     */
    public static final Feature<OathSession> FEATURE_RENAME = new Feature.Versioned<>("Rename Credential", 5, 3, 0);

    // Tlv tags YKOATH data
    private static final int TAG_NAME = 0x71;
    private static final int TAG_KEY = 0x73;
    private static final int TAG_RESPONSE = 0x75;
    private static final int TAG_PROPERTY = 0x78;
    private static final int TAG_IMF = 0x7a;
    private static final int TAG_CHALLENGE = 0x74;
    private static final int TAG_VERSION = 0x79;

    // Instruction bytes for APDU commands
    private static final byte INS_LIST = (byte) 0xa1;
    private static final byte INS_PUT = 0x01;
    private static final byte INS_DELETE = 0x02;
    private static final byte INS_SET_CODE = 0x03;
    private static final byte INS_RESET = 0x04;
    private static final byte INS_RENAME = 0x05;
    private static final byte INS_CALCULATE = (byte) 0xa2;
    private static final byte INS_VALIDATE = (byte) 0xa3;
    private static final byte INS_CALCULATE_ALL = (byte) 0xa4;
    private static final byte INS_SEND_REMAINING = (byte) 0xa5;

    private static final byte PROPERTY_REQUIRE_TOUCH = (byte) 0x02;

    private static final byte[] AID = new byte[]{(byte) 0xa0, 0x00, 0x00, 0x05, 0x27, 0x21, 0x01, 0x01};

    private static final long MILLS_IN_SECOND = 1000;
    private static final int DEFAULT_TOTP_PERIOD = 30;
    private static final int CHALLENGE_LEN = 8;
    private static final int ACCESS_KEY_LEN = 16;

    private final SmartCardProtocol protocol;
    private final Version version;

    private String deviceId;
    private byte[] salt;
    @Nullable
    private byte[] challenge;

    /**
     * Establishes a new session with a YubiKeys OATH application.
     *
     * @param connection to the YubiKey
     * @throws IOException                      in case of connection error
     * @throws ApplicationNotAvailableException if the application is missing or disabled
     */
    public OathSession(SmartCardConnection connection) throws IOException, ApplicationNotAvailableException {
        protocol = new SmartCardProtocol(connection, INS_SEND_REMAINING);
        SelectResponse selectResponse = new SelectResponse(protocol.select(AID));
        version = selectResponse.version;
        deviceId = selectResponse.getDeviceId();
        salt = selectResponse.salt;
        challenge = selectResponse.challenge;
        protocol.enableWorkarounds(version);
    }

    @Override
    public void close() throws IOException {
        protocol.close();
    }

    @Override
    public Version getVersion() {
        return version;
    }

    /**
     * Returns a unique ID which can be used to identify a particular YubiKey.
     * <p>
     * This ID is randomly generated upon invocation of {@link #reset()}.
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Resets the application, deleting all credentials and removing any lock code.
     *
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public void reset() throws IOException, ApduException {
        protocol.sendAndReceive(new Apdu(0, INS_RESET, 0xde, 0xad, null));
        try {
            // Re-select since the device ID has changed
            SelectResponse selectResponse = new SelectResponse(protocol.select(AID));
            deviceId = selectResponse.getDeviceId();
            salt = selectResponse.salt;
            challenge = null;
        } catch (ApplicationNotAvailableException e) {
            throw new IllegalStateException(e);  // This shouldn't happen
        }
    }

    /**
     * Returns true if an Access Key is currently set.
     */
    public boolean hasAccessKey() {
        return challenge != null && challenge.length != 0;
    }

    /**
     * Unlocks other commands when an Access Key is set, using a password to derive the Access Key.
     * <p>
     * Once unlocked, the application will remain unlocked for the duration of the session.
     * See the YKOATH protocol specification for further details.
     *
     * @param password user-supplied password
     * @return true if password valid
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public boolean unlock(char[] password) throws IOException, ApduException {
        if (hasAccessKey() && (password.length == 0)) {
            return false;
        }

        byte[] secret = deriveAccessKey(password);
        try {
            return unlock(challenge -> doHmacSha1(secret, challenge));
        } finally {
            Arrays.fill(secret, (byte) 0);
        }
    }

    /**
     * Unlocks other commands when an Access Key is set.
     * <p>
     * Once unlocked, the application will remain unlocked for the duration of the session.
     * See the YKOATH protocol specification for further details.
     *
     * @param validator to provide a correct response to a challenge, using the Access Key.
     * @return if the command was successful or not
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public boolean unlock(AccessKey validator) throws IOException, ApduException {
        // if no validation/authentication required we consider that validation was successful
        if (challenge == null) {
            return true;
        }

        try {
            Map<Integer, byte[]> request = new LinkedHashMap<>();
            request.put(TAG_RESPONSE, validator.calculateResponse(challenge));

            byte[] clientChallenge = RandomUtils.getRandomBytes(CHALLENGE_LEN);
            request.put(TAG_CHALLENGE, clientChallenge);

            byte[] data = protocol.sendAndReceive(new Apdu(0, INS_VALIDATE, 0, 0, Tlvs.encodeMap(request)));
            Map<Integer, byte[]> map = Tlvs.decodeMap(data);
            // return false if response from validation does not match verification
            return (MessageDigest.isEqual(validator.calculateResponse(clientChallenge), map.get(TAG_RESPONSE)));
        } catch (ApduException e) {
            if (e.getSw() == SW.INCORRECT_PARAMETERS) {
                // key didn't recognize secret
                return false;
            }
            throw e;
        }
    }

    /**
     * Sets an Access Key derived from a password. Once a key is set, any usage of the credentials stored will
     * require the application to be unlocked via one of the validate methods. Also see {@link #setAccessKey(byte[])}.
     *
     * @param password user-supplied password to set, encoded as UTF-8 bytes
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public void setPassword(char[] password) throws IOException, ApduException {
        setAccessKey(deriveAccessKey(password));
    }

    /**
     * Sets an access key. Once an access key is set, any usage of the credentials stored will require the application
     * to be unlocked via one of the validate methods, which requires knowledge of the access key. Typically this key is
     * derived from a password (see {@link #deriveAccessKey}) and is set by instead using the
     * {@link #setPassword} method. This method sets the raw 16 byte key.
     *
     * @param key the shared secret key used to unlock access to the application
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public void setAccessKey(byte[] key) throws IOException, ApduException {
        if (key.length != ACCESS_KEY_LEN) {
            throw new IllegalArgumentException("Secret should be 16 bytes");
        }

        Map<Integer, byte[]> request = new LinkedHashMap<>();
        request.put(TAG_KEY, ByteBuffer.allocate(1 + key.length)
                .put((byte) (OathType.TOTP.value | HashAlgorithm.SHA1.value))
                .put(key)
                .array());

        byte[] challenge = RandomUtils.getRandomBytes(CHALLENGE_LEN);
        request.put(TAG_CHALLENGE, challenge);
        request.put(TAG_RESPONSE, doHmacSha1(key, challenge));

        protocol.sendAndReceive(new Apdu(0, INS_SET_CODE, 0, 0, Tlvs.encodeMap(request)));
    }

    /**
     * Removes the access key, if one is set.
     *
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public void deleteAccessKey() throws IOException, ApduException {
        protocol.sendAndReceive(new Apdu(0, INS_SET_CODE, 0, 0, new Tlv(TAG_KEY, null).getBytes()));
    }

    /**
     * Get a list of all Credentials stored on the YubiKey.
     *
     * @return list of credentials on device
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public List<Credential> getCredentials() throws IOException, ApduException {
        byte[] response = protocol.sendAndReceive(new Apdu(0, INS_LIST, 0, 0, null));
        List<Tlv> list = Tlvs.decodeList(response);
        List<Credential> result = new ArrayList<>();
        for (Tlv tlv : list) {
            result.add(new Credential(deviceId, new ListResponse(tlv)));
        }
        return result;
    }

    /**
     * Get a map of all Credentials stored on the YubiKey, together with a Code for each of them.
     * <p>
     * Credentials which use HOTP, or which require touch, will not be calculated.
     * They will still be present in the result, but with a null value.
     * The current system time will be used for TOTP calculation.
     *
     * @return a Map mapping Credentials to Code
     * @throws IOException          in case of connection error
     * @throws ApduException        in case of communication error
     * @throws BadResponseException in case of incorrect YubiKey response
     */
    public Map<Credential, Code> calculateCodes() throws IOException, ApduException, BadResponseException {
        return calculateCodes(System.currentTimeMillis());
    }

    /**
     * Get a map of all Credentials stored on the YubiKey, together with a Code for each of them.
     * <p>
     * Credentials which use HOTP, or which require touch, will not be calculated.
     * They will still be present in the result, but with a null value.
     *
     * @param timestamp the timestamp which is used as start point for TOTP
     * @return a Map mapping Credentials to Code
     * @throws IOException          in case of connection error
     * @throws ApduException        in case of communication error
     * @throws BadResponseException in case of incorrect YubiKey response
     */
    public Map<Credential, Code> calculateCodes(long timestamp) throws IOException, ApduException, BadResponseException {
        long timeStep = (timestamp / MILLS_IN_SECOND / DEFAULT_TOTP_PERIOD);
        byte[] challenge = ByteBuffer.allocate(CHALLENGE_LEN).putLong(timeStep).array();

        // using default period to 30 second for all _credentials and then recalculate those that have different period
        byte[] data = protocol.sendAndReceive(new Apdu(0, INS_CALCULATE_ALL, 0, 1, new Tlv(TAG_CHALLENGE, challenge).getBytes()));
        Iterator<Tlv> responseTlvs = Tlvs.decodeList(data).iterator();
        Map<Credential, Code> map = new HashMap<>();
        while (responseTlvs.hasNext()) {
            Tlv nameTlv = responseTlvs.next();
            if (nameTlv.getTag() != TAG_NAME) {
                throw new BadResponseException(String.format("Unexpected tag: %02x", nameTlv.getTag()));
            }
            byte[] credentialId = nameTlv.getValue();
            CalculateResponse response = new CalculateResponse(responseTlvs.next());

            // parse credential properties
            Credential credential = new Credential(deviceId, credentialId, response);

            if (credential.getOathType() == OathType.TOTP && credential.getPeriod() != DEFAULT_TOTP_PERIOD) {
                // recalculate credentials that have different period
                map.put(credential, calculateCode(credential, timestamp));
            } else if (response.response.length == 4) {
                // Note: codes are typically valid in 'DEFAULT_PERIOD' second slices
                // so the valid period actually starts before the calculation happens
                // and potentially might happen even way before (so that code is valid only 1 second after calculation)
                long validFrom = validFrom(timestamp, DEFAULT_TOTP_PERIOD);
                map.put(credential, new Code(formatTruncated(response), validFrom, validFrom + DEFAULT_TOTP_PERIOD * MILLS_IN_SECOND));
            } else {
                map.put(credential, null);
            }
        }

        return map;
    }

    /**
     * Calculate a full (non-truncated) HMAC signature using a Credential.
     * <p>
     * Using this command a Credential can be used as an HMAC key to calculate a result for an
     * arbitrary challenge. The hash algorithm specified for the Credential is used.
     *
     * @param credentialId the ID of a stored Credential
     * @param challenge    the input to the HMAC operation
     * @return the calculated response
     * @throws IOException          in case of connection error
     * @throws ApduException        in case of communication error
     * @throws BadResponseException in case an unexpected response was sent from the YubiKey
     */
    public byte[] calculateResponse(byte[] credentialId, byte[] challenge) throws IOException, ApduException, BadResponseException {
        Map<Integer, byte[]> request = new LinkedHashMap<>();
        request.put(TAG_NAME, credentialId);
        request.put(TAG_CHALLENGE, challenge);
        byte[] data = protocol.sendAndReceive(new Apdu(0, INS_CALCULATE, 0, 0, Tlvs.encodeMap(request)));
        byte[] response = Tlvs.unpackValue(TAG_RESPONSE, data);
        return Arrays.copyOfRange(response, 1, response.length);
    }

    /**
     * Returns a new Code for a stored Credential.
     * The current system time will be used for TOTP calculation.
     *
     * @param credential credential that will get new code
     * @return calculated code
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public Code calculateCode(Credential credential) throws IOException, ApduException {
        return calculateCode(credential, System.currentTimeMillis());
    }

    /**
     * Returns a new Code for a stored Credential.
     *
     * @param credential credential that will get new code
     * @param timestamp  the timestamp which is used as start point for TOTP, this is ignored for HOTP
     * @return a new code
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public Code calculateCode(Credential credential, @Nullable Long timestamp) throws IOException, ApduException {
        if (!credential.deviceId.equals(deviceId)) {
            throw new IllegalArgumentException("The given credential belongs to a different device!");
        }
        byte[] challenge = new byte[CHALLENGE_LEN];
        if (timestamp != null && credential.getPeriod() != 0) {
            long timeStep = (timestamp / MILLS_IN_SECOND / credential.getPeriod());
            ByteBuffer.wrap(challenge).putLong(timeStep);
        }

        Map<Integer, byte[]> requestTlv = new LinkedHashMap<>();
        requestTlv.put(TAG_NAME, credential.getId());
        requestTlv.put(TAG_CHALLENGE, challenge);
        byte[] data = protocol.sendAndReceive(new Apdu(0, INS_CALCULATE, 0, 1, Tlvs.encodeMap(requestTlv)));
        String value = formatTruncated(new CalculateResponse(Tlv.parse(data)));

        switch (credential.getOathType()) {
            case TOTP:
                long validFrom = validFrom(timestamp, credential.getPeriod());
                return new Code(value, validFrom, validFrom + credential.getPeriod() * MILLS_IN_SECOND);
            case HOTP:
            default:
                return new Code(value, System.currentTimeMillis(), Long.MAX_VALUE);
        }
    }

    /**
     * Adds a new Credential to the YubiKey.
     * <p>
     * The Credential ID (see {@link CredentialData#getId()}) must be unique to the YubiKey, or the
     * existing Credential with the same ID will be overwritten.
     * <p>
     * Setting requireTouch requires support for {@link #FEATURE_TOUCH}, available on YubiKey 4.2 or later.
     * Using SHA-512 requires support for {@link #FEATURE_SHA512}, available on YubiKey 4.3.1 or later.
     *
     * @param credentialData credential data to add
     * @param requireTouch   true if the credential should require touch to be used
     * @return the newly added Credential
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public Credential putCredential(CredentialData credentialData, boolean requireTouch) throws IOException, ApduException {
        if (credentialData.getHashAlgorithm() == HashAlgorithm.SHA512) {
            require(FEATURE_SHA512);
        }

        byte[] key = credentialData.getHashAlgorithm().prepareKey(credentialData.getSecret());
        Map<Integer, byte[]> requestTlvs = new LinkedHashMap<>();
        requestTlvs.put(TAG_NAME, credentialData.getId());

        requestTlvs.put(TAG_KEY, ByteBuffer.allocate(2 + key.length)
                .put((byte) (credentialData.getOathType().value | credentialData.getHashAlgorithm().value))
                .put((byte) credentialData.getDigits())
                .put(key)
                .array());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(Tlvs.encodeMap(requestTlvs));

        if (requireTouch) {
            require(FEATURE_TOUCH);
            output.write(TAG_PROPERTY);
            output.write(PROPERTY_REQUIRE_TOUCH);
        }

        if (credentialData.getOathType() == OathType.HOTP && credentialData.getCounter() > 0) {
            output.write(TAG_IMF);
            output.write(4);
            output.write(ByteBuffer.allocate(4).putInt(credentialData.getCounter()).array());
        }

        protocol.sendAndReceive(new Apdu(0x00, INS_PUT, 0, 0, output.toByteArray()));
        return new Credential(deviceId, credentialData.getId(), credentialData.getOathType(), requireTouch);
    }

    /**
     * Deletes an existing Credential from the YubiKey.
     *
     * @param credentialId the ID of the credential to remove
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public void deleteCredential(byte[] credentialId) throws IOException, ApduException {
        protocol.sendAndReceive(new Apdu(0x00, INS_DELETE, 0, 0, new Tlv(TAG_NAME, credentialId).getBytes()));
    }

    /**
     * Deletes an existing Credential from the YubiKey.
     *
     * @param credential the Credential to remove
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public void deleteCredential(Credential credential) throws IOException, ApduException {
        if (!credential.deviceId.equals(deviceId)) {
            throw new IllegalArgumentException("The given credential belongs to a different device!");
        }
        deleteCredential(credential.getId());
    }

    /**
     * Change the issuer and name of a Credential already stored on the YubiKey.
     * <p>
     * This functionality requires support for {@link #FEATURE_RENAME}, available on YubiKey 5.3 or later.
     *
     * @param credentialId    the ID of the credential to rename
     * @param newCredentialId the new ID to use
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public void renameCredential(byte[] credentialId, byte[] newCredentialId) throws IOException, ApduException {
        require(FEATURE_RENAME);
        protocol.sendAndReceive(new Apdu(0x00, INS_RENAME, 0, 0, Tlvs.encodeList(Arrays.asList(
                new Tlv(TAG_NAME, credentialId),
                new Tlv(TAG_NAME, newCredentialId)
        ))));
    }

    /**
     * Change the issuer and name of a Credential already stored on the YubiKey.
     * <p>
     * This functionality requires support for {@link #FEATURE_RENAME}, available on YubiKey 5.3 or later.
     *
     * @param credential  the Credential to rename
     * @param accountName the new name of the credential
     * @param issuer      the new issuer of the credential
     * @return the updated Credential
     * @throws IOException   in case of connection error
     * @throws ApduException in case of communication error
     */
    public Credential renameCredential(Credential credential, String accountName, @Nullable String issuer) throws IOException, ApduException {
        if (!credential.deviceId.equals(deviceId)) {
            throw new IllegalArgumentException("The given credential belongs to a different device!");
        }
        byte[] newId = CredentialIdUtils.formatId(issuer, accountName, credential.getOathType(), credential.getPeriod());
        renameCredential(credential.getId(), newId);
        return new Credential(
                credential.deviceId,
                newId,
                credential.getOathType(),
                credential.isTouchRequired()
        );
    }

    /**
     * Derives an access key from a password and the device-specific salt.
     * The key is derived by running 1000 rounds of PBKDF2 using the password and salt as inputs, with a 16 byte output.
     *
     * @param password a user-supplied password, encoded as UTF-8 bytes.
     * @return an access key for unlocking the session
     */
    public byte[] deriveAccessKey(char[] password) {
        PBEKeySpec keyspec = new PBEKeySpec(password, salt, 1000, ACCESS_KEY_LEN * 8);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return factory.generateSecret(keyspec).getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } finally {
            keyspec.clearPassword();
        }
    }

    /**
     * Calculates an HMAC-SHA1 response
     *
     * @param secret  the secret
     * @param message data in bytes
     * @return the MAC result
     */
    private static byte[] doHmacSha1(byte[] secret, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, mac.getAlgorithm()));
            return mac.doFinal(message);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculate the time when the valid period of code starts
     *
     * @param timestamp current timestamp
     * @param period    period in seconds for how long code is valid from its calculation/generation time
     * @return the start of period when code is valid
     */
    private static long validFrom(@Nullable Long timestamp, int period) {
        if (timestamp == null || period == 0) {
            // handling case to avoid ArithmeticException
            // we can't divide by zero, we can set that it valid from now
            // but its valid period ends the very same time
            return System.currentTimeMillis();
        }

        // Note: codes are valid in 'period' second slices
        // so the valid sliced period actually starts before the calculation happens
        // and potentially might happen even way before
        // (so that code is valid only 1 second after calculation)
        return timestamp - timestamp % (period * MILLS_IN_SECOND);
    }

    /**
     * Truncate result from key to have digits number of code
     *
     * @param data the code received within calculate or calculate all response
     * @return truncated code
     */
    private String formatTruncated(CalculateResponse data) {
        String result = Integer.toString(ByteBuffer.wrap(data.response).getInt());
        String value;
        // truncate result length (align it with value of digits)
        if (result.length() > data.digits) {
            // take last digits
            value = result.substring(result.length() - data.digits);
        } else if (result.length() < data.digits) {
            // or append 0 at the beginning of string
            value = String.format("%" + data.digits + "s", result).replace(' ', '0');
        } else {
            value = result;
        }
        return value;
    }

    static class ListResponse {
        final byte[] id;
        final OathType oathType;
        final HashAlgorithm hashAlgorithm;

        private ListResponse(Tlv tlv) {
            byte[] value = tlv.getValue();
            id = Arrays.copyOfRange(value, 1, value.length);
            oathType = OathType.fromValue((byte) (0xf0 & value[0]));
            hashAlgorithm = HashAlgorithm.fromValue((byte) (0x0f & value[0]));
        }
    }

    static class CalculateResponse {
        final byte responseType;
        final int digits;
        final byte[] response;

        private CalculateResponse(Tlv tlv) {
            responseType = (byte) tlv.getTag();
            byte[] value = tlv.getValue();
            digits = value[0];
            response = Arrays.copyOfRange(value, 1, value.length);
        }
    }

    private static class SelectResponse {
        private final Version version;
        private final byte[] salt;
        @Nullable
        private final byte[] challenge;

        /**
         * Creates an instance of OATH application info from SELECT response
         *
         * @param response the response from OATH SELECT command
         */
        SelectResponse(byte[] response) {
            Map<Integer, byte[]> map = Tlvs.decodeMap(response);
            version = Version.fromBytes(map.get(TAG_VERSION));
            salt = map.get(TAG_NAME);
            challenge = map.get(TAG_CHALLENGE);
        }

        private String getDeviceId() {
            MessageDigest messageDigest;
            try {
                messageDigest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                // Shouldn't happen.
                throw new IllegalStateException(e);
            }
            messageDigest.update(salt);
            byte[] digest = messageDigest.digest();
            return Base64.encodeBase64String(Arrays.copyOfRange(digest, 0, 16)).replaceAll("=", "");
        }
    }
}
