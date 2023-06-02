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

package com.yubico.yubikit.core.application;

import com.yubico.yubikit.core.Version;

/**
 * A feature for a YubiKey application, which may not be supported by all YubiKeys.
 *
 * @param <T> The type of Session for which the Feature is relevant.
 */
public abstract class Feature<T extends ApplicationSession<T>> {
    protected final String featureName;

    protected Feature(String featureName) {
        this.featureName = featureName;
    }

    /**
     * Get a human readable name of the feature.
     *
     * @return the name of the feature
     */
    public String getFeatureName() {
        return featureName;
    }

    /**
     * Checks if the Feature is supported by the given Application version.
     *
     * @param version the version of the Application to check support for.
     * @return true if the Feature is supported, false if not
     */
    public abstract boolean isSupportedBy(Version version);

    protected String getRequiredMessage() {
        return String.format("%s is not supported by this YubiKey", featureName);
    }

    /**
     * A Feature which has a minimum version which it checks against.
     *
     * @param <T> The type of Session for which the Feature is relevant.
     */
    public static class Versioned<T extends ApplicationSession<T>> extends Feature<T> {
        private final Version requiredVersion;

        public Versioned(String featureName, int major, int minor, int micro) {
            super(featureName);
            requiredVersion = new Version(major, minor, micro);
        }

        @Override
        protected String getRequiredMessage() {
            return String.format("%s requires YubiKey %s or later", featureName, requiredVersion);
        }

        @Override
        public boolean isSupportedBy(Version version) {
            return version.major == 0 || version.compareTo(requiredVersion) >= 0;
        }
    }
}
