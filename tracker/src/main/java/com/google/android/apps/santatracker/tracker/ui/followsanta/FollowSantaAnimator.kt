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

package com.google.android.apps.santatracker.tracker.ui.followsanta

import android.os.Handler
import com.google.android.apps.santatracker.tracker.time.Clock
import com.google.android.apps.santatracker.tracker.ui.SantaMarker
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng

class FollowSantaAnimator(googleMap: GoogleMap, santaMarker: SantaMarker, clock: Clock) {

    private val handler: Handler = Handler()
    private val travellingAnimation: MoveAroundSanta
    private val pathAnimation: CurrentPathAnimation
    private var startedDeparting: Boolean = false
    private var startedArriving: Boolean = false

    private var paused = false

    init {
        pathAnimation = CurrentPathAnimation(handler, googleMap, santaMarker, clock)
        travellingAnimation = MoveAroundSanta(handler, googleMap, santaMarker, clock)
    }

    fun triggerPaddingAnimation() {
        travellingAnimation.triggerPaddingAnimation()
    }

    fun animate(position: LatLng?, remainingTime: Long, elapsedTime: Long) {
        if (!paused && position != null) {
            if (!startedDeparting && elapsedTime < DEPARTING_TIME) {
                // reset variables and show first departing animation
                pathAnimation.start()
                travellingAnimation.reset()
                startedArriving = false
                startedDeparting = true
            } else if (!startedArriving && remainingTime < ARRIVING_TRIGGER) {
                // arriving animation
                pathAnimation.start()
                startedArriving = true
            } else if (remainingTime >= ARRIVING_TRIGGER && elapsedTime >= DEPARTING_TIME) {
                // between departing and arriving times, animate travelling animation
                travellingAnimation.onSantaMoving(position)
            }
        }
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }

    fun cancel() {
        travellingAnimation.cancel()
        pathAnimation.cancel()
        handler.removeCallbacksAndMessages(null)
    }

    fun reset() {
        pathAnimation.reset()
        travellingAnimation.reset()
        paused = false
        startedDeparting = false
        startedArriving = false
    }

    companion object {
        /**
         * Frames to stay in departing animation
         */
        private val DEPARTING_TIME = 10000

        // Frames to wait for the current animation
        private val ARRIVING_TRIGGER: Long = 15000
    }
}
