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

package com.google.android.apps.santatracker.config

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.google.android.apps.santatracker.common.BuildConfig
import com.google.android.apps.santatracker.common.R
import com.google.android.apps.santatracker.data.WebSceneState
import com.google.android.apps.santatracker.data.webSceneState
import com.google.android.apps.santatracker.util.SantaLog
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigFetchThrottledException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
 * Wrapper class for accessing Firebase Remote Config. Making it open because we want it to be
 * mocked in tests.
 */
open class Config {
    open val firebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance().apply {
        setConfigSettings(CONFIG_SETTINGS)
        setDefaults(R.xml.remote_config_defaults)
    }

    private val configCacheExpiry: Long
    private var throttleEndTimeMillis: Long = 0

    open val takeoffTimeMs: Long
        get() = get(SANTA_TAKEOFF) * 1000

    open val arrivalTimeMs: Long
        get() = get(SANTA_ARRIVAL) * 1000

    init {
        // Set cache expiration to 0s when debugging to allow easy testing, otherwise
        // use the default value
        configCacheExpiry = if (CONFIG_SETTINGS.isDeveloperModeEnabled) {
            0
        } else {
            DEFAULT_CACHE_EXPIRY_S
        }
    }

    @SuppressLint("VisibleForTests")
    @VisibleForTesting
    open operator fun <T> get(param: ConfigParam<T>): T {
        return param.getValue(this)
    }

    /**
     * Sync the config values with Firebase Remote Config asynchronously.
     *
     * @param paramChangedCallback the callback when any of [ConfigParam]s are changed.
     */
    open fun syncConfigAsync(paramChangedCallback: ParamChangedCallback?) {
        val currentTime = System.currentTimeMillis()
        if (currentTime > throttleEndTimeMillis) {
            val getConfigTask = firebaseRemoteConfig.fetch(configCacheExpiry)

            getConfigTask.addOnCompleteListener { task ->
                onTaskCompleted(task, paramChangedCallback)
            }
        } else {
            val msRemaining = throttleEndTimeMillis - currentTime
            SantaLog.d(TAG, "Not trying config, throttled for " + msRemaining + "ms")
        }
    }

    fun syncConfig() {
        val currentTime = System.currentTimeMillis()
        if (currentTime > throttleEndTimeMillis) {
            val task = firebaseRemoteConfig.fetch(configCacheExpiry)
            Tasks.await(task, 60, TimeUnit.SECONDS)
        } else {
            val msRemaining = throttleEndTimeMillis - currentTime
            SantaLog.d(TAG, "Not trying config, throttled for " + msRemaining + "ms")
        }
    }

    private fun onTaskCompleted(
        getConfigTask: Task<Void>,
        paramChangedCallback: ParamChangedCallback?
    ) {
        if (getConfigTask.isSuccessful) {
            SantaLog.d(TAG, "fetchConfig:SUCCESS")

            val oldParams = Bundle()
            for (param in ALL_LONG_PARAMS) {
                oldParams.putLong(param.key, param.getValue(this))
            }
            for (param in ALL_STRING_PARAMS) {
                oldParams.putString(param.key, param.getValue(this))
            }
            for (param in ALL_BOOLEAN_PARAMS) {
                oldParams.putBoolean(param.key, param.getValue(this))
            }
            val oldWebSceneState = webSceneState()

            // Activate config and notify clients of any changes
            firebaseRemoteConfig.activateFetched()
            val changedKeys = getChangedKeys(oldParams, oldWebSceneState)
            if (changedKeys.isNotEmpty() && paramChangedCallback != null) {
                paramChangedCallback.onChanged(changedKeys)
            }
        } else {
            val e = getConfigTask.exception
            if (e is FirebaseRemoteConfigFetchThrottledException) {
                // Store throttle end time
                val ex = e as FirebaseRemoteConfigFetchThrottledException?
                throttleEndTimeMillis = ex!!.throttleEndTimeMillis
                SantaLog.w(TAG, "fetchConfig:THROTTLED until $throttleEndTimeMillis")
            } else {
                SantaLog.w(TAG, "fetchConfig:UNEXPECTED_ERROR", e!!)
            }
        }
    }

