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
import android.util.AttributeSet;
import android.view.View;

public class SnowFlakeView extends View {

    private boolean mAnimationDisabled = false;
    private SnowFlake[] mSnowFlakes;

    public SnowFlakeView(Context context) {
        super(context);
        init();
    }

    public SnowFlakeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SnowFlakeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SnowFlakeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void init() {
        int numFlakes = getResources().getInteger(R.integer.flakeCount);
        mSnowFlakes = new SnowFlake[numFlakes];
        for (int i = 0; i < mSnowFlakes.length; i++) {
            mSnowFlakes[i] = new SnowFlake(getResources(), getHeight(), getWidth());
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        for (SnowFlake flake : mSnowFlakes) {
            flake.setScreenDimensions(getHeight(), getWidth());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mAnimationDisabled) {
            return;
        }

        for (SnowFlake flake : mSnowFlakes) {
            flake.onDraw(canvas, getHeight(), getWidth());
        }

        invalidate();
    }

    @VisibleForTesting
    public void disableAnimation() {
        mAnimationDisabled = true;
    }
}
