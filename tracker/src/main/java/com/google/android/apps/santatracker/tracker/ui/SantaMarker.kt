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

import android.graphics.Color
import android.os.Handler
import android.os.SystemClock
import com.google.android.apps.santatracker.tracker.R
import com.google.android.apps.santatracker.tracker.time.Clock
import com.google.android.apps.santatracker.tracker.util.watchLayoutOnce
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.SphericalUtil
import java.lang.ref.WeakReference
import java.util.ArrayList

/**
 * Manages the Santa Marker on a [TrackerMapFragment].
 */
class SantaMarker internal constructor(
    private val mapFragment: TrackerMapFragment,
    private val googleMap: GoogleMap, // The googleMap to which this marker is attached
    private val clock: Clock
) {

    // The santa marker
    private val movementMarkers: Array<Marker?>

    // The movement thread
    private var movementThread: SantaMarkerMovementThread? = null

    // The animation thread (marker icon)
    private var animationThread: SantaMarkerAnimationThread? = null
    private val animationMarkers: Array<Marker?>

    // Santa's path
    private var path: Polyline? = null

    // orientation of camera
    private var cameraOrientation: Double = 0.toDouble()
    // Santa's heading when moving
    private var heading = -1.0
    // current movement marker
    private var movingMarker = 0

    private var presentMarkers: Array<PresentMarker?>? = null

    // State of Santa Marker - visiting or travelling
    var isVisiting = false

    // Flag to indicate whether draw presents or not.
    private var presentsDrawingPaused = false

    /**
     * Santa's position.
     */
    /**
     * Returns the current position of this marker.
     */
    @get:Synchronized
    var position: LatLng? = LatLng(0.0, 0.0)

    init {
        val tempLocation = LatLng(0.0, 0.0)

        // setup array of Santa animation markers and make them invisible
        val animationIcons = intArrayOf(
                R.drawable.marker_santa_presents1,
                R.drawable.marker_santa_presents2,
                R.drawable.marker_santa_presents3,
                R.drawable.marker_santa_presents4,
                R.drawable.marker_santa_presents5,
                R.drawable.marker_santa_presents6,
                R.drawable.marker_santa_presents7,
                R.drawable.marker_santa_presents8)
        animationMarkers = animationIcons.map {
            addSantaMarker(it, 0.5f, 1f, tempLocation)
        }.toTypedArray()

        // Present marker
        mapFragment.view?.watchLayoutOnce { v ->
            presentMarkers = PRESENTS.map { resId ->
                PresentMarker(googleMap, this@SantaMarker, Handler(), resId, v.width, v.height)
            }.toTypedArray()
        }

        // Movement markers
        movementMarkers = MOVEMENT_MARKERS.map { resId ->
            addSantaMarker(resId, 0.5f, 0.5f, tempLocation)
        }.toTypedArray()

        movingMarker = 0
    }

    /**
     * Move all Markers used for the present animation to the given position
     */
    private fun moveAnimationMarkers(position: LatLng) {
        for (m in animationMarkers) {
            m?.position = position
        }
    }

    /**
     * Adds a new marker at the given position. u, describes the anchor
     * position.
     */
    private fun addSantaMarker(iconDrawable: Int, u: Float, v: Float, position: LatLng): Marker? {
        return googleMap.addMarker(MarkerOptions().position(position)
                .anchor(u, v) // anchor in center
                .title(TITLE)
                .visible(false)
                .icon(BitmapDescriptorFactory.fromResource(iconDrawable)))
    }

    /**
     * Sets the camera orientation and update the marker if moving.
     */
    fun setCameraOrientation(bearing: Float) {
        cameraOrientation = ((bearing + 360.0f) % 360.0f).toDouble()

        movementThread?.let {
            if (it.isMoving) {
                setMovingIcon()
            }
        }
    }

    /**
     * Update the movement marker.
     */
    private fun setMovingIcon() {

        val angle = (heading - cameraOrientation + 360.0) % 360.0
        val index = Math.round(Math.abs(angle) / 360f * (movementMarkers.size - 1)).toInt() % movementMarkers.size

        setMovingMarker(index)
    }

    /**
     * Hides the previous marker, moves the new marker and makes it visible.
     */
    private fun setMovingMarker(i: Int) {
        if (movingMarker != i) {
            val pos = movementMarkers[movingMarker]?.position
            pos?.let {
                movementMarkers[i]?.position = pos
            }
            movementMarkers[i]?.isVisible = true
            movementMarkers[movingMarker]?.isVisible = false
            movingMarker = i
        }
    }

    /**
     * Sets the position of the current movement marker.
     */
    private fun setMovingPosition(pos: LatLng?) {
        setCachedPosition(pos)
        pos?.let {
            movementMarkers[movingMarker]?.position = pos
        }
    }

    /**
     * Hides the current movement marker.
     */
    private fun hideMovingMarker() {
        movementMarkers[movingMarker]?.isVisible = false
    }

    /**
     * Santa is visiting this location, display animation.
     */
    fun setVisiting(pos: LatLng) {
        isVisiting = true
        setCachedPosition(pos)
        removePath()
        animationThread = SantaMarkerAnimationThread(this, animationMarkers)
        animationThread?.startAnimation(pos)
        hideMovingMarker()

        // reset heading
        heading = -1.0
    }

    /**
     * Saves a location as Santa's current location. This makes it available to other classes.
     *
     * @param position The location to be saved.
     */
    @Synchronized
    private fun setCachedPosition(position: LatLng?) {
        this.position = position
    }

    /**
     * Returns the destination position if the marker is moving, null otherwise.
     */
    val destination: LatLng?
        get() = movementThread?.destination

    /**
     * Animate this marker to the given position for the timestamps.
     */
    internal fun animateTo(
        originLocation: LatLng,
        destinationLocation: LatLng,
        departure: Long,
        arrival: Long
    ) {
        isVisiting = false

        setMovingIcon()
        // create new animation runnable and post to handler
        val thread = SantaMarkerMovementThread(this, departure, arrival,
                destinationLocation, originLocation, true, clock)
        movementThread = thread
        thread.startAnimation()

        val animationThreadLocal = animationThread
        if (animationThreadLocal != null && animationThreadLocal.isAlive) {
            animationThreadLocal.stopAnimation()
        }
    }

    /**
     * Remove the path.
     */
    private fun removePath() {
        path?.let {
            it.remove()
            path = null
        }
    }

    /**
     * Stops all marker animations. Should be called by attached Activity in
     * lifecycle methods.
     */
    internal fun stopAnimations() {
        movementThread?.stopAnimation()
        animationThread?.stopAnimation()
    }

    /**
     * If this marker is currently moving, calculate its future position at the
     * given timestamp. If this marker is not moving, return its current
     * position
     */
    fun getFuturePosition(timestamp: Long): LatLng? {
        val thread = movementThread
        return if (thread != null && thread.isMoving) {
            thread.calculatePosition(timestamp)
        } else {
            position
        }
    }

    internal fun resumePresentsDrawing() {
        presentsDrawingPaused = false
    }

    /**
     * Thread that toggles visibility of the markers, making one marker at a
     * time visible.
     *
     */
    private class SantaMarkerAnimationThread internal constructor(santaMarker: SantaMarker, private val toggleMarkers: Array<Marker?>) : Thread() {
        private var current = 0
        private var frame = 0
        private var stopThread = false
        private val swapRunnable: SwapMarkersRunnable
        private val TEMP_POSITION = LatLng(0.0, 0.0)
        private val santaMarkerRef: WeakReference<SantaMarker>

        init {
            swapRunnable = SwapMarkersRunnable()
            santaMarkerRef = WeakReference(santaMarker)
        }

        override fun run() {
            while (!this.stopThread) {
                val marker = santaMarkerRef.get()
                if (marker == null) {
                    stopThread = true
                    break
                }
                if (frame == 0) {

                    val currentMarker = current
                    val nextMarker = ++current % toggleMarkers.size
                    current = nextMarker

                    swapRunnable.currentMarker = currentMarker
                    swapRunnable.nextMarker = nextMarker
                    val view = marker.mapFragment.view
                    view?.handler?.postAtTime(swapRunnable, TOKEN,
                            SystemClock.uptimeMillis())
                }
                frame = (frame + 1) % ANIMATION_DELAY

                marker.presentMarkers?.let { markers ->
                    markers.map { it?.draw() }
                }

                try {
                    Thread.sleep(REFRESH_RATE.toLong())
                } catch (e: InterruptedException) {
                    // if interrupted, cancel
                    this.stopThread = true
                }
            }
        }

        /**
         * Hide and move markers, need to restart thread to make visible again.
         */
        internal fun hideAll() {
            for (m in toggleMarkers) {
                m?.isVisible = false
                m?.position = TEMP_POSITION
            }
        }

        /**
         * Start this thread. All animated markers (and the normal santa marker)
         * are hidden.
         */
        internal fun startAnimation(position: LatLng) {
            stopThread = false
            hideAll()

            val marker = santaMarkerRef.get() ?: return
            marker.setCachedPosition(position)

            marker.presentMarkers?.let {
                for (m in it) {
                    m?.reset()
                }
            }
            marker.moveAnimationMarkers(position)

            start()
        }

        /**
         * Stop this thread. All animated markers are hidden and the original
         * santa marker is made visible.
         */
        internal fun stopAnimation() {
            // stop execution by removing all callbacks
            stopThread = true
            val marker = santaMarkerRef.get()
            if (marker != null) {
                val view = marker.mapFragment.view
                view?.handler?.removeCallbacksAndMessages(TOKEN)
            }
            hideAll()
        }

        internal inner class SwapMarkersRunnable : Runnable {

            var currentMarker: Int = 0
            var nextMarker: Int = 0

            override fun run() {
                val marker = santaMarkerRef.get() ?: return
                val mapFragment = marker.mapFragment
                toggleMarkers[currentMarker]?.isVisible = false
                toggleMarkers[nextMarker]?.isVisible = true

                val zoom = marker.googleMap.cameraPosition?.zoom
                zoom?.let {
                    PresentMarker.setViewParameters(zoom.toDouble(),
                            mapFragment.isFollowingSanta)
                }
            }
        }

        companion object {

            internal val REFRESH_RATE = SantaMarkerMovementThread.REFRESH_RATE
            internal val ANIMATION_DELAY = 6 // should be equivalent to a post delay of 150ms
        }
    }

    /**
     * Animation Thread for a Santa Marker. Animates the marker between two
     * locations.
     *
     */
    class SantaMarkerMovementThread
    internal constructor(
        marker: SantaMarker,
        private val mStart: Long,
        private val mArrival: Long,
        val destination: LatLng,
        private val startLocation: LatLng,
        drawPath: Boolean,
        private val clock: Clock
    ) : Thread() {

        private var stopThread = false
        private val duration: Double = (mArrival - mStart).toDouble()
        var isMoving = false
        var pathPoints: ArrayList<LatLng>? = null
        val santaMarkerRef: WeakReference<SantaMarker> = WeakReference(marker)

        // Threads
        private val movementRunnable: MovementRunnable

        init {

            marker.removePath()
            if (drawPath) {
                // set up path
                val line = PolylineOptions().add(startLocation)
                        .add(startLocation).color(lineColour)
                marker.path = marker.googleMap.addPolyline(line)?.apply {
                    isGeodesic = true
                }
                pathPoints = ArrayList<LatLng>(2).apply {
                    add(startLocation) // origin
                    add(startLocation) // destination - updated in loop
                }
            } else {
                marker.path = null // already removed
            }

            movementRunnable = MovementRunnable()
        }

        internal fun stopAnimation() {
            this.stopThread = true
            val marker = santaMarkerRef.get()
            if (marker != null) {
                val view = marker.mapFragment.view
                view?.handler?.removeCallbacksAndMessages(TOKEN)
            }
        }

        internal fun startAnimation() {
            this.stopThread = false
            start()
        }

        private val setIconRunnable = Runnable {
            val santaMarker = santaMarkerRef.get()
            santaMarker?.setMovingIcon()
        }
        private val reachedDestinationRunnable = Runnable {
            val santaMarker = santaMarkerRef.get() ?: return@Runnable
            santaMarker.removePath()
            // notify callback
            santaMarker.mapFragment.onSantaReachedDestination(destination)
        }

        override fun run() {
            while (!stopThread) {
                val marker = santaMarkerRef.get()
                if (marker == null) {
                    stopThread = true
                    break
                }
                // need to initialise, marker not set as animated yet
                val view = marker.mapFragment.view
                if (!isMoving) {

                    isMoving = true

                    // calculate heading and update icon
                    marker.heading = SphericalUtil.computeHeading(startLocation,
                            destination)
                    marker.heading = (marker.heading + 360f) % 360f

                    view?.handler?.postAtTime(setIconRunnable, TOKEN, SystemClock.uptimeMillis())
                }

                var t = calculateProgress(clock.nowMillis())

                // Don't go backwards, but it could be negative if this thread is started too early
                t = Math.max(t, 0.0)
                // loop until finished or thread was notified to be stopped
                if (t < 1.0 && !stopThread) {

                    movementRunnable.position = calculatePositionProgress(t)
                    // move marker and update path
                    view?.handler?.postAtTime(movementRunnable, TOKEN,
                            SystemClock.uptimeMillis())

                    if (!marker.presentsDrawingPaused) {
                        marker.presentMarkers?.let {
                            for (p in it) {
                                p?.draw()
                            }
                        }
                    }

                    try {
                        Thread.sleep(REFRESH_RATE.toLong())
                    } catch (e: InterruptedException) {
                        this.stopThread = true
                    }
                } else {
                    // reached final destination,stop moving
                    isMoving = false
                    stopThread = true
                    marker.setCachedPosition(destination)

                    view?.handler?.postAtTime(reachedDestinationRunnable, TOKEN,
                            SystemClock.uptimeMillis())
                }
            }
        }

        /**
         * Calculate the position for the given future timestamp. If the
         * destination is reached before this timestamp, its destination is
         * returned.
         */
        internal fun calculatePosition(timestamp: Long): LatLng {
            val progress = calculateProgress(timestamp)
            return calculatePositionProgress(progress)
        }

        /**
         * Calculates the progress through the animation for the given timestamp
         */
        private fun calculateProgress(currentTimestamp: Long): Double {
            return (currentTimestamp - mStart) / duration // linear progress
        }

        /**
         * Calculate the position for the given progress (start at 0, finished
         * at 1).
         */
        private fun calculatePositionProgress(progress: Double): LatLng {

            return SphericalUtil.interpolate(startLocation, destination, progress)
        }

        internal inner class MovementRunnable : Runnable {

            var position: LatLng? = null

            override fun run() {
                val marker = santaMarkerRef.get() ?: return
                marker.setMovingPosition(position)

                val positionLocal = position
                // update path if it is enabled
                val pathPointsLocal = pathPoints
                val pathLocal = marker.path
                if (pathLocal != null && positionLocal != null && pathPointsLocal != null) {
                    pathPointsLocal[1] = positionLocal
                    pathLocal.points = pathPoints
                }

                val zoom = marker.googleMap.cameraPosition?.zoom
                if (zoom != null) {
                    PresentMarker.setViewParameters(zoom.toDouble(),
                            marker.mapFragment.isFollowingSanta)
                }

                val time = clock.nowMillis()
                marker.mapFragment.onSantaIsMovingProgress(position, mArrival - time, time - mStart)
            }
        }

        companion object {

            /**
             * Refresh rate of this thread (it is called again every X ms.)
             */
            internal val REFRESH_RATE = 17
        }
    }

    /**
     * Interface for callbacks from a [SantaMarker].
     *
     */
    internal interface SantaMarkerInterface {

        fun onSantaReachedDestination(destination: LatLng)
    }

    companion object {

        // private static final String TAG = "SantaMarker";

        /**
         * Snippet used by all markers that make up a santa marker (including all
         * animation frame markers).
         */
        internal val TITLE = "santa-marker"

        private val TOKEN = Any()

        // line colour
        private val lineColour = Color.parseColor("#AA109f65")

        // 2D array: for each present type, 4 types of presents, 0=100
        private val PRESENTS = arrayOf(
                intArrayOf(R.drawable.blue_100, R.drawable.blue_75, R.drawable.blue_50, R.drawable.blue_25),
                intArrayOf(R.drawable.purple_100, R.drawable.purple_75, R.drawable.purple_50, R.drawable.purple_25),
                intArrayOf(R.drawable.yellow_100, R.drawable.yellow_75, R.drawable.yellow_50, R.drawable.yellow_25),
                intArrayOf(R.drawable.red_100, R.drawable.red_75, R.drawable.red_50, R.drawable.red_25),
                intArrayOf(R.drawable.green_100, R.drawable.green_75, R.drawable.green_50, R.drawable.green_25))

        /**
         * Markers for santa movement
         */
        private val MOVEMENT_MARKERS = intArrayOf(R.drawable.santa_n,
                R.drawable.santa_ne,
                R.drawable.santa_e,
                R.drawable.santa_se,
                R.drawable.santa_s,
                R.drawable.santa_sw,
                R.drawable.santa_w,
                R.drawable.santa_nw,
                R.drawable.santa_n)
    }
}
