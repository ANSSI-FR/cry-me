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

import java.io.Closeable;

/**
 * A base class for Sessions with a YubiKey.
 * <p>
 * Subclasses should use their own type as the parameter T:
 * <pre>{@code class FooSession extends ApplicationSession<FooSession>}</pre>
 *
 * @param <T> the type of the subclass
 */
public abstract class ApplicationSession<T extends ApplicationSession<T>> implements Closeable {
    /**
     * Get the version of the Application from the YubiKey. This is typically the same as the YubiKey firmware, but can be versioned separately as well.
     *
     * @return the Application version
     */
    public abstract Version getVersion();

    /**
     * Check if a Feature is supported by the YubiKey.
     *
     * @param feature the Feature to check support for.
     * @return true if the Feature is supported, false if not.
     */
    public boolean supports(Feature<T> feature) {
        return feature.isSupportedBy(getVersion());
    }

    protected void require(Feature<T> feature) {
        if (!supports(feature)) {
            throw new UnsupportedOperationException(feature.getRequiredMessage());
        }
    }
}
