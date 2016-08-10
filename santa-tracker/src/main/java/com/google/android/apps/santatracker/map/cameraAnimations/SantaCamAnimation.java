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

package com.google.android.apps.santatracker.map.cameraAnimations;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.apps.santatracker.map.SantaMarker;

import android.os.Handler;

/**
 * An animation executed during Santa Cam mode.
 *
 * @author jfschmakeit
 */
public abstract class SantaCamAnimation {

    //private static final String TAG = "SantaCamAnimation";
    protected GoogleMap mMap;
    protected SantaMarker mSanta;
    private Handler mHandler;
    protected boolean mIsCancelled;

    public SantaCamAnimation(Handler mHandler, GoogleMap mMap,
            SantaMarker mSanta) {
        super();
        this.mMap = mMap;
        this.mSanta = mSanta;
        this.mHandler = mHandler;
        this.mIsCancelled = false;
    }

    public void reset() {
        this.mIsCancelled = false;
    }

    public void cancel() {
        // stop execution
        this.mIsCancelled = true;
    }

    protected void executeRunnable(Runnable r) {
        if (!mIsCancelled) {
            mHandler.post(r);
        }
    }

}
