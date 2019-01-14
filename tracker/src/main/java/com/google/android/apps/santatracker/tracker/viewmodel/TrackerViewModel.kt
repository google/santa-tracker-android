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

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.apps.santatracker.config.Config
import com.google.android.apps.santatracker.data.FeatureState
import com.google.android.apps.santatracker.data.featureState
import com.google.android.apps.santatracker.tracker.R
import com.google.android.apps.santatracker.tracker.repository.SantaDataRepository
import com.google.android.apps.santatracker.tracker.time.Clock
import com.google.android.apps.santatracker.tracker.ui.PresentCounter
import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.apps.santatracker.tracker.vo.StreamEntry
import com.google.android.apps.santatracker.tracker.vo.TrackerCard
import com.google.android.apps.santatracker.util.SantaLog
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TrackerViewModel @Inject constructor(
    application: Application,
    private val repository: SantaDataRepository,
    private val clock: Clock,
    executor: Executor,
    private val executorService: ScheduledExecutorService,
    private val config: Config
)
    : AndroidViewModel(application) {

    companion object {
        private const val TAG = "TrackerViewModel"
        /**
         * Percentage of presents to hand out when travelling between destinations
         * (the rest is handed out when the destination is reached)
         */
        private const val FACTOR_PRESENTS_TRAVELING: Double = 0.3

        private val PRESENTS_FORMAT = NumberFormat.getNumberInstance(Locale.US)

        private const val COUNTDOWN_HMS = "%d:%02d:%02d"
        private const val COUNTDOWN_MS = "%02d:%02d"
    }

    /** The index of the next destination */
    @SuppressLint("VisibleForTests")
    var destinationIndex = 0

    /** The index of the next stream entry */
    private var streamEntryIndex = 0

    private val countdownLabelArrivingIn: String
    private val countdownLabelDepartingIn: String
    private val locationLabelCurrent: String
    private val locationLabelNext: String

    private var presentCounter: PresentCounter = PresentCounter(0, 0, 0, 0)

    /**
     * List of all the destinations Santa visited or will visit.
     */
    private var destinations = emptyList<Destination>()

    /**
     * List of all the stream entries in the past and the future
     */
    private var streamEntries = emptyList<StreamEntry>()

    /**
     * The card stream.
     */
    val stream: LiveData<List<TrackerCard>>
        get() = _stream
    private val _stream = MutableLiveData<List<TrackerCard>>()

    /**
     * Santa's route related state including the santa's next destination and past visited
     * destinations.
     */
    val santaState: LiveData<SantaState>
        get() = _santaState
    private val _santaState = MutableLiveData<SantaState>()

    /**
     * Countdown until Santa's next action (arrival or departure).
     */
    val countdown: LiveData<String>
        get() = _countdown
    private val _countdown = MutableLiveData<String>()

    /**
     * Label for the countdown.
     */
    val countdownLabel: LiveData<String>
        get() = _countdownLabel
    private val _countdownLabel = MutableLiveData<String>()

    /**
     * Santa's location (current or next)
     */
    val location: LiveData<String>
        get() = _location
    private val _location = MutableLiveData<String>()

    /**
     * Label for the location.
     */
    val locationLabel: LiveData<String>
        get() = _locationLabel
    private val _locationLabel = MutableLiveData<String>()

    /**
     * The number of presents Santa delivered so far.
     */
    val presentsDelivered: LiveData<String>
        get() = _presentsDelivered
    private val _presentsDelivered = MutableLiveData<String>()

    /**
     * Whether the stream dashboard should show the countdown (`true`) or the presents (`false`).
     */
    val showCountdown: LiveData<Boolean>
        get() = _showCountdown
    private val _showCountdown = MutableLiveData<Boolean>().apply { value = false }

    /**
     * Represents if Santa has finished traveling (arrived the last destination)
     */
    val finishedTraveling: LiveData<Boolean>
        get() = _finishedTraveling
    private val _finishedTraveling = MutableLiveData<Boolean>()

    /**
     * The flag for enabling/disabling features related to the Tracker.
     */
    val featureState: LiveData<FeatureState>
        get() = _featureState
    private val _featureState = MutableLiveData<FeatureState>()

    init {
        val resources = getApplication<Application>().resources
        countdownLabelArrivingIn = resources.getString(R.string.arriving_in)
        countdownLabelDepartingIn = resources.getString(R.string.departing_in)
        locationLabelCurrent = resources.getString(R.string.current_location)
        locationLabelNext = resources.getString(R.string.next_destination)

        executor.execute {
            destinations = repository.loadDestinations() ?: return@execute
            streamEntries = repository.loadStreamEntries() ?: return@execute
            startSanta()
            updateFeatureState()
        }
    }

    private fun updateFeatureState() {
        _featureState.postValue(config.featureState())
    }

    private fun startSanta() {
        initializeIndices(clock.nowMillis(), destinations, streamEntries)
        initializeSantaState(destinations, streamEntries)
        executorService.scheduleAtFixedRate({
            updateSantaState(destinations, streamEntries)
        }, 0L, 1L, TimeUnit.SECONDS)
    }

    override fun onCleared() {
        super.onCleared()
        executorService.shutdown()
    }

    @SuppressLint("VisibleForTests")
    fun initializeSantaState(
        destinationList: List<Destination>,
        streamEntryList: List<StreamEntry>
    ) {
        if (destinationIndex <= 0 || destinationIndex >= destinationList.size) {
            return
        }
        val isTraveling = isSantaTraveling(destinationList)
        val previousDestination = destinationList[destinationIndex - 1]
        val currentDestination = destinationList[destinationIndex]
        val presentsAtDestination = previousDestination.presentsDelivered + Math.round(
                (currentDestination.presentsDelivered - previousDestination.presentsDelivered) *
                        FACTOR_PRESENTS_TRAVELING)
        presentCounter = if (isTraveling) {
            PresentCounter(previousDestination.presentsDelivered,
                    presentsAtDestination,
                    previousDestination.departure,
                    currentDestination.arrival)
        } else {
            val delivered = previousDestination.presentsDelivered +
                    (currentDestination.presentsDelivered - previousDestination.presentsDelivered) *
                            ((clock.nowMillis() - currentDestination.arrival) /
                                    (currentDestination.departure - currentDestination.arrival))
            PresentCounter(delivered,
                    currentDestination.presentsDelivered,
                    currentDestination.arrival,
                    currentDestination.departure)
        }
        _locationLabel.postValue(if (isTraveling) {
            locationLabelNext
        } else {
            locationLabelCurrent
        })
        _location.postValue(currentDestination.city)
        val visitedDestinations = destinationList.take(destinationIndex)
        _santaState.postValue(
                SantaState(isTraveling, currentDestination, visitedDestinations))
        _finishedTraveling.postValue(false)
        val pastStreamEntries = streamEntryList.take(streamEntryIndex)
        initializeStream(
                visitedDestinations,
                pastStreamEntries,
                if (isTraveling) {
                    listOf(currentDestination)
                } else {
                    emptyList()
                })
    }

    @SuppressLint("VisibleForTests")
    fun updateSantaState(destinationList: List<Destination>, streamEntryList: List<StreamEntry>) {
        val currentDestination = destinationList[destinationIndex]
        val nextDestination = if (destinationIndex < destinationList.size - 1)
            destinationList[destinationIndex + 1] else null
        val travelingNow = _santaState.value?.isTraveling == true
        val now = clock.nowMillis()
        updateCountdown(currentDestination, now)
        _presentsDelivered.postValue(PRESENTS_FORMAT.format(
                presentCounter.getDeliveredPresents(now)))
        if (shouldVisit(currentDestination, travelingNow)) {
            visit(currentDestination, destinationList)
        } else if (shouldDepart(currentDestination, travelingNow)) {
            depart(currentDestination, nextDestination, destinationList)
            if (nextDestination != null) {
                addToStream(nextDestination)
            }
        }
        updateStream(streamEntryList, now)
    }

    private fun visit(destination: Destination, destinationList: List<Destination>) {
        if (destinationIndex == destinationList.size - 1) {
            _finishedTraveling.postValue(true)
            SantaLog.d(TAG, "Santa has finished traveling")
            return
        }
        presentCounter = PresentCounter(
                presentCounter.getDeliveredPresents(clock.nowMillis()),
                destination.presentsDelivered,
                destination.arrival,
                destination.departure)
        SantaLog.d(TAG, "Santa visited " + destination.city)
        _locationLabel.postValue(locationLabelCurrent)
        _location.postValue(destination.city)
        _santaState.value?.apply {
            isTraveling = false
        }
        _santaState.postValue(_santaState.value)
    }

    private fun depart(
        current: Destination,
        next: Destination?,
        destinationList: List<Destination>
    ) {
        if (next == null) {
            _finishedTraveling.postValue(true)
            SantaLog.d(TAG, "Santa departed the last destination")
            return
        }
        val presentsAtDestination = (current.presentsDelivered +
                Math.round((next.presentsDelivered - current.presentsDelivered) *
                        FACTOR_PRESENTS_TRAVELING))
        presentCounter = PresentCounter(current.presentsDelivered,
                presentsAtDestination,
                current.departure,
                next.arrival)
        destinationIndex++

        _locationLabel.postValue(locationLabelNext)
        _location.postValue(next.city)
        _santaState.postValue(SantaState(true, next,
                destinationList.take(destinationIndex)))
        SantaLog.d(TAG, "Santa departed " + current.city + ". Next destination: " + next.city)
    }

    private fun shouldVisit(current: Destination, travelingNow: Boolean): Boolean {
        return clock.nowMillis() >= current.arrival && travelingNow
    }

    private fun shouldDepart(current: Destination, travelingNow: Boolean): Boolean {
        return clock.nowMillis() >= current.departure && !travelingNow
    }

    private fun updateCountdown(current: Destination, now: Long) {
        val arrivesIn = current.arrival - now
        // The value will be negative when Santa is visiting his destination.
        if (arrivesIn > 0) {
            _countdownLabel.postValue(countdownLabelArrivingIn)
            _countdown.postValue(formatCountdown(arrivesIn))
            _showCountdown.postValue(shouldShowCountdown(arrivesIn))
        } else {
            val departsIn = current.departure - now
            _countdownLabel.postValue(countdownLabelDepartingIn)
            _countdown.postValue(formatCountdown(departsIn))
            _showCountdown.postValue(shouldShowCountdown(departsIn))
        }
    }

    private fun shouldShowCountdown(next: Long) = (next / 5000) % 2 == 0L

    private fun isSantaTraveling(destinationList: List<Destination>): Boolean {
        if (destinationIndex == 0 ||
                destinationIndex >= destinationList.size) {
            return false
        }
        val current = destinationList[destinationIndex]
        val previous = destinationList[destinationIndex - 1]
        return clock.nowMillis() >= previous.departure && clock.nowMillis() < current.arrival
    }

    @SuppressLint("VisibleForTests")
    fun initializeStream(vararg cardLists: List<TrackerCard>) {
        val list = mutableListOf<TrackerCard>()
        cardLists.forEach { list.addAll(it) }
        list.sortBy { it.value }
        list.reverse()
        _stream.postValue(list)
    }

    @SuppressLint("VisibleForTests")
    fun updateStream(streamEntryList: List<StreamEntry>, now: Long) {
        while (streamEntryIndex < streamEntryList.size) {
            val next = streamEntryList[streamEntryIndex]
            if (next.timestamp < now) {
                addToStream(next)
                streamEntryIndex++
            } else {
                break
            }
        }
    }

    private fun addToStream(card: TrackerCard) {
        _stream.value?.let { trackerCards ->
            _stream.postValue(trackerCards.toMutableList().apply {
                add(0, card)
            })
        }
    }

    @SuppressLint("VisibleForTests")
    fun initializeIndices(
        timestamp: Long,
        destinationList: List<Destination>,
        streamEntryList: List<StreamEntry>
    ) {
        if (destinationList.isEmpty()) {
            return
        }
        destinationIndex = findIndex(timestamp, destinationList) + 1
        streamEntryIndex = findIndex(timestamp, streamEntryList) + 1
    }

    /**
     * Finds the corresponding index from the list of [TrackerCard].
     * This function assumes the list is ordered in ascending order by the value of [TrackerCard].
     *
     * @return one of the following values:
     *         <-1> if the target is less than all [TrackerCard] in the list or the list is empty
     *         <n> where target <= list[n].value and target < list[n + 1].value
     *         <list.size - 1> if the target is greater than all [TrackerCard] in the list
     */
    private fun findIndex(target: Long, list: List<TrackerCard>): Int {
        if (list.isEmpty()) {
            return -1
        }
        var low = 0
        var high = list.size - 1
        while (low < high) {
            val mid = low + (high - low) / 2
            if (mid <= 0 || target < list[0].value) {
                return -1
            } else if (mid >= list.size - 1 || target >= list[list.size - 1].value) {
                return list.size - 1
            } else if (list[mid - 1].value <= target && target < list[mid].value) {
                return mid - 1
            } else if (list[mid].value <= target && target < list[mid + 1].value) {
                return mid
            }

            if (target < list[mid].value) {
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return -1
    }

    /**
     * @param time The time in milliseconds
     * @return A string representation of the countdown time.
     */
    private fun formatCountdown(time: Long): String {
        val iHours = Math.floor((time / (60 * 60 * 1000) % 24).toDouble()).toInt()
        val iMinutes = Math.floor((time / (60 * 1000) % 60).toDouble()).toInt()
        val iSeconds = Math.floor((time / 1000 % 60).toDouble()).toInt()
        return if (iHours > 0) {
            String.format(Locale.US, COUNTDOWN_HMS, iHours, iMinutes, iSeconds)
        } else {
            String.format(Locale.US, COUNTDOWN_MS, iMinutes, iSeconds)
        }
    }

    data class SantaState(
        var isTraveling: Boolean,
        var destination: Destination,
        var visitedDestinations: List<Destination>
    )
}
