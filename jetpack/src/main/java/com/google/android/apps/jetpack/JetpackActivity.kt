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

package com.google.android.apps.jetpack

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import com.google.android.apps.playgames.simpleengine.Scene
import com.google.android.apps.playgames.simpleengine.SceneManager
import com.google.android.apps.santatracker.util.MeasurementManager
import com.google.firebase.analytics.FirebaseAnalytics

class JetpackActivity : SceneActivity(), SensorEventListener {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var sensorManager: SensorManager

    override val gameScene: Scene
        get() = JetpackScene()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // App Measurement
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        MeasurementManager.recordScreenView(
                firebaseAnalytics,
                getString(
                        com.google
                                .android
                                .apps
                                .santatracker
                                .common
                                .R
                                .string
                                .analytics_screen_jetpack))

        var presentIsLarge = false
        if (intent != null && intent.extras != null) {
            presentIsLarge = intent
                    .extras!!
                    .getBoolean(
                            getString(
                                    com.google
                                            .android
                                            .apps
                                            .santatracker
                                            .common
                                            .R
                                            .string
                                            .extra_large_present_key),
                            false)
        }

        SceneManager.getInstance().largePresentMode = presentIsLarge

        sensorManager = getSystemService(android.app.Activity.SENSOR_SERVICE) as SensorManager
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_jetpack
    }

    override fun onResume() {
        super.onResume()

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onBackPressed() {
        var handled = false
        val scene = SceneManager.getInstance().currentScene
        if (scene is JetpackScene) {
            handled = scene.onBackKeyPressed()
        }
        if (!handled) {
            super.onBackPressed()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        SceneManager.getInstance().onSensorChanged(event)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    override fun getGameId(): String {
        return getString(com.google.android.apps.playgames.R.string.jetpack_game_id)
    }

    override fun getGameTitle(): String {
        return getString(com.google.android.apps.santatracker.common.R.string.elf_jetpack)
    }

    companion object {
        internal const val JETPACK_SCORE = "jetpack_score"
    }
}
