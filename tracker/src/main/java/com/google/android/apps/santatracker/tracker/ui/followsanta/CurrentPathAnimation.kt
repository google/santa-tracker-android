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
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

/**
 * Camera animation that shows the path from Santa's current position to the
 * destination. The camera is animated independently of calls to
 * [MoveAroundSanta.onSantaMoving] until the [.MAX_ZOOM] level is reached.
 */
internal class CurrentPathAnimation(
    handler: Handler,
    map: GoogleMap,
    marker: SantaMarker,
    val clock: Clock
)
    : FollowSantaAnimation(handler, map, marker) {

    private var cameraUpdate: CameraUpdate? = null

    fun start() {
        // Start the first animation - zoom to capture the current position and
        // destination
        animateShowSantaDestination(santaMarker.position)
    }

    // animate to a new bounds with santa and his destination
    private val threadAnimate: Runnable by lazy {
        // The reason to initialize the Runnable by lazy is to avoid the type checking from
        // run into a recursion.
        // https://stackoverflow.com/questions/45442838/type-checking-has-run-into-a-recursive-in-kotlin
        Runnable {
            val cameraUpdateLocal = cameraUpdate
            if (cameraUpdate != null) {
                googleMap.animateCamera(cameraUpdateLocal, ANIMATION_DURATION, cancelCallback)
            }
        }
    }

    /**
     * Animate showing the destination and the position.
     */
    private fun animateShowSantaDestination(futurePosition: LatLng?) {
        val santaDestination = santaMarker.destination
        // Only construct a camera update if both positions are valid
        if (futurePosition == null || santaDestination == null) {
            return
        }
        cameraUpdate = CameraUpdateFactory.newLatLngBounds(
                LatLngBounds.Builder().include(futurePosition)
                        .include(santaDestination).build(), PADDING)
        executeRunnable(threadAnimate)
    }

    /**
     * Animate at current zoom level to center on the position.
     */
    private fun animateFollowSanta(futurePosition: LatLng?) {
        if (futurePosition == null) {
            return
        }
        cameraUpdate = CameraUpdateFactory.newLatLng(futurePosition)
        executeRunnable(threadAnimate)
    }

    private val cancelCallback = object : GoogleMap.CancelableCallback {

        override fun onFinish() {
            // only zoom until max zoom level, after that only move camera
            val futurePosition = santaMarker
                    .getFuturePosition(clock.nowMillis() + ANIMATION_DURATION)

            if (futurePosition == null || googleMap.cameraPosition.zoom <= MAX_ZOOM) {
                animateShowSantaDestination(futurePosition)
            } else {
                // Animate to where Santa is going to be
                animateFollowSanta(futurePosition)
            }
            executeRunnable(threadAnimate)
        }

        override fun onCancel() {}
    }

    companion object {

        private val ANIMATION_DURATION = 2000
        private val PADDING = 50 // TODO: move to constructor

        private val MAX_ZOOM = 10f
    }
}
