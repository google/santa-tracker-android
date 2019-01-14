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

package com.google.android.apps.santatracker.tracker.repository

import androidx.lifecycle.LiveData
import com.google.android.apps.santatracker.tracker.api.SantaApi
import com.google.android.apps.santatracker.tracker.db.SantaDatabase
import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.apps.santatracker.tracker.vo.DestinationTimestamp
import com.google.android.apps.santatracker.tracker.vo.Metadata
import com.google.android.apps.santatracker.tracker.vo.StreamEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for the data that needs to update the UI and the state of the santa
 * responsible for fetching event data as [LiveData] from either the local DB or the remote server.
 */
@Singleton
open class SantaDataRepository @Inject constructor(
    private val santaDatabase: SantaDatabase,
    private val santaApi: SantaApi
) {

    open fun ensureReady(language: String): Boolean {
        val metadataDao = santaDatabase.metadata()
        val cachedLanguage = metadataDao.get(Metadata.KEY_LANGUAGE)
        if (cachedLanguage == null || cachedLanguage != language) {
            reloadAll(language)
        }
        return santaDatabase.destination().count() >= 2
    }

    open fun loadDestinations(): List<Destination>? {
        return santaDatabase.destination().all()
    }

    open fun loadStreamEntries(): List<StreamEntry>? {
        return santaDatabase.stream().all()
    }

    open fun firstDestination(): DestinationTimestamp? {
        return santaDatabase.destination().first()
    }

    open fun lastDestination(): DestinationTimestamp? {
        return santaDatabase.destination().last()
    }

    fun reloadAll(language: String) {
        santaDatabase.runInTransaction {
            santaDatabase.destination().run {
                deleteAll()
                insertAll(santaApi.getDestinations(language))
            }
            santaDatabase.stream().run {
                deleteAll()
                insertAll(santaApi.getStream(language))
            }
            santaDatabase.metadata().run {
                deleteAll()
                insertAll(santaApi.getMetadataList(language))
            }
        }
    }
}
