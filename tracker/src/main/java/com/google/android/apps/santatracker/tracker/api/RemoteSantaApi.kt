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

import android.content.Context
import com.google.android.apps.santatracker.tracker.parser.SantaParser
import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.apps.santatracker.tracker.vo.Metadata
import com.google.android.apps.santatracker.tracker.vo.StreamEntry
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import kotlin.concurrent.withLock

/**
 * Implementation of [SantaApi] that fetches the data from the remote server such as FirebaseStorage
 */
class RemoteSantaApi @Inject constructor (private val context: Context) : SantaApi {

    companion object {
        private const val TAG = "RemoteSantaApi"
        private const val ROUTE_JSON_PATH = "route/santa_%s.json"
    }

    override fun getDestinations(language: String): List<Destination> {
        loadJson(language)
        return destinationsField
    }

    override fun getStream(language: String): List<StreamEntry> {
        loadJson(language)
        return streamEntriesField
    }

    override fun getMetadataList(language: String): List<Metadata> {
        loadJson(language)
        return metadataListField
    }

    private val destinationsField: MutableList<Destination> = mutableListOf()
    private val streamEntriesField: MutableList<StreamEntry> = mutableListOf()
    private val metadataListField: MutableList<Metadata> = mutableListOf()
    private val lock = ReentrantLock()
    private var loadedJson: String? = null

    private fun loadJson(locale: String) {
        lock.withLock {
            // TODO(samstern): Configure timeout
            val fetcher = FirebaseStorageFetcher(context)
            val getDataTask = fetcher[ROUTE_JSON_PATH.format(locale), 5, TimeUnit.MINUTES]

            // Get the JSON data from Firebase Storage
            val data: String?
            try {
                data = Tasks.await(getDataTask, 60, TimeUnit.SECONDS)
            } catch (e: Exception) {
                SantaLog.w(TAG, "Santa Communication Error 0", e)
                return
            }
            // Check that data was retrieved or changed from the one already downloaded.
            if (data == null || data == loadedJson) {
                return
            }

            destinationsField.clear()
            streamEntriesField.clear()
            metadataListField.clear()

            val parser = SantaParser()
            val apiData = parser.parse(data.byteInputStream())

            apiData.destinations.forEach {
                destinationsField.add(it)
            }

            apiData.streamEntries.forEach {
                streamEntriesField.add(it)
            }

            apiData.metadata.forEach {
                metadataListField.add(it)
            }

            loadedJson = data
        }
    }
}
