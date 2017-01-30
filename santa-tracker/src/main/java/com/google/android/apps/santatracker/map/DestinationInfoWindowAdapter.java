/*
 * Copyright (C) 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.map;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.data.Destination;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;

/**
 * {@link InfoWindowAdapter} for Destinations.
 *
 */
class DestinationInfoWindowAdapter implements InfoWindowAdapter {

    private final TextView mTitle;
    private final View mWindow;

    private Destination mDestination = null;

    DestinationInfoWindowAdapter(LayoutInflater inflater, Context c) {
        mWindow = inflater.inflate(R.layout.infowindow, null);

        mTitle = (TextView) mWindow.findViewById(R.id.info_title);

        mTitle.setTypeface(Typeface.createFromAsset(c.getAssets(),
                c.getResources().getString(R.string.typeface_roboto_black)));
    }

    public void setData(Destination destination) {
        mDestination = destination;
    }

    public View getInfoWindow(Marker marker) {
        // name
        mTitle.setText(mDestination.getPrintName());
        mTitle.setContentDescription(mTitle.getText());

        return mWindow;
    }

    public View getInfoContents(Marker marker) {
        return null;
    }

}
