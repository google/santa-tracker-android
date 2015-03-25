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

package com.google.android.apps.santatracker.village;

import com.google.android.apps.santatracker.village.R;
import com.google.android.apps.santatracker.util.AnalyticsManager;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;

/**
 * Santa's village.
 */
public class Village extends Fragment {

    private StretchedFullWidthImage mImageSkyDay;
    private StretchedFullWidthImage mImageSkyNight;
    private HorizontalScrollingImage mImageUfo;
    private SimpleImage mImageSun;
    private SimpleImage mImageMoon;
    private HorizontalScrollingImage mImagePlane;
    private HorizontalScrollingImageGroup mImageClouds;
    private HorizontallyTiledImage mImageMountainsDay;
    private HorizontallyTiledImage mImageMountainsNight;
    private SolidPaint mPaintMountainsDay;
    private SolidPaint mPaintMountainsNight;
    private HorizontalScrollingImage mImageMonoRail;
    private HorizontallyRepeatingImage mImageRail;
    private HorizontallyTiledImage mImageVillage;
    private HorizontallyTiledImage mImageSnow;
    private SolidPaint mPaintSnow;
    private int mViewHeight;
    private float mTimedOffsetHorizontal;
    private float mMaxOffsetHorizontalVillage;
    private int mOffsetVertical = 0;
    private boolean mImagesInitialised = false;
    private float mScrollPerSecond;
    private long mLastFrameTime = 0;
    private int sunOffset;
    private int moonOffset;
    private TouchListener mTouchListener = new TouchListener();
    private AnimatorSet mFinalAnimations;
    private boolean mDirectionForward = false;
    private boolean mAnimate = true;

    private VillageListener mCallback;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (VillageListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + "must implement VillageInterface.");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public int[] getIntArray(Resources resources, int resId) {
        TypedArray array = resources.obtainTypedArray(resId);
        int[] rc = new int[array.length()];
        TypedValue value = new TypedValue();
        for (int i = 0; i < array.length(); i++) {
            array.getValue(i, value);
            rc[i] = value.resourceId;
        }
        array.recycle();
        return rc;
    }

    private int getOffset(int resId) {
        return getResources().getInteger(resId);
    }

    private float getParallax(int resId) {
        return ((float) getResources().getInteger(resId))
                / ((float) getResources().getInteger(R.integer.parallaxDivider));
    }

