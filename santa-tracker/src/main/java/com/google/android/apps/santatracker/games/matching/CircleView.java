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
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;

/**
 * View that contains a round red circle, with a cut out in its center.
 * Used for the end level screen.
 */
public class CircleView extends View {

    private Paint mPaint = new Paint();
    private Path mInsideCircle = new Path();

    public CircleView(Context context) {
        super(context);
    }

    public CircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(Color.RED);
        mInsideCircle.addCircle(getWidth() / 2, getHeight() / 2, getHeight() / 512, Direction.CW);
        mInsideCircle.close();
        try {
            canvas.clipPath(mInsideCircle, Region.Op.XOR);
        } catch (UnsupportedOperationException e) {
            //ignore clipping path for devices that don't support it (i.e. ICS)
        }

        mPaint.setColor(Color.RED);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, getHeight() / 8, mPaint);

    }

}
