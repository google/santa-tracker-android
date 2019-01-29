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

package com.google.android.apps.santatracker.viewmodel

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.android.apps.santatracker.config.Config
import com.google.android.apps.santatracker.data.FlyingState
import com.google.android.apps.santatracker.data.SantaTravelState
import com.google.android.apps.santatracker.data.TakeoffLandingTimes
import com.google.android.apps.santatracker.tracker.repository.SantaDataRepository
import com.google.android.apps.santatracker.tracker.time.Clock
import com.google.android.apps.santatracker.tracker.vo.DestinationTimestamp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.isA
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor

/**
 * Unit tests for [VillageViewModel]
 */
// Adding Silent to avoid the org.mockito.exceptions.misusing.UnnecessaryStubbingException for
// stubbing Config.LongParamChangedCallback
@RunWith(MockitoJUnitRunner.Silent::class)
class VillageViewModelTest {

    companion object {

        private const val FIRST_DEPARTURE = 15L
        private const val LAST_ARRIVAL = 30L
    }

    private lateinit var viewModel: VillageViewModel
    @Mock private lateinit var mockApplication: Application
    @Mock private lateinit var mockRepository: SantaDataRepository
    @Mock private lateinit var mockClock: Clock
    @Mock private lateinit var mockFirebaseRemoteConfig: FirebaseRemoteConfig
    @Mock private lateinit var mockConfig: Config
    @Mock private lateinit var mockTravelState: MutableLiveData<SantaTravelState>
    @Mock private lateinit var mockTimeToTakeoff: MutableLiveData<Long>

    private val first = DestinationTimestamp(
            arrival = FIRST_DEPARTURE - 2,
            departure = FIRST_DEPARTURE
    )

    private val last = DestinationTimestamp(
            arrival = LAST_ARRIVAL,
            departure = LAST_ARRIVAL + 2
    )

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        doReturn(first).`when`(mockRepository).firstDestination()
        doReturn(last).`when`(mockRepository).lastDestination()
        doReturn(mockFirebaseRemoteConfig).`when`(mockConfig).firebaseRemoteConfig
        doNothing().`when`(mockConfig).syncConfigAsync(isA(Config.ParamChangedCallback::class.java))
        doReturn(false).`when`(mockConfig)[Config.DISABLE_SANTA]
        doReturn("dummy")
                .`when`(mockFirebaseRemoteConfig).getString(isA(String::class.java))

        viewModel = VillageViewModel(mockApplication,
                    mockRepository, mockClock, mockConfig, Executor { it.run() },
                ScheduledThreadPoolExecutor(1))
        viewModel._timeToTakeoff = mockTimeToTakeoff
        viewModel._santaTravelState = mockTravelState
    }

    @Test
    fun testUpdateTravelState_beforeFirstDeparture() {
        Mockito.`when`(mockClock.nowMillis()).thenReturn(FIRST_DEPARTURE - 10)

        viewModel.updateTravelState(FIRST_DEPARTURE, LAST_ARRIVAL)

        verify(mockTravelState).postValue(
                SantaTravelState(FlyingState.PRE_FLIGHT,
                TakeoffLandingTimes(FIRST_DEPARTURE, LAST_ARRIVAL)))
        verify(mockTimeToTakeoff).postValue(FIRST_DEPARTURE - mockClock.nowMillis())
    }

    @Test
    fun testUpdateTravelState_middleOfTravel() {
        Mockito.`when`(mockClock.nowMillis()).thenReturn((LAST_ARRIVAL + FIRST_DEPARTURE) / 2)

        viewModel.updateTravelState(FIRST_DEPARTURE, LAST_ARRIVAL)

        verify(mockTravelState).postValue(
                SantaTravelState(FlyingState.FLYING,
                        TakeoffLandingTimes(FIRST_DEPARTURE, LAST_ARRIVAL)))
        verify(mockTimeToTakeoff).postValue(FIRST_DEPARTURE - mockClock.nowMillis())
    }

    @Test
    fun testUpdateTravelState_afterLastArrival() {
        Mockito.`when`(mockClock.nowMillis()).thenReturn(LAST_ARRIVAL + 10)

        viewModel.updateTravelState(FIRST_DEPARTURE, LAST_ARRIVAL)

        verify(mockTravelState).postValue(
                SantaTravelState(FlyingState.POST_FLIGHT,
                        TakeoffLandingTimes(FIRST_DEPARTURE, LAST_ARRIVAL)))
        verify(mockTimeToTakeoff).postValue(FIRST_DEPARTURE - mockClock.nowMillis())
    }

    @Test
    fun testUpdateTravelState_santaDisabled() {
        Mockito.`when`(mockClock.nowMillis()).thenReturn((LAST_ARRIVAL + FIRST_DEPARTURE) / 2)
        Mockito.`when`(mockConfig.get(Config.DISABLE_SANTA)).thenReturn(true)

        viewModel.updateTravelState(FIRST_DEPARTURE, LAST_ARRIVAL)

        verify(mockTravelState).postValue(
                SantaTravelState(FlyingState.DISABLED,
                        TakeoffLandingTimes(FIRST_DEPARTURE, LAST_ARRIVAL)))
        verify(mockTimeToTakeoff).postValue(FIRST_DEPARTURE - mockClock.nowMillis())
    }
}
