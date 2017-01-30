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
package com.google.android.apps.santatracker.presentquest.model;

import com.google.android.gms.maps.model.LatLng;

import com.orm.SugarRecord;

abstract class Location extends SugarRecord {

    // These are strings to ensure values don't lose consistency due to sqlite floats,
    // as we need to compare them to avoid duplicate places/presents.
    public String lat;
    public String lng;
    public long updated = 0;

    public Location() {}

    public LatLng getLatLng() {
        return new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
    }

    public void setLatLng(LatLng latLng) {
        lat = String.valueOf(latLng.latitude);
        lng = String.valueOf(latLng.longitude);
    }

    public void saveWithTimestamp() {
        updated = System.currentTimeMillis();
        save();
    }
}

