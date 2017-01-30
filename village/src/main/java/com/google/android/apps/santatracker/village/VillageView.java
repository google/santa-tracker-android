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

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.VisibleForTesting;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Host view for Santa's village.
 */
public class VillageView extends View {

    public interface VillageInterface {
        void onDraw(Canvas canvas, int height, int width);
        void initialiseVillageViews();
        GestureDetector.OnGestureListener getTouchListener();
        void setPlaneEnabled(boolean planeEnabled);
    }

    private boolean mAnimationDisabled = false;
    private VillageInterface mVillage;
    private GestureDetectorCompat mDetector;

    public VillageView(Context context) {
        super(context);
    }

    public VillageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VillageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setVillage(VillageInterface village) {
        mVillage = village;

        if (mVillage.getTouchListener() != null) {
            mDetector = new GestureDetectorCompat(getContext(), mVillage.getTouchListener());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mAnimationDisabled) {
            return;
        }

        if (mVillage != null) {
            mVillage.onDraw(canvas, getHeight(), getWidth());
        }
        // Redraw next frame
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
            int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (mVillage != null) {
            mVillage.initialiseVillageViews();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (mDetector != null) {
            this.mDetector.onTouchEvent(event);
        }

        return super.onTouchEvent(event);
    }

    @VisibleForTesting
    public void disableAnimation() {
        mAnimationDisabled = true;
    }
}
