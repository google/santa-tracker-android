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

import com.google.android.gms.maps.SupportMapFragment

object MapHelper {

    private val TAG = MapHelper::class.java.simpleName

    /**
     * Calculate a valid padding for a map's markers. Using hard coded padding can be problematic
     * when device is small, so using a padding based on the size of the map view would improve
     * results.
     *
     * @param supportMapFragment The map used to determine padding.
     * @return A valid padding.
     */
    @JvmStatic
    fun getMapPadding(supportMapFragment: SupportMapFragment): Int {
        val height = supportMapFragment.view!!.height
        val width = supportMapFragment.view!!.width
        val factor = 0.3
        val padding = if (height < width) height * factor else width * factor
        SantaLog.d(TAG, "padding used: $padding")
        return padding.toInt()
    }
}
