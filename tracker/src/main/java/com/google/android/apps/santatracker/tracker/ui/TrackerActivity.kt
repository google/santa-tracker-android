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

import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.google.android.apps.santatracker.Intents
import com.google.android.apps.santatracker.config.Config
import com.google.android.apps.santatracker.games.OnDemandActivity
import com.google.android.apps.santatracker.tracker.R
import com.google.android.apps.santatracker.tracker.cast.CastUtil
import com.google.android.apps.santatracker.tracker.cast.LoggingCastSessionListener
import com.google.android.apps.santatracker.tracker.cast.LoggingCastStateListener
import com.google.android.apps.santatracker.tracker.time.Clock
import com.google.android.apps.santatracker.tracker.util.TrackerIntents
import com.google.android.apps.santatracker.tracker.util.observeNonNull
import com.google.android.apps.santatracker.tracker.viewmodel.TrackerViewModel
import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.apps.santatracker.tracker.vo.DestinationStreetView
import com.google.android.apps.santatracker.util.AccessibilityUtil
import com.google.android.apps.santatracker.util.MeasurementManager
import com.google.android.apps.santatracker.util.PlayServicesUtil
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.IntroductoryOverlay
import com.google.android.gms.cast.framework.Session
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

open class TrackerActivity : OnDemandActivity(), HasSupportFragmentInjector,
        TrackerMapFragment.TrackerMapCallback {

    companion object {
        private const val TAG = "TrackerActivity"
        private const val FRAGMENT_MAP = "map"
        private const val FAB_THRESHOLD = 0.8f
        private const val SCREEN_IDLE_TIMEOUT_MS = (5 * 60 * 1000).toLong() // 5 minutes

        private val FADE = Fade().apply { duration = 200 }
    }

    @Inject lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var clock: Clock
    @Inject lateinit var firebaseAnalytics: FirebaseAnalytics
    @Inject lateinit var config: Config
    private lateinit var trackerViewModel: TrackerViewModel

    private lateinit var mapFragment: TrackerMapFragment

    private lateinit var bottomSheet: FrameLayout
    private var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>? = null
    private lateinit var buttonTop: ImageButton
    private lateinit var buttonFollowSanta: FollowSantaButton
    private lateinit var stream: RecyclerView
    private lateinit var cardAdapter: CardAdapter

    private val accessibilityManager: AccessibilityManager by lazy {
        getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }

    /**
     * BroadcastReceiver that is called when any of config values are changed relevant to tracker
     */
    private val finishTrackerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            SantaLog.d(TAG, "Some values of Config relevant to tracker has changed. Finishing")
            finish()
        }
    }

    // TODO: Remove this field once there is a good way to toggle the visibility of the cast button
    //       by observing the LaunchFlags LiveData
    private var castDisabledState: Boolean = false

    private val lostContactString: String by lazy {
        resources.getString(R.string.lost_contact_with_santa)
    }
    private val announceTravelTo: String by lazy {
        buildString {
            append(resources.getString(R.string.in_transit))
            append(" ")
            append(resources.getString(R.string.next_destination))
            append(" %s")
        }
    }
    private val announceArrivedAt: String by lazy {
        resources.getString(R.string.santa_is_now_arriving_in_x)
    }

    private val screenLockHandler = Handler()
    private val screenUnlockRunnable =
            Runnable { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }

    // Cast
    private var haveGooglePlayServices = false
    private var mediaRouteMenuItem: MenuItem? = null
    private lateinit var castListener: SessionManagerListener<*>
    private lateinit var castStateListener: OverlayCastStateListener
    private var castOverlayShown = false

    private val isTv: Boolean by lazy {
        (getSystemService(Context.UI_MODE_SERVICE) as UiModeManager)
                .currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracker)

        // Cast
        if (!isTv) {
            haveGooglePlayServices = PlayServicesUtil.hasPlayServices(this)
            castListener = LoggingCastSessionListener(this,
                    com.google.android.apps.santatracker.common.R.string.analytics_cast_session_launch,
                    firebaseAnalytics)
            castStateListener = OverlayCastStateListener(this,
                    com.google.android.apps.santatracker.common.R.string.analytics_cast_statechange_map)
        }

        // Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        // App Measurement
        MeasurementManager.recordScreenView(firebaseAnalytics,
                getString(com.google.android.apps.santatracker.common.R.string.analytics_screen_tracker))

        // Set up timer to remove screen lock
        resetScreenTimer()

        val rootLayout = findViewById<CoordinatorLayout>(R.id.coordinator)
        val mapHolder = findViewById<FrameLayout>(R.id.map)
        val streamContainer = findViewById<View>(R.id.stream_container)

        mapHolder.setOnApplyWindowInsetsListener { _, insets ->
            if (bottomSheetBehavior != null) {
                rootLayout.doOnLayout {
                    streamContainer.layoutParams.height = rootLayout.height - insets.systemWindowInsetTop
                    streamContainer.requestLayout()
                }
            } else {
                // In tablet mode
                streamContainer.updatePadding(top = insets.systemWindowInsetTop)
            }

            insets
        }

        // The map
        if (savedInstanceState == null) {
            mapFragment = TrackerMapFragment()
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.map, mapFragment, FRAGMENT_MAP)
                    .commit()
        } else {
            mapFragment = supportFragmentManager.findFragmentByTag(FRAGMENT_MAP)
                    as TrackerMapFragment
        }

        // The card stream
        val callback = object : CardAdapter.Callback {
            override fun onPlayMovie(youtubeId: String) {
                // App Measurement
                MeasurementManager.recordCustomEvent(firebaseAnalytics,
                        getString(com.google.android.apps.santatracker.common.R.string.analytics_event_category_tracker),
                        getString(com.google.android.apps.santatracker.common.R.string.analytics_tracker_action_video),
                        youtubeId)

                startActivity(TrackerIntents.getYoutubeIntent(this@TrackerActivity, youtubeId))
            }

            override fun onStreetView(streetView: DestinationStreetView) {
                MeasurementManager.recordCustomEvent(firebaseAnalytics,
                        getString(com.google.android.apps.santatracker.common.R.string.analytics_event_category_tracker),
                        getString(com.google.android.apps.santatracker.common.R.string.analytics_tracker_action_streetview),
                        streetView.id)

                startActivity(TrackerIntents.getStreetViewIntent(streetView))
            }
        }
        val streamLayoutManager = LinearLayoutManager(this)
        stream = findViewById<RecyclerView>(R.id.stream).apply {
            setHasFixedSize(true)
            layoutManager = streamLayoutManager
            addItemDecoration(SeparatorDecoration(this@TrackerActivity))
        }
        cardAdapter = CardAdapter(clock, callback, stream, false, isTv)
        stream.adapter = cardAdapter

        // View references
        bottomSheet = findViewById(R.id.stream_container)
        @Suppress("unchecked_cast")
        bottomSheetBehavior = (bottomSheet.layoutParams as CoordinatorLayout.LayoutParams)
                .behavior as? BottomSheetBehavior<FrameLayout>
        buttonTop = findViewById(R.id.top)
        buttonFollowSanta = findViewById(R.id.follow_santa)
        // View Model
        trackerViewModel = ViewModelProviders.of(this, viewModelFactory).get(TrackerViewModel::class.java)
        trackerViewModel.presentsDelivered.observeNonNull(this) {
            cardAdapter.dashboard.presents.text = it
        }
        trackerViewModel.countdown.observeNonNull(this) {
            cardAdapter.dashboard.countdown.text = it
        }
        trackerViewModel.countdownLabel.observeNonNull(this) {
            cardAdapter.dashboard.countdownLabel.text = it
        }
        trackerViewModel.location.observeNonNull(this) {
            cardAdapter.dashboard.location.text = it
        }
        trackerViewModel.locationLabel.observeNonNull(this) {
            cardAdapter.dashboard.locationLabel.text = it
        }
        trackerViewModel.showCountdown.observeNonNull(this) {
            TransitionManager.beginDelayedTransition(
                    cardAdapter.dashboard.itemView as ViewGroup, FADE)
            if (it) {
                cardAdapter.dashboard.countdownContainer.visibility = View.VISIBLE
                cardAdapter.dashboard.presentsContainer.visibility = View.INVISIBLE
            } else {
                cardAdapter.dashboard.countdownContainer.visibility = View.INVISIBLE
                cardAdapter.dashboard.presentsContainer.visibility = View.VISIBLE
            }
        }
        trackerViewModel.stream.observeNonNull(this) { cards ->
            cardAdapter.setCards(cards)
        }
        trackerViewModel.finishedTraveling.observeNonNull(this) {
            if (it) {
                // App Measurement
                MeasurementManager.recordCustomEvent(firebaseAnalytics,
                        getString(com.google.android.apps.santatracker.common.R.string.analytics_event_category_tracker),
                        getString(com.google.android.apps.santatracker.common.R.string.analytics_tracker_action_finished),
                        getString(com.google.android.apps.santatracker.common.R.string.analytics_tracker_error_nodata))

                finish()
            }
        }
        trackerViewModel.featureState.observeNonNull(this) {
            setSantaDisabled(it.santaDisabled)
            setCastDisabled(it.castDisabled)
            setDestinationPhotoDisabled(it.photoDisabled)
        }

        // Event handlers
        bottomSheetBehavior?.setBottomSheetListener(
                object : BottomSheetBehavior.BottomSheetListener() {
                    override fun onStateChanged(newState: Int) {
                        adjustMapPadding(newState)
                    }

                    override fun onSlide(slideOffset: Float) {
                        if (slideOffset > FAB_THRESHOLD) {
                            buttonFollowSanta.setHasEnoughSpace(false)
                        } else {
                            buttonFollowSanta.setHasEnoughSpace(true)
                        }
                    }
                })
        buttonFollowSanta.setOnClickListener {
            mapFragment.followSanta(true)
            MeasurementManager.recordCustomEvent(firebaseAnalytics,
                    getString(com.google.android.apps.santatracker.common.R.string.analytics_event_category_tracker),
                    getString(com.google.android.apps.santatracker.common.R.string.analytics_tracker_action_cam),
                    getString(com.google.android.apps.santatracker.common.R.string.analytics_tracker_cam_fab))
        }
        buttonTop.setOnClickListener {
            if (streamLayoutManager.findFirstVisibleItemPosition() > 100) {
                // Too far; jump
                stream.scrollToPosition(0)
            } else {
                stream.smoothScrollToPosition(0)
            }
        }
        stream.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val visibility =
                        if (!isTv && streamLayoutManager.findFirstVisibleItemPosition() > 0) {
                            View.VISIBLE
                        } else {
                            View.INVISIBLE
                        }
                if (buttonTop.visibility != visibility) {
                    TransitionManager.beginDelayedTransition(bottomSheet, FADE)
                    buttonTop.visibility = visibility
                }
            }
        })
        bottomSheetBehavior?.let { adjustMapPadding(it.state) }

        LocalBroadcastManager.getInstance(this).registerReceiver(finishTrackerReceiver,
                IntentFilter(Intents.FINISH_TRACKER_INTENT))
    }

    override fun onResume() {
        super.onResume()
        bottomSheetBehavior?.let { adjustMapPadding(it.state) }

        registerCastListeners()
    }

    override fun onPause() {
        @Suppress("UNCHECKED_CAST")
        if (!isTv) {
            CastUtil.removeCastListener(this, castListener as SessionManagerListener<Session>)
            CastUtil.removeCastStateListener(this, castStateListener)
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(finishTrackerReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.tracker, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // Add cast button
        if (!castDisabled && mediaRouteMenuItem == null) {
            mediaRouteMenuItem = CastButtonFactory
                    .setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
        }

        // Toggle cast visibility
        mediaRouteMenuItem?.let { item ->
            item.isVisible = true
            // Display the cast overlay if the item exists.
            // The overlay is only shown if the item is visible.
            showCastOverlay()
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetScreenTimer()
    }

    private fun resetScreenTimer() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        screenLockHandler.removeCallbacks(screenUnlockRunnable)
        screenLockHandler.postDelayed(screenUnlockRunnable, SCREEN_IDLE_TIMEOUT_MS)
    }

    private val castDisabled: Boolean
        get() = !haveGooglePlayServices || castDisabledState

    private fun adjustMapPadding(newState: Int) {
        bottomSheetBehavior?.let { behavior ->
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                mapFragment.setMapPadding(0, 0, 0, behavior.peekHeight -
                        behavior.hiddenPeekHeight)
            } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                mapFragment.setMapPadding(0, 0, 0, 0)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onBackPressed() {
        bottomSheetBehavior.let { behavior ->
            if (behavior != null && behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                stream.stopScroll()
                stream.smoothScrollToPosition(0)
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onFollowingSantaChanged(followingSanta: Boolean) {
        buttonFollowSanta.setIsFollowingSanta(followingSanta)
    }

    override fun onTravelToDestination(destination: Destination) {
        AccessibilityUtil.announceText(String.format(announceTravelTo, destination.printName),
                stream, accessibilityManager)
    }

    override fun onVisitDestination(destination: Destination) {
        AccessibilityUtil
                .announceText(String.format(announceArrivedAt, destination.printName),
                        stream, accessibilityManager)
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = dispatchingAndroidInjector

    private fun setSantaDisabled(disableApp: Boolean) {
        if (disableApp) {
            SantaLog.d(TAG, "Lost Santa.")

            // App Measurement
            MeasurementManager.recordCustomEvent(firebaseAnalytics,
                    getString(com.google.android.apps.santatracker.common.R.string.analytics_event_category_tracker),
                    getString(com.google.android.apps.santatracker.common.R.string.analytics_tracker_action_error),
                    getString(com.google.android.apps.santatracker.common.R.string.analytics_tracker_error_switchoff))

            handleErrorFinish()
        }
    }

    private fun setDestinationPhotoDisabled(disablePhoto: Boolean) {
        cardAdapter.disableDestinationPhoto = disablePhoto
    }

    private fun registerCastListeners() {
        if (isTv) {
            return
        }
        @Suppress("UNCHECKED_CAST")
        CastUtil.registerCastListener(this, castListener as SessionManagerListener<Session>)
        CastUtil.registerCastStateListener(this, castStateListener)
    }

    private fun setCastDisabled(disableCast: Boolean) {
        // TODO: Storing this as a field is not a proper way to handle LiveData.
        // It needs to be removed once there is a good way to toggle the visibility of the cast
        // button in the menu
        castDisabledState = disableCast

        if (!haveGooglePlayServices || isTv) {
            return
        }

        if (disableCast) {
            // If cast was previously enabled and we are disabling it, try to stop casting
            CastUtil.stopCasting(this)
        } else {
            // If cast was disabled and is becoming enabled, register listeners
            registerCastListeners()
        }

        // Update menu
        invalidateOptionsMenu()
    }

    /**
     * Displays a friendly toast and returns to the startup activity with the given message.
     */
    private fun handleErrorFinish() {
        SantaLog.d(TAG, "Returning to village.")
        Toast.makeText(applicationContext, lostContactString,
                Toast.LENGTH_LONG).show()
        finish()
    }

    private fun showCastOverlay() {
        if (!castOverlayShown && (mediaRouteMenuItem?.isVisible == true)) {
            val overlay = IntroductoryOverlay.Builder(this, mediaRouteMenuItem)
                    .setTitleText(R.string.cast_overlay_text)
                    .setSingleTime()
                    .setOnOverlayDismissedListener {
                        MeasurementManager.recordCustomEvent(firebaseAnalytics,
                                getString(com.google.android.apps.santatracker.common
                                        .R.string.analytics_event_category_cast),
                                getString(com.google.android.apps.santatracker.common
                                        .R.string.analytics_cast_overlayshown))
                    }
                    .build()
            overlay.show()
            castOverlayShown = true
        }
    }

    inner class OverlayCastStateListener(context: Context, category: Int)
        : LoggingCastStateListener(context, category, firebaseAnalytics) {
        override fun onCastStateChanged(newState: Int) {
            super.onCastStateChanged(newState)
            if (newState != CastState.NO_DEVICES_AVAILABLE) {
                showCastOverlay()
            }
        }
    }
}
