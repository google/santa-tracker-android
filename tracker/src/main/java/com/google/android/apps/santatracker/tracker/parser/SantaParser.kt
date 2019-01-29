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

import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.apps.santatracker.tracker.vo.Metadata
import com.google.android.apps.santatracker.tracker.vo.StreamEntry
import com.google.gson.GsonBuilder
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Parses JSON files from Santa.
 */
class SantaParser {

    data class ApiData(
        val metadata: List<Metadata>,
        val destinations: List<Destination>,
        val streamEntries: List<StreamEntry>
    )

    private fun apiResponseToApiData(response: ApiResponse): ApiData {
        val metadata = listOf(
                Metadata(Metadata.KEY_STATUS, response.status),
                Metadata(Metadata.KEY_LANGUAGE, response.language),
                Metadata(Metadata.KEY_FINGERPRINT, response.fingerprint)

                // TODO(samstern): Metadata only accepts a String but TimeOffset is a Long...
                // callback.onMetadata(Metadata(Metadata.KEY_TIME_OFFSET, response.timeOffset))
        )

        val destinations: List<Destination> = response.destinations.map {
            Destination(it)
        }

        val regularStreamEntries: List<StreamEntry> = response.stream.map {
            StreamEntry(it)
        }

        val notificationStreamEntries: List<StreamEntry> = response.notificationStream.map {
            StreamEntry(it)
        }

        val streamEntries = arrayListOf<StreamEntry>()
        streamEntries.addAll(regularStreamEntries)
        streamEntries.addAll(notificationStreamEntries)

        return ApiData(metadata, destinations, streamEntries)
    }

    /**
     * Parses a JSON from Santa.
     *
     * @param input The input.
     */
    fun parse(input: InputStream): ApiData {
        val reader = InputStreamReader(input, "UTF-8")
        val gson = GsonBuilder().create()

        val response = gson.fromJson<ApiResponse>(reader, ApiResponse::class.java)
        return apiResponseToApiData(response)
    }
}
