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
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * An image that will scroll across the screen from side to side. An example of this is the monorail
 * in Santa's village.
 */
public class HorizontalScrollingImage extends ImageWithAlphaAndSize {

    private boolean mLoadedImage = false;
    private int mImageId;
    private int mOriginalHeight;
    private int mVerticalOffset;
    private Bitmap mImage;
    private boolean mLeftToRight;
    private float mOffset = Float.MAX_VALUE;
    private long mLastTime = System.currentTimeMillis();

    private float mScrollPerSecond;

    public HorizontalScrollingImage(int imageId, int originalHeight,
            int verticalOffset, boolean leftToRight, int scrollPerSecond) {
        mImageId = imageId;
        mOriginalHeight = originalHeight;
        mVerticalOffset = verticalOffset;
        mLeftToRight = leftToRight;
        mScrollPerSecond = scrollPerSecond / 100f; // As a percentage
    }

    public void loadImages(Resources resources) {
        if (!mLoadedImage) {
            mImage = BitmapFactory.decodeResource(resources, mImageId);
            mLoadedImage = true;
        }
    }

    // Declared here to avoid heap allocations during onDraw
    private Rect dest = new Rect();
    private Rect slice = new Rect();
    private Paint paint = new Paint();

    public void onDraw(Canvas canvas, int viewHeight, int viewWidth, int verticalOffset) {
        if (!mLoadedImage) {
            return;
        }
        // Move X% of screen width per second
        long currentTime = System.currentTimeMillis();
        mOffset += (currentTime - mLastTime) / 1000f * viewWidth * mScrollPerSecond;
        mLastTime = currentTime;
        if (mOffset > viewWidth) {
            // As a cheap way of keeping the monorail infrequent,
            // allow it go off the side of the screen for a while.
            mOffset = -viewWidth;
        }

        float scale = (float) viewHeight / (mOriginalHeight);
        dest.set(0, Math.round(mVerticalOffset * scale) + verticalOffset, 0,
                Math.round((mVerticalOffset + mImage.getHeight()) * scale) + verticalOffset);

        if (mLeftToRight) {
            dest.left = Math.round(mOffset);
        } else {
            dest.left = Math.round(viewWidth - mOffset);
        }
        dest.right = dest.left + Math.round(mImage.getWidth() * scale);

        slice.left = Math.round((-viewWidth) / 2);
        slice.top = dest.top;
        slice.right = Math.round((viewWidth) / 2);
        slice.bottom = dest.bottom;

        if (slice.intersects(dest.left, dest.top, dest.right, dest.bottom)) {
            dest.left = dest.left - slice.left;
            dest.right = dest.left + Math.round(mImage.getWidth() * scale);
            if (!getIsInvisible()) {
                dest.right *= mSize;
                dest.bottom *= mSize;
                paint.setAlpha(getAlpha());
                canvas.drawBitmap(mImage, null, dest, paint);
            }
        }
    }

    public boolean isTouched(int x, int y) {
        return dest.contains(x, y) && !getIsInvisible();
    }
}
