/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class SantaWatchFaceService extends AbstractBaseWatchFaceService {

    private static final String TAG = "SantaWatchFaceService";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    @Override
    protected Class<?> getWatchFaceService() {
        return SantaWatchFaceService.class;
    }

    private class Engine extends AbstractBaseWatchFaceService.Engine {

        private final Resources mResources = getResources();
        private Paint mFilterPaint;
        private Paint[] mCloudFilterPaints;
        private Paint mAmbientBackgroundPaint;
        private Paint mAmbientPeekCardBorderPaint;
        private boolean mAmbient;
        private boolean mMute;
        private boolean mRegisteredTimeZoneReceiver = false;
        private Calendar mCalendar;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;

        /**
         * Whether the display is OLED and subject to pixel burn-in. When true, we display only
         * outlines and a 95%+ black screen.
         */
        private boolean mBurnInProtection;

        // Figure and head positioning offsets
        private static final int SANTA_FIGURE_OFFSET_X = -10;
        private static final int SANTA_FIGURE_OFFSET_Y = 30;
        private static final int SANTA_FIGURE_ADDITIONAL_OFFSET_X = -8;
        private static final float BORDER_WIDTH_PX = 3.0f;

        // Hyper-speed settings
        private static final int HYPERSPEED_DURATION_MS = 1500;
        private static final float HYPERSPEED_HANDOVER_EPSILON = 10f;
        private static final int HYPERSPEED_HOUR_TO_MINUTE_SPEED_RATIO = 2;

        private Bitmap[] mBackgroundBitmap;
        private Bitmap[] mFigureBitmap;
        private Bitmap[] mFaceBitmap;
        private Bitmap[] mHourHandBitmap;
        private Bitmap[] mMinuteHandBitmap;

        private Bitmap[] mCloudBitmaps;
        private int[] mCloudSpeeds;
        private int[] mCloudDegrees;

        private long mHyperSpeedStartTime = -1;
        private boolean[] mHyperSpeedOverrun;
        private int[] mCloudHyperSpeeds;

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

        private final BroadcastReceiver mTimeZoneReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        mCalendar.setTimeZone(TimeZone.getDefault());
                        invalidate();
                    }
                };

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            // noinspection deprecation Added for Wear 1.0 compatibility
            setWatchFaceStyle(
                    new WatchFaceStyle.Builder(SantaWatchFaceService.this)
                            .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                            .setBackgroundVisibility(
                                    WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                            .setShowSystemUiTime(false)
                            .setAcceptsTapEvents(true)
                            .build());

            init();
        }

        @Override
        public void onTapCommand(@TapType int tapType, int x, int y, long eventTime) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTapCommand");
            }
            super.onTapCommand(tapType, x, y, eventTime);

            if (System.currentTimeMillis() >= mHyperSpeedStartTime + HYPERSPEED_DURATION_MS
                    && !mAmbient
                    && tapType == TAP_TYPE_TAP) {
                mHyperSpeedStartTime = System.currentTimeMillis();
                for (int i = 0; i < mCloudBitmaps.length; i++) {
                    mHyperSpeedOverrun[i] = true;
                }
            }
        }

        /** Setting up settings variables (paint, settings, etc) and pre-load images */
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
            mCloudHyperSpeeds = mResources.getIntArray(R.array.cloudHyperSpeeds);
            mHyperSpeedOverrun = new boolean[mCloudBitmaps.length];

            // We need different paints because the alpha applies is different for different cloud
            for (int i = 0; i < mCloudBitmaps.length; i++) {
                Paint paint = new Paint();
                paint.setFilterBitmap(true);
                mCloudFilterPaints[i] = paint;
            }

            // Initialising time
            mCalendar = GregorianCalendar.getInstance();
        }

        /**
         * Loading all versions (interactive, ambient and low bit) into a bitmap array. The correct
         * version will be pluck out at runtime.
         *
         * @param arrayId Key to the type of bitmap that we are initialising. The full list can be
         *     found in res/values/images_santa_watchface.xml
         * @return Array of three bitmaps for interactive, ambient and low bit modes
         */
        private Bitmap[] loadBitmaps(int arrayId) {
            int[] bitmapIds = getIntArray(arrayId);
            Bitmap[] bitmaps = new Bitmap[bitmapIds.length];
            for (int i = 0; i < bitmapIds.length; i++) {
                bitmaps[i] = BitmapFactory.decodeResource(mResources, bitmapIds[i]);
            }
            return bitmaps;
        }

        /**
         * At runtime, this is used to load the appropriate bitmap depending on display mode
         * dynamically.
         *
         * @param bitmaps A bitmap array containing all bitmaps appropriate to all the display modes
         * @return Bitmap determined to be appropriate for the display mode
         */
        private Bitmap getBitmap(Bitmap[] bitmaps) {
            if (!mAmbient) {
                // Active mode
                return bitmaps[0];
            } else if (!mLowBitAmbient && !mBurnInProtection) {
                // Ambient mode
                return bitmaps[1];
            } else if (mBurnInProtection) {
                // Burn in protection mode
                return bitmaps[3];
            } else {
                // Low bit ambient mode
                return bitmaps[2];
            }
        }

        /**
         * Used to pick out whether the device is a low-bit device (i.e. pixels only capable of
         * displaying on and off in ambient mode) or requires burn-in protection (has an OLED screen
         * subject to pixel burn-out)
         *
         * @param properties device properties
         */
        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(
                        TAG,
                        "onPropertiesChanged: low-bit ambient = "
                                + mLowBitAmbient
                                + ", "
                                + "burn-in protection = "
                                + mBurnInProtection);
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
            } else {
                mHyperSpeedStartTime = -1;
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
            mCalendar.setTimeInMillis(System.currentTimeMillis());

            // Draw background.
            canvas.drawRect(0, 0, mWidth, mHeight, mAmbientBackgroundPaint);
            canvas.drawBitmap(getBitmap(mBackgroundBitmap), 0, 0, mFilterPaint);

            if (!mAmbient) {
                // Draw animation layer (above the background, below the figure and arms.)
                drawAnimationLayer(canvas, mCenterX, mCenterY);
            }

            mFigure = getBitmap(mFigureBitmap);

            // Draw figure.
            canvas.drawBitmap(
                    mFigure,
                    mCenterX - mFigure.getWidth() / 2 + mScaledXAdditionalOffset,
                    mCenterY - mFigure.getHeight() / 2 + mScaledYOffset,
                    mFilterPaint);

            if (mAmbient) {
                // Draw a black box as the peek card background
                canvas.drawRect(mCardBounds, mAmbientBackgroundPaint);
            }

            mMinutes = mCalendar.get(Calendar.MINUTE);
            mMinDeg = mMinutes * 6;
            mHrDeg = ((mCalendar.get(Calendar.HOUR_OF_DAY) + (mMinutes / 60f)) * 30);

            // HYPER SPEEEEEED
            if (System.currentTimeMillis() <= mHyperSpeedStartTime + HYPERSPEED_DURATION_MS) {
                // Spin the hour hand around for 1 rotation during the hyper speed cycle
                long hyperDeg =
                        (long)
                                (((System.currentTimeMillis() - mHyperSpeedStartTime)
                                                / (float) HYPERSPEED_DURATION_MS)
                                        * 360);
                // Spin the minute hand around based on the defined ratio
                mMinDeg = (mMinDeg + (hyperDeg * HYPERSPEED_HOUR_TO_MINUTE_SPEED_RATIO)) % 360;
                mHrDeg = (mHrDeg + hyperDeg) % 360;
            }

            canvas.save();

            drawComplications(canvas, mCalendar.getTimeInMillis());

            // Draw the minute hand
            canvas.rotate(mMinDeg, mCenterX, mCenterY);
            mMinHand = getBitmap(mMinuteHandBitmap);
            canvas.drawBitmap(
                    mMinHand,
                    mCenterX - mMinHand.getWidth() / 2f,
                    mCenterY - mMinHand.getHeight(),
                    mFilterPaint);

            // Draw the hour hand
            canvas.rotate(360 - mMinDeg + mHrDeg, mCenterX, mCenterY);
            mHrHand = getBitmap(mHourHandBitmap);
            canvas.drawBitmap(
                    mHrHand,
                    mCenterX - mHrHand.getWidth() / 2f,
                    mCenterY - mHrHand.getHeight(),
                    mFilterPaint);

            canvas.restore();

            // Draw face.  (We do this last so it's not obscured by the arms.)
            mFace = getBitmap(mFaceBitmap);
            canvas.drawBitmap(
                    mFace,
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
         * @param canvas Canvas to be drawn on
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

                    int speed = mCloudSpeeds[mLoop];
                    if (mHyperSpeedOverrun[mLoop]) {
                        speed = mCloudHyperSpeeds[mLoop];
                    }

                    mRadius = centerX - (mTimeElapsed / speed) % centerX;

                    // if hyper-speed has finished, but this cloud is still racing
                    if (System.currentTimeMillis() >= mHyperSpeedStartTime + HYPERSPEED_DURATION_MS
                            && mHyperSpeedOverrun[mLoop]) {
                        // then let it ride until it syncs up with its original trajectory, so the
                        // clouds appear to be moving smoothly.
                        float slowRadius = centerX - (mTimeElapsed / mCloudSpeeds[mLoop]) % centerX;
                        if (Math.abs(mRadius - slowRadius) < HYPERSPEED_HANDOVER_EPSILON) {
                            mHyperSpeedOverrun[mLoop] = false;
                        }
                    }

                    mCloudFilterPaints[mLoop].setAlpha((int) (mRadius / centerX * 255));

                    canvas.drawBitmap(
                            mCloudBitmaps[mLoop],
                            centerX,
                            centerY - mRadius,
                            mCloudFilterPaints[mLoop]);

                    canvas.restore();
                }
            }
        }

        /**
         * Scale bitmap array in place.
         *
         * @param bitmaps Bitmaps to be scaled
         * @param scale Scale factor. 1.0 represents the original size.
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
         * @param scale Scale factor. 1.0 represents the original size.
         * @return Scaled bitmap
         */
        private Bitmap scaleBitmap(Bitmap bitmap, float scale) {
            int width = (int) ((float) bitmap.getWidth() * scale);
            int height = (int) ((float) bitmap.getHeight() * scale);
            if (bitmap.getWidth() != width || bitmap.getHeight() != height) {
                return Bitmap.createScaledBitmap(bitmap, width, height, true /* filter */);
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
            array.recycle();
            return rc;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
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

        @SuppressWarnings("deprecation") // Added for Wear 1.0 Compatibility
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
