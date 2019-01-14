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
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;

@Entity
public class Workshop {

    // These are strings to ensure values don't lose consistency due to sqlite floats,
    // as we need to compare them to avoid duplicate places/presents.

    @Ignore public static final LatLng NULL_LATLNG = new LatLng(0, 0);

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String lat;
    public String lng;
    public long updated;

    public Workshop() {
        updated = 0;
    }

    public LatLng getLatLng() {
        return new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
    }

    public void setLatLng(LatLng latLng) {
        lat = String.valueOf(latLng.latitude);
        lng = String.valueOf(latLng.longitude);
    }

    public boolean isMovable() {
        Calendar lastUpdated = Calendar.getInstance();
        lastUpdated.setTimeInMillis(updated);
        return lastUpdated.get(Calendar.DAY_OF_YEAR)
                != Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
    }
}
