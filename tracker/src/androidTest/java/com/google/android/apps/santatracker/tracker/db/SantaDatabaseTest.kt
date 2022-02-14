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

package com.google.android.apps.santatracker.tracker.db

import androidx.room.Room
import androidx.test.InstrumentationRegistry
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import com.google.android.apps.santatracker.tracker.util.JsonLoader
import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.apps.santatracker.tracker.vo.DestinationLocation
import com.google.android.apps.santatracker.tracker.vo.DestinationPhoto
import com.google.android.apps.santatracker.tracker.vo.Metadata
import com.google.android.apps.santatracker.tracker.vo.StreamEntry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.greaterThan
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SantaDatabaseTest {

    private val database =
            Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getTargetContext(),
                    SantaDatabase::class.java).build()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    @SmallTest
    fun writeAndRead() {
        val destination = Destination(
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
                photo = DestinationPhoto("a", "b")
        )
        assertThat(database.destination().count(), `is`(0))
        database.destination().insertAll(listOf(destination))
        assertThat(database.destination().count(), `is`(1))
        val result = database.destination().byId("destination")!!
        assertThat(result.id, `is`("destination"))
        assertThat(result.arrival, `is`(0L))
    }

    @Test
    @MediumTest
    fun populateFromJson() {
        val jsonLoader = JsonLoader()
        val jsonData = jsonLoader.parseJson(InstrumentationRegistry.getTargetContext())

        val destinations = jsonData.destinations
        val streamEntries = jsonData.streamEntries
        val metadataList = jsonData.metadata

        database.destination().insertAll(destinations)
        database.stream().insertAll(streamEntries)
        database.metadata().insertAll(metadataList)

        assertThat(database.destination().count(), `is`(destinations.size))
        val orderedDestinations = database.destination().all()
        assertThat(orderedDestinations.size, `is`(destinations.size))
        if (orderedDestinations.size > 1) {
            assertThat(orderedDestinations[1].arrival,
                    `is`(greaterThan(orderedDestinations[0].arrival)))
        }
        assertThat(database.stream().count(), `is`(streamEntries.size))
        val orderedStreamEntries = database.stream().all()
        assertThat(orderedStreamEntries.size, `is`(streamEntries.size))
        if (orderedStreamEntries.size > 1) {
            assertThat(orderedStreamEntries[1].timestamp,
                    `is`(greaterThan(orderedStreamEntries[0].timestamp)))
        }
        assertThat(database.metadata().count(), `is`(greaterThan(0)))
        val destination = database.destination().byId("tokyo")!!
        assertThat(destination.arrival, `is`(1514124000000L))
        assertThat(destination.departure, `is`(1514124060000L))
        assertThat(destination.population, `is`(12790000L))
        assertThat(destination.presentsDelivered, `is`(582331036L))
        assertThat(destination.city, `is`("Tokyo"))
        assertThat(destination.region, `is`("Japan"))
        assertThat(destination.location.lat, `is`(closeTo(35.678451, 0.1)))
        assertThat(destination.location.lng, `is`(closeTo(139.682282, 0.1)))
        assertThat(destination.timezone, `is`(32400L))
        assertThat(destination.altitude, `is`(0.0))
        assertThat(destination.weather?.url,
                `is`("http://www.wunderground.com/global/stations/47644.html"))
        assertThat(destination.weather?.tempC, `is`(closeTo(13.4, 0.1)))
        assertThat(destination.weather?.tempF, `is`(closeTo(56.2, 0.1)))
        assertThat(destination.streetView?.id, `is`("XfWNQjE5-akAAAQIt-4A2Q"))
        assertThat(destination.streetView?.latitude, `is`(0.0))
        assertThat(destination.streetView?.longitude, `is`(0.0))
        assertThat(destination.streetView?.heading, `is`(closeTo(645.0, 0.1)))
        assertThat(destination.gmmStreetView?.id, `is`("9tCq9g0p5T-jebdyiAekug"))
        assertThat(destination.gmmStreetView?.latitude, `is`(closeTo(35.682398, 0.1)))
        assertThat(destination.gmmStreetView?.longitude, `is`(closeTo(139.756522, 0.1)))
        assertThat(destination.gmmStreetView?.heading, `is`(closeTo(87.27, 0.1)))
        val streamEntry = database.stream().byTimestamp(1514191650000L)!!
        assertThat(streamEntry.timestamp, `is`(1514191650000L))
        assertThat(streamEntry.type, `is`(StreamEntry.TYPE_YOUTUBE_ID))
        assertThat(streamEntry.notification, `is`(false))
        assertThat(streamEntry.content, `is`("BfF7vfw6Zjw"))
        val metadata = database.metadata().get(Metadata.KEY_LANGUAGE)!!
        assertThat(metadata, `is`("en"))
    }

    @Test
    @SmallTest
    fun testDeleteEntities() {
        val jsonLoader = JsonLoader()
        val jsonData = jsonLoader.parseJson(InstrumentationRegistry.getTargetContext())
        val destinations = jsonData.destinations
        val streamEntries = jsonData.streamEntries
        val metadataList = jsonData.metadata

        database.destination().insertAll(destinations)
        database.stream().insertAll(streamEntries)
        database.metadata().insertAll(metadataList)
        assertThat(database.destination().count(), `is`(destinations.size))
        assertThat(database.stream().count(), `is`(streamEntries.size))
        assertThat(database.metadata().count(), `is`(metadataList.size))

        database.destination().deleteAll()
        database.stream().deleteAll()
        database.metadata().deleteAll()

        assertThat(database.destination().count(), `is`(0))
        assertThat(database.stream().count(), `is`(0))
        assertThat(database.metadata().count(), `is`(0))
    }
}