    /**
     * @return a list of String representing key names of the parameters changed from the previous
     * values. Returns an empty list if nothing changed.
     */
    private fun getChangedKeys(oldParams: Bundle, oldWebSceneState: WebSceneState): List<String> {
        val changedParamsKeys = ArrayList<String>()
        for (param in ALL_LONG_PARAMS) {
            if (oldParams.getLong(param.key) != param.getValue(this)) {
                changedParamsKeys.add(param.key)
            }
        }
        for (param in ALL_STRING_PARAMS) {
            val oldValue = oldParams.getString(param.key)
            if (oldValue != null && oldValue != param.getValue(this)) {
                changedParamsKeys.add(param.key)
            }
        }
        for (param in ALL_BOOLEAN_PARAMS) {
            if (oldParams.getBoolean(param.key) != param.getValue(this)) {
                changedParamsKeys.add(param.key)
            }
        }
        val newWebSceneState = webSceneState()
        if (oldWebSceneState != newWebSceneState) {
            changedParamsKeys.add(WebConfig.WEBCONFIG)
        }
        return changedParamsKeys
    }

    /** Interface for a callback when any of config values are changed from the previous values.  */
    interface ParamChangedCallback {
        /**
         * Called when any of parameters are changed from the previous values.
         *
         * @param changedKeys has the list of key names whose values are changed from the previous
         * values.
         */
        fun onChanged(changedKeys: List<String>)
    }

