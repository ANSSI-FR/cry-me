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

package org.matrix.android.sdk.session.room.timeline

import kotlinx.coroutines.runBlocking
import org.amshove.kluent.internal.assertEquals
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.MethodSorters
import org.matrix.android.sdk.InstrumentedTest
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.isTextMessage
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageTextContent
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.common.CommonTestHelper
import org.matrix.android.sdk.common.CryptoTestHelper
import org.matrix.android.sdk.common.TestConstants

@RunWith(JUnit4::class)
@FixMethodOrder(MethodSorters.JVM)
class TimelineSimpleBackPaginationTest : InstrumentedTest {

    private val commonTestHelper = CommonTestHelper(context())
    private val cryptoTestHelper = CryptoTestHelper(commonTestHelper)

    @Test
    fun timeline_backPaginate_shouldReachEndOfTimeline() {
        val numberOfMessagesToSent = 200

        val cryptoTestData = cryptoTestHelper.doE2ETestWithAliceAndBobInARoom(false)

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession!!
        val roomId = cryptoTestData.roomId

        aliceSession.cryptoService().setWarnOnUnknownDevices(false)
        bobSession.cryptoService().setWarnOnUnknownDevices(false)

        val roomFromAlicePOV = aliceSession.getRoom(roomId)!!
        val roomFromBobPOV = bobSession.getRoom(roomId)!!

        // Alice sends X messages
        val message = "Message from Alice"
        commonTestHelper.sendTextMessage(
                roomFromAlicePOV,
                message,
                numberOfMessagesToSent)

        val bobTimeline = roomFromBobPOV.createTimeline(null, TimelineSettings(30))
        bobTimeline.start()

        commonTestHelper.waitWithLatch(timeout = TestConstants.timeOutMillis * 10) {
            val listener = object : Timeline.Listener {

                override fun onStateUpdated(direction: Timeline.Direction, state: Timeline.PaginationState) {
                    if (direction == Timeline.Direction.FORWARDS) {
                        return
                    }
                    if (state.hasMoreToLoad && !state.loading) {
                        bobTimeline.paginate(Timeline.Direction.BACKWARDS, 30)
                    } else if (!state.hasMoreToLoad) {
                        bobTimeline.removeListener(this)
                        it.countDown()
                    }
                }
            }
            bobTimeline.addListener(listener)
            bobTimeline.paginate(Timeline.Direction.BACKWARDS, 30)
        }
        assertEquals(false, bobTimeline.hasMoreToLoad(Timeline.Direction.FORWARDS))
        assertEquals(false, bobTimeline.hasMoreToLoad(Timeline.Direction.BACKWARDS))

        val onlySentEvents = runBlocking {
            bobTimeline.getSnapshot()
        }
                .filter {
                    it.root.isTextMessage()
                }.filter {
                    (it.root.content.toModel<MessageTextContent>())?.body?.startsWith(message).orFalse()
                }
        assertEquals(numberOfMessagesToSent, onlySentEvents.size)

        bobTimeline.dispose()
        cryptoTestData.cleanUp(commonTestHelper)
    }
}
