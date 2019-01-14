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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import androidx.annotation.DrawableRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.apps.santatracker.tracker.R
import com.google.android.apps.santatracker.tracker.audio.TrackerSoundPlayer
import com.google.android.apps.santatracker.tracker.di.Injectable
import com.google.android.apps.santatracker.tracker.time.Clock
import com.google.android.apps.santatracker.tracker.ui.followsanta.AtLocation
import com.google.android.apps.santatracker.tracker.ui.followsanta.FollowSantaAnimator
import com.google.android.apps.santatracker.tracker.util.observeNonNull
import com.google.android.apps.santatracker.tracker.viewmodel.TrackerViewModel
import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.apps.santatracker.util.MeasurementManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject

class TrackerMapFragment : SupportMapFragment(), SantaMarker.SantaMarkerInterface, Injectable {

    private companion object {
        // duration of camera animation to santa when starting to follow Santa
        const val MOVE_TO_SANTA_DURATION_MILLIS = 2000
        val BOGUS_LOCATION = LatLng(0.0, 0.0)
        val MARKER_PAST = "MARKER_PAST"
        val MARKER_NEXT = "MARKER_NEXT"
        val MARKER_ACTIVE = "MARKER_ACTIVE"
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var clock: Clock
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var viewModel: TrackerViewModel
    private val handler = Handler()
    private var santaMarker: SantaMarker? = null
    private var followSantaAnimator: FollowSantaAnimator? = null
    private var paddingCamLeft: Int = 0
    private var paddingCamTop: Int = 0
    private var paddingCamRight: Int = 0
    private var paddingCamBottom: Int = 0

    private var nextMarker: Marker? = null

    /** Markers used for the active marker */
    private var activeMarker: Marker? = null
    private var pendingInfoMarker: Marker? = null
    private var currentInfoMarker: Marker? = null

    // info window for marker pop-up bubbles
    private var infoWindowAdapter: DestinationInfoWindowAdapter? = null

    private var markerIconVisited: BitmapDescriptor? = null
    private var trackerMapCallback: TrackerMapCallback? = null
    private var soundPlayer: TrackerSoundPlayer? = null

    /**
     * Stored destinations where Santa has already visited.
     * Used for putting the markers already visited and retrieving the previous destination
     * when Santa starts to travel to a next city.
     */
    private var visitedDestinations: List<Destination>? = null
    /**
     * Stored current destination. When the Santa is traveling, updated Destination can be
     * observed from the [TrackerViewModel], but storing the destination to make the current
     * destination marker clickable.
     */
    private var destination: Destination? = null

    var isFollowingSanta: Boolean = true
        private set

    private val movingCatchupCallback = object : GoogleMap.CancelableCallback {
        override fun onFinish() {
            followSantaAnimator?.resume()
            santaMarker?.resumePresentsDrawing()
        }

        override fun onCancel() {
            followSantaAnimator?.resume()
        }
    }

    private val reachedAnimationCallback = object : GoogleMap.CancelableCallback {
        override fun onFinish() {
            getMapAsync { map ->
                val marker = santaMarker
                handler.post({
                    if (marker == null) {
                        return@post
                    }
                    val position = marker.position
                    position?.let {
                        map.animateCamera(CameraUpdateFactory.newLatLng(position),
                                MOVE_TO_SANTA_DURATION_MILLIS, null)
                    }
                })
            }
        }

        override fun onCancel() {
            // ignore
        }
    }

    /**
     * Marker click listener. Handles clicks on markers. When a destination
     * marker is clicked, the active marker is set to this position and the
     * corresponding info window is displayed. If santa is followed, it is disabled.
     */
    private val markerClickListener = GoogleMap.OnMarkerClickListener { marker ->
        if (marker.title == null) {
            false
        } else if (marker.title == SantaMarker.TITLE) {
            soundPlayer?.sayHoHoHo()
            hideInfoWindow()

            // App Measurement
            MeasurementManager.recordCustomEvent(firebaseAnalytics,
                    getString(com.google.android.apps.santatracker.common.R.string.analytics_event_category_tracker),
                    getString(com.google.android.apps.santatracker.common.R.string.analytics_tracker_action_clicksanta), null)

            getMapAsync { it.whirl() }
            true
        } else if (marker.title == PresentMarker.MARKER_TITLE) {
            true
        } else if (marker.title == MARKER_NEXT || marker.title == MARKER_PAST) {
            showInfoWindow(marker)
            true
        } else if (marker.title == MARKER_ACTIVE) {
            hideInfoWindow()
            true
        } else {
            false
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = activity ?: return
        viewModel = ViewModelProviders.of(activity, viewModelFactory)
                .get(TrackerViewModel::class.java)
        val lifecycleOwner = activity as LifecycleOwner
        viewModel.santaState.observeNonNull(lifecycleOwner, this::adjustSantaState)
    }

    private fun adjustSantaState(state: TrackerViewModel.SantaState) {
        val visited = state.visitedDestinations
        if (state.isTraveling && visited.isNotEmpty()) {
            val previous = visited[visited.size - 1]
            setSantaTraveling(previous, state.destination)
            trackerMapCallback?.onTravelToDestination(state.destination)
            followSanta(true)
            destination = state.destination
        } else {
            setSantaVisiting(state.destination, true)
            if (isFollowingSanta) {
                followSanta(true)
            }
            trackerMapCallback?.onVisitDestination(state.destination)
        }

        val visitedCountSoFar = visitedDestinations?.size ?: 0
        for (i in visitedCountSoFar until visited.size) {
            addVisitedLocation(visited[i])
        }
        visitedDestinations = state.visitedDestinations
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        val activity = activity ?: return
        soundPlayer = TrackerSoundPlayer(activity)
        try {
            trackerMapCallback = activity as TrackerMapCallback
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement TrackerMapCallback")
        }
    }

    override fun onResume() {
        super.onResume()
        getMapAsync {
            // Set up the googleMap
            it.setMapStyle(MapStyleOptions.loadRawResourceStyle(activity,
                    com.google.android.apps.santatracker.common.R.raw.map_style))
            it.uiSettings.apply {
                isCompassEnabled = false
                isZoomControlsEnabled = false
            }
            setupMap(it)
            viewModel.santaState.value?.let(this::adjustSantaState)
        }
    }

    override fun onPause() {
        super.onPause()
        getMapAsync {
            it.clear()
        }
        santaMarker?.stopAnimations()
    }

    override fun onDetach() {
        super.onDetach()
        trackerMapCallback = null
        soundPlayer?.release()
    }

    override fun onStop() {
        super.onStop()
        soundPlayer?.release()
    }

    /**
     * Called when Santa has reached the given destination.
     */
    override fun onSantaReachedDestination(destination: LatLng) {
        nextMarker?.isVisible = false

        // Santa has reached destination - update camera
        // center on Santa's current position at lower zoom level
        if (isFollowingSanta) {
            getMapAsync { map ->
                // Post camera update through Handler to allow for subsequent camera animation in
                // CancellableCallback
                handler.post({
                    val marker = santaMarker ?: return@post
                    val position = marker.position
                    position?.let {
                        map.animateCamera(AtLocation.getCameraUpdate(position,
                                map.cameraPosition.bearing),
                                MOVE_TO_SANTA_DURATION_MILLIS, reachedAnimationCallback)
                    }
                })
            }
        }
        followSantaAnimator?.reset()
    }

    private fun setupMap(map: GoogleMap) {
        map.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isFollowingSanta = false
                trackerMapCallback?.onFollowingSantaChanged(false)
            }
        }
        map.setOnMarkerClickListener(markerClickListener)
        val activity = activity ?: return
        infoWindowAdapter = DestinationInfoWindowAdapter(activity.layoutInflater)
        map.setInfoWindowAdapter(infoWindowAdapter)

        santaMarker?.stopAnimations()
        val marker = SantaMarker(this, map, clock)
        santaMarker = marker
        followSantaAnimator?.cancel()
        followSantaAnimator = FollowSantaAnimator(map, marker, clock)

        val activeMarker = map.addMarker(MarkerOptions()
                .position(BOGUS_LOCATION)
                .icon(createMarker(R.drawable.marker_pin_active))
                .title(MARKER_ACTIVE)
                .visible(false)
                .snippet("0")
                .anchor(0.5f, 1f))
        activeMarker.isVisible = false
        this.activeMarker = activeMarker

        val nextMarker = map.addMarker(MarkerOptions()
                .position(BOGUS_LOCATION)
                .icon(createMarker(R.drawable.marker_pin))
                .alpha(0.6f)
                .visible(false)
                .snippet("0")
                .title(MARKER_NEXT)
                .anchor(0.5f, 1f))
        nextMarker.isVisible = false
        this.nextMarker = nextMarker

        markerIconVisited = createMarker(R.drawable.marker_pin)
    }

