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

package com.google.android.apps.santatracker.games.simpleengine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.WindowManager;

import java.util.ArrayList;

class BitmapTextureMaker implements Runnable {

    private boolean mStartedLoading = false;
    private boolean mFinishedLoading = false;
    static final int DIM_WIDTH = 0;
    static final int DIM_HEIGHT = 1;
    private Context mContext = null;
    private int mScreenWidth = 0;
    private int mScreenHeight = 0;

    private ArrayList<BitmapEntry> mEntries = new ArrayList<BitmapEntry>();

    BitmapTextureMaker() {
    }

    public void request(int tag, int resId, String name, int dimType, float maxDim) {
        if (mStartedLoading) {
            Logger.e("Can't request a new bitmap after loading has started.");
            return;
        }

        BitmapEntry e = new BitmapEntry();
        e.tag = tag;
        e.resId = resId;
        e.dimType = dimType;
        e.maxDim = maxDim;
        e.name = name;
        mEntries.add(e);
        Logger.d("Bitmap requested: " + e.toString() + ", #" + (mEntries.size() - 1));
    }

    public void startLoading(Context ctx) {
        mStartedLoading = true;
        mContext = ctx.getApplicationContext();
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mScreenWidth = wm.getDefaultDisplay().getWidth();
        mScreenHeight = wm.getDefaultDisplay().getHeight();
        Logger.d("Starting async load of bitmaps. Screen dimensions " + mScreenWidth + "screenX" +
                mScreenHeight);
        Thread t = new Thread(this);
        t.start();
    }

    class BitmapEntry {

        int tag;
        int dimType;
        float maxDim;
        int resId;
        Bitmap bitmap = null;
        String name = "";

        @Override
        public String toString() {
            return "[BitmapEntry name=" + name + ", " +
                    ((dimType == DIM_HEIGHT) ? "maxH=" : "maxW=") + maxDim +
                    ", resId=" + resId + ", bitmap=" + (bitmap == null ? "(null)" : "loaded!" +
                    "]");
        }
    }

    @Override
    public void run() {
        for (BitmapEntry e : mEntries) {
            loadBitmapEntry(e);
        }
        mContext = null;
        mFinishedLoading = true;
        Logger.d("Finished loading bitmaps.");
    }

    void loadBitmapEntry(BitmapEntry e) {
        Logger.d("Loading bitmap entry " + e.toString());

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(mContext.getResources(), e.resId, options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        float imageAspect = imageWidth / (float) imageHeight;
        float screenUnit = (float) mScreenHeight;

        Logger.d(e.name + " dimensions " + imageWidth + "screenX" + imageHeight);

        int reqWidth = 0, reqHeight = 0;

        if (e.dimType == DIM_HEIGHT) {
            reqHeight = (int) (e.maxDim * screenUnit);
            reqWidth = (int) (imageAspect * reqHeight);
        } else if (e.dimType == DIM_WIDTH) {
            reqWidth = (int) (e.maxDim * screenUnit);
            reqHeight = (int) (reqWidth / imageAspect);
        }

        Logger.d(e.name + " requested dimensions " + reqWidth + "screenX" + reqHeight);

        int inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        Logger.d(e.name + " in sample size " + inSampleSize);

        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        e.bitmap = BitmapFactory.decodeResource(mContext.getResources(), e.resId, options);
        Logger.d("Loaded bitmap for " + e.name + ", " + e.bitmap.getWidth() + "x" +
                e.bitmap.getHeight());
    }

    // From http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public boolean isFinishedLoading() {
        return mFinishedLoading;
    }

    public boolean hasStartedLoading() {
        return mStartedLoading;
    }

    public Bitmap getBitmap(int index) {
        if (!mFinishedLoading) {
            Logger.e("Can't call getBitmap before BitmapTextureMaker is finished loading.");
            return null;
        }
        if (index < 0 || index >= mEntries.size()) {
            return null;
        }
        return mEntries.get(index).bitmap;
    }

    public int getBitmapCount() {
        return mEntries.size();
    }

    public int getTag(int index) {
        return (index >= 0 && index < mEntries.size()) ? mEntries.get(index).tag : 0;
    }

    public void dispose() {
        mFinishedLoading = mStartedLoading = false;
        for (BitmapEntry e : mEntries) {
            if (e.bitmap != null) {
                e.bitmap.recycle();
                e.bitmap = null;
            }
        }
    }
}
