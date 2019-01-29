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

import android.annotation.SuppressLint
import android.app.Application
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.google.android.apps.santatracker.config.Config
import com.google.android.apps.santatracker.data.FeatureState
import com.google.android.apps.santatracker.data.FlyingState
import com.google.android.apps.santatracker.data.GameState
import com.google.android.apps.santatracker.data.SantaTravelState
import com.google.android.apps.santatracker.data.TakeoffLandingTimes
import com.google.android.apps.santatracker.data.VideoState
import com.google.android.apps.santatracker.data.WebSceneState
import com.google.android.apps.santatracker.data.featureState
import com.google.android.apps.santatracker.data.gameState
import com.google.android.apps.santatracker.data.videoState
import com.google.android.apps.santatracker.data.webSceneState
import com.google.android.apps.santatracker.tracker.api.LocaleMapper
import com.google.android.apps.santatracker.tracker.repository.SantaDataRepository
import com.google.android.apps.santatracker.tracker.time.Clock
import com.google.android.apps.santatracker.tracker.util.Utils
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import kotlin.concurrent.withLock

/**
 * [ViewModel] that exposes [LiveData] related to the timing santa flies.
 */
class VillageViewModel @Inject
internal constructor(
    app: Application,
    private val repository: SantaDataRepository,
    private val clock: Clock,
    private val config: Config,
    private val executor: Executor,
    private val executorService: ScheduledExecutorService
)
    : AndroidViewModel(app) {

    open val santaTravelState: LiveData<SantaTravelState>
        get() = _santaTravelState
    // To verify the behavior of the LiveData in tests, defining this as var
    @SuppressLint("VisibleForTests")
    internal var _santaTravelState = MutableLiveData<SantaTravelState>()

    val timeToTakeoff: LiveData<Long>
        get() = _timeToTakeoff
    // To verify the behavior of the LiveData in tests, defining this as var
    @SuppressLint("VisibleForTests")
    internal var _timeToTakeoff = MutableLiveData<Long>()

    val launchFlags: LiveData<LaunchFlags>
        get() = _launchFlags
    private val _launchFlags = MutableLiveData<LaunchFlags>()

    // Current scroll position.
    var scrollPosition = RecyclerView.NO_POSITION

    private val lock = ReentrantLock()

    /** Time Santa takes off the first destination in milli seconds */
    private var firstDepartureMs: Long
    /** Time Santa arrives the last destination in milli seconds */
    private var lastArrivalMs: Long

    init {
        firstDepartureMs = config.takeoffTimeMs
        lastArrivalMs = config.arrivalTimeMs

        executor.execute {
            lock.withLock {
                executorService.scheduleAtFixedRate({
                    updateTravelState(firstDepartureMs, lastArrivalMs)
                }, 0L, 1L, TimeUnit.SECONDS)

                val localeMapper = LocaleMapper()
                val locale = Utils.extractLocale(LocaleListCompat.getAdjustedDefault().toLanguageTags())
                if (repository.ensureReady(
                        localeMapper.toServerLanguage(locale))) {
                    val firstDestination = repository.firstDestination()
                    val lastDestination = repository.lastDestination()
                    firstDepartureMs = firstDestination?.departure ?: firstDepartureMs
                    lastArrivalMs = lastDestination?.arrival ?: lastArrivalMs

                    config.syncConfigAsync(object : Config.ParamChangedCallback {
                        override fun onChanged(changedKeys: List<String>) {
                            if (changedKeys.isNotEmpty()) {
                                updateLaunchFlags()
                            }
                        }
                    })
                }
            }
        }
        updateLaunchFlags()
    }

    override fun onCleared() {
        super.onCleared()
        executorService.shutdown()
    }

    fun updateTravelState(departure: Long, arrival: Long) {
        val takeoffLandingTime = TakeoffLandingTimes(departure, arrival)
        val flyingState = judgeFlyingState(takeoffLandingTime)
        val value = _santaTravelState.value
        if (value == null || value.flyingState != flyingState) {
            _santaTravelState.postValue(
                    SantaTravelState(flyingState, takeoffLandingTime))
        }
        _timeToTakeoff.postValue(departure - clock.nowMillis())
    }

    private fun updateLaunchFlags() {
        _launchFlags.postValue(
                LaunchFlags(config.featureState(), config.gameState(),
                        config.videoState(), config.webSceneState())
        )
    }

    fun refresh() {
        // Execute in a worker thread since this method may be called from the main thread.
        executor.execute {
            updateTravelState(firstDepartureMs, lastArrivalMs)
            updateLaunchFlags()
        }
    }

    fun syncRoute() {
        executor.execute {
            lock.withLock {
                val localeMapper = LocaleMapper()
                val locale =
                        Utils.extractLocale(LocaleListCompat.getAdjustedDefault().toLanguageTags())
                repository.reloadAll(localeMapper.toServerLanguage(locale))
                // This function does't call updateTravelState because it should be already
                // scheduled repeatedly in init
            }
        }
    }

    private fun judgeFlyingState(takeoffLandingTimes: TakeoffLandingTimes): FlyingState {
        return if (config.get(Config.DISABLE_SANTA)) {
            FlyingState.DISABLED
        } else if (clock.nowMillis() < takeoffLandingTimes.takeoffTime) {
            FlyingState.PRE_FLIGHT
        } else if (takeoffLandingTimes.takeoffTime <= clock.nowMillis() &&
                repository.firstDestination() == null) {
            // The device is offline.
            FlyingState.DISABLED
        } else if (takeoffLandingTimes.takeoffTime <= clock.nowMillis() &&
                clock.nowMillis() < takeoffLandingTimes.landingTime) {
            FlyingState.FLYING
        } else {
            FlyingState.POST_FLIGHT
        }
    }

    data class LaunchFlags(
        val featureState: FeatureState,
        val gameState: GameState,
        val videoState: VideoState,
        val webSceneState: WebSceneState
    )
}
