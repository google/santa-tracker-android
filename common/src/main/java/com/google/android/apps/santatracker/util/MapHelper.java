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
package com.google.android.apps.santatracker.util;

import android.util.Log;

import com.google.android.gms.maps.SupportMapFragment;

public class MapHelper {

    private static final String TAG = MapHelper.class.getSimpleName();

    /**
     * Calculate a valid padding for a map's markers. Using hard coded padding can be problematic
     * when device is small, so using a padding based on the size of the map view would improve results.
     *
     * @param supportMapFragment The map used to determine padding.
     * @return A valid padding.
     */
    public static int getMapPadding(SupportMapFragment supportMapFragment) {
        int height = supportMapFragment.getView().getHeight();
        int width = supportMapFragment.getView().getWidth();
        double factor = 0.3;
        double padding = height < width ? height * factor : width * factor;
        Log.d(TAG, "padding used: " + padding);
        return (int) padding;
    }

}
