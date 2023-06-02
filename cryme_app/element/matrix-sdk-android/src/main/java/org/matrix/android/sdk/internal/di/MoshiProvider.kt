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

package org.matrix.android.sdk.internal.di

import com.squareup.moshi.Moshi
import org.matrix.android.sdk.api.session.room.model.message.MessageAudioContent
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageDefaultContent
import org.matrix.android.sdk.api.session.room.model.message.MessageEmoteContent
import org.matrix.android.sdk.api.session.room.model.message.MessageFileContent
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageLocationContent
import org.matrix.android.sdk.api.session.room.model.message.MessageNoticeContent
import org.matrix.android.sdk.api.session.room.model.message.MessagePollResponseContent
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.matrix.android.sdk.internal.network.parsing.CipherSuiteMoshiAdapter
import org.matrix.android.sdk.internal.network.parsing.ForceToBooleanJsonAdapter
import org.matrix.android.sdk.internal.network.parsing.RuntimeJsonAdapterFactory
import org.matrix.android.sdk.internal.network.parsing.TlsVersionMoshiAdapter
import org.matrix.android.sdk.internal.network.parsing.UriMoshiAdapter
import org.matrix.android.sdk.internal.session.sync.parsing.DefaultLazyRoomSyncEphemeralJsonAdapter

object MoshiProvider {

    private val moshi: Moshi = Moshi.Builder()
            .add(UriMoshiAdapter())
            .add(ForceToBooleanJsonAdapter())
            .add(CipherSuiteMoshiAdapter())
            .add(TlsVersionMoshiAdapter())
            // Use addLast here so we can inject a SplitLazyRoomSyncJsonAdapter later to override the default parsing.
            .addLast(DefaultLazyRoomSyncEphemeralJsonAdapter())
            .add(RuntimeJsonAdapterFactory.of(MessageContent::class.java, "msgtype", MessageDefaultContent::class.java)
                    .registerSubtype(MessageTextContent::class.java, MessageType.MSGTYPE_TEXT)
                    .registerSubtype(MessageNoticeContent::class.java, MessageType.MSGTYPE_NOTICE)
                    .registerSubtype(MessageEmoteContent::class.java, MessageType.MSGTYPE_EMOTE)
                    .registerSubtype(MessageAudioContent::class.java, MessageType.MSGTYPE_AUDIO)
                    .registerSubtype(MessageImageContent::class.java, MessageType.MSGTYPE_IMAGE)
                    .registerSubtype(MessageVideoContent::class.java, MessageType.MSGTYPE_VIDEO)
                    .registerSubtype(MessageLocationContent::class.java, MessageType.MSGTYPE_LOCATION)
                    .registerSubtype(MessageFileContent::class.java, MessageType.MSGTYPE_FILE)
                    .registerSubtype(MessageVerificationRequestContent::class.java, MessageType.MSGTYPE_VERIFICATION_REQUEST)
                    .registerSubtype(MessagePollResponseContent::class.java, MessageType.MSGTYPE_POLL_RESPONSE)
            )
            .add(SerializeNulls.JSON_ADAPTER_FACTORY)
            .build()

    fun providesMoshi(): Moshi {
        return moshi
    }
}
