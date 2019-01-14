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

package com.google.android.apps.santatracker.cityquiz

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.postDelayed
import androidx.core.view.doOnLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.apps.playgames.common.PlayGamesActivity
import com.google.android.apps.santatracker.games.EndOfGameView
import com.google.android.apps.santatracker.util.MapHelper
import com.google.android.apps.santatracker.util.MeasurementManager
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.apps.santatracker.util.VectorUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.storage.FirebaseStorage
import java.lang.ref.WeakReference

/** Main container for the City Quiz game.  */
class CityQuizActivity : PlayGamesActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var cityQuizGame: CityQuizGame

    private lateinit var cityImageView: ImageView
    private lateinit var cityImageProgressBar: ProgressBar
    private lateinit var cloudOffImageView: ImageView
    private lateinit var cityImageAuthorTextView: TextView
    private lateinit var mapScrim: View
    private lateinit var roundCountTextView: TextView
    private lateinit var pointsTextView: TextView

    private lateinit var supportMapFragment: SupportMapFragment
    private lateinit var googleMap: GoogleMap

    private var mapLaidOut: Boolean = false
    private var mapReady: Boolean = false
    private var initialRoundLoaded: Boolean = false

    private val cityImageGlideListener = CityImageGlideListener(this)
    private val handler = Handler()

    private lateinit var analytics: FirebaseAnalytics

    override fun getLayoutId() = R.layout.activity_city_quiz

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val roundCount = FirebaseRemoteConfig.getInstance().getLong(CITY_QUIZ_ROUND_COUNT_CONFIG_KEY)
        cityQuizGame = CityQuizGame(this, roundCount.toInt())

        // [ANALYTICS]
        analytics = FirebaseAnalytics.getInstance(this)
        MeasurementManager.recordScreenView(analytics,
                getString(com.google.android.apps.santatracker.common.R.string.analytics_screen_city_quiz))

        cityImageView = findViewById(R.id.cityQuizImageView)
        cityImageProgressBar = findViewById(R.id.cityImageProgressBar)
        cloudOffImageView = findViewById(R.id.cloudOffImageView)
        cityImageAuthorTextView = findViewById(R.id.cityImageAuthorTextView)
        mapScrim = findViewById(R.id.map_scrim)
        roundCountTextView = findViewById(R.id.roundCountTextView)
        pointsTextView = findViewById(R.id.pointsTextView)

        // Clicking the "offline" image will attempt a reload
        cloudOffImageView.setOnClickListener {
            cityQuizGame.currentRound?.city?.let { city ->
                loadCityImage(city.imageName, city.imageAuthor)
            }
        }

        // Map scrim prevents clicking on map
        mapScrim.setOnClickListener {
            // No-op, eat the click
        }

        supportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        supportMapFragment.view?.doOnLayout {
            mapLaidOut = true
            loadInitialRound()
        }
        supportMapFragment.getMapAsync(this)
    }

    override fun getGameId(): String {
        return getString(com.google.android.apps.playgames.R.string.city_quiz_game_id)
    }

    override fun getGameTitle(): String {
        return getString(com.google.android.apps.santatracker.common.R.string.cityquiz)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mapReady = true
        this.googleMap = googleMap

        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,
                com.google.android.apps.santatracker.common.R.raw.map_style))
        googleMap.setOnMarkerClickListener(this)

        loadInitialRound()
    }

    /**
     * Load the initial round only if the map is ready and laid out and has not been already loaded.
     */
    private fun loadInitialRound() {
        if (mapLaidOut && mapReady && !initialRoundLoaded) {
            loadRound()
            initialRoundLoaded = true
        }
    }

    /** Load the current round of the game for user interaction.  */
    private fun loadRound() {
        if (isFinishing || supportMapFragment.view == null) {
            // The user exit the game between rounds.
            return
        }

        updateScore()

        // Get next city in game.
        val city = cityQuizGame.currentRound?.city ?: return

        // Load city image
        loadCityImage(city.imageName, city.imageAuthor)

        // Set up city markers
        googleMap.clear()

        if (BuildConfig.DEBUG) {
            SantaLog.d(TAG, "Moving to ${city.name}")
        }
        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                        city.bounds, MapHelper.getMapPadding(supportMapFragment)))

        // Add markers and set appropriate tags.
        val locationMarker = googleMap.addMarker(
                MarkerOptions()
                        .position(city.correctLocation)
                        .icon(VectorUtil.vectorToBitmap(this, R.drawable.ic_pin_blue)))
        locationMarker.tag = CORRECT_MARKER

        val firsIncorrectLocationMarker = googleMap.addMarker(
                MarkerOptions()
                        .position(city.incorrectLocationOne)
                        .icon(VectorUtil.vectorToBitmap(this, R.drawable.ic_pin_blue)))
        firsIncorrectLocationMarker.tag = FIRST_FAKE_MARKER

        val secondIncorrectLocationMarker = googleMap.addMarker(
                MarkerOptions()
                        .position(city.incorrectLocationTwo)
                        .icon(VectorUtil.vectorToBitmap(this, R.drawable.ic_pin_blue)))
        secondIncorrectLocationMarker.tag = SECOND_FAKE_MARKER
    }

    /**
     * Load the image matching the given image name into the city ImageView.
     *
     * @param imageName Name used to retrieve the image from Firebase Storage.
     * @param imageAuthor Name used to give attribution for the image.
     */
    private fun loadCityImage(imageName: String, imageAuthor: String) {
        if (isFinishing) {
            return
        }
        // Clear current image
        Glide.with(this)
                .load(com.google.android.apps.playgames.R.color.cityQuizPrimaryGreenDark)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(cityImageView)

        cityImageProgressBar.visibility = View.VISIBLE
        showImageAuthor(false)
        showOnlineUI()

        // Load new image
        val task = FirebaseStorage.getInstance().reference.child(imageName).downloadUrl
        task.addOnCompleteListener(this, LoadCityImageTaskCompleteListener(this, imageAuthor))

        // After timeout check if image URL has been retrieved, if not update UI.
        handler.postDelayed(IMAGE_LOAD_TIMEOUT_MILLIS) {
            if (!task.isComplete) {
                // Firebase was did not complete image URL retrieval.
                cityImageProgressBar.visibility = View.GONE
                showOfflineUI()
            }
        }
    }

    private class LoadCityImageTaskCompleteListener(
        activity: CityQuizActivity,
        private val imageAuthor: String
    ) : OnCompleteListener<Uri> {
        private val activityRef = WeakReference(activity)

        override fun onComplete(task: Task<Uri>) {
            val activity = activityRef.get()
            if (activity == null || activity.isFinishing) {
                return
            }
            activity.cityImageProgressBar.visibility = View.GONE

            if (task.isSuccessful) {
                val uri = task.result
                if (BuildConfig.DEBUG) {
                    SantaLog.d(TAG, "Image uri: $uri")
                }

                Glide.with(activity)
                        .load(uri)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .listener(activity.cityImageGlideListener)
                        .into(activity.cityImageView)

                activity.cityImageAuthorTextView.text = activity.resources.getString(R.string.photo_by, imageAuthor)
                activity.showImageAuthor(true)
            } else {
                if (BuildConfig.DEBUG) {
                    SantaLog.e(TAG, "Unable to get image URI from Firebase Storage", task.exception)
                }
                activity.showOfflineUI()
            }
        }
    }

    private fun showOfflineUI() {
        cloudOffImageView.visibility = View.VISIBLE
        mapScrim.visibility = View.VISIBLE
    }

    private fun showOnlineUI() {
        cloudOffImageView.visibility = View.GONE
        mapScrim.visibility = View.GONE
    }

    private fun updateScore() {
        // Update game information, round count and score.
        roundCountTextView.text = getString(
                R.string.round_count_fmt,
                cityQuizGame.currentRoundCount + 1,
                cityQuizGame.totalRoundCount)
        pointsTextView.text = getString(R.string.game_score_fmt, cityQuizGame.calculateScore())
    }

    private fun showImageAuthor(visible: Boolean) {
        if (visible) {
            cityImageAuthorTextView.visibility = TextView.VISIBLE
        } else {
            cityImageAuthorTextView.visibility = TextView.INVISIBLE
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        // Check if the round is already solved, if so ignore the marker click.
        if (cityQuizGame.isFinished || cityQuizGame.currentRound?.isSolved == true) {
            return true
        }

        val currentRound = cityQuizGame.currentRound ?: return false

        // Identify which marker was tapped and update the round status.
        val tag = marker.tag as Int
        currentRound.updateLocationStatus(tag, true)

        // Check if user tapped on the correct marker and if move to the next round.
        if (tag == 0) {
            marker.setIcon(VectorUtil.vectorToBitmap(this, R.drawable.ic_pin_green))
            updateScore()

            // [ANALYTICS]
            val numIncorrectAttempts = 5 - currentRound.calculateRoundScore()
            MeasurementManager.recordCorrectCitySelected(
                    analytics,
                    currentRound.city.imageName,
                    numIncorrectAttempts)

            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLng(currentRound.city.correctLocation),
                    object : GoogleMap.CancelableCallback {
                        override fun onFinish() {
                            showCityInfo(marker)
                        }

                        override fun onCancel() {
                            showCityInfo(marker)
                        }
                    })

            // Wait a while before moving to next round or end of game.
            handler.postDelayed(NEXT_ROUND_DELAY) {
                cityQuizGame.moveToNextRound()
                // Check if the last round has completed.
                if (!cityQuizGame.isFinished) {
                    loadRound()
                } else {
                    // or exit. For now I will clear the markers.
                    goToGameSummary()
                    pointsTextView.text = getString(
                            R.string.game_score_fmt,
                            cityQuizGame.calculateScore())
                }
            }
        } else {
            // [ANALYTICS]
            cityQuizGame.currentRound?.let { round ->
                MeasurementManager.recordIncorrectCitySelected(analytics, round.city.imageName)
            }

            marker.setIcon(VectorUtil.vectorToBitmap(this, R.drawable.ic_pin_red))
        }

        return true
    }

    private fun showCityInfo(marker: Marker) {
        marker.title = cityQuizGame.currentRound?.city?.name
        marker.showInfoWindow()
    }

    private fun goToGameSummary() {
        // Show the end-game view
        val gameView = findViewById<View>(R.id.view_end_game) as EndOfGameView
        gameView.initialize(cityQuizGame.calculateScore(),
                {
                    // Restart this activity
                    recreate()
                },
                {
                    // Back to the village
                    finish()
                })

        // Show end game view over everything
        gameView.visibility = View.VISIBLE
        gameView.z = 1000f
    }

    // Listener to handle Glide action completion.
    private class CityImageGlideListener(activity: CityQuizActivity) : RequestListener<Drawable> {
        private val weakActivity = WeakReference(activity)

        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            // Glide failed to load image.
            if (weakActivity.get() != null) {
                SantaLog.e(TAG, "Glide unable to load city image.")
                weakActivity.get()?.showOfflineUI()
            }
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            if (weakActivity.get() != null) {
                // Glide loaded image, hide "cloud off" ImageView and show author TextView.
                weakActivity.get()?.showOnlineUI()
                weakActivity.get()?.showImageAuthor(true)
            }
            return false
        }
    }

    companion object {
        private const val TAG = "CityQuizActivity"
        private const val NEXT_ROUND_DELAY = 3500L
        private const val CORRECT_MARKER = 0
        private const val FIRST_FAKE_MARKER = 1
        private const val SECOND_FAKE_MARKER = 2
        private const val IMAGE_LOAD_TIMEOUT_MILLIS = 5000L // 5 seconds
        private const val CITY_QUIZ_ROUND_COUNT_CONFIG_KEY = "CityQuizRoundCount"
    }
}
