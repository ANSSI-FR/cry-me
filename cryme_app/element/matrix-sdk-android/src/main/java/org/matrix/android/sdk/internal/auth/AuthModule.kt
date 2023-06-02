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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.auth

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import io.realm.RealmConfiguration
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.legacy.LegacySessionImporter
import org.matrix.android.sdk.internal.auth.db.AuthRealmMigration
import org.matrix.android.sdk.internal.auth.db.AuthRealmModule
import org.matrix.android.sdk.internal.auth.db.RealmPendingSessionStore
import org.matrix.android.sdk.internal.auth.db.RealmSessionParamsStore
import org.matrix.android.sdk.internal.auth.login.DefaultDirectLoginTask
import org.matrix.android.sdk.internal.auth.login.DirectLoginTask
import org.matrix.android.sdk.internal.database.RealmKeysUtils
import org.matrix.android.sdk.internal.di.AuthDatabase
import org.matrix.android.sdk.internal.legacy.DefaultLegacySessionImporter
import org.matrix.android.sdk.internal.wellknown.WellknownModule
import java.io.File

@Module(includes = [WellknownModule::class])
internal abstract class AuthModule {

    @Module
    companion object {
        private const val DB_ALIAS = "matrix-sdk-auth"

        @JvmStatic
        @Provides
        @AuthDatabase
        fun providesRealmConfiguration(context: Context, realmKeysUtils: RealmKeysUtils): RealmConfiguration {
            val old = File(context.filesDir, "matrix-sdk-auth")
            if (old.exists()) {
                old.renameTo(File(context.filesDir, "matrix-sdk-auth.realm"))
            }

            return RealmConfiguration.Builder()
                    .apply {
                        realmKeysUtils.configureEncryption(this, DB_ALIAS)
                    }
                    .name("matrix-sdk-auth.realm")
                    .modules(AuthRealmModule())
                    .schemaVersion(AuthRealmMigration.SCHEMA_VERSION)
                    .migration(AuthRealmMigration)
                    .build()
        }
    }

    @Binds
    abstract fun bindLegacySessionImporter(importer: DefaultLegacySessionImporter): LegacySessionImporter

    @Binds
    abstract fun bindSessionParamsStore(store: RealmSessionParamsStore): SessionParamsStore

    @Binds
    abstract fun bindPendingSessionStore(store: RealmPendingSessionStore): PendingSessionStore

    @Binds
    abstract fun bindAuthenticationService(service: DefaultAuthenticationService): AuthenticationService

    @Binds
    abstract fun bindSessionCreator(creator: DefaultSessionCreator): SessionCreator

    @Binds
    abstract fun bindDirectLoginTask(task: DefaultDirectLoginTask): DirectLoginTask

    @Binds
    abstract fun bindIsValidClientServerApiTask(task: DefaultIsValidClientServerApiTask): IsValidClientServerApiTask

    @Binds
    abstract fun bindHomeServerHistoryService(service: DefaultHomeServerHistoryService): HomeServerHistoryService
}
