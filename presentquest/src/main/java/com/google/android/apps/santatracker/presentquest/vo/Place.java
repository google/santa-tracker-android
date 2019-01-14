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

package com.google.android.apps.santatracker.presentquest.vo;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

/** Cached result from Places API. */
@Entity(
        indices = {
            @Index(
                    value = {"lat", "lng"},
                    unique = true)
        })
public class Place {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public int used;
    // TODO make lat/long an embedded object
    public String lat;
    public String lng;
    public long updated;

    public Place(LatLng latLng) {
        super();
        setLatLng(latLng);
        updated = 0;
        used = 0;
    }

    public Place() {}

    public LatLng getLatLng() {
        return new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
    }

    public void setLatLng(LatLng latLng) {
        lat = String.valueOf(latLng.latitude);
        lng = String.valueOf(latLng.longitude);
    }
}
