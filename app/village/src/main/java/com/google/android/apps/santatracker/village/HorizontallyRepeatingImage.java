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

package com.google.android.apps.santatracker.village;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;

public class HorizontallyRepeatingImage {

    private static final float PARALLAX_DIVISOR = 1.5f;
    private boolean mLoadedImage = false;
    private int mImageId;
    private int mOriginalHeight;
    private int mVerticalOffset;
    private Bitmap mImage;
    private float mParallax;

    public HorizontallyRepeatingImage(int imageId, int originalHeight, int verticalOffset,
            float parallax) {
        mImageId = imageId;
        mOriginalHeight = originalHeight;
        mVerticalOffset = verticalOffset;
        mParallax = parallax / PARALLAX_DIVISOR;
    }

    public void loadImages(Resources resources) {
        if (!mLoadedImage) {
            mImage = BitmapFactory.decodeResource(resources, mImageId);
            source.set(0, 0, mImage.getWidth(), mImage.getHeight());

            mLoadedImage = true;
        }
    }

    private Rect source = new Rect();
    private Rect dest = new Rect();

    public void onDraw(Canvas canvas, int viewHeight, int viewWidth, int verticalOffset,
            int horizontalOffset) {
        if (!mLoadedImage) {
            return;
        }

        float scale = (float) viewHeight / (mOriginalHeight);

        // Initial then repeat
        horizontalOffset = Math.round(horizontalOffset / scale * mParallax)
                % mImage.getWidth(); // Bring to source scale and calculate based on parallax
        float startPoint = horizontalOffset > 0 ? horizontalOffset
                : mImage.getWidth() + horizontalOffset;
        source.set(Math.round(startPoint), 0,
                mImage.getWidth(), mImage.getHeight());
        dest.set(0,
                Math.round(mVerticalOffset * scale) + verticalOffset,
                Math.round((mImage.getWidth() - startPoint) * scale),
                Math.round((mVerticalOffset + source.height()) * scale) + verticalOffset);
        canvas.drawBitmap(mImage, source, dest, null);

        // Now repeat image till width
        int horizontalProgress = Math.round(source.width() * scale);
        source.set(0, 0, mImage.getWidth(), mImage.getHeight()); // full image
        while (horizontalProgress < viewWidth) {
            dest.set(horizontalProgress,
                    dest.top,
                    horizontalProgress + Math.round(source.width() * scale),
                    dest.bottom);
            canvas.drawBitmap(mImage, source, dest, null);
            horizontalProgress += dest.width();
        }
    }
}