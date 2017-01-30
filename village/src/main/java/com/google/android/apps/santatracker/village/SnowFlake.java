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

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class SnowFlake {

    private static Paint mPaint;
    private static float MAX_SIZE;
    private static final int VERTICAL_MULTIPLIER = 3; // Fall this much faster down than across
    private final float mAngleDelta = (float) (Math.random() / 100); // [0, 0.01)
    private float mX, mY, mRadius, mFlakeVelocity, mAngle;

    public SnowFlake(Resources resources, int height, int width) {
        // MAX_SIZE is the fraction of the shorter edge of the screen to use for a single flake
        if (MAX_SIZE == 0) {
            MAX_SIZE = Float.valueOf(resources.getString(R.string.maxSize));
        }
        if (mPaint == null) {
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(Color.WHITE); // Intentionally no alpha as it didn't test well.
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        }
        setScreenDimensions(height, width);
    }

    public void setScreenDimensions(int height, int width) {
        // Choose the smaller of height/width for radius calculations
        int shortEdge = Math.min(width, height);
        mRadius = shortEdge * (float) Math.random() * MAX_SIZE;
        // Individual flake velocity is related to, but not tied to actual flake radius.
        mFlakeVelocity = shortEdge * (float) Math.random() * MAX_SIZE / VERTICAL_MULTIPLIER;

        mAngle = newAngle();
        mX = newX(width);  // Randomly position along x-axis
        mY = newY(height, mRadius, false);
    }

    // Initial falling angle [0, 2π)
    private static float newAngle() {
            return (float) (Math.random() * 2 * Math.PI);
    }

    // Randomly position along x-axis
    private static float newX(int width) {
        return width * (float) Math.random();
    }

    // Place Y position. Deduct diameter to have it off screen.
    private static float newY(int height, float radius, boolean atTop) {
        return atTop ? -radius * 2 : height * (float) Math.random() - radius * 2;
    }

    public void onDraw(Canvas canvas, int height, int width) {
        mX += mFlakeVelocity * Math.cos(mAngle);
        // Make the snow vertically fall at VERTICAL_MULTIPLIER times the increment
        mY += mFlakeVelocity * VERTICAL_MULTIPLIER * Math.abs(Math.sin(mAngle));
        mAngle += mAngleDelta;

        // If the flake went off screen, bring it back at the top.
        if (mY > height + mRadius * 2 || mX < -mRadius * 2 || mX > width + mRadius * 2) {
            mAngle = newAngle();
            mX = newX(width);
            mY = newY(height, mRadius, true); // Move to the top of the screen, just out of view
        }

        canvas.drawCircle(mX, mY, mRadius, mPaint);
    }
}