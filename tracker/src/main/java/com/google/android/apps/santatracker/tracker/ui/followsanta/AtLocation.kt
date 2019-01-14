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

import com.google.android.apps.santatracker.tracker.util.Utils
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

object AtLocation {

    private val TILT_MIN = 45f
    private val TILT_MAX = 37.5f
    private val ZOOM_MIN = 11f
    private val ZOOM_MAX = 13f
    private val BEARING_RANGE = 45f

    fun getCameraUpdate(position: LatLng, camBearing: Float): CameraUpdate {

        val tilt = Utils.getRandom(TILT_MIN, TILT_MAX)
        val zoom = Utils.getRandom(ZOOM_MIN, ZOOM_MAX)

        // Limit bearing to 45 deg to either side - MapView rotation bug
        var bearing: Float = Utils.getRandom(camBearing - BEARING_RANGE, camBearing + BEARING_RANGE)
        bearing = (bearing + 360f) % 360f

        val camera = CameraPosition.builder()
                .target(position).tilt(tilt)
                .zoom(zoom).bearing(bearing).build()
        return CameraUpdateFactory.newCameraPosition(camera)
    }
}