    public void initialiseVillageViews(int viewHeight, int viewWidth) {
        mViewHeight = viewHeight;
        int maxDimension = viewHeight > viewWidth ? viewHeight : viewWidth;
        Resources resources = getResources();
        if (!mImagesInitialised) {
            UiModeManager mgr = (UiModeManager) getActivity().
                    getApplicationContext().getSystemService(Context.UI_MODE_SERVICE);
            if (Configuration.UI_MODE_TYPE_WATCH == mgr.getCurrentModeType()) {
                int referenceHeight = resources.getInteger(R.integer.wear_referenceHeight);

                mImageSkyDay = new StretchedFullWidthImage(R.drawable.sky_day,
                        resources.getInteger(R.integer.wear_skyReferenceHeight),
                        resources.getInteger(R.integer.wear_skyStart));
                mImageSkyNight = new StretchedFullWidthImage(R.drawable.sky_night,
                        resources.getInteger(R.integer.wear_skyReferenceHeight),
                        resources.getInteger(R.integer.wear_skyStart));
                mImageUfo = new HorizontalScrollingImage(R.drawable.ufo, referenceHeight,
                        resources.getInteger(R.integer.wear_ufoVerticalOffset), false,
                        resources.getInteger(R.integer.wear_ufoPercentagePerSecond));
                mImageSun = new SimpleImage(R.drawable.sun, resources
                        .getInteger(R.integer.wear_sunReferenceHeight), resources
                        .getInteger(R.integer.wear_sunVerticalOffset), resources
                        .getInteger(R.integer.wear_sunHorizontalOffset));
                mImageMoon = new SimpleImage(R.drawable.moon, resources
                        .getInteger(R.integer.wear_sunReferenceHeight), resources
                        .getInteger(R.integer.wear_moonVerticalOffset), resources
                        .getInteger(R.integer.wear_moonHorizontalOffset));
                mImagePlane = new HorizontalScrollingImage(R.drawable.plane, referenceHeight,
                        resources.getInteger(R.integer.wear_planeVerticalOffset), true,
                        resources.getInteger(R.integer.wear_planePercentagePerSecond));

                mImageClouds = new HorizontalScrollingImageGroup(R.drawable.cloud,
                        resources.getInteger(R.integer.wear_numClouds),
                        resources.getInteger(R.integer.wear_sunVerticalOffset),
                        resources.getInteger(R.integer.wear_cloudsEnd),
                        getParallax(R.integer.wear_cloudsParallax), referenceHeight);
                mImageMountainsDay = loadImage(R.array.wear_mountainsDayIds, referenceHeight,
                        R.integer.wear_mountainsStart, R.integer.wear_mountainsParallax,
                        R.integer.wear_med_res);
                mImageMountainsNight = loadImage(R.array.wear_mountainsNightIds, referenceHeight,
                        R.integer.wear_mountainsStart, R.integer.wear_mountainsParallax,
                        R.integer.wear_med_res);
                mPaintMountainsDay = new SolidPaint(getString(R.color.colorMountainsDay),
                        resources.getInteger(R.integer.wear_mountainsPaintStart),
                        resources.getInteger(R.integer.wear_mountainsEnd), referenceHeight);
                mPaintMountainsNight = new SolidPaint(getString(R.color.colorMountainsNight),
                        resources.getInteger(R.integer.wear_mountainsPaintStart),
                        resources.getInteger(R.integer.wear_mountainsEnd), referenceHeight);

                mImageMonoRail = new HorizontalScrollingImage(R.drawable.monorail, referenceHeight,
                        resources.getInteger(R.integer.wear_monorailStart), false,
                        resources.getInteger(R.integer.wear_monorailPercentagePerSecond));
                mImageRail = new HorizontallyRepeatingImage(R.drawable.rail, referenceHeight,
                        resources.getInteger(R.integer.wear_railStart),
                        getParallax(R.integer.wear_railParallax));

                mImageVillage = loadImage(R.array.wear_villageIds, referenceHeight,
                        R.integer.wear_villageStart, R.integer.wear_villageParallax,
                        R.integer.wear_high_res);
                mImageSnow = loadImage(R.array.wear_snowIds, referenceHeight,
                        R.integer.wear_snowStart, R.integer.wear_snowParallax,
                        R.integer.wear_low_res);
                mPaintSnow = new SolidPaint(getString(R.color.colorSnow),
                        resources.getInteger(R.integer.wear_snowPaintStart),
                        resources.getInteger(R.integer.wear_snowEnd), referenceHeight);

                mOffsetVertical = -1 * resources.getInteger(R.integer.wear_verticalOffset);
            } else {
                int referenceHeight = resources.getInteger(R.integer.referenceHeight);

                mImageSkyDay = new StretchedFullWidthImage(R.drawable.sky_day,
                        resources.getInteger(R.integer.skyReferenceHeight),
                        resources.getInteger(R.integer.skyStart));
                mImageSkyNight = new StretchedFullWidthImage(R.drawable.sky_night,
                        resources.getInteger(R.integer.skyReferenceHeight),
                        resources.getInteger(R.integer.skyStart));
                mImageUfo = new HorizontalScrollingImage(R.drawable.ufo, referenceHeight,
                        resources.getInteger(R.integer.ufoVerticalOffset), false,
                        resources.getInteger(R.integer.ufoPercentagePerSecond));
                mImageSun = new SimpleImage(R.drawable.sun, resources
                        .getInteger(R.integer.sunReferenceHeight), resources
                        .getInteger(R.integer.sunVerticalOffset), resources
                        .getInteger(R.integer.sunHorizontalOffset));
                mImageMoon = new SimpleImage(R.drawable.moon, resources
                        .getInteger(R.integer.sunReferenceHeight), resources
                        .getInteger(R.integer.moonVerticalOffset), resources
                        .getInteger(R.integer.moonHorizontalOffset));
                mImagePlane = new HorizontalScrollingImage(R.drawable.plane, referenceHeight,
                        resources.getInteger(R.integer.planeVerticalOffset), true,
                        resources.getInteger(R.integer.wear_planePercentagePerSecond));

                mImageClouds = new HorizontalScrollingImageGroup(R.drawable.cloud,
                        resources.getInteger(R.integer.numClouds),
                        resources.getInteger(R.integer.sunVerticalOffset),
                        resources.getInteger(R.integer.cloudsEnd),
                        getParallax(R.integer.cloudsParallax), referenceHeight);
                mImageMountainsDay = loadImage(R.array.mountainsDayIds, referenceHeight,
                        R.integer.mountainsStart, R.integer.mountainsParallax,
                        R.integer.med_res);
                mImageMountainsNight = loadImage(R.array.mountainsNightIds, referenceHeight,
                        R.integer.mountainsStart, R.integer.mountainsParallax,
                        R.integer.med_res);
                mPaintMountainsDay = new SolidPaint(getString(R.color.colorMountainsDay),
                        resources.getInteger(R.integer.mountainsPaintStart),
                        resources.getInteger(R.integer.mountainsEnd), referenceHeight);
                mPaintMountainsNight = new SolidPaint(getString(R.color.colorMountainsNight),
                        resources.getInteger(R.integer.mountainsPaintStart),
                        resources.getInteger(R.integer.mountainsEnd), referenceHeight);

                mImageMonoRail = new HorizontalScrollingImage(R.drawable.monorail, referenceHeight,
                        resources.getInteger(R.integer.monorailStart), false,
                        resources.getInteger(R.integer.wear_monorailPercentagePerSecond));
                mImageRail = new HorizontallyRepeatingImage(R.drawable.rail, referenceHeight,
                        resources.getInteger(R.integer.railStart),
                        getParallax(R.integer.railParallax));

                mImageVillage = loadImage(R.array.villageIds, referenceHeight,
                        R.integer.villageStart, R.integer.villageParallax,
                        R.integer.high_res);
                mImageSnow = loadImage(R.array.snowIds, referenceHeight,
                        R.integer.snowStart, R.integer.snowParallax,
                        R.integer.low_res);
                mPaintSnow = new SolidPaint(getString(R.color.colorSnow),
                        resources.getInteger(R.integer.snowPaintStart),
                        resources.getInteger(R.integer.snowEnd), referenceHeight);
            }

            mImageSkyDay.loadImages(resources);
            mImageSkyNight.loadImages(resources);
            mImageUfo.loadImages(resources);
            mImageSun.loadImages(resources);
            mImageMoon.loadImages(resources);
            mImagePlane.loadImages(resources);

            mImageClouds.loadImages(resources);
            mImageMountainsDay.loadImages(resources);
            mImageMountainsNight.loadImages(resources);

            mImageMonoRail.loadImages(resources);
            mImageRail.loadImages(resources);

            mImageVillage.loadImages(resources);
            mImageSnow.loadImages(getResources());

            mImagesInitialised = true;

            setIsDay(initialiseSunMoon(), false);
        }

        // Set easter egg state to empty
        for (int i = 0; i < EGG_COUNT; i++) {
            mEasterEggTracker[i] = false;
        }
        mImageUfo.setAlpha(ImageWithAlphaAndSize.INVISIBLE);

        mMaxOffsetHorizontalVillage = mImageVillage.geTotalWidthScaled(viewHeight) / 2;
        int percentPerSecond = getResources().getInteger(
                R.integer.scrollPercentagePerSecond);
        mScrollPerSecond = (float) (percentPerSecond) / 100f * maxDimension;
    }

