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

package com.google.android.apps.santatracker.games.matching;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.apps.santatracker.R;

/**
 * View that displays the level at 60% of the available height of the canvas.
 */
public class LevelTextView extends View {

    private Paint mPaint = new Paint();
    private int mLevelNumber = 0;
    private Typeface mRobotoTypeface;

    /**
     * @param context
     * @param attrs
     */
    public LevelTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mRobotoTypeface = Typeface.createFromAsset(context.getAssets(),
                context.getString(R.string.typeface_roboto_black));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(Color.WHITE);
        mPaint.setTextAlign(Align.CENTER);
        // Text size is set as 60% of the available height of the canvas
        mPaint.setTextSize(getHeight() / 6);
        mPaint.setTypeface(mRobotoTypeface);
        canvas.drawText(String.valueOf(mLevelNumber), getWidth() / 2,
                (getHeight() - (mPaint.ascent() + mPaint.descent())) / 2, mPaint);
    }

    public void setLevelNumber(int levelNumber) {
        mLevelNumber = levelNumber;
    }
}