    companion object {
        private const val TAG = "Config"

        // Santa kill switch
        val DISABLE_SANTA = BooleanConfigParam("DisableSanta")

        // Game kill switches
        val DISABLE_CASTBUTTON = BooleanConfigParam("DisableCastButton")
        val DISABLE_PHOTO = BooleanConfigParam("DisableDestinationPhoto")
        val DISABLE_GUMBALLGAME = BooleanConfigParam("DisableGumballGame")
        val DISABLE_JETPACKGAME = BooleanConfigParam("DisableJetpackGame")
        val DISABLE_MEMORYGAME = BooleanConfigParam("DisableMemoryGame")
        val DISABLE_ROCKETGAME = BooleanConfigParam("DisableRocketGame")
        val DISABLE_DANCERGAME = BooleanConfigParam("DisableDancerGame")
        val DISABLE_SWIMMINGGAME = BooleanConfigParam("DisableSwimmingGame")
        val DISABLE_BMXGAME = BooleanConfigParam("DisableBmxGame")
        val DISABLE_RUNNINGGAME = BooleanConfigParam("DisableRunningGame")
        val DISABLE_TENNISGAME = BooleanConfigParam("DisableTennisGame")
        val DISABLE_WATERPOLOGAME = BooleanConfigParam("DisableWaterpoloGame")
        val DISABLE_CITY_QUIZ = BooleanConfigParam("DisableCityQuiz")
        val DISABLE_PRESENTQUEST = BooleanConfigParam("DisablePresentQuest")
        val DISABLE_SANTA_SNAP = BooleanConfigParam("DisableSantaSnap")
        val DISABLE_PRESENT_THROW = BooleanConfigParam("DisablePresentThrow")

        // YouTube video IDs
        val VIDEO_1 = StringConfigParam("Video1")
        val VIDEO_15 = StringConfigParam("Video15")
        val VIDEO_23 = StringConfigParam("Video23")

        // Unlock times
        @JvmField val SANTA_TAKEOFF = LongConfigParam("SantaTakeoff")
        @JvmField val SANTA_ARRIVAL = LongConfigParam("SantaArrival")
        @JvmField val UNLOCK_GUMBALL = LongConfigParam("UnlockGumball")
        @JvmField val UNLOCK_MEMORY = LongConfigParam("UnlockMemory")
        @JvmField val UNLOCK_ROCKET = LongConfigParam("UnlockRocket")
        @JvmField val UNLOCK_DANCER = LongConfigParam("UnlockDancer")
        @JvmField val UNLOCK_CITYQUIZ = LongConfigParam("UnlockCityQuiz")
        @JvmField val UNLOCK_VIDEO_1 = LongConfigParam("UnlockVideo1")
        @JvmField val UNLOCK_VIDEO_15 = LongConfigParam("UnlockVideo15")
        @JvmField val UNLOCK_VIDEO_23 = LongConfigParam("UnlockVideo23")
        @JvmField val UNLOCK_JETPACK = LongConfigParam("UnlockJetpack")
        @JvmField val UNLOCK_PRESENT_THROW = LongConfigParam("UnlockPresentThrow")

        // Game isFeatured switches
        val FEATURE_GUMBALLGAME = BooleanConfigParam("FeatureGumballGame")
        val FEATURE_JETPACKGAME = BooleanConfigParam("FeatureJetpackGame")
        val FEATURE_MEMORYGAME = BooleanConfigParam("FeatureMemoryGame")
        val FEATURE_ROCKETGAME = BooleanConfigParam("FeatureRocketGame")
        val FEATURE_DANCERGAME = BooleanConfigParam("FeatureDancerGame")
        val FEATURE_SWIMMINGGAME = BooleanConfigParam("FeatureSwimmingGame")
        val FEATURE_BMXGAME = BooleanConfigParam("FeatureBmxGame")
        val FEATURE_RUNNINGGAME = BooleanConfigParam("FeatureRunningGame")
        val FEATURE_TENNISGAME = BooleanConfigParam("FeatureTennisGame")
        val FEATURE_WATERPOLOGAME = BooleanConfigParam("FeatureWaterpoloGame")
        val FEATURE_CITY_QUIZ = BooleanConfigParam("FeatureCityQuiz")
        val FEATURE_PRESENTQUEST = BooleanConfigParam("FeaturePresentQuest")
        val FEATURE_SANTA_SNAP = BooleanConfigParam("FeatureSantaSnap")
        val FEATURE_PRESENT_THROW = BooleanConfigParam("FeaturePresentThrow")

        val STICKERS_CONFIG_URL = StringConfigParam("StickersConfigUrl")

        // Time offset
        val TIME_OFFSET = LongConfigParam("TimeOffset")

        val WEB_SCENES = WebConfig()

        // Array of params related to tracker
        val ALL_PARAMS_TRACKER = arrayOf(
                TIME_OFFSET, DISABLE_SANTA, DISABLE_CASTBUTTON, DISABLE_PHOTO
        )

        // Array of all String params
        private val ALL_STRING_PARAMS = arrayOf(VIDEO_1, VIDEO_15, VIDEO_23)

        // Array of all Boolean params
        private val ALL_BOOLEAN_PARAMS = arrayOf(
                DISABLE_SANTA, DISABLE_CASTBUTTON, DISABLE_PHOTO, DISABLE_GUMBALLGAME,
                        FEATURE_GUMBALLGAME, DISABLE_JETPACKGAME, FEATURE_JETPACKGAME,
                        DISABLE_MEMORYGAME, FEATURE_MEMORYGAME, DISABLE_ROCKETGAME,
                        FEATURE_ROCKETGAME, DISABLE_DANCERGAME, FEATURE_DANCERGAME,
                        DISABLE_SWIMMINGGAME, FEATURE_SWIMMINGGAME, DISABLE_BMXGAME,
                        FEATURE_BMXGAME, DISABLE_RUNNINGGAME, FEATURE_RUNNINGGAME,
                        DISABLE_TENNISGAME, FEATURE_TENNISGAME, DISABLE_WATERPOLOGAME,
                        FEATURE_WATERPOLOGAME, DISABLE_CITY_QUIZ, FEATURE_CITY_QUIZ,
                        DISABLE_PRESENTQUEST, FEATURE_PRESENTQUEST, DISABLE_SANTA_SNAP,
                        FEATURE_SANTA_SNAP, DISABLE_PRESENT_THROW, FEATURE_PRESENT_THROW
        )

        // Array of all Long params
        private val ALL_LONG_PARAMS = arrayOf(
                        SANTA_TAKEOFF, SANTA_ARRIVAL, UNLOCK_GUMBALL, UNLOCK_MEMORY, UNLOCK_JETPACK,
                        UNLOCK_ROCKET, UNLOCK_DANCER, UNLOCK_CITYQUIZ, UNLOCK_VIDEO_1,
                        UNLOCK_VIDEO_15, UNLOCK_VIDEO_23, TIME_OFFSET, UNLOCK_PRESENT_THROW
                )

        private const val DEFAULT_CACHE_EXPIRY_S = (60 * 12).toLong() // 5 requests / h

        private val CONFIG_SETTINGS = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build()
    }
}
