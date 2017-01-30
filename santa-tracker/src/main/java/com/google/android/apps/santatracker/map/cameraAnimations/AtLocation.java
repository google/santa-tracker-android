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

package com.google.android.apps.santatracker.map.cameraAnimations;

import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public abstract class AtLocation {

    private static final float TILT_MIN = 45f;
    private static final float TILT_MAX = 37.5f;
    private static final float ZOOM_MIN = 11f;
    private static final float ZOOM_MAX = 13f;
    private static final float BEARING_RANGE = 45f;

    public static CameraUpdate GetCameraUpdate(LatLng position, float camBearing) {

        final float tilt = SantaPreferences.getRandom(TILT_MIN, TILT_MAX);
        final float zoom = SantaPreferences.getRandom(ZOOM_MIN, ZOOM_MAX);

        // Limit bearing to 45 deg to either side - MapView rotation bug
        float bearing =
                SantaPreferences.getRandom(camBearing - BEARING_RANGE, camBearing + BEARING_RANGE);
        bearing = (bearing + 360f) % 360f;

        final CameraPosition camera = CameraPosition.builder()
                .target(position).tilt(tilt)
                .zoom(zoom).bearing(bearing).build();

        return CameraUpdateFactory.newCameraPosition(camera);
    }


}
