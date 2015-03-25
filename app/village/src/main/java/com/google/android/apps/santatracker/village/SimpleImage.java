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

public class SimpleImage extends ImageWithAlphaAndSize {

    private boolean mLoadedImage = false;
    private int mImageId;
    private int mOriginalHeight;
    private int mVerticalOffset;
    private int mHorizontalOffset;
    private Bitmap mImage;

    public SimpleImage(int imageId, int originalHeight, int verticalOffset, int horizontalOffset) {
        mImageId = imageId;
        mOriginalHeight = originalHeight;
        mVerticalOffset = verticalOffset;
        mHorizontalOffset = horizontalOffset;
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
    private Paint paint = new Paint();

    public void onDraw(Canvas canvas, int viewHeight, int verticalOffset) {
        if (!mLoadedImage) {
            return;
        }

        float scale = (float) viewHeight / (mOriginalHeight);
        dest.set(Math.round(mHorizontalOffset * scale),
                Math.round(mVerticalOffset * scale) + verticalOffset,
                Math.round((mHorizontalOffset + source.width()) * scale),
                Math.round((mVerticalOffset + source.height()) * scale) + verticalOffset);
        if (!getIsInvisible()) {
            paint.setAlpha(getAlpha());
            canvas.drawBitmap(mImage, source, dest, paint);
        }
    }

    public boolean isTouched(int x, int y) {
        return dest.contains(x, y) && !getIsInvisible();
    }
}
