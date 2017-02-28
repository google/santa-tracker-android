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

public class BackgroundLoadTask extends AsyncTask<Void, Void, Void> {
    private Resources mResources;
    private Bitmap[] mBackgrounds;
    private Bitmap[] mBackgrounds2;
    private Bitmap[] mForegrounds;
    private Bitmap[] mForegrounds2;
    private Bitmap[] mExitTransitions;
    private Bitmap[] mEntryTransitions;
    private int mBackgroundRes;
    private int mForegroundRes;
    private int mExitTransitionRes;
    private int mEntryTransitionRes;
    private float mScaleX;
    private float mScaleY;
    private int mLevel;
    private int mScreenWidth;
    private int mScreenHeight;

    public BackgroundLoadTask(
            Resources resources,
            int level,
            int backgroudRes,
            int exitTransitionRes,
            int entryTransitionRes,
            float scaleX,
            float scaleY,
            Bitmap[] backgrounds,
            Bitmap[] backgrounds2,
            Bitmap[] exitTransitions,
            Bitmap[] entryTransitions,
            int screenWidth,
            int screenHeight)
    {
        mResources = resources;
        mBackgrounds = backgrounds;
        mBackgrounds2 = backgrounds2;
        mExitTransitions = exitTransitions;
        mEntryTransitions = entryTransitions;
        mBackgroundRes = backgroudRes;
        mExitTransitionRes = exitTransitionRes;
        mEntryTransitionRes = entryTransitionRes;
        mScaleX = scaleX;
        mScaleY = scaleY;
        mLevel = level;
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
    }
    
    @Override
    protected Void doInBackground(Void... params) {
        if (mResources != null) {
            // Since exit transitions are for a previous level, use mLevel - 1
            if ((mExitTransitions != null) && ((mLevel - 1) >= 0) && ((mLevel - 1) < mExitTransitions.length) && (mExitTransitionRes != -1)) {
                Bitmap bmp = BitmapFactory.decodeResource(mResources, mExitTransitionRes);
                if ((mScaleX != 1.0f) || (mScaleY != 1.0f)) {
                    Bitmap tmp = Bitmap.createScaledBitmap(bmp, mScreenWidth, mScreenHeight, false);
                    synchronized (mExitTransitions) {
                        mExitTransitions[mLevel - 1] = tmp;
                        mExitTransitions.notify();
                    }
                    if (bmp != tmp) {
                        bmp.recycle();
                    }
                } else {
                    synchronized (mExitTransitions) {
                        mExitTransitions[mLevel - 1] = bmp;
                        mExitTransitions.notify();
                    }
                }
            }
            if (mLevel == 6) {
                mLevel = 1;
            }
            if ((mBackgrounds != null) && (mLevel >= 0) && (mLevel < mBackgrounds.length)) {
                Bitmap bmp = BitmapFactory.decodeResource(mResources, mBackgroundRes);
                if ((mScaleX != 1.0f) || (mScaleY != 1.0f)) {
                    Bitmap tmp = Bitmap.createScaledBitmap(bmp, 2 * mScreenWidth, mScreenHeight, false);
                    if (tmp != bmp) {
                        bmp.recycle();
                        bmp = tmp;
                    }
                }
                synchronized (mBackgrounds) {
                    createTwoBitmaps(bmp, mBackgrounds, mBackgrounds2, mLevel);
                    mBackgrounds.notify();
                }
                bmp.recycle();
            }

            if ((mEntryTransitions != null) && (mLevel >= 0) && (mLevel < mEntryTransitions.length) && (mEntryTransitionRes != -1)) {
                Bitmap bmp = BitmapFactory.decodeResource(mResources, mEntryTransitionRes);
                if ((mScaleX != 1.0f) || (mScaleY != 1.0f)) {
                    Bitmap tmp = Bitmap.createScaledBitmap(bmp, mScreenWidth, (int)((float)bmp.getHeight() * mScaleY), false);
                    synchronized (mEntryTransitions) {
                        mEntryTransitions[mLevel] = tmp;
                        mEntryTransitions.notify();
                    }
                    if (bmp != tmp) {
                        bmp.recycle();
                    }
                } else {
                    synchronized (mEntryTransitions) {
                        mEntryTransitions[mLevel] = bmp;
                        mEntryTransitions.notify();
                    }
                }
            }
        }
        return null;
    }

    public static void createTwoBitmaps(Bitmap src, Bitmap[] bmps1, Bitmap[] bmps2, int level) {
        int hw = src.getWidth()/2;
        Bitmap bmp1 = Bitmap.createBitmap(src, 0, 0, hw, src.getHeight());
        Bitmap bmp2 = Bitmap.createBitmap(src, src.getWidth()/2, 0, src.getWidth() - hw, src.getHeight());
        bmps1[level] = bmp1;
        bmps2[level] = bmp2;
        src.recycle();
    }
}
