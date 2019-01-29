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

package com.google.android.apps.santatracker.tracker.viewmodel

import androidx.lifecycle.Observer
import androidx.test.InstrumentationRegistry
import androidx.test.filters.MediumTest
import com.google.android.apps.santatracker.config.Config
import com.google.android.apps.santatracker.tracker.TestActivity
import com.google.android.apps.santatracker.tracker.db.LiveDataTestUtil
import com.google.android.apps.santatracker.tracker.repository.SantaDataRepository
import com.google.android.apps.santatracker.tracker.time.Clock
import com.google.android.apps.santatracker.tracker.util.JsonLoader
import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.apps.santatracker.tracker.vo.StreamEntry
import com.google.android.apps.santatracker.tracker.vo.TrackerCard
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [TrackerViewModel]
 */
@RunWith(MockitoJUnitRunner::class)
class TrackerViewModelTest {

    companion object {

        private const val NORTH_POLE_DEPARTURE = 1514109600000L
        private const val SYDNEY_ARRIVAL = 1514120880000L
        private const val SYDNEY_DEPARTURE = 1514120940000L
        private const val NORTH_POLE_LANDING = 1545645600000L
        private const val LAST_ARRIVAL = 1514199600000L

        // The indices of the cities within the destination list in the santa.json file
        private const val NORTH_POLE_INDEX = 0
        private const val PROVIDENIYA_INDEX = 1
        private const val BRISBANE_INDEX = 35
        private const val SYDNEY_INDEX = 36
        private const val WOLLONGONG_INDEX = 37
    }

    @JvmField
    @Rule
    val activityRule = ActivityTestRule(TestActivity::class.java)

    private lateinit var viewModel: TrackerViewModel
    @Mock private lateinit var mockRepository: SantaDataRepository
    @Mock private lateinit var mockClock: Clock
    @Mock private lateinit var mockConfig: Config
    @Mock private lateinit var mockFirebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var executorService: ScheduledExecutorService
    private lateinit var destinationList: List<Destination>
    private val streamEntryList = listOf<StreamEntry>()

