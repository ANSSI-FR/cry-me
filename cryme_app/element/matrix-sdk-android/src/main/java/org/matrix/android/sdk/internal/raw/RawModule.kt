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
 *
 */

package org.matrix.android.sdk.internal.raw

import com.zhuinden.monarchy.Monarchy
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import io.realm.RealmConfiguration
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.internal.database.RealmKeysUtils
import org.matrix.android.sdk.internal.di.GlobalDatabase
import org.matrix.android.sdk.internal.di.MatrixScope
import org.matrix.android.sdk.internal.di.Unauthenticated
import org.matrix.android.sdk.internal.network.RetrofitFactory

@Module
internal abstract class RawModule {

    @Module
    companion object {
        private const val DB_ALIAS = "matrix-sdk-global"

        @JvmStatic
        @Provides
        @GlobalDatabase
        fun providesMonarchy(@GlobalDatabase realmConfiguration: RealmConfiguration): Monarchy {
            return Monarchy.Builder()
                    .setRealmConfiguration(realmConfiguration)
                    .build()
        }

        @JvmStatic
        @Provides
        @GlobalDatabase
        @MatrixScope
        fun providesRealmConfiguration(realmKeysUtils: RealmKeysUtils): RealmConfiguration {
            return RealmConfiguration.Builder()
                    .apply {
                        realmKeysUtils.configureEncryption(this, DB_ALIAS)
                    }
                    .name("matrix-sdk-global.realm")
                    .schemaVersion(GlobalRealmMigration.SCHEMA_VERSION)
                    .migration(GlobalRealmMigration)
                    .allowWritesOnUiThread(true)
                    .modules(GlobalRealmModule())
                    .build()
        }

        @Provides
        @JvmStatic
        fun providesRawAPI(@Unauthenticated okHttpClient: Lazy<OkHttpClient>,
                           retrofitFactory: RetrofitFactory): RawAPI {
            return retrofitFactory.create(okHttpClient, "https://example.org").create(RawAPI::class.java)
        }
    }

    @Binds
    abstract fun bindRawService(service: DefaultRawService): RawService

    @Binds
    abstract fun bindGetUrlTask(task: DefaultGetUrlTask): GetUrlTask

    @Binds
    abstract fun bindCleanRawCacheTask(task: DefaultCleanRawCacheTask): CleanRawCacheTask
}
