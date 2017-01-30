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

package com.google.android.apps.santatracker.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

import com.google.android.apps.santatracker.R;

import java.util.HashMap;
import java.util.Map;

public class SantaCamButton extends FloatingActionButton {

    private final Map<String, Drawable> mFlashDrawables = new HashMap<>();
    private final Paint mPaint = new Paint();
    private final int mIconSize;

    public SantaCamButton(Context context) {
        this(context, null);
    }

    public SantaCamButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SantaCamButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mIconSize = getResources().getDimensionPixelSize(R.dimen.fab_icon_size);
        mPaint.setColor(Color.WHITE);
        mPaint.setTextSize(mIconSize);
        mPaint.setTextAlign(Paint.Align.CENTER);
    }

    /**
     * Show the specified message for the duration of time.
     *
     * @param message  The message to show (needs to be very short to fit in the FAB)
     * @param duration The duration in milliseconds
     */
    public void showMessage(String message, long duration) {
        final Drawable current = getDrawable();
        setImageDrawable(getFlashDrawable(message));
        postDelayed(new Runnable() {
            @Override
            public void run() {
                setImageDrawable(current);
            }
        }, duration);
    }

    private Drawable getFlashDrawable(String message) {
        if (mFlashDrawables.containsKey(message)) {
            return mFlashDrawables.get(message);
        }
        Bitmap bitmap = Bitmap.createBitmap(mIconSize, mIconSize, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(message, mIconSize / 2,
                (mIconSize - mPaint.descent() - mPaint.ascent()) / 2, mPaint);
        Drawable d = new BitmapDrawable(getResources(), bitmap);
        mFlashDrawables.put(message, d);
        return d;
    }

}