    @Before
    fun setUp() {
        executorService = ScheduledThreadPoolExecutor(1)

        val jsonLoader = JsonLoader()
        val jsonData = jsonLoader.parseJson(InstrumentationRegistry.getTargetContext())
        val destinations = jsonData.destinations
        val streamEntries = jsonData.streamEntries

        destinationList = destinations
        doReturn(destinations).`when`(mockRepository).loadDestinations()
        doReturn(streamEntries).`when`(mockRepository).loadStreamEntries()
        doReturn(mockFirebaseRemoteConfig).`when`(mockConfig).firebaseRemoteConfig
        activityRule.runOnUiThread {
            viewModel = TrackerViewModel(activityRule.activity.application,
                    mockRepository, mockClock, Executor { it.run() }, executorService, mockConfig)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Test
    fun `testInitializeSantaState_before_takeoff`() {
        `when`(mockClock.nowMillis()).thenReturn(NORTH_POLE_DEPARTURE -
                TimeUnit.SECONDS.toMillis(10))

        viewModel.initializeIndices(mockClock.nowMillis(), destinationList, streamEntryList)
        viewModel.initializeSantaState(destinationList, streamEntryList)

        assertThat(viewModel.destinationIndex, `is`(NORTH_POLE_INDEX))
        activityRule.runOnUiThread({
            assertThat(LiveDataTestUtil.getValue(viewModel.santaState), `is`(nullValue()))
        })
    }

    @Test
    fun `testInitializeSantaState_after_takeoff`() {
        `when`(mockClock.nowMillis()).thenReturn(NORTH_POLE_DEPARTURE +
                TimeUnit.SECONDS.toMillis(10))

        viewModel.initializeIndices(mockClock.nowMillis(), destinationList, streamEntryList)
        viewModel.initializeSantaState(destinationList, streamEntryList)

        assertThat(viewModel.destinationIndex, `is`(PROVIDENIYA_INDEX))
        activityRule.runOnUiThread({
            assertThat(LiveDataTestUtil.getValue(viewModel.santaState).isTraveling,
                    `is`(true))
        })
    }

    @Test
    fun `testInitializeSantaState_before_sydney_arrival`() {
        `when`(mockClock.nowMillis()).thenReturn(SYDNEY_ARRIVAL -
                TimeUnit.SECONDS.toMillis(10))

        viewModel.initializeIndices(mockClock.nowMillis(), destinationList, streamEntryList)
        viewModel.initializeSantaState(destinationList, streamEntryList)

        assertThat(viewModel.destinationIndex, `is`(SYDNEY_INDEX))
        activityRule.runOnUiThread({
            assertThat(LiveDataTestUtil.getValue(viewModel.santaState).isTraveling, `is`(true))
        })
    }

    @Test
    fun `testInitializeSantaState_after_sydney_arrival`() {
        `when`(mockClock.nowMillis()).thenReturn(SYDNEY_ARRIVAL +
                TimeUnit.SECONDS.toMillis(10))

        viewModel.initializeIndices(mockClock.nowMillis(), destinationList, streamEntryList)
        viewModel.initializeSantaState(destinationList, streamEntryList)

        assertThat(viewModel.destinationIndex, `is`(SYDNEY_INDEX))
        activityRule.runOnUiThread({
            assertThat(LiveDataTestUtil.getValue(viewModel.santaState).isTraveling, `is`(false))
        })
    }

    @Test
    fun `testInitializeSantaState_after_sydney_departure`() {
        `when`(mockClock.nowMillis()).thenReturn(SYDNEY_DEPARTURE +
                TimeUnit.SECONDS.toMillis(10))

        viewModel.initializeIndices(mockClock.nowMillis(), destinationList, streamEntryList)
        viewModel.initializeSantaState(destinationList, streamEntryList)

        assertThat(viewModel.destinationIndex, `is`(WOLLONGONG_INDEX))

        activityRule.runOnUiThread({
            assertThat(LiveDataTestUtil.getValue(viewModel.santaState).isTraveling, `is`(true))
        })
    }

    @Test
    fun `testInitializeSantaState_after_north_pole_landing`() {
        `when`(mockClock.nowMillis()).thenReturn(NORTH_POLE_LANDING +
                TimeUnit.SECONDS.toMillis(10))

        viewModel.initializeIndices(mockClock.nowMillis(), destinationList, streamEntryList)
        viewModel.initializeSantaState(destinationList, streamEntryList)

        assertThat(viewModel.destinationIndex, `is`(destinationList.size))
    }

    @Test
    fun `testUpdateSantaState_between_takeoff`() {
        `when`(mockClock.nowMillis()).thenReturn(NORTH_POLE_DEPARTURE -
                TimeUnit.SECONDS.toMillis(10))
        val provideniya = destinationList[PROVIDENIYA_INDEX]
        viewModel.initializeIndices(mockClock.nowMillis(), destinationList, streamEntryList)
        viewModel.initializeSantaState(destinationList, streamEntryList)
        assertThat(viewModel.destinationIndex, `is`(NORTH_POLE_INDEX))
        activityRule.runOnUiThread({
            val santaState: TrackerViewModel.SantaState =
                    LiveDataTestUtil.getValue(viewModel.santaState)
            assertThat(santaState, `is`(nullValue()))
        })

        `when`(mockClock.nowMillis()).thenReturn(NORTH_POLE_DEPARTURE +
                TimeUnit.SECONDS.toMillis(10))

        viewModel.updateSantaState(destinationList, streamEntryList)

        val northPole = destinationList[NORTH_POLE_INDEX]
        activityRule.runOnUiThread({
            assertThat(viewModel.destinationIndex, `is`(PROVIDENIYA_INDEX))
            // Asserting this in the main thread because LiveData can be observed only from
            // the main thread.
            val santaState = LiveDataTestUtil.getValue(viewModel.santaState)
            assertThat(santaState.isTraveling, `is`(true))
            assertThat(santaState.destination, `is`(provideniya))
            val visited = santaState.visitedDestinations
            assertThat(visited.size, `is`(NORTH_POLE_INDEX + 1))
            assertThat(visited[visited.size - 1], `is`(northPole))
        })
    }

    @Test
    @MediumTest
    fun `testUpdateSantaState_between_sydney_arrival`() {
        `when`(mockClock.nowMillis()).thenReturn(SYDNEY_ARRIVAL -
                TimeUnit.SECONDS.toMillis(10))
        val brisbane = destinationList[BRISBANE_INDEX]
        val sydney = destinationList[SYDNEY_INDEX]
        activityRule.runOnUiThread({
            viewModel.initializeIndices(mockClock.nowMillis(), destinationList, streamEntryList)
            viewModel.initializeSantaState(destinationList, streamEntryList)
        })
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        activityRule.runOnUiThread({
            val santaState: TrackerViewModel.SantaState =
                    LiveDataTestUtil.getValue(viewModel.santaState)
            assertThat(santaState.isTraveling, `is`(true))
            assertThat(santaState.destination, `is`(sydney))
            val visited = santaState.visitedDestinations
            assertThat(visited.size, `is`(SYDNEY_INDEX))
            assertThat(visited[visited.size - 1], `is`(brisbane))
        })

        `when`(mockClock.nowMillis()).thenReturn(SYDNEY_ARRIVAL +
                TimeUnit.SECONDS.toMillis(10))

        viewModel.updateSantaState(destinationList, streamEntryList)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        activityRule.runOnUiThread({
            // Asserting this in the main thread because LiveData can be observed only from
            // the main thread.
            val santaState: TrackerViewModel.SantaState =
                    LiveDataTestUtil.getValue(viewModel.santaState)
            assertThat(santaState.isTraveling, `is`(false))
            assertThat(santaState.destination, `is`(sydney))
            val visited = santaState.visitedDestinations
            assertThat(visited.size, `is`(BRISBANE_INDEX + 1))
            assertThat(visited[visited.size - 1], `is`(brisbane))
        })
    }

    @Test
    @MediumTest
    fun `testUpdateSantaState_between_sydney_departure`() {
        `when`(mockClock.nowMillis()).thenReturn(SYDNEY_DEPARTURE -
                TimeUnit.SECONDS.toMillis(10))
        val brisbane = destinationList[BRISBANE_INDEX]
        val sydney = destinationList[SYDNEY_INDEX]
        val wollongong = destinationList[WOLLONGONG_INDEX]
        activityRule.runOnUiThread({
            viewModel.initializeIndices(mockClock.nowMillis(), destinationList, streamEntryList)
            viewModel.initializeSantaState(destinationList, streamEntryList)
            assertThat(viewModel.destinationIndex, `is`(SYDNEY_INDEX))
        })
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        activityRule.runOnUiThread({
            val santaState: TrackerViewModel.SantaState =
                    LiveDataTestUtil.getValue(viewModel.santaState)
            assertThat(santaState.isTraveling, `is`(false))
            assertThat(santaState.destination, `is`(sydney))
            val visited = santaState.visitedDestinations
            assertThat(visited.size, `is`(BRISBANE_INDEX + 1))
            assertThat(visited[visited.size - 1], `is`(brisbane))
        })

        `when`(mockClock.nowMillis()).thenReturn(SYDNEY_DEPARTURE +
                TimeUnit.SECONDS.toMillis(10))

        viewModel.updateSantaState(destinationList, streamEntryList)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        activityRule.runOnUiThread({
            assertThat(viewModel.destinationIndex, `is`(WOLLONGONG_INDEX))
            // Asserting this in the main thread because LiveData can be observed only from
            // the main thread.
            val santaState: TrackerViewModel.SantaState =
                    LiveDataTestUtil.getValue(viewModel.santaState)
            assertThat(santaState.isTraveling, `is`(true))
            assertThat(santaState.destination, `is`(wollongong))
            val visited = santaState.visitedDestinations
            assertThat(visited.size, `is`(SYDNEY_INDEX + 1))
            assertThat(visited[visited.size - 1], `is`(sydney))
        })
    }

    @Test
    @MediumTest
    fun `testUpdateSantaState_between_last_arrival`() {
        `when`(mockClock.nowMillis()).thenReturn(LAST_ARRIVAL -
                TimeUnit.SECONDS.toMillis(10))
        val lastDestination = destinationList[destinationList.size - 1]
        activityRule.runOnUiThread({
            viewModel.initializeIndices(mockClock.nowMillis(), destinationList, streamEntryList)
            viewModel.initializeSantaState(destinationList, streamEntryList)
            assertThat(viewModel.destinationIndex, `is`(destinationList.size - 1))
        })
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        activityRule.runOnUiThread({
            val santaState: TrackerViewModel.SantaState =
                    LiveDataTestUtil.getValue(viewModel.santaState)
            assertThat(santaState.isTraveling, `is`(true))
            assertThat(santaState.destination, `is`(lastDestination))
            val visited = santaState.visitedDestinations
            assertThat(visited.size, `is`(destinationList.size - 1))
            assertThat(LiveDataTestUtil.getValue(viewModel.finishedTraveling), `is`(false))
        })

        `when`(mockClock.nowMillis()).thenReturn(LAST_ARRIVAL +
                TimeUnit.SECONDS.toMillis(10))

        viewModel.updateSantaState(destinationList, streamEntryList)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        activityRule.runOnUiThread({
            assertThat(LiveDataTestUtil.getValue(viewModel.finishedTraveling), `is`(true))
        })
    }

    @Test
    @MediumTest
    fun testTrackerCards() {
        `when`(mockClock.nowMillis()).thenReturn(2)
        @Suppress("UNCHECKED_CAST")
        val observer = mock(Observer::class.java) as Observer<List<TrackerCard>>
        viewModel.stream.observeForever(observer)
        viewModel.initializeStream(emptyList())
        verify(observer, timeout(100)).onChanged(eq(emptyList()))
        viewModel.updateStream(listOf(StreamEntry(1, StreamEntry.TYPE_DID_YOU_KNOW, false, "a")), 2)
        verify(observer, timeout(100))
                .onChanged(eq(listOf(StreamEntry(1, StreamEntry.TYPE_DID_YOU_KNOW, false, "a"))))
    }
}
