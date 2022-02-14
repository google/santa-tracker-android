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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.google.android.apps.santatracker.tracker.R
import com.google.android.apps.santatracker.tracker.vo.Destination
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

internal class DestinationInfoWindowAdapter(inflater: LayoutInflater)
    : GoogleMap.InfoWindowAdapter {

    private val title: TextView
    @SuppressLint("InflateParams")
    private val window: View = inflater.inflate(R.layout.infowindow, null)

    private var destination: Destination? = null

    init {
        title = window.findViewById(R.id.info_title)
    }

    fun setData(destination: Destination) {
        this.destination = destination
    }

    override fun getInfoWindow(marker: Marker): View {
        title.text = destination?.printName
        title.contentDescription = title.text
        return window
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }
}
