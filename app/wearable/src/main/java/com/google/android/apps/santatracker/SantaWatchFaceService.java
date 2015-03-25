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

package com.google.android.apps.santatracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;

import java.util.TimeZone;

/**
 * A watch face with a static background, animated clouds, and a Santa whose arms
 * rotate independently as the minute and hour hands.
 */
public class SantaWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "SantaWatchFaceService";

    @Override
    public Engine onCreateEngine() {

        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private final Resources mResources = getResources();
        private Paint mFilterPaint;
        private Paint[] mCloudFilterPaints;
        private Paint mAmbientBackgroundPaint;
        private Paint mAmbientPeekCardBorderPaint;
        private boolean mAmbient;
        private boolean mMute;
        private Time mTime;
        private boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;

        // Figure and head positioning offsets
        private static final int SANTA_FIGURE_OFFSET_X = -10;
        private static final int SANTA_FIGURE_OFFSET_Y = 30;
        private static final int SANTA_FIGURE_ADDITIONAL_OFFSET_X = -8;
        private static final float BORDER_WIDTH_PX = 3.0f;

        private Bitmap[] mBackgroundBitmap;
        private Bitmap[] mFigureBitmap;
        private Bitmap[] mFaceBitmap;
        private Bitmap[] mHourHandBitmap;
        private Bitmap[] mMinuteHandBitmap;

        private Bitmap[] mCloudBitmaps;
        private int[] mCloudSpeeds;
        private int[] mCloudDegrees;

        private long mTimeMotionStart = -1;

        private final Rect mCardBounds = new Rect();

        // Variables for onDraw
        private int mWidth = -1;
        private int mHeight = -1;
        private float mScale;
        private float mCenterX;
        private float mCenterY;
        private int mMinutes;
        private float mMinDeg;
        private float mHrDeg;
        private Bitmap mFigure;
        private Bitmap mMinHand;
        private Bitmap mHrHand;
        private Bitmap mFace;
        private float mScaledXOffset;
        private float mScaledXAdditionalOffset;
        private float mScaledYOffset;
        private long mTimeElapsed;
        private int mLoop;
        private float mRadius;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SantaWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            init();
        }

        /**
         * Setting up settings variables (paint, settings, etc) and pre-load images
         */
        private void init() {

            // Pre-loading bitmaps
            mBackgroundBitmap = loadBitmaps(R.array.backgroundIds);
            mFigureBitmap = loadBitmaps(R.array.figureIds);
            mFaceBitmap = loadBitmaps(R.array.faceIds);
            mHourHandBitmap = loadBitmaps(R.array.hourHandIds);
            mMinuteHandBitmap = loadBitmaps(R.array.minuteHandIds);

            // Initialising paint object for Bitmap draws
            mFilterPaint = new Paint();
            mFilterPaint.setFilterBitmap(true);

            // Initialising background paint
            mAmbientBackgroundPaint = new Paint();
            mAmbientBackgroundPaint.setARGB(255, 0, 0, 0);
            mAmbientPeekCardBorderPaint = new Paint();
            mAmbientPeekCardBorderPaint.setColor(Color.WHITE);
            mAmbientPeekCardBorderPaint.setStrokeWidth(BORDER_WIDTH_PX);

            // Initialing cloud bitmaps and settings
            mCloudDegrees = mResources.getIntArray(R.array.cloudDegrees);
            mCloudBitmaps = loadBitmaps(R.array.cloudIds);
            mCloudSpeeds = mResources.getIntArray(R.array.cloudSpeed);
            mCloudFilterPaints = new Paint[mCloudBitmaps.length];

            // We need different paints because the alpha applied is different for different clouds
            for (int i = 0; i < mCloudBitmaps.length; i++) {
                Paint paint = new Paint();
                paint.setFilterBitmap(true);
                mCloudFilterPaints[i] = paint;
            }

            // Initialising time
            mTime = new Time();
        }


        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        /**
         * Loading all versions (interactive, ambient and low bit) into a bitmap array. The correct
         * version will be pluck out at runtime.
         *
         * @param arrayId Key to the type of bitmap that we are initialising. The full list can be
         *                found in res/values/images_santa_watchface.xml
         * @return Array of three bitmaps for interactive, ambient and low bit modes
         */
        private Bitmap[] loadBitmaps(int arrayId) {
            int[] bitmapIds = getIntArray(arrayId);
            Bitmap[] bitmaps = new Bitmap[bitmapIds.length];
            for (int i = 0; i < bitmapIds.length; i++) {
                Drawable backgroundDrawable = mResources.getDrawable(bitmapIds[i]);
                bitmaps[i] = ((BitmapDrawable) backgroundDrawable).getBitmap();
            }
            return bitmaps;
        }

        /**
         * At runtime, this is used to load the appropriate bitmap depending on display mode
         * dynamically.
         *
         * @param bitmaps A bitmap array containing all bitmaps appropriate to all the display
         *                modes
         * @return Bitmap determined to be appropriate for the display mode
         */
        private Bitmap getBitmap(Bitmap[] bitmaps) {
            if (!mAmbient) {
                return bitmaps[0];
            } else if (!mLowBitAmbient) {
                return bitmaps[1];
            } else {
                return bitmaps[2];
            }
        }

        /**
         * Used to pick out whether the device is a low-bit device (i.e. pixels only capable of
         * displaying on and off in ambient mode).
         *
         * @param properties device properties
         */
        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);
            }
        }

        /**
         * Called periodically to update the watchface. Update at least once a minute - appropriate
         * for ambient mode
         */
        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + mAmbient);
            }
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            Log.e(TAG, "onAmbientModeChanged, " + (inAmbientMode ? "ambient" : "ACTIVE"));
            super.onAmbientModeChanged(inAmbientMode);
            if (!inAmbientMode) {
                // Watch has just been set to active mode.
                mTimeMotionStart = System.currentTimeMillis();
            }
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                invalidate();
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                // Do nothing at present
                // If we add more informational element to the watchface in the future we can
                // remove those elements from display from here.
            }
        }

        @Override
        public void onDraw(Canvas canvas, Rect rect) {
            mTime.setToNow();

            //Draw background.
            canvas.drawRect(0, 0, mWidth, mHeight, mAmbientBackgroundPaint);
            canvas.drawBitmap(getBitmap(mBackgroundBitmap), 0, 0, mFilterPaint);

            if (!mAmbient) {
                // Draw animation layer (above the background, below the figure and arms.)
                drawAnimationLayer(canvas, mCenterX, mCenterY);
            }

            mFigure = getBitmap(mFigureBitmap);

            //Draw figure.
            canvas.drawBitmap(mFigure,
                    mCenterX - mFigure.getWidth() / 2 +
                            mScaledXAdditionalOffset,
                    mCenterY - mFigure.getHeight() / 2 + mScaledYOffset,
                    mFilterPaint);

            if (mAmbient) {
                // Draw a black box as the peek card background
                canvas.drawRect(mCardBounds, mAmbientBackgroundPaint);
            }

            mMinutes = mTime.minute;
            mMinDeg = mMinutes * 6;
            mHrDeg = ((mTime.hour + (mMinutes / 60f)) * 30);

            canvas.save();

            // Draw the minute hand
            canvas.rotate(mMinDeg, mCenterX, mCenterY);
            mMinHand = getBitmap(mMinuteHandBitmap);
            canvas.drawBitmap(mMinHand, mCenterX - mMinHand.getWidth() / 2f,
                    mCenterY - mMinHand.getHeight(), mFilterPaint);

            // Draw the hour hand
            canvas.rotate(360 - mMinDeg + mHrDeg, mCenterX, mCenterY);
            mHrHand = getBitmap(mHourHandBitmap);
            canvas.drawBitmap(mHrHand, mCenterX - mHrHand.getWidth() / 2f,
                    mCenterY - mHrHand.getHeight(),
                    mFilterPaint);

            canvas.restore();

            // Draw face.  (We do this last so it's not obscured by the arms.)
            mFace = getBitmap(mFaceBitmap);
            canvas.drawBitmap(mFace,
                    mCenterX - mFace.getWidth() / 2 + mScaledXOffset,
                    mCenterY - mFigure.getHeight() / 2 + mScaledYOffset,
                    mFilterPaint);

            // While watch face is active, immediately request next animation frame.
            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            mWidth = width;
            mHeight = height;

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            mCenterX = mWidth / 2f;
            mCenterY = mHeight / 2f;

            mScale = ((float) mWidth) / (float) mBackgroundBitmap[0].getWidth();
            scaleBitmaps(mBackgroundBitmap, mScale);
            scaleBitmaps(mFigureBitmap, mScale);
            scaleBitmaps(mFaceBitmap, mScale);
            scaleBitmaps(mHourHandBitmap, mScale);
            scaleBitmaps(mMinuteHandBitmap, mScale);

            mScaledXOffset = SANTA_FIGURE_OFFSET_X * mScale;
            mScaledXAdditionalOffset =
                    (SANTA_FIGURE_OFFSET_X + SANTA_FIGURE_ADDITIONAL_OFFSET_X) * mScale;
            mScaledYOffset = SANTA_FIGURE_OFFSET_Y * mScale;

            scaleBitmaps(mCloudBitmaps, mScale);
        }

        /**
         * Drawing the moving cloud
         *
         * @param canvas  Canvas to be drawn on
         * @param centerX Center of the display
         * @param centerY Center of the display
         */
        private void drawAnimationLayer(Canvas canvas, float centerX, float centerY) {
            if (mAmbient) {
                // Do nothing - static background in ambient mode
                mTimeMotionStart = -1;
            } else {

                if (mTimeMotionStart < 0) {
                    mTimeMotionStart = System.currentTimeMillis();
                }

                mTimeElapsed = System.currentTimeMillis() - mTimeMotionStart;

                for (mLoop = 0; mLoop < mCloudBitmaps.length; mLoop++) {
                    canvas.save();
                    canvas.rotate(mCloudDegrees[mLoop], centerX, centerY);

                    mRadius = centerX - (mTimeElapsed / (mCloudSpeeds[mLoop])) % centerX;
                    mCloudFilterPaints[mLoop].setAlpha((int) (mRadius / centerX * 255));

                    canvas.drawBitmap(mCloudBitmaps[mLoop], centerX, centerY - mRadius,
                            mCloudFilterPaints[mLoop]);

                    canvas.restore();
                }
            }
        }

        /**
         * Scale bitmap array in place.
         *
         * @param bitmaps Bitmaps to be scaled
         * @param scale   Scale factor. 1.0 represents the original size.
         */
        private void scaleBitmaps(Bitmap[] bitmaps, float scale) {
            for (int i = 0; i < bitmaps.length; i++) {
                bitmaps[i] = scaleBitmap(bitmaps[i], scale);
            }
        }

        /**
         * Scale individual bitmap inputs by creating a new bitmap according to the scale
         *
         * @param bitmap Original bitmap
         * @param scale  Scale factor. 1.0 represents the original size.
         * @return Scaled bitmap
         */
        private Bitmap scaleBitmap(Bitmap bitmap, float scale) {
            int width = (int) ((float) bitmap.getWidth() * scale);
            int height = (int) ((float) bitmap.getHeight() * scale);
            if (bitmap.getWidth() != width
                    || bitmap.getHeight() != height) {
                return Bitmap.createScaledBitmap(bitmap,
                        width, height, true /* filter */);
            } else {
                return bitmap;
            }
        }

        /**
         * Loading an int array from resource file
         *
         * @param resId ResourceId of the integer array
         * @return int array
         */
        private int[] getIntArray(int resId) {
            TypedArray array = mResources.obtainTypedArray(resId);
            int[] rc = new int[array.length()];
            TypedValue value = new TypedValue();
            for (int i = 0; i < array.length(); i++) {
                array.getValue(i, value);
                rc[i] = value.resourceId;
            }
            return rc;
        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();

                invalidate();
            } else {
                unregisterReceiver();
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SantaWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SantaWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onPeekCardPositionUpdate(Rect bounds) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPeekCardPositionUpdate: " + bounds);
            }
            super.onPeekCardPositionUpdate(bounds);
            if (!bounds.equals(mCardBounds)) {
                mCardBounds.set(bounds);
                invalidate();
            }
        }

    }
}
