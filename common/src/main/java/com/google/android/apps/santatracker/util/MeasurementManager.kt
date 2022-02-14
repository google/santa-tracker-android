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

package com.google.android.apps.santatracker.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import com.google.android.apps.santatracker.common.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.Locale

/** Handles communication with Firebase Analytics.  */
object MeasurementManager {

    private val TAG = "MeasurementManager"

    private val GAME_TITLE = "game_title"
    private val TYPE_SCREEN = "type_screen"

    /** User properties  */
    private val BUILD_DEBUG = "BUILD_DEBUG"

    private val BUILD_VERSION_NAME = "BUILD_VERSION_NAME"
    private val DEVICE_BOARD = "DEVICE_BOARD"
    private val DEVICE_BRAND = "DEVICE_BRAND"
    private val DEVICE_LOCALE = "DEVICE_LOCALE"
    private val API_LEVEL = "API_LEVEL"

    @JvmStatic
    fun recordDeviceProperties(context: Context) {
        val analytics = FirebaseAnalytics.getInstance(context.applicationContext)

        // Set some user properties based on the device, this can be used for Analytics or
        // for Remote Config
        analytics.setUserProperty(BUILD_DEBUG, BuildConfig.DEBUG.toString())
        analytics.setUserProperty(DEVICE_BOARD, Build.BOARD)
        analytics.setUserProperty(DEVICE_BRAND, Build.BRAND)
        analytics.setUserProperty(DEVICE_LOCALE, Locale.getDefault().language)
        analytics.setUserProperty(API_LEVEL, Build.VERSION.SDK_INT.toString())

        try {
            // Set version name, if we can get it
            val pm = context.packageManager
            val info = pm.getPackageInfo(context.packageName, 0)
            analytics.setUserProperty(BUILD_VERSION_NAME, info.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            SantaLog.w(TAG, "Could not get package info", e)
        }
    }

    @JvmOverloads
    @JvmStatic
    fun recordCustomEvent(
        measurement: FirebaseAnalytics,
        name: String,
        action: String,
        label: String? = null,
        value: Long = Integer.MIN_VALUE.toLong()
    ) {
        SantaLog.d(TAG, "recordCustomEvent:$name:$action:$label")

        val params = Bundle()
        params.putString("action", action)
        if (label != null) {
            params.putString("label", label)
        }
        if (value != Integer.MIN_VALUE.toLong()) {
            params.putString("value", java.lang.Long.toString(value))
        }
        measurement.logEvent(name, params)
    }

    @JvmStatic
    fun recordScreenView(measurement: FirebaseAnalytics, id: String) {
        SantaLog.d(TAG, "recordScreenView:$id")

        val params = Bundle()
        params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, TYPE_SCREEN)
        params.putString(FirebaseAnalytics.Param.ITEM_ID, id)
        measurement.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params)
    }

    @JvmStatic
    fun recordInvitationReceived(
        measurement: FirebaseAnalytics,
        deepLink: String
    ) {
        SantaLog.d(TAG, "recordInvitationReceived:$deepLink")

        val params = Bundle()
        params.putString("deepLink", deepLink)
        measurement.logEvent(FirebaseAnalytics.Event.APP_OPEN, params)
    }

    @JvmStatic
    fun recordInvitationSent(
        measurement: FirebaseAnalytics,
        type: String,
        deepLink: String
    ) {
        SantaLog.d(TAG, "recordInvitationSent:$type:$deepLink")

        val params = Bundle()
        params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, type)
        params.putSerializable(FirebaseAnalytics.Param.ITEM_ID, deepLink)
        measurement.logEvent(FirebaseAnalytics.Event.SHARE, params)
    }

    @JvmStatic
    fun recordLogin(measurement: FirebaseAnalytics) {
        SantaLog.d(TAG, "recordLogin")
        measurement.logEvent(FirebaseAnalytics.Event.LOGIN, null)
    }

    @JvmStatic
    fun recordAchievement(
        measurement: FirebaseAnalytics,
        achId: String,
        gameTitle: String?
    ) {
        SantaLog.d(TAG, "recordAchievement:$achId:$gameTitle")

        val params = Bundle()
        params.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, achId)
        if (gameTitle != null) {
            params.putString(GAME_TITLE, gameTitle)
        }
        measurement.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, params)
    }

    @JvmStatic
    fun recordGameScore(
        measurement: FirebaseAnalytics,
        score: Long,
        level: Long?,
        gameTitle: String?
    ) {
        SantaLog.d(TAG, "recordGameEnd:$gameTitle:$score:$level")

        val params = Bundle()
        params.putLong(FirebaseAnalytics.Param.SCORE, score)
        if (level != null) {
            params.putLong(FirebaseAnalytics.Param.LEVEL, level)
        }
        if (gameTitle != null) {
            params.putString(GAME_TITLE, gameTitle)
        }
        measurement.logEvent(FirebaseAnalytics.Event.POST_SCORE, params)
    }

    @JvmStatic
    fun recordVillageSantaClick(measurement: FirebaseAnalytics) {
        SantaLog.d(TAG, "recordVillageSantaClick")
        measurement.logEvent("village_santa_clicked", Bundle())
    }

    @JvmStatic
    fun recordSwimmingEnd(
        measurement: FirebaseAnalytics,
        numStars: Int,
        score: Int,
        end_reason: String?
    ) {
        SantaLog.d(TAG, "recordSwimmingEnd:$numStars:$score:$end_reason")

        val params = Bundle()
        params.putInt("num_stars", numStars)
        params.putInt("score", score)
        if (end_reason != null) {
            params.putString("end_reason", end_reason)
        }

        // Log custom swimming event
        measurement.logEvent("swimming_game_end", params)

        // Log generic game score event
        recordGameScore(measurement, score.toLong(), null, "swimming")
    }

    @JvmStatic
    fun recordRunningEnd(measurement: FirebaseAnalytics, numStars: Int, score: Int) {
        SantaLog.d(TAG, "recordRunningEnd:$numStars:$score")

        val params = Bundle()
        params.putInt("num_stars", numStars)
        params.putInt("score", score)

        // Log custom swimming event
        measurement.logEvent("running_game_end", params)

        // Log generic game score event
        recordGameScore(measurement, score.toLong(), null, "running")
    }

    @JvmStatic
    fun recordPresentDropped(analytics: FirebaseAnalytics, isLarge: Boolean) {
        SantaLog.d(TAG, "recordPresentDropped:$isLarge")

        val params = Bundle()
        if (isLarge) {
            params.putString("size", "large")
        } else {
            params.putString("size", "small")
        }

        analytics.logEvent("pq_present_dropped", params)
    }

    @JvmStatic
    fun recordPresentsCollected(analytics: FirebaseAnalytics, numPresents: Int) {
        SantaLog.d(TAG, "recordPresentsCollected:$numPresents")

        val params = Bundle()
        params.putInt("num_presents", numPresents)

        analytics.logEvent("pq_presents_collected", params)
    }

    @JvmStatic
    fun recordPresentsReturned(analytics: FirebaseAnalytics, numPresents: Int) {
        SantaLog.d(TAG, "recordPresentsReturned:$numPresents")

        val params = Bundle()
        params.putInt("num_presents", numPresents)

        analytics.logEvent("pq_presents_returned", params)
    }

    @JvmStatic
    fun recordPresentQuestLevel(analytics: FirebaseAnalytics, level: Int) {
        SantaLog.d(TAG, "recordPresentQuestLevel:$level")

        val params = Bundle()
        params.putInt("level", level)

        // Log custom event
        analytics.logEvent("pq_level_unlocked", params)

        // Log standard LEVEL_UP event
        val params2 = Bundle()
        params2.putLong(FirebaseAnalytics.Param.LEVEL, level.toLong())
        analytics.logEvent(FirebaseAnalytics.Event.LEVEL_UP, params2)
    }

    @JvmStatic
    fun recordWorkshopMoved(analytics: FirebaseAnalytics) {
        SantaLog.d(TAG, "recordWorkshopMoved")

        analytics.logEvent("pq_workshop_moved", Bundle())
    }

    @JvmStatic
    fun recordHundredMetersWalked(analytics: FirebaseAnalytics, distance: Int) {
        SantaLog.d(TAG, "recordHundredMetersWalked")

        val bundle = Bundle()
        bundle.putInt("distance", distance)
        analytics.logEvent("pq_hundred_meters_walked", bundle)
    }

    @JvmStatic
    fun recordCorrectCitySelected(
        analytics: FirebaseAnalytics,
        cityId: String,
        numIncorrectAttempts: Int
    ) {
        SantaLog.d(TAG, "recordCorrectCitySelected:$cityId:$numIncorrectAttempts")

        val params = Bundle()
        params.putString("city_id", cityId)
        params.putInt("incorrect_attempts", numIncorrectAttempts)

        analytics.logEvent("cq_select_correct", params)
    }

    @JvmStatic
    fun recordIncorrectCitySelected(
        analytics: FirebaseAnalytics,
        cityId: String
    ) {
        SantaLog.d(TAG, "recordIncorrectCitySelected:$cityId")

        val params = Bundle()
        params.putString("city_id", cityId)

        analytics.logEvent("cq_select_incorrect", params)
    }
}
