/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

import com.google.android.apps.santatracker.R;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;
import com.google.android.apps.santatracker.data.Destination;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * {@link InfoWindowAdapter} for Destinations.
 *
 * @author jfschmakeit
 */
public class DestinationInfoWindowAdapter implements InfoWindowAdapter {

    //private static final String TAG = "DestinationInfoAdapter";
    private final TextView mTitle;
    private final View mWindow;

    private Destination mDestination = null;

    private DestinationInfoWindowInterface mCallback;

    public DestinationInfoWindowAdapter(LayoutInflater inflater,
            DestinationInfoWindowInterface callback, Context c) {
        mWindow = inflater.inflate(R.layout.infowindow, null);

        mTitle = (TextView) mWindow.findViewById(R.id.info_title);
        this.mCallback = callback;

        // TypeFaces: label,title=roboto-condensed, content=roboto-light
        final Typeface robotoCondensed = Typeface.createFromAsset(c.getAssets(),
                c.getResources().getString(R.string.typeface_robotocondensed_regular));
        final Typeface robotoLight = Typeface.createFromAsset(c.getAssets(),
                c.getResources().getString(R.string.typeface_roboto_light));

        mTitle.setTypeface(robotoCondensed);
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

    public interface DestinationInfoWindowInterface {

        public Destination getDestinationInfo(int id);
    }
}
