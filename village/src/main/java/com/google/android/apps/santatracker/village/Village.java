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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.google.android.apps.santatracker.util.AnalyticsManager;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Santa's village.
 */
public class Village extends Fragment implements VillageView.VillageInterface {

    private HorizontalScrollingImage mImageUfo;
    private HorizontalScrollingImage mImagePlane;
    private HorizontalScrollingImageGroup mImageClouds;
    private int mOffsetVertical = 0;
    private boolean mPlaneEnabled = true;
    private boolean mImagesInitialised = false;

    private TouchListener mTouchListener = new TouchListener();

    private VillageListener mCallback;
    private FirebaseAnalytics mMeasurement;

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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mMeasurement = FirebaseAnalytics.getInstance(getContext());
    }

    public void initialiseVillageViews() {
        Resources resources = getResources();
        if (!mImagesInitialised) {
            UiModeManager mgr = (UiModeManager) getActivity().
                    getApplicationContext().getSystemService(Context.UI_MODE_SERVICE);
            if (Configuration.UI_MODE_TYPE_WATCH == mgr.getCurrentModeType()) {
                int referenceHeight = resources.getInteger(R.integer.wear_referenceHeight);

                mImageUfo = new HorizontalScrollingImage(R.drawable.ufo, referenceHeight,
                        resources.getInteger(R.integer.wear_ufoVerticalOffset), false,
                        resources.getInteger(R.integer.wear_ufoPercentagePerSecond));

                mImagePlane = new HorizontalScrollingImage(R.drawable.plane, referenceHeight,
                        resources.getInteger(R.integer.wear_planeVerticalOffset), true,
                        resources.getInteger(R.integer.wear_planePercentagePerSecond));

                mImageClouds = new HorizontalScrollingImageGroup(R.drawable.cloud,
                        resources.getInteger(R.integer.wear_numClouds),
                        resources.getInteger(R.integer.wear_skyStart),
                        resources.getInteger(R.integer.wear_cloudsEnd),
                        resources.getInteger(R.integer.wear_cloudPercentagePerSecond),
                        resources.getInteger(R.integer.wear_cloudSpeedJitterPercent),
                        referenceHeight);

                mOffsetVertical = -1 * resources.getInteger(R.integer.wear_verticalOffset);
            } else {
                int referenceHeight = resources.getInteger(R.integer.referenceHeight);

                mImageUfo = new HorizontalScrollingImage(R.drawable.ufo, referenceHeight,
                        resources.getInteger(R.integer.ufoVerticalOffset), false,
                        resources.getInteger(R.integer.ufoPercentagePerSecond));

                mImagePlane = new HorizontalScrollingImage(R.drawable.plane, referenceHeight,
                        resources.getInteger(R.integer.planeVerticalOffset), true,
                        resources.getInteger(R.integer.planePercentagePerSecond));

                mImageClouds = new HorizontalScrollingImageGroup(R.drawable.cloud,
                        resources.getInteger(R.integer.numClouds),
                        resources.getInteger(R.integer.skyStart),
                        resources.getInteger(R.integer.cloudsEnd),
                        resources.getInteger(R.integer.cloudPercentagePerSecond),
                        resources.getInteger(R.integer.cloudSpeedJitterPercent),
                        referenceHeight);
            }

            mImagePlane.loadImages(resources);
            mImageClouds.loadImages(resources);

            mImagesInitialised = true;
        }

        // Set easter egg state to empty
        for (int i = 0; i < EGG_COUNT; i++) {
            mEasterEggTracker[i] = false;
        }
    }

    public void onDraw(Canvas canvas, int height, int width) {
        if (mPlaneEnabled) {
            mImagePlane.onDraw(canvas, height, 3 * width, mOffsetVertical);
        }
        mImageClouds.onDraw(canvas, height, width, mOffsetVertical);
        mImageUfo.onDraw(canvas, height, width, mOffsetVertical);
    }

    public TouchListener getTouchListener() {
        return mTouchListener;
    }

    public void setPlaneEnabled(boolean planeEnabled) {
        mPlaneEnabled = planeEnabled;
    }

    public class TouchListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            boolean handled = false;
            if (mImagePlane.isTouched(Math.round(e.getX()), Math.round(e.getY()))) {
                trackClick(R.string.analytics_event_village_plane);
                easterEggProgress(EGG_PLANE);
                handled = true;
            } else if (mImageUfo.isTouched(Math.round(e.getX()), Math.round(e.getY()))) {
                trackClick(R.string.analytics_event_village_ufo);
                easterEggInteraction();
                handled = true;
            }
            if (!handled) {
                handled = super.onDown(e);
            }
            return handled;
        }
    }

    private static final int EGG_PLANE = 0;
    private static final int EGG_COUNT = 1;
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

        // App Measurement
        MeasurementManager.recordCustomEvent(mMeasurement,
                getString(R.string.analytics_event_category_village),
                getString(R.string.analytics_event_village_unlock_easteregg), null);

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
        // App Measurement
        MeasurementManager.recordCustomEvent(mMeasurement,
                getString(R.string.analytics_event_category_village),
                getString(R.string.analytics_event_village_click),
                getString(resId));

        // [ANALYTICS EVENT]: Village Click
        AnalyticsManager.sendEvent(R.string.analytics_event_category_village,
                R.string.analytics_event_village_click,
                resId);
    }

    public interface VillageListener {
        void playSoundOnce(int resSoundId);
    }
}