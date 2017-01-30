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

package com.google.android.apps.santatracker.village;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;

/**
 * A single image, repeated a number of times within a rectangle, that scroll from side to side
 * across the screen.
 */
public class HorizontalScrollingImageGroup {

    private boolean mLoadedImage = false;
    private int mImageId;
    private int mNumImages;
    private Point[] mLocations;
    private boolean[] mLeftToRight;
    private float[] mOffsets;
    private boolean mInitialised = false;
    private int mTopBound;
    private int mBottomBound;
    private Bitmap mImage;

    private long mLastTime = System.currentTimeMillis();

    private float mScrollPerSecond;

    private int mReferenceHeight;
    public HorizontalScrollingImageGroup(int imageId, int numImages, int topBound, int bottomBound,
                                         float scrollPerSecond, int jitter, int referenceHeight) {
        mImageId = imageId;
        mNumImages = numImages;
        mTopBound = topBound;
        mBottomBound = bottomBound;
        mScrollPerSecond = scrollPerSecond / 100f; // As a percentage
        mScrollPerSecond = mScrollPerSecond * (1 + (float) ((Math.random() - 0.5) * jitter));
        mReferenceHeight = referenceHeight;

        mOffsets = new float[mNumImages];
        mLocations = new Point[mNumImages];
        mLeftToRight = new boolean[mNumImages];
        for (int i = 0; i < mNumImages; i++) {
            mLocations[i] = new Point();
        }
    }
    public void loadImages(Resources resources) {
        if (!mLoadedImage) {
            mImage = BitmapFactory.decodeResource(resources, mImageId);
            mLoadedImage = true;
        }
        for (int i = 0; i < mNumImages; i++) {
            mLocations[i].x = 0;
            mLocations[i].y = (int) Math.round((mBottomBound - mTopBound) * Math.random() + mTopBound);
            mLeftToRight[i] = Math.round(Math.random()) == 0;
        }
    }

    // Declared here to save heap allocations during onDraw
    private Rect dest = new Rect();

    public void onDraw(Canvas canvas, int viewHeight, int viewWidth, int verticalOffset) {
        if (!mLoadedImage) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        float scale = (float) viewHeight / mReferenceHeight;
        for (int i = 0; i < mNumImages; i++) {
            if (!mInitialised) {
                mOffsets[i] = (float) (Math.random() * viewWidth);
            }
            mOffsets[i] += (currentTime - mLastTime) / 1000f * viewWidth * mScrollPerSecond;
            if (mOffsets[i] > viewWidth + mImage.getWidth() * scale) {
                mOffsets[i] = -(viewWidth + 2 * (float) (Math.random()) * mImage.getWidth() * scale);
            }

            dest.set(0, Math.round(scale * mLocations[i].y) + verticalOffset,
                    0, Math.round(scale * (mLocations[i].y + mImage.getHeight())) + verticalOffset);
            if (mLeftToRight[i]) {
                dest.left = Math.round(mOffsets[i]);
            } else {
                dest.left = Math.round(viewWidth - mOffsets[i]);
            }
            dest.right = dest.left + Math.round(mImage.getWidth() * scale);

            canvas.drawBitmap(mImage, null, dest, null);
        }
        mInitialised = true;
        mLastTime = currentTime;
    }
}