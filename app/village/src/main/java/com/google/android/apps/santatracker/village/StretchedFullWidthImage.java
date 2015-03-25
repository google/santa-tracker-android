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

public class StretchedFullWidthImage extends ImageWithAlphaAndSize {

    private boolean mLoadedImage = false;
    private int mImageId;
    private int mOriginalHeight;
    private int mVerticalOffset;
    private Bitmap mImage;

    public StretchedFullWidthImage(int imageId, int originalHeight, int verticalOffset) {
        mImageId = imageId;
        mOriginalHeight = originalHeight;
        mVerticalOffset = verticalOffset;
    }

    public void loadImages(Resources resources) {
        if (!mLoadedImage) {
            mImage = BitmapFactory.decodeResource(resources, mImageId);
            mLoadedImage = true;
        }
    }

    private Rect dest = new Rect();
    private Paint paint = new Paint();

    public void onDraw(Canvas canvas, int viewHeight, int viewWidth, int verticalOffset) {
        if (!mLoadedImage) {
            return;
        }

        float scale = (float) viewHeight / (mOriginalHeight);
        dest.set(0, Math.round(mVerticalOffset * scale) + verticalOffset, viewWidth,
                Math.round((mVerticalOffset + mImage.getHeight()) * scale) + verticalOffset);
        if (!getIsInvisible()) {
            paint.setAlpha(getAlpha());
            canvas.drawBitmap(mImage, null, dest, paint);
        }
    }
}
