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
import android.graphics.Paint;
import android.graphics.Rect;

import java.util.ArrayList;

/**
 * A single image that is repeatedly, horizontally, tiled. Create textured backgrounds with a few
 * pixels and use this class to repeat, or fill, the texture.
 *
 * This class assumes that all the images are the same size and have a max width/height of 2048.
 */
public class HorizontallyTiledImage extends ImageWithAlphaAndSize {

    private static final int RETRY_COUNT = 5;
    private static final float PARALLAX_DIVISOR = 1.5f;
    private final int[] mImageIds;
    private final int mVerticalOffset;
    private final float mParallax;

    private ArrayList<Bitmap> mImages;
    private int mInSampleSize;
    private int mOriginalHeight;
    private int mHeight;
    private int mSliceWidth;
    private int mTotalWidth = 0;
    private boolean mLoadedImages = false;

    public HorizontallyTiledImage(int[] imageIds, int originalHeight,
            int verticalOffset, float parallax, int imageSampleSize) {
        mImageIds = imageIds;
        mImages = new ArrayList<>(mImageIds.length);
        mOriginalHeight = originalHeight;
        mVerticalOffset = verticalOffset;
        mParallax = parallax / PARALLAX_DIVISOR;
        mInSampleSize = imageSampleSize;
    }

    public void loadImages(Resources resources) {
        if (!mLoadedImages) {
            int retryCount = 0;

            // Calculate image scale based on view height.
            BitmapFactory.Options options = new BitmapFactory.Options();
            while (retryCount <= RETRY_COUNT) {
                for (int imageId : mImageIds) {
                    try {
                        options.inSampleSize = mInSampleSize;
                        Bitmap image = BitmapFactory.decodeResource(resources, imageId, options);
                        mImages.add(image);
                        mHeight = image.getHeight();
                        mSliceWidth = image.getWidth();
                        mTotalWidth += mSliceWidth;
                    } catch (OutOfMemoryError e) {
                        // Retry with a smaller sample size. Retry all images to
                        // ensure they use the same scale;
                        ++retryCount;
                        mInSampleSize++;
                        mImages.clear();
                        mTotalWidth = 0;
                        break;
                    }
                }
                // All images loaded successfully
                if (mTotalWidth > 0) {
                    mLoadedImages = true;
                    break;
                }
            }
        }
    }

    private Rect source = new Rect();
    private Rect dest = new Rect();

    private float scale;
    private Paint paint = new Paint();

    // A horizontal offset of 0 means show the center
    public void onDraw(Canvas canvas, int viewHeight, int viewWidth, int verticalOffset,
            int horizontalOffset) {
        if (!mLoadedImages) {
            return;
        }

        scale = (float) viewHeight / (mOriginalHeight);

        // Calculate info about initial slice
        float startCalc = ((geTotalWidthScaled(viewHeight) - viewWidth) / 2 + horizontalOffset
                        * mParallax)
                / (mSliceWidth * scale * mInSampleSize);
        int sliceStart = (int) startCalc;
        float sliceStartOffset = startCalc % 1;

        // Calculate info about final slice
        float endCalc = ((geTotalWidthScaled(viewHeight) + viewWidth) / 2 + horizontalOffset
                        * mParallax)
                / (mSliceWidth * scale * mInSampleSize);
        int sliceEnd = (int) endCalc;
        float sliceEndOffset = endCalc % 1;

        paint.setAlpha(getAlpha());

        // Draw start slice
        if (sliceStart >= 0 && sliceStart < mImages.size()) {
            source.set(Math.round(mSliceWidth * sliceStartOffset), 0, mSliceWidth, mHeight);
            dest.set(
                    0,
                    Math.round(mVerticalOffset * scale) + verticalOffset,
                    Math.round(source.width() * scale * mInSampleSize),
                    Math.round((mVerticalOffset + source.height()
                            * mInSampleSize)
                            * scale) + verticalOffset);
            if (!getIsInvisible()) {
                canvas.drawBitmap(mImages.get(sliceStart), source, dest, paint);
            }

            // Draw middle slices
            source.set(0, 0, mSliceWidth, mHeight);
            for (int fullSlice = sliceStart + 1; fullSlice < mImages.size(); fullSlice++) {
                dest.set(
                        dest.right,
                        Math.round(mVerticalOffset * scale) + verticalOffset,
                        Math.round(dest.right + source.width() * scale
                                * mInSampleSize),
                        Math.round((mVerticalOffset + source.height()
                                * mInSampleSize)
                                * scale) + verticalOffset);
                if (!getIsInvisible()) {
                    canvas.drawBitmap(mImages.get(fullSlice), source, dest, paint);
                }
            }

            // Draw end slice
            if (sliceStart != sliceEnd && sliceEnd < mImages.size()) {
                source.set(0, 0, Math.round(mSliceWidth * sliceEndOffset), mHeight);
                dest.set(
                        dest.right,
                        Math.round(mVerticalOffset * scale) + verticalOffset,
                        Math.round(dest.right + source.width() * scale
                                * mInSampleSize),
                        Math.round((mVerticalOffset + source.height()
                                * mInSampleSize)
                                * scale) + verticalOffset);
                if (!getIsInvisible()) {
                    canvas.drawBitmap(mImages.get(sliceEnd), source, dest, paint);
                }
            }
        }
    }

    public int geTotalWidthScaled(int viewHeight) {
        scale = (float) viewHeight / (mOriginalHeight);
        return Math.round(mTotalWidth * scale * mInSampleSize);
    }
}
