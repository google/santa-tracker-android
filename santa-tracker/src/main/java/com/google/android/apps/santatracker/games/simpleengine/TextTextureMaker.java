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

package com.google.android.apps.santatracker.games.simpleengine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.WindowManager;

import java.util.ArrayList;

public class TextTextureMaker implements Runnable {

    private final static int PADDING_LEFT = 2;
    private final static int PADDING_RIGHT = 5;
    private final static int PADDING_BOTTOM = 2;
    private final static int PADDING_TOP = 2;

    private final static int SUPERSAMPLING = 2;

    private class Entry {

        int tag;
        String text;
        float fontSize;
        Bitmap bitmap = null;
        int color;
    }

    ArrayList<Entry> mEntries = new ArrayList<Entry>();
    boolean mStartedLoading = false;
    boolean mFinishedLoading = false;
    Context mCtx = null;

    public TextTextureMaker() {
    }

    void requestTex(int tag, String text, float fontSize, int color) {
        Entry e = new Entry();
        e.tag = tag;
        e.text = text;
        e.fontSize = fontSize;
        e.color = color;
        mEntries.add(e);
    }

    void startLoading(Context ctx) {
        if (mStartedLoading) {
            Logger.e("TextTextureMaker.startLoading() called twice!");
            return;
        }
        mCtx = ctx.getApplicationContext();
        mStartedLoading = true;
        (new Thread(this)).start();
    }

    boolean isFinishedLoading() {
        return mFinishedLoading;
    }

    int getCount() {
        return mEntries.size();
    }

    Bitmap getBitmap(int index) {
        if (!mFinishedLoading) {
            Logger.e("Can't call TextTextureMaker.getBitmap before load is finished!");
            return null;
        }
        return (index >= 0 && index < mEntries.size()) ? mEntries.get(index).bitmap : null;
    }

    int getTag(int index) {
        return (index >= 0 && index < mEntries.size()) ? mEntries.get(index).tag : null;
    }

    @Override
    public void run() {
        for (Entry e : mEntries) {
            makeBitmapForEntry(e);
        }

        mFinishedLoading = true;
    }

    private void makeBitmapForEntry(Entry e) {
        Logger.d("Making bitmap for text '" + e.text + "', font size " + e.fontSize);
        Paint p = new Paint();
        Rect bounds = new Rect();
        WindowManager wm = (WindowManager) mCtx.getSystemService(Context.WINDOW_SERVICE);
        float fontUnit = SUPERSAMPLING * wm.getDefaultDisplay().getWidth() / 1000.0f;

        p.setColor(e.color);
        p.setTextSize(e.fontSize * fontUnit);
        p.getTextBounds(e.text, 0, e.text.length(), bounds);

        Logger.d("Text bounds: " + bounds.toString());

        int width = bounds.width() + PADDING_LEFT + PADDING_RIGHT;
        int height = bounds.height() + PADDING_TOP + PADDING_BOTTOM;
        int textX = -bounds.left + PADDING_LEFT;
        int textY = -bounds.top + PADDING_TOP;

        Logger.d("Bitmap will be " + width + "x" + height + ", offset will be " + textX + "," +
                textY);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        bmp.eraseColor(0);
        c.drawColor(0);
        c.drawText(e.text, textX, textY, p);

        if (SUPERSAMPLING > 1) {
            e.bitmap = Bitmap.createScaledBitmap(bmp, bmp.getWidth() / SUPERSAMPLING,
                    bmp.getHeight() / SUPERSAMPLING, true);
            bmp.recycle();
        } else {
            e.bitmap = bmp;
        }
    }

    public void dispose() {
        mFinishedLoading = mStartedLoading = false;
        for (Entry e : mEntries) {
            if (e.bitmap != null) {
                e.bitmap.recycle();
                e.bitmap = null;
            }
        }
        mEntries.clear();
    }
}
