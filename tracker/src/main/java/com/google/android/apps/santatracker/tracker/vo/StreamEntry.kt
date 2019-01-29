/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.tracker.vo

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.apps.santatracker.tracker.parser.ApiNotificationStreamEntry
import com.google.android.apps.santatracker.tracker.parser.ApiStreamEntry

@Keep
@Entity(tableName = "stream_entries")
data class StreamEntry(
    @PrimaryKey
    val timestamp: Long,
    val type: Int,
    val notification: Boolean,
    val content: String
) : TrackerCard {

    override val value: Long
        get() = timestamp

    constructor(obj: ApiStreamEntry) : this(
            obj.timestamp,
            obj.getType(),
            false,
            obj.getContent()
    )

    constructor(obj: ApiNotificationStreamEntry) : this(
            obj.timestamp,
            obj.getType(),
            true,
            obj.getContent()
    )

    companion object {
        const val TYPE_IMAGE_URL = 1
        const val TYPE_STATUS = 2
        const val TYPE_YOUTUBE_ID = 3
        const val TYPE_DID_YOU_KNOW = 4
    }
}