    private fun GoogleMap.whirl() {
        val oldPosition1 = cameraPosition
        // Calculate bearing, +1 so that the camera always moves in the same direction
        val bearing1 = (oldPosition1.bearing + 181f) % 360f
        animateCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder(oldPosition1)
                        .bearing(bearing1)
                        .build()),
                MOVE_TO_SANTA_DURATION_MILLIS,
                object : GoogleMap.CancelableCallback {
                    override fun onFinish() {
                        val oldPosition2 = cameraPosition
                        val bearing2 = (oldPosition2.bearing + 181f) % 360f
                        animateCamera(CameraUpdateFactory.newCameraPosition(
                                CameraPosition.builder(oldPosition2)
                                        .bearing(bearing2)
                                        .build()),
                                MOVE_TO_SANTA_DURATION_MILLIS, null)
                    }

                    override fun onCancel() {
                    }
                })
    }

    /**
     * If the active marker is set, hide its info window and restore the original
     * marker.
     */
    private fun restoreClickedMarker() {
        currentInfoMarker?.let { currentInfo ->
            activeMarker?.let { active ->
                active.hideInfoWindow()
                active.isVisible = false
                currentInfo.position = active.position
                active.position = BOGUS_LOCATION
                currentInfo.isVisible = true
                currentInfoMarker = null
            }
        }
    }

    /**
     * Hides the current info window if it is displayed
     */
    private fun hideInfoWindow() {
        currentInfoMarker?.let {
            restoreClickedMarker()
        }
    }

    /**
     * Display the info window for a marker. The database is queried using a DestinationTask to
     * retrieve a Destination object.
     */
    private fun showInfoWindow(marker: Marker) {
        if (isFollowingSanta) {
            unfollowSanta()
        }
        pendingInfoMarker = marker
        hideInfoWindow()
        findDestination(marker.snippet)?.let {
            showInfoWindow(it)
        }
        // App Measurement
        MeasurementManager.recordCustomEvent(firebaseAnalytics,
                getString(com.google.android.apps.santatracker.common.R.string.analytics_event_category_tracker),
                getString(com.google.android.apps.santatracker.common.R.string.analytics_tracker_action_location),
                marker.snippet)
    }

    /**
     * Finds the Destination from the current destination and visited destinations
     */
    private fun findDestination(id: String): Destination? {
        if (destination?.id == id) {
            return destination
        }
        val filtered = visitedDestinations?.filter { it.id == id }
        filtered?.let {
            if (filtered.size == 1) {
                return filtered[0]
            }
        }
        return null
    }

    private fun showInfoWindow(destination: Destination) {
        // ensure that destination data belongs to the pending info marker, ignore otherwise
        pendingInfoMarker?.let { pending ->
            if (pending.snippet == null || destination.id != pending.snippet) {
                return
            }
            currentInfoMarker = pending
            pending.isVisible = false

            updateActiveDestination(destination, currentInfoMarker)
            infoWindowAdapter?.setData(destination)
            activeMarker?.showInfoWindow()
            pendingInfoMarker = null
        }
    }

    /**
     * Sets the active marker to the given destination and makes it visible.
     */
    private fun updateActiveDestination(
        destination: Destination,
        clickedMarker: Marker?
    ) {
        activeMarker?.let {
            it.position = destination.latLng
            clickedMarker?.position = BOGUS_LOCATION
            it.isVisible = true
            it.snippet = destination.id
        }
    }

    /**
     * Animate the santa marker to destination to arrive at its arrival time.
     */
    private fun setSantaTraveling(previous: Destination, next: Destination) {
        soundPlayer?.sayHoHoHo()
        soundPlayer?.startSleighBells()

        nextMarker?.apply {
            snippet = next.id
            position = next.latLng
            isVisible = true
        }
        santaMarker?.animateTo(previous.latLng, next.latLng, previous.departure, next.arrival)
    }

    private fun setSantaVisiting(destination: Destination, playSound: Boolean) {
        santaMarker?.setVisiting(destination.latLng)

        soundPlayer?.stopSleighBells()
        if (playSound) {
            soundPlayer?.sayHoHoHo()
        }

        // hide the next marker from this position, move it off-screen to
        // prevent touch events
        nextMarker?.isVisible = false
        nextMarker?.position = BOGUS_LOCATION

        val activeMarkerLocal = activeMarker
        // if the info window for this position is open, dismiss it
        if (activeMarkerLocal != null && activeMarkerLocal.isVisible &&
                activeMarkerLocal.snippet == destination.id) {
            hideInfoWindow()
        }
    }

    fun followSanta(animateToSanta: Boolean) {
        getMapAsync { map ->
            map.setPadding(paddingCamLeft, paddingCamTop, paddingCamRight, paddingCamBottom)
            val marker = santaMarker ?: return@getMapAsync
            isFollowingSanta = true
            (activity as? TrackerMapCallback)?.onFollowingSantaChanged(true)
            hideInfoWindow()
            followSantaAnimator?.reset()
            if (animateToSanta) {
                // santa is already enroute, start animation to Santa and pause animator to speed up
                // camera animation
                if (!marker.isVisiting) {
                    followSantaAnimator?.pause()
                    val futurePosition = marker.getFuturePosition(
                            clock.nowMillis() + MOVE_TO_SANTA_DURATION_MILLIS)
                    if (futurePosition != null && !(futurePosition.latitude == 0.0 &&
                                    futurePosition.longitude == 0.0)) {
                        map.animateCamera(CameraUpdateFactory.newLatLng(futurePosition),
                                MOVE_TO_SANTA_DURATION_MILLIS, movingCatchupCallback)
                    } else {
                        followSantaAnimator?.resume()
                        marker.resumePresentsDrawing()
                    }
                } else {
                    // Santa is at a location
                    val position = marker.position
                    position?.let {
                        onSantaReachedDestination(position)
                    }
                }
            } else {
                marker.resumePresentsDrawing()
            }
        }
    }

    private fun unfollowSanta() {
        if (isFollowingSanta) {
            isFollowingSanta = false
            trackerMapCallback?.onFollowingSantaChanged(false)

            followSantaAnimator?.cancel()
        }
    }

    /**
     * Santa is currently moving, called with a progress update. If in SantaCam,
     * the camera is repositioned to capture santa.
     */
    fun onSantaIsMovingProgress(
        position: LatLng?,
        remainingTime: Long,
        elapsedTime: Long
    ) {
        val marker = santaMarker
        if (isFollowingSanta && marker != null && position != null &&
                marker.position != null) {
            // use animator to update camera if in santa cam mode
            followSantaAnimator?.animate(position, remainingTime, elapsedTime)
        }
    }

    fun setMapPadding(left: Int, top: Int, right: Int, bottom: Int) {
        paddingCamLeft = left
        paddingCamTop = top
        paddingCamRight = right
        paddingCamBottom = bottom
        getMapAsync { googleMap ->
            googleMap.setPadding(paddingCamLeft, paddingCamTop,
                    paddingCamRight, paddingCamBottom)
        }
        val animator = followSantaAnimator
        if (isFollowingSanta && animator != null) {
            animator.triggerPaddingAnimation()
        }
    }

    private fun createMarker(@DrawableRes id: Int): BitmapDescriptor? {
        val activity = activity ?: return null
        val drawable = VectorDrawableCompat.create(resources, id, activity.theme) ?: return null
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        drawable.setBounds(0, 0, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun addVisitedLocation(destination: Destination) {
        getMapAsync {
            it.addMarker(MarkerOptions()
                    .position(destination.latLng)
                    .icon(markerIconVisited)
                    .anchor(0.5f, 1f)
                    .title(MARKER_PAST)
                    .snippet(destination.id))
        }
    }

    interface TrackerMapCallback {
        fun onFollowingSantaChanged(followingSanta: Boolean)
        fun onTravelToDestination(destination: Destination)
        fun onVisitDestination(destination: Destination)
    }
}