    private boolean initialiseSunMoon() {
        boolean isDay = false;
        sunOffset = mViewHeight;
        moonOffset = 0;
        // 6am till 6pm is considered day time;
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        if (today.hour >= 6 || today.hour < 18) {
            isDay = true;
            sunOffset = 0;
            moonOffset = -mViewHeight;
        }
        return isDay;
    }

    private HorizontallyTiledImage loadImage(int idsArray, int referenceHeight,
            int verticalOffset, int parallax, int sampleSize) {
        return new HorizontallyTiledImage(getIntArray(getResources(), idsArray),
                referenceHeight, getOffset(verticalOffset),
                getParallax(parallax), getResources().getInteger(sampleSize));
    }

    public void onDraw(Canvas canvas, int height, int width) {
        int offsetHorizontal = Math.round(mTimedOffsetHorizontal);

        mImageSkyNight.onDraw(canvas, height, width, mOffsetVertical);
        mImageSkyDay.onDraw(canvas, height, width, mOffsetVertical);

        mImageMoon.onDraw(canvas, height, mOffsetVertical + moonOffset);
        mImageSun.onDraw(canvas, height, mOffsetVertical + sunOffset);

        mImagePlane.onDraw(canvas, height, width, mImageVillage.geTotalWidthScaled(height),
                mOffsetVertical, offsetHorizontal);

        mImageClouds.onDraw(canvas, height, width, mOffsetVertical);

        mImageMountainsNight.onDraw(canvas, height, width, mOffsetVertical, offsetHorizontal);
        mPaintMountainsNight.onDraw(canvas, height, width, mOffsetVertical);
        mImageMountainsDay.onDraw(canvas, height, width, mOffsetVertical, offsetHorizontal);
        mPaintMountainsDay.onDraw(canvas, height, width, mOffsetVertical);

        mImageRail.onDraw(canvas, height, width, mOffsetVertical, offsetHorizontal);
        mImageMonoRail.onDraw(canvas, height, width, mImageVillage.geTotalWidthScaled(height),
                mOffsetVertical, offsetHorizontal);

        mImageVillage.onDraw(canvas, height, width, mOffsetVertical, offsetHorizontal);
        mImageSnow.onDraw(canvas, height, width, mOffsetVertical, offsetHorizontal);
        mPaintSnow.onDraw(canvas, height, width, mOffsetVertical);

        // UFO shouldn't fly across the full village width. Lets do 3 screen widths
        mImageUfo.onDraw(canvas, height, width, 3 * width, mOffsetVertical, 0);

        long currentTime = System.currentTimeMillis();
        float offsetIncrement;
        if (mLastFrameTime != 0) {
            offsetIncrement = (float) ((currentTime - mLastFrameTime)) / 1000f
                    * mScrollPerSecond;
        } else {
            offsetIncrement = 0;
        }

        if (mAnimate) {
            if (mDirectionForward) {
                mTimedOffsetHorizontal += offsetIncrement; // Offset image
                // If the offset has hit the end, reverse direction
                if (mTimedOffsetHorizontal >= mMaxOffsetHorizontalVillage) {
                    mDirectionForward = false;
                    mTimedOffsetHorizontal = mMaxOffsetHorizontalVillage;
                }
            } else {
                mTimedOffsetHorizontal -= offsetIncrement;
                if (mTimedOffsetHorizontal <= -mMaxOffsetHorizontalVillage) {
                    mDirectionForward = true;
                    mTimedOffsetHorizontal = -mMaxOffsetHorizontalVillage;
                }
            }
        }
        mLastFrameTime = currentTime;
    }

