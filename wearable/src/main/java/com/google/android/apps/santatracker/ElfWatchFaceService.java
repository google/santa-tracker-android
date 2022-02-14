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
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * The base class for all Santa Tracker watch faces, which are analog watch faces with a static
 * background, one or more animated background layers, a central figure, a face which is always in
 * the foreground, and two rotating arms. In ambient mode, the image is shown in greyscale. On
 * devices with low-bit ambient mode, the image is drawn without anti-aliasing in ambient mode.
 */
public class ElfWatchFaceService extends AbstractBaseWatchFaceService {

    private static final String TAG = "ElfWatchFaceService";
    static final int MSG_UPDATE_TIME = 0;

    // Declared a static class and keep a weak reference to avoid memory leak
    private static class TimeUpdateHandler extends Handler {

        private final WeakReference<CanvasWatchFaceService.Engine> mWatchFaceEngine;

        TimeUpdateHandler(CanvasWatchFaceService.Engine watchFaceEngine) {
            mWatchFaceEngine = new WeakReference<>(watchFaceEngine);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_UPDATE_TIME:
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.v(TAG, "updating time");
                    }
                    CanvasWatchFaceService.Engine watchFaceService = mWatchFaceEngine.get();
                    if (null != watchFaceService) {
                        watchFaceService.invalidate();
                    }
                    break;
            }
        }
    }

    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {

        return new Engine();
    }

    protected class Engine extends AbstractBaseWatchFaceService.Engine {

        private final Resources mResources = getResources();
        private Paint mFilterPaint;
        private Paint mAmbientBackgroundPaint;
        private Paint mAmbientPeekCardBorderPaint;
        private boolean mAmbient;
        private boolean mMute;
        private Calendar mCalendar;
        private boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        /**
         * Whether the display is OLED and subject to pixel burn-in. When true, we display only
         * outlines and a 95%+ black screen.
         */
        private boolean mBurnInProtection;

        // Figure and head positioning offsets
        private static final int SANTA_FIGURE_OFFSET_X = 5;
        private static final int SANTA_FIGURE_OFFSET_Y = 28;
        private static final int SANTA_FIGURE_ADDITIONAL_OFFSET_X = -5;
        private static final float BORDER_WIDTH_PX = 3.0f;
        private static final int UFO_DURATION_MS = 700;
        private static final int UFO_DISTANCE = 2; // bigger is farther
        private static final float UFO_OFFSET_MIN_Y = 0f;
        private static final float UFO_OFFSET_MAX_Y = 0.4f;

        private Bitmap[] mBackgroundBitmap;
        private Bitmap[] mFigureBitmap;
        private Bitmap[] mFaceBitmap;
        private Bitmap[] mHourHandBitmap;
        private Bitmap[] mMinuteHandBitmap;
        private Bitmap mUfoBitmap;

        private final Rect mCardBounds = new Rect();

        private static final int HOUR_MORNING = 6;
        private static final int HOUR_DAY = 10;
        private static final int HOUR_EVENING = 17;
        private static final int HOUR_NIGHT = 20;

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
        private long mUfoFlightStart = -1;
        private float mUfoHeight;

        private final BroadcastReceiver mTimeZoneReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        mCalendar.setTimeZone(TimeZone.getDefault());
                        invalidate();
                    }
                };

        /** Handler to update the time once a second in interactive mode. */
        final TimeUpdateHandler mUpdateTimeHandler = new TimeUpdateHandler(this);

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            // noinspection deprecation Wear 1.0 compatibility
            setWatchFaceStyle(
                    new WatchFaceStyle.Builder(ElfWatchFaceService.this)
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

            if (!mAmbient
                    && tapType == TAP_TYPE_TAP
                    && System.currentTimeMillis() >= mUfoFlightStart + UFO_DURATION_MS) {
                mUfoFlightStart = System.currentTimeMillis();
                mUfoHeight =
                        (float)
                                (Math.random() * (UFO_OFFSET_MAX_Y - UFO_OFFSET_MIN_Y)
                                        + UFO_OFFSET_MIN_Y);
                invalidate();
            }
        }

        /** Setting up settings variables (paint, settings, etc) and pre-load images */
        private void init() {

            // Pre-loading bitmaps
            mBackgroundBitmap = loadBitmaps(R.array.elfBackgroundIds);
            mFigureBitmap = loadBitmaps(R.array.elfFigureIds);
            mFaceBitmap = loadBitmaps(R.array.elfFaceIds);
            mHourHandBitmap = loadBitmaps(R.array.elfHourHandIds);
            mMinuteHandBitmap = loadBitmaps(R.array.elfMinuteHandIds);

            // Initialising paint object for Bitmap draws
            mFilterPaint = new Paint();
            mFilterPaint.setFilterBitmap(true);

            // Initialising background paint
            mAmbientBackgroundPaint = new Paint();
            mAmbientBackgroundPaint.setARGB(255, 0, 0, 0);
            mAmbientPeekCardBorderPaint = new Paint();
            mAmbientPeekCardBorderPaint.setColor(Color.WHITE);
            mAmbientPeekCardBorderPaint.setStrokeWidth(BORDER_WIDTH_PX);

            // Load UFO and pre-scale
            Bitmap fullSizeUfo = BitmapFactory.decodeResource(getResources(), R.drawable.ufo);

            mUfoBitmap =
                    Bitmap.createScaledBitmap(
                            fullSizeUfo,
                            fullSizeUfo.getWidth() / UFO_DISTANCE,
                            fullSizeUfo.getHeight() / UFO_DISTANCE,
                            true);

            // Initialising time
            mCalendar = GregorianCalendar.getInstance();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
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
         * At runtime, this is used to load the appropriate background bitmap depending on display
         * mode and time of day.
         *
         * @param bitmaps A bitmap array containing all bitmaps appropriate to all the display modes
         * @return Bitmap determined to be appropriate for the display mode
         */
        private Bitmap getBackgroundBitmap(Bitmap[] bitmaps) {
            if (!mAmbient) {
                int hour = mCalendar.get(Calendar.HOUR_OF_DAY);
                if (hour >= HOUR_NIGHT || hour < HOUR_MORNING) {
                    return bitmaps[6];
                } else if (hour >= HOUR_EVENING) {
                    return bitmaps[5];
                } else if (hour >= HOUR_DAY) {
                    return bitmaps[0];
                } else if (hour >= HOUR_MORNING) {
                    return bitmaps[4];
                } else {
                    return bitmaps[0];
                }
            } else {
                return getBitmap(bitmaps);
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
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            Log.v(TAG, "onAmbientModeChanged, " + (inAmbientMode ? "ambient" : "ACTIVE"));
            super.onAmbientModeChanged(inAmbientMode);
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

            long frameStartTimeMs = System.currentTimeMillis();

            mCalendar.setTimeInMillis(frameStartTimeMs);

            // Draw background.
            canvas.drawRect(0, 0, mWidth, mHeight, mAmbientBackgroundPaint);
            canvas.drawBitmap(getBackgroundBitmap(mBackgroundBitmap), 0, 0, mFilterPaint);

            boolean redrawUFO = false;

            // Draw UFO.
            if (!mAmbient && System.currentTimeMillis() <= mUfoFlightStart + UFO_DURATION_MS) {
                redrawUFO = true;
                float distancePercent =
                        (System.currentTimeMillis() - mUfoFlightStart) / (float) UFO_DURATION_MS;
                long width = mWidth;
                float x = distancePercent * width - mUfoBitmap.getWidth();
                float y = mUfoHeight * mHeight;
                canvas.drawBitmap(mUfoBitmap, x, y, null);
            }

            mMinutes = mCalendar.get(Calendar.MINUTE);
            mMinDeg = mMinutes * 6;
            mHrDeg = ((mCalendar.get(Calendar.HOUR_OF_DAY) + (mMinutes / 60f)) * 30);

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

            if (redrawUFO) {
                invalidate();
            } else if (!mAmbient) {
                long delayMs = System.currentTimeMillis() - frameStartTimeMs;
                if (delayMs > INTERACTIVE_UPDATE_RATE_MS) {
                    // This scenario occurs when drawing all of the components takes longer than an
                    // actual frame. It may be helpful to log how many times this happens, so you
                    // can fix it when it occurs.
                    // In general, you don't want to redraw immediately, but on the next
                    // appropriate frame (else block below).
                    delayMs = 0;
                } else {
                    // Sets the delay as close as possible to the intended frame rate.
                    // Note that the recommended interactive update rate is 1 frame per second.
                    // However, if you want to include the sweeping hand gesture, set the
                    // interactive update rate up to 30 frames per second.
                    delayMs = INTERACTIVE_UPDATE_RATE_MS - delayMs;
                }
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
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
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            } else {
                mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
                unregisterReceiver();
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            ElfWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            ElfWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @SuppressWarnings("deprecation") // Wear 1.0 compatibility
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

    @Override
    protected Class<?> getWatchFaceService() {
        return ElfWatchFaceService.class;
    }
}
