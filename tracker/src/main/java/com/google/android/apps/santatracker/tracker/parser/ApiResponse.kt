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

package com.google.android.apps.santatracker.tracker.parser

import androidx.annotation.Keep
import com.google.android.apps.santatracker.tracker.vo.DestinationLocation
import com.google.android.apps.santatracker.tracker.vo.DestinationPhoto
import com.google.android.apps.santatracker.tracker.vo.DestinationStreetView
import com.google.android.apps.santatracker.tracker.vo.DestinationWeather
import com.google.android.apps.santatracker.tracker.vo.StreamEntry

/**
 * Raw response from Santa API, used for Gson deserialization.
 */
@Keep
data class ApiResponse(

    /**
     * For these four fields see Metadata.kt
     */
    val status: String,
    val language: String,
    val timeOffset: Long,
    val fingerprint: String,

    /**
     * Santa's destinations.
     */
    val destinations: List<ApiDestination>,

    /**
     * Stream events and notification events.
     */
    val stream: List<ApiStreamEntry>,
    val notificationStream: List<ApiNotificationStreamEntry>

)

@Keep
data class ApiDestination(

    val id: String,
    val arrival: Long,
    val departure: Long,
    val population: Long = 0,
    val presentsDelivered: Long = 0,
    val city: String,
    val region: String,
    val location: DestinationLocation,
    val details: ApiDestinationDetails
)

@Keep
data class ApiDestinationDetails(

    val timezone: Long?,
    val altitude: Double = 0.0,

    val weather: DestinationWeather?,
    val streetView: DestinationStreetView?,
    val gmmStreetView: DestinationStreetView?,
    val photos: List<ApiDestinationPhoto>?

) {
    fun firstPhoto(): DestinationPhoto? {
        return when {
            photos == null -> null
            photos.isEmpty() -> null
            else -> DestinationPhoto(photos[0])
        }
    }
}

@Keep
data class ApiDestinationPhoto(
    val url: String,

        // Some JSON files may not capitalize this attribute, so we
        // take either and pick only one.
    private val attributionHtml: String?,
    private val attributionhtml: String?

) {
    fun getAttribution(): String {
        return when {
            attributionHtml != null -> attributionHtml
            attributionhtml != null -> attributionhtml
            else -> ""
        }
    }
}

@Keep
data class ApiStreamEntry(

    val timestamp: Long,

    val status: String?,
    private val didyouknow: String?,
    private val imageUrl: String?,
    private val youtubeId: String?

) {
    fun getType(): Int {
        return when {
            status != null -> StreamEntry.TYPE_STATUS
            didyouknow != null -> StreamEntry.TYPE_DID_YOU_KNOW
            imageUrl != null -> StreamEntry.TYPE_IMAGE_URL
            youtubeId != null -> StreamEntry.TYPE_YOUTUBE_ID
            else -> 0
        }
    }

    fun getContent(): String {
        return when (getType()) {
            StreamEntry.TYPE_STATUS -> status!!
            StreamEntry.TYPE_DID_YOU_KNOW -> didyouknow!!
            StreamEntry.TYPE_IMAGE_URL -> imageUrl!!
            StreamEntry.TYPE_YOUTUBE_ID -> youtubeId!!
            else -> ""
        }
    }
}

@Keep
data class ApiNotificationStreamEntry(

    val timestamp: Long,

    val status: String?,
    private val didyouknow: String?,
    private val imageUrl: String?,
    private val youtubeId: String?

) {
    fun getType(): Int {
        return when {
            status != null -> StreamEntry.TYPE_STATUS
            didyouknow != null -> StreamEntry.TYPE_DID_YOU_KNOW
            imageUrl != null -> StreamEntry.TYPE_IMAGE_URL
            youtubeId != null -> StreamEntry.TYPE_YOUTUBE_ID
            else -> 0
        }
    }

    fun getContent(): String {
        return when (getType()) {
            StreamEntry.TYPE_STATUS -> status!!
            StreamEntry.TYPE_DID_YOU_KNOW -> didyouknow!!
            StreamEntry.TYPE_IMAGE_URL -> imageUrl!!
            StreamEntry.TYPE_YOUTUBE_ID -> youtubeId!!
            else -> ""
        }
    }
}
