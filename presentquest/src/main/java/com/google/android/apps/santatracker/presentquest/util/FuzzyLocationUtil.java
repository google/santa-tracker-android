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
package com.google.android.apps.santatracker.presentquest.util;

import android.util.Log;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.Random;

/**
 * Utility class to slightly fuzz locations
 */
public class FuzzyLocationUtil {

    private static final double MIN_FUZZ = 0.0001f;
    private static final double MAX_FUZZ = 0.0005f;
    private static final String TAG = FuzzyLocationUtil.class.getSimpleName();

    public static LatLng fuzz(LatLng input) {
        return new LatLng(input.latitude + getRandomOffset(), input.longitude + getRandomOffset());
    }

    private static double getRandomOffset() {
        double range = MAX_FUZZ - MIN_FUZZ;

        Random random = new Random();
        double randOffset = (random.nextDouble() * range) + MIN_FUZZ;

        if (random.nextBoolean()) {
            return randOffset;
        } else {
            return -1.0 * randOffset;
        }
    }

}
