/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.voicebroadcast.usecase

import im.vector.app.features.voicebroadcast.STATE_ROOM_VOICE_BROADCAST_INFO
import im.vector.app.features.voicebroadcast.model.MessageVoiceBroadcastInfoContent
import im.vector.app.features.voicebroadcast.model.VoiceBroadcastState
import im.vector.app.features.voicebroadcast.model.asVoiceBroadcastEvent
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toContent
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.relation.RelationDefaultContent
import timber.log.Timber
import javax.inject.Inject

class StopVoiceBroadcastUseCase @Inject constructor(
        private val session: Session,
) {

    suspend fun execute(roomId: String): Result<Unit> = runCatching {
        val room = session.getRoom(roomId) ?: error("Unknown roomId: $roomId")

        Timber.d("## StopVoiceBroadcastUseCase: Stop voice broadcast requested")

        val lastVoiceBroadcastEvent = room.stateService().getStateEvent(
                STATE_ROOM_VOICE_BROADCAST_INFO,
                QueryStringValue.Equals(session.myUserId)
        )?.asVoiceBroadcastEvent()
        when (val voiceBroadcastState = lastVoiceBroadcastEvent?.content?.voiceBroadcastState) {
            VoiceBroadcastState.STARTED,
            VoiceBroadcastState.PAUSED,
            VoiceBroadcastState.RESUMED -> stopVoiceBroadcast(room, lastVoiceBroadcastEvent.reference)
            else -> Timber.d("## StopVoiceBroadcastUseCase: Cannot stop voice broadcast: currentState=$voiceBroadcastState")
        }
    }

    private suspend fun stopVoiceBroadcast(room: Room, reference: RelationDefaultContent?) {
        Timber.d("## StopVoiceBroadcastUseCase: Send new voice broadcast info state event")
        room.stateService().sendStateEvent(
                eventType = STATE_ROOM_VOICE_BROADCAST_INFO,
                stateKey = session.myUserId,
                body = MessageVoiceBroadcastInfoContent(
                        relatesTo = reference,
                        voiceBroadcastStateStr = VoiceBroadcastState.STOPPED.value,
                ).toContent(),
        )

        // TODO stop recording audio files
    }
}
