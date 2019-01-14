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
import com.google.android.apps.santatracker.tracker.util.Utils
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

/**
 * Camera animation that centers on Santa, then follows his position.
 */
internal class MoveAroundSanta(
    handler: Handler,
    map: GoogleMap,
    marker: SantaMarker,
    val clock: Clock
)
    : FollowSantaAnimation(handler, map, marker) {

    private var state = 0
    private var scrollFrames: Long = 0

    private var animateCameraUpdate: CameraUpdate? = null
    private var moveCameraUpdate: CameraUpdate? = null
    private var animationDuration = ANIMATION_DURATION

    init {
        reset()
    }

    override fun reset() {
        super.reset()
        state = 0
    }

    private var animationStart: Long = 0
    private var animationBearingChange: Float = 0.toFloat()
    private var initialBearing: Float = 0.toFloat()

    fun onSantaMoving(position: LatLng) {
        // only execute animation if not cancelled
        // (required so that scroll won't be animated)
        if (!isCancelled) {
            when (ORDER[state]) {
                STATE_CATCHUP -> {
                    catchupAnimation()
                    nextState()
                }
                STATE_FULL -> {
                    fullAnimation()
                    nextState()
                }
                STATE_SMALL -> {
                    smallAnimation()
                    nextState()
                }
                STATE_CATCHUP_IN -> {
                }
                STATE_IN_ANIMATION -> if (animationStart > 0) {
                    updateHeading()
                }
                STATE_SCROLL -> if (scrollFrames > SCROLL_FRAME_DURATION) {
                    nextState()
                } else {
                    scrollAnimation(position)
                    scrollFrames++
                }
            } // ignore  during catchup animation, heading does not change
        }
    }

    private fun updateHeading() {

        // never exceed progress, could be called with off-timings.
        val p = Math.min(
                (System.currentTimeMillis() - animationStart).toFloat() / ANIMATION_DURATION.toFloat(), 1f)

        var b = initialBearing + animationBearingChange * p
        if (b < 0f) {
            b += 360f
        }
        santaMarker.setCameraOrientation(b)
    }

    private fun nextState() {
        state = (state + 1) % ORDER.size
        scrollFrames = 0
    }

    private fun catchupAnimation() {
        val position = santaMarker.getFuturePosition(clock.nowMillis() +
                ANIMATION_CATCHUP_DURATION)

        animationDuration = ANIMATION_CATCHUP_DURATION
        animateCameraUpdate = CameraUpdateFactory.newLatLng(position)

        executeRunnable(mThreadAnimate)
    }

    private fun smallAnimation() {
        val pos = santaMarker.getFuturePosition(clock.nowMillis() + ANIMATION_DURATION)
        val tilt = Utils.getRandom(MIN_TILT, MAX_TILT)
        val bearing = Utils.getRandom(0f, 306f)

        val camera = CameraPosition.Builder().target(pos)
                .tilt(tilt).zoom(googleMap.cameraPosition.zoom)
                .bearing(bearing).build()

        saveBearing(bearing)

        animationDuration = ANIMATION_DURATION
        animateCameraUpdate = CameraUpdateFactory.newCameraPosition(camera)
        executeRunnable(mThreadAnimate)
    }

    private fun scrollAnimation(position: LatLng) {
        moveCameraUpdate = CameraUpdateFactory.newLatLng(position)
        executeRunnable(mThreadMove)
    }

    private var skipScroll = false

    private val mThreadMove = Runnable {
        if (moveCameraUpdate != null && !isCancelled && !skipScroll) {
            googleMap.moveCamera(moveCameraUpdate!!)
            animationStart = System.currentTimeMillis()
        }
        skipScroll = false
    }

    private fun fullAnimation() {
        // get position in future so that camera is centered when camera
        // animation is finished
        val pos = santaMarker.getFuturePosition(clock.nowMillis() + ANIMATION_DURATION)
        val tilt = Utils.getRandom(MIN_TILT, MAX_TILT)
        val zoom = Utils.getRandom(MIN_ZOOM, MAX_ZOOM)
        val bearing = Utils.getRandom(0f, 306f)

        // store animation heading changes
        saveBearing(bearing)

        val camera = CameraPosition.Builder().target(pos)
                .tilt(tilt).zoom(zoom).bearing(bearing).build()

        animationDuration = ANIMATION_DURATION
        animateCameraUpdate = CameraUpdateFactory.newCameraPosition(camera)
        executeRunnable(mThreadAnimate)
    }

    private fun saveBearing(endBearing: Float) {
        var endBearingLocal = endBearing
        var startBearing = googleMap.cameraPosition.bearing
        if (initialBearing > endBearingLocal) {
            if (startBearing - endBearingLocal > 180) {
                startBearing -= 360f
            }
        } else {
            if (endBearingLocal - startBearing > 180) {
                endBearingLocal -= 360f
            }
        }
        initialBearing = startBearing
        animationBearingChange = endBearingLocal - startBearing
    }

    fun triggerPaddingAnimation() {
        // Cancel the scroll animation
        if (ORDER[state] == STATE_SCROLL) {
            nextState()
            skipScroll = true
        }
    }

    private val mThreadAnimate = Runnable {
        if (animateCameraUpdate != null) {
            googleMap.animateCamera(animateCameraUpdate!!, animationDuration,
                    cancelListener)
            animationStart = System.currentTimeMillis()
        }
    }

    private val cancelListener = object : GoogleMap.CancelableCallback {

        override fun onFinish() {
            nextState()
        }

        override fun onCancel() {
        }
    }

    companion object {

        private val ANIMATION_DURATION = 10000
        private val ANIMATION_CATCHUP_DURATION = 5000

        private val MAX_ZOOM = 10f
        private val MIN_ZOOM = 7.8f
        private val MAX_TILT = 40f
        private val MIN_TILT = 0f

        private val SCROLL_FRAME_DURATION = 300

        private val STATE_FULL = 1
        private val STATE_CATCHUP = 2
        private val STATE_CATCHUP_IN = 3
        private val STATE_SCROLL = 4
        private val STATE_SMALL = 5
        private val STATE_IN_ANIMATION = 6

        // Order: Full, catchup, scroll, small, catchup, scroll
        private val ORDER = intArrayOf(STATE_FULL, STATE_IN_ANIMATION, STATE_CATCHUP, STATE_CATCHUP_IN, STATE_SCROLL,
                /*STATE_SMALL, STATE_IN_ANIMATION, */ STATE_CATCHUP, STATE_CATCHUP_IN, STATE_SCROLL)
    }
}
