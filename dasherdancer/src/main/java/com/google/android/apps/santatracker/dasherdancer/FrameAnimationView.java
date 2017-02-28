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

package com.google.android.apps.santatracker.dasherdancer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class FrameAnimationView extends ImageView {

    private Drawable[] mFrames;

    private int[] mFrameIndices;
    private int mFrameIndex;
    private final Paint mPaint = new Paint();
    private InsetDrawableCompat mInsetDrawable;
    private BitmapDrawable mBitmapDrawable;


    public FrameAnimationView(Context context) {
        super(context);
        init();
    }

    public FrameAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FrameAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint.setAntiAlias(true);
        setImageDrawable(mInsetDrawable);
    }

    /**
     * Will attempt to recycle the old frames before setting the new frames.
     *
     * @param frames
     * @param frameIndices
     */
    public void setFrames(Drawable[] frames, int[] frameIndices) {
        if (mFrames != null) {
            mFrames = null;
        }
        mFrames = frames;
        mFrameIndices = frameIndices;
    }

    public int getFrameIndex() {
        return mFrameIndex;
    }

    public void setFrameIndex(int frameIndex) {
        mFrameIndex = frameIndex;
        if (mFrames != null && mFrameIndex >= 0 && mFrameIndex < mFrameIndices.length
                && mFrames[mFrameIndices[mFrameIndex]] != null && !isBitmapRecycled(mFrames[mFrameIndices[mFrameIndex]])) {
            invalidate();
        }
    }

    private boolean isBitmapRecycled(Drawable drawable) {
        if (drawable != null && drawable instanceof InsetDrawableCompat) {
            drawable = ((InsetDrawableCompat) drawable).getDrawable();
        }
        if (drawable != null && drawable instanceof BitmapDrawable) {
            Bitmap bmp = ((BitmapDrawable) drawable).getBitmap();
            if (bmp != null) {
                return bmp.isRecycled();
            }
        }
        return true;
    }

    Matrix matrix = new Matrix();

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        recalculateMatrix(w, h);
    }

    private void recalculateMatrix(int vwidth, int vheight) {
        if (mFrames != null && mFrameIndex >= 0 && mFrameIndex < mFrameIndices.length) {
            InsetDrawableCompat insetDrawableCompat =
                    (InsetDrawableCompat) mFrames[mFrameIndices[mFrameIndex]];

            if (isBitmapRecycled(insetDrawableCompat)) {
                return;
            }

            matrix.reset();
            float scale;
            float dx = 0, dy = 0;

            int dwidth = insetDrawableCompat.getDrawable().getIntrinsicWidth() +
                    insetDrawableCompat.getLeft() + insetDrawableCompat.getRight();
            int dheight = insetDrawableCompat.getDrawable().getIntrinsicHeight() +
                    insetDrawableCompat.getTop() + insetDrawableCompat.getBottom();

            if (dwidth * vheight > vwidth * dheight) {
                scale = (float) vheight / (float) dheight;
                dx = (vwidth - dwidth * scale) * 0.5f;
            } else {
                scale = (float) vwidth / (float) dwidth;
                dy = (vheight - dheight * scale) * 0.5f;
            }

            matrix.setTranslate(insetDrawableCompat.getLeft(), insetDrawableCompat.getTop());
            matrix.postScale(scale, scale);
            matrix.postTranslate(Math.round(dx), Math.round(dy));
            setImageMatrix(matrix);
        }
    }

    public void onDraw(Canvas c) {
        if (mFrames != null && mFrameIndex >= 0 && mFrameIndex < mFrameIndices.length) {
            //the line below should work with InsetDrawable with CENTER_CROP,
            //but it doesn't work on older APIs (JB) beacause of different handling of insets:
            //setImageDrawable(mFrames[mFrameIndices[mFrameIndex]]);

            //code below fixes the bug in InsetDrawable and works on all API levels:
            //instead of setting the InsetDrawable on FrameAnimationView,
            //we set the Bitmap directly and use the insets to calculate the correct matrix

            InsetDrawableCompat insetDrawableCompat = (InsetDrawableCompat) mFrames[mFrameIndices[mFrameIndex]];
            if (isBitmapRecycled(insetDrawableCompat)) {
                return;
            }
            Bitmap newBitmap = ((BitmapDrawable) insetDrawableCompat.getDrawable()).getBitmap();

            Drawable current = getDrawable();
            if (current == null
                    || (current instanceof BitmapDrawable
                    && !newBitmap.equals(((BitmapDrawable) current).getBitmap()))) {
                setImageBitmap(newBitmap);
                recalculateMatrix(getWidth(), getHeight());
            }
        }

        if (isBitmapRecycled(getDrawable())) {
            return;
        }
        super.onDraw(c);
    }
}
