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

import android.os.Handler;

import com.google.android.apps.santatracker.map.SantaMarker;
import com.google.android.gms.maps.GoogleMap;

/**
 * An animation executed during Santa Cam mode.
 *
 */
abstract class SantaCamAnimation {

    GoogleMap mMap;
    SantaMarker mSanta;
    boolean mIsCancelled;

    private Handler mHandler;

    SantaCamAnimation(Handler handler, GoogleMap map, SantaMarker santa) {
        mMap = map;
        mSanta = santa;
        mHandler = handler;
        mIsCancelled = false;
    }

    public void reset() {
        mIsCancelled = false;
    }

    public void cancel() {
        // stop execution
        mIsCancelled = true;
    }

    void executeRunnable(Runnable r) {
        if (!mIsCancelled) {
            mHandler.post(r);
        }
    }

}
