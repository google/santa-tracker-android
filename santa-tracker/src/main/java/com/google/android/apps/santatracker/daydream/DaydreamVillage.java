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

package com.google.android.apps.santatracker.daydream;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.view.GestureDetector;

import com.google.android.apps.santatracker.village.HorizontalScrollingImage;
import com.google.android.apps.santatracker.village.HorizontalScrollingImageGroup;
import com.google.android.apps.santatracker.village.VillageView;

import java.lang.ref.WeakReference;

/**
 * Village implementation for supporting Daydream on Android TV
 */
public class DaydreamVillage implements VillageView.VillageInterface {
    private HorizontalScrollingImage mImagePlane;
    private HorizontalScrollingImageGroup mImageClouds;
    private boolean mPlaneEnabled = true;

    private WeakReference<Context> mCtxRef;

    public DaydreamVillage(Context context) {
        mCtxRef = new WeakReference<>(context);
    }


    public void initialiseVillageViews() {

        final Context ctx = mCtxRef.get();

        if (ctx == null) {
            return;
        }

        Resources resources = ctx.getResources();

        int referenceHeight = resources.getInteger(com.google.android.apps.santatracker.village.R.integer.referenceHeight);

        mImagePlane = new HorizontalScrollingImage(com.google.android.apps.santatracker.village.R.drawable.plane, referenceHeight,
                resources.getInteger(com.google.android.apps.santatracker.village.R.integer.planeVerticalOffset), true,
                resources.getInteger(com.google.android.apps.santatracker.village.R.integer.planePercentagePerSecond));

        mImageClouds = new HorizontalScrollingImageGroup(com.google.android.apps.santatracker.village.R.drawable.cloud,
                resources.getInteger(com.google.android.apps.santatracker.village.R.integer.numClouds),
                resources.getInteger(com.google.android.apps.santatracker.village.R.integer.skyStart),
                resources.getInteger(com.google.android.apps.santatracker.village.R.integer.cloudsEnd),
                resources.getInteger(com.google.android.apps.santatracker.village.R.integer.cloudPercentagePerSecond),
                resources.getInteger(com.google.android.apps.santatracker.village.R.integer.cloudSpeedJitterPercent),
                referenceHeight);

        mImagePlane.loadImages(resources);
        mImageClouds.loadImages(resources);
    }

    public void onDraw(Canvas canvas, int height, int width) {

        if (mPlaneEnabled) {
            mImagePlane.onDraw(canvas, height, 3 * width, 0);
        }
        mImageClouds.onDraw(canvas, height, width, 0);
    }

    public GestureDetector.OnGestureListener getTouchListener() {

        // Touch is not supported on Daydream
        return null;
    }

    public void setPlaneEnabled(boolean planeEnabled) {
        mPlaneEnabled = planeEnabled;
    }
}
