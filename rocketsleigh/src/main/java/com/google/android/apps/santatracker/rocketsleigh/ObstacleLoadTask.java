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

package com.google.android.apps.santatracker.rocketsleigh;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Created by rpetit on 11/18/14.
 */
public class ObstacleLoadTask extends AsyncTask<Void, Void, Void> {
    private TreeMap<Integer, Bitmap> mMap;
    private ArrayList<Integer> mList;
    private int mIndex;
    private Resources mResources;
    private int[] mResourceIds;
    private int mStride;
    private float mScaleX;
    private float mScaleY;

    public ObstacleLoadTask(Resources resources,
                            int[] resourceIds,
                            TreeMap<Integer, Bitmap> map,
                            ArrayList<Integer> list,
                            int index,
                            int stride,
                            float scaleX,
                            float scaleY)
    {
        mResources = resources;
        mResourceIds = resourceIds;
        mMap = map;
        mList = list;
        mIndex = index;
        mStride = stride;
        mScaleX = scaleX;
        mScaleY = scaleY;
    }

    @Override
    protected Void doInBackground(Void... params) {
        for (int i = mIndex; i < (mIndex + 20); i++) {
            if (isCancelled()) {
                break;
            }
            if (i < mList.size()) {
                int obstacle = mList.get(i);
                for (int j = (obstacle * mStride); j < ((obstacle + 1) * mStride); j++) {
                    // Check just in case something is wonky
                    if (j < mResourceIds.length) {
                        int id = mResourceIds[j];
                        if (id != -1) {
                            // Only need to load it once...
                            if (!mMap.containsKey(id)) {
                                Bitmap bmp = BitmapFactory.decodeResource(mResources, id);
                                if ((mScaleX != 1.0f) || (mScaleY != 1.0f)) {
                                    Bitmap tmp = Bitmap.createScaledBitmap(bmp, (int)((float)bmp.getWidth() * mScaleX), (int)((float)bmp.getHeight() * mScaleY), false);
                                    if (tmp != bmp) {
                                        bmp.recycle();
                                    }
                                    synchronized (mMap) {
                                        mMap.put(id, tmp);
                                        mMap.notify();
                                    }
                                } else {
                                    synchronized (mMap) {
                                        mMap.put(id, bmp);
                                        mMap.notify();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
