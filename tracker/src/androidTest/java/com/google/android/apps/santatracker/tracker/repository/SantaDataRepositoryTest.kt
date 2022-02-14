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

import androidx.room.Room
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.google.android.apps.santatracker.tracker.api.SantaApi
import com.google.android.apps.santatracker.tracker.db.SantaDatabase
import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.apps.santatracker.tracker.vo.DestinationLocation
import com.google.android.apps.santatracker.tracker.vo.Metadata
import com.google.android.apps.santatracker.tracker.vo.StreamEntry
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class SantaDataRepositoryTest {

    companion object {
        const val LANGUAGE = "en"
    }

    private val database =
            Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getTargetContext(),
                    SantaDatabase::class.java).build()
    private val destination = Destination(
            id = "destination",
            arrival = 0,
            departure = 1,
            presentsDelivered = 2,
            population = 3,
            city = "city",
            region = "region",
            location = DestinationLocation(4.0, 5.0),
            timezone = 6,
            altitude = 7.0,
            weather = null,
            streetView = null,
            gmmStreetView = null,
            photo = null
    )
    private val streamEntry = StreamEntry(
            timestamp = 0,
            type = StreamEntry.TYPE_STATUS,
            notification = true,
            content = "content"
    )
    private val metadata = Metadata(
            key = Metadata.KEY_LANGUAGE,
            content = "en")

    private lateinit var repository: SantaDataRepository
    private lateinit var mockSantaApi: SantaApi

    @Before
    fun init() {
        mockSantaApi = mock(SantaApi::class.java)
        `when`(mockSantaApi.getDestinations(LANGUAGE)).thenReturn(listOf(destination))
        `when`(mockSantaApi.getStream(LANGUAGE)).thenReturn(listOf(streamEntry))
        `when`(mockSantaApi.getMetadataList((LANGUAGE))).thenReturn(listOf(metadata))
        repository = SantaDataRepository(database, mockSantaApi)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testFetchFromNetwork() {
        // Metadata isn't stored in the local DB, the data should be fetched from the remote source
        assertThat(repository.ensureReady(LANGUAGE), `is`(false)) // we only have 1 dummy destination
        verify(mockSantaApi).getDestinations(LANGUAGE)
        verify(mockSantaApi).getStream(LANGUAGE)
        verify(mockSantaApi).getMetadataList(LANGUAGE)
        assertThat(repository.loadDestinations(), `is`(listOf(destination)))
        assertThat(repository.loadStreamEntries(), `is`(listOf(streamEntry)))
    }

    @Test
    fun testFetchDestinationsFromLocalDB() {
        database.metadata().insertAll(listOf(metadata))
        database.destination().insertAll(listOf(destination))

        // Since the same language metadata is already stored in the local DB,
        // the network calls shouldn't happen
        repository.ensureReady("en")
        val destinations = repository.loadDestinations()
        verify(mockSantaApi, never()).getDestinations(LANGUAGE)
        verify(mockSantaApi, never()).getStream(LANGUAGE)
        verify(mockSantaApi, never()).getMetadataList(LANGUAGE)
        assertThat(destinations, `is`(listOf(destination)))
    }
}