    public void enableAnimation(boolean animate) {
        mAnimate = animate;
    }

    private void setIsDay(final boolean isDay, boolean smoothTransition) {
        ObjectAnimator sunAnim = ObjectAnimator.ofInt(this, "sunOffset",
                sunOffset, isDay ? 0 : mViewHeight);
        sunAnim.setInterpolator(new AnticipateOvershootInterpolator());
        sunAnim.setDuration(smoothTransition ? ImageWithAlphaAndSize.ANIM_DURATION : 0);
        sunAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (isDay) {
                    mImageSun.setAlpha(ImageWithAlphaAndSize.OPAQUE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isDay) {
                    mImageSun.setAlpha(ImageWithAlphaAndSize.INVISIBLE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        AnimatorSet dayAnims = new AnimatorSet();
        dayAnims.playTogether(
                // Day values
                mImageSkyDay.fadeTransition(isDay, smoothTransition),
                mImageMountainsDay.fadeTransition(isDay, smoothTransition),
                mPaintMountainsDay.fadeTransition(isDay, smoothTransition)
        );

        ObjectAnimator moonAnim = ObjectAnimator.ofInt(this, "moonOffset",
                moonOffset, isDay ? -mViewHeight : 0);
        moonAnim.setInterpolator(new AnticipateOvershootInterpolator());
        moonAnim.setDuration(smoothTransition ? ImageWithAlphaAndSize.ANIM_DURATION : 0);
        moonAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (!isDay) {
                    mImageMoon.setAlpha(ImageWithAlphaAndSize.OPAQUE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (isDay) {
                    mImageMoon.setAlpha(ImageWithAlphaAndSize.INVISIBLE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        AnimatorSet nightAnims = new AnimatorSet();
        nightAnims.playTogether(
                // Night values
                mImageSkyNight.fadeTransition(!isDay, !isDay && smoothTransition),
                mImageMountainsNight.fadeTransition(!isDay, !isDay && smoothTransition),
                mPaintMountainsNight.fadeTransition(!isDay, !isDay && smoothTransition)
        );
        // When going to the day, delay night animation till after day time has kicked in
        if (isDay) {
            nightAnims.setStartDelay(ImageWithAlphaAndSize.ANIM_DURATION);
        }

        mFinalAnimations = nightAnims;

        sunAnim.start();
        dayAnims.start();
        moonAnim.start();
        nightAnims.start();
    }

    @SuppressWarnings("unused") // used reflectively by object animator
    public int getSunOffset() {
        return sunOffset;
    }

    @SuppressWarnings("unused") // used reflectively by object animator
    public void setSunOffset(int sunOffset) {
        this.sunOffset = sunOffset;
    }

    @SuppressWarnings("unused") // used reflectively by object animator
    public int getMoonOffset() {
        return moonOffset;
    }

    @SuppressWarnings("unused") // used reflectively by object animator
    public void setMoonOffset(int moonOffset) {
        this.moonOffset = moonOffset;
    }

    public TouchListener getTouchListener() {
        return mTouchListener;
    }

    public class TouchListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            boolean handled = false;
            // Ignore touch events if one of our animations is already in progress
            if (mFinalAnimations == null || !mFinalAnimations.isStarted()) {
                if (mImageSun.isTouched(Math.round(e.getX()), Math.round(e.getY()))) {
                    // transition from day to night
                    setIsDay(false, true);
                    trackClick(R.string.analytics_event_village_sun);
                    easterEggProgress(EGG_SUN);
                    handled = true;
                } else if (mImageMoon.isTouched(Math.round(e.getX()), Math.round(e.getY()))) {
                    // transition from night to day
                    setIsDay(true, true);
                    trackClick(R.string.analytics_event_village_moon);
                    easterEggProgress(EGG_MOON);
                    handled = true;
                } else if (mImagePlane.isTouched(Math.round(e.getX()), Math.round(e.getY()))) {
                    trackClick(R.string.analytics_event_village_plane);
                    easterEggProgress(EGG_PLANE);
                    handled = true;
                } else if (mImageUfo.isTouched(Math.round(e.getX()), Math.round(e.getY()))) {
                    trackClick(R.string.analytics_event_village_ufo);
                    easterEggInteraction();
                    handled = true;
                }
            }
            if (!handled) {
                handled = super.onDown(e);
            }
            return handled;
        }
    }

    static private final int EGG_SUN = 0;
    static private final int EGG_MOON = 1;
    static private final int EGG_PLANE = 2;
    static private final int EGG_COUNT = 3;
    private boolean[] mEasterEggTracker = new boolean[EGG_COUNT];

    private void easterEggProgress(int eggIndex) {
        int eggsFound = 0;
        for (int i = 0; i < EGG_COUNT; i++) {
            if (mEasterEggTracker[i]) {
                eggsFound++;
            }
        }

        if (!mEasterEggTracker[eggIndex]) {
            mEasterEggTracker[eggIndex] = true;
            eggsFound++;
            // Play sound
            switch (eggsFound) {
                case 1:
                    mCallback.playSoundOnce(R.raw.confirm1);
                    break;
                case 2:
                    mCallback.playSoundOnce(R.raw.confirm2);
                    break;
                case 3:
                    mCallback.playSoundOnce(R.raw.confirm3);
                    break;
            }
        }

        if (eggsFound == EGG_COUNT) {
            showEasterEgg();
            // reset easter egg state
            for (int i = 0; i < EGG_COUNT; i++) {
                mEasterEggTracker[i] = false;
            }

        }
    }

    private void showEasterEgg() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(mImageUfo, "size", 0f, 1.0f);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.setDuration(ImageWithAlphaAndSize.ANIM_DURATION);
        anim.start();
        mImageUfo.setAlpha(ImageWithAlphaAndSize.OPAQUE);
        // [ANALYTICS EVENT]: Village Click
        AnalyticsManager.sendEvent(R.string.analytics_event_category_village,
                R.string.analytics_event_village_unlock_easteregg);
    }

    // Make the UFO spin around and fly away
    private void easterEggInteraction() {
        mCallback.playSoundOnce(R.raw.easter_egg);

        // Fade into the distance
        ObjectAnimator anim = ObjectAnimator.ofFloat(mImageUfo, "size", 1.0f, 0f);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.setDuration(ImageWithAlphaAndSize.ANIM_DURATION);
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mImageUfo.setAlpha(ImageWithAlphaAndSize.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        anim.start();
    }

    private void trackClick(int resId) {
        // [ANALYTICS EVENT]: Village Click
        AnalyticsManager.sendEvent(R.string.analytics_event_category_village,
                R.string.analytics_event_village_click,
                resId);
    }

    public interface VillageListener {
        public void playSoundOnce(int resSoundId);
    }
}