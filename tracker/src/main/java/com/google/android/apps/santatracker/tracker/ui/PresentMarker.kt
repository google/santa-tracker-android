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

package com.google.android.apps.santatracker.tracker.ui

import android.graphics.Point
import android.os.Handler
import com.google.android.apps.santatracker.tracker.util.Utils
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Marker representing a present on the googleMap.
 *
 * TODO: This class is almost copies as is from the "santa-tracker" module without removing the
 * unnecessary logic/methods for the new tracker module except the conversion into Kotlin and
 * removed the dependency for SantaPreferences
 * (that was used to get the current time including offset).
 * Need to revisit to remove unnecessary part.
 */
internal class PresentMarker(
    private val googleMap: GoogleMap,
    private val santaMarker: SantaMarker,
    private val handler: Handler,
    animIcons: IntArray,
    private val sizeX: Int,
    private val sizeY: Int
) {

    private val animationMarkers: Array<Marker?> = arrayOfNulls(animIcons.size - 1)
    private val movementMarker: Marker
    private var index = 0

    private var destination: LatLng? = null
    private var frame = 0
    private var directionLat: Double = 0.toDouble()
    private var directionLng: Double = 0.toDouble()
    private var totalAnimationLength: Int = 0

    private var location: LatLng? = null
    private var animationDuration: Int = 0
    private var projection: Projection? = null
    private var waitingForProjection = false
    private var santaPosition: LatLng? = null

    init {

        // setup markers, one per icon
        val position = LatLng(0.0, 0.0)
        for (i in 1 until animIcons.size) {
            animationMarkers[i - 1] = googleMap.addMarker(MarkerOptions()
                    .title(MARKER_TITLE)
                    .icon(BitmapDescriptorFactory.fromResource(animIcons[i]))
                    .position(position).visible(false))
            animationMarkers[i - 1]?.isVisible = false
        }
        movementMarker = googleMap.addMarker(MarkerOptions()
                .title(MARKER_TITLE)
                .icon(BitmapDescriptorFactory.fromResource(animIcons[0]))
                .position(position).visible(false))
        movementMarker.isVisible = false

        // Wait before start
        frame = Utils.getRandom(-ANIMATION_FRAMES_WAIT, 0)

        reset()
    }

    private fun setProjection(p: Projection, santaPosition: LatLng?) {
        this.projection = p
        this.santaPosition = santaPosition
        this.waitingForProjection = false
    }

    fun draw() {

        var destinationLocal = destination
        val projectionLocal = projection
        val santaPositionLocal = santaPosition
        val locationLocal = location
        // 5 States: waiting for valid camera for new present location, waiting
        // for start,
        // New present, moving, animating/disappearing
        if (!VALID_CAMERA && destinationLocal == null && projectionLocal == null) {
        } else if (animationDuration < 0 || waitingForProjection) {
            // wait to start and until projection has been set

            // need to initialise the projection
        } else if (VALID_CAMERA && destination == null && projection == null) {
            waitingForProjection = true
            handler.post(getProjectionRunnable)
        } else if (destinationLocal == null && projectionLocal != null &&
                santaPositionLocal != null) {
            // pick a new destination from screen coordinates
            val y = Utils.getRandom(0, sizeY)
            val x = Utils.getRandom(0, sizeX)

            destinationLocal = projectionLocal.fromScreenLocation(Point(x, y))
            destination = destinationLocal
            if (destinationLocal == null) {
                SantaLog.d("SantaPresents", "Point = " + Point(x, y))
            } else {
                animationDuration = Utils.getRandom(ANIMATION_FRAMES_MOVING_MIN,
                        ANIMATION_FRAMES_MOVING_MAX)
                totalAnimationLength = animationDuration +
                        ANIMATION_FRAMES_FADEOUT * animationMarkers.size
                // calculate speed
                directionLat = (destinationLocal.latitude - santaPositionLocal.latitude) /
                        animationDuration
                directionLng = (destinationLocal.longitude - santaPositionLocal.longitude) /
                        animationDuration
                location = santaPosition
                handler.post(setVisibleLocationRunnable)

                frame = 0
                projection = null
            }
        } else if (frame < animationDuration && locationLocal != null) {
            // Moving animation

            location = LatLng(locationLocal.latitude + directionLat,
                    locationLocal.longitude + directionLng)
            handler.post(setLocationRunnable)

            // animate out if frames left for all animation markers
        } else if (frame in animationDuration..totalAnimationLength) {

            if ((frame - animationDuration) % ANIMATION_FRAMES_FADEOUT == 0) {
                // switch to the next marker
                handler.post(swapIconRunnable)
            }
        } else if (frame > totalAnimationLength) {
            // animation finished, reset and start again after wait
            destination = null
            frame = Utils.getRandom(-ANIMATION_FRAMES_MOVING_MAX, 0)
        }

        // Wait
        if (!waitingForProjection) {
            frame++
        }
    }

    /**
     * Hides the previous animation marker and marks the given marker visible.
     * If this is is the first marker, only it will be set visible. If this is
     * not a marker, nothing will be done.
     */
    private fun showAnimationMarker(i: Int) {
        if (i >= 0 && i < animationMarkers.size) {
            animationMarkers[i]?.position = location
            animationMarkers[i]?.isVisible = true
        }

        // hide the previous marker
        if (i - 1 < 0) {
            movementMarker.isVisible = false
        } else if (i - 1 < animationMarkers.size) {
            animationMarkers[i - 1]?.isVisible = false
        }
    }

    fun reset() {
        animationMarkers[index]?.isVisible = false
        index = 0
    }

    private val getProjectionRunnable = Runnable { setProjection(googleMap.projection, santaMarker.position) }

    private val swapIconRunnable = Runnable { showAnimationMarker((frame - animationDuration) / ANIMATION_FRAMES_FADEOUT) }

    private val setVisibleLocationRunnable = Runnable {
        location?.let {
            movementMarker.position = it
        }
        movementMarker.isVisible = true
    }

    private val setLocationRunnable = Runnable {
        location?.let {
            movementMarker.position = it
        }
    }

    companion object {

        private val ANIMATION_FRAMES_FADEOUT = 4 // per marker
        private val ANIMATION_FRAMES_MOVING_MAX = 275
        private val ANIMATION_FRAMES_MOVING_MIN = 175
        private val ANIMATION_FRAMES_WAIT = 500
        private val MAXIMUM_ZOOM_LEVEL = 8.7

        val MARKER_TITLE = "PresentMarker"

        private var VALID_CAMERA: Boolean = false

        fun setViewParameters(zoom: Double, inSantaCam: Boolean) {
            VALID_CAMERA = zoom > MAXIMUM_ZOOM_LEVEL || inSantaCam
        }
    }
}
