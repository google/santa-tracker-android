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

package com.google.android.apps.santatracker.tracker.api

import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.apps.santatracker.tracker.vo.Metadata
import com.google.android.apps.santatracker.tracker.vo.StreamEntry

/**
 * API interface for fetching the data from the remote server.
 */
interface SantaApi {

    /**
     * @returns a list of [Destination]. Returns an empty list even if there is an error fetching
     * the data.
     */
    fun getDestinations(language: String): List<Destination>

    /**
     * @returns a list of [StreamEntry]. Returns an empty list even if there is an error fetching
     * the data.
     */
    fun getStream(language: String): List<StreamEntry>

    /**
     * @returns a list of [Metadata]. Returns an empty list even if there is an error fetching
     * the data.
     */
    fun getMetadataList(language: String): List<Metadata>
}
