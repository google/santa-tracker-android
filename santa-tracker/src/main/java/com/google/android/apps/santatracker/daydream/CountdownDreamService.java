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

package com.google.android.apps.santatracker.daydream;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.launch.LaunchCountdown;
import com.google.android.apps.santatracker.village.VillageView;

import java.util.Random;

/**
 * Dream service to show Christmas countdown with waving Santa.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class CountdownDreamService extends DreamService
        implements LaunchCountdown.LaunchCountdownContext {

    private static final String LOG_TAG = "CountdownDreamService";

    private static final long MS = 1000L;
    private static final int MAX_SANTA_ANI_INTERVAL_IN_SEC = 20;

    final Random mRandom = new Random();

    private LaunchCountdown mCountDown;
    private DaydreamVillage mVillage;
    private Animation mWavingAnim;
    private Handler mWavingHandsHandler;

    private final Runnable mWavingAnimRunnable = new Runnable() {
        @Override
        public void run() {
            // keep waving
            Log.d(LOG_TAG, "Santa says 'hohooh'");
            waveSantaHand();
            mWavingHandsHandler.postDelayed(this, getRandomIntervalTime());
        }
    };

    @Override
    public void onDreamingStarted() {
        setContentView(R.layout.layout_daydream);
        initializeDreamView();

        enableCountdown(true);
        enableSantaAnimation(true);
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();

        enableCountdown(false);
        enableSantaAnimation(false);
    }

    @Override
    public void onCountdownFinished() {

        if (mCountDown != null) {
            mCountDown.cancel();
        }

        // Change background!
        ((ImageView)findViewById(R.id.villageBackground))
                .setImageResource(R.drawable.village_bg_launch);

        mVillage.setPlaneEnabled(false);
        findViewById(R.id.countdown_container).setVisibility(View.GONE);
        findViewById(R.id.santa_waving).setVisibility(View.GONE);

        enableSantaAnimation(false);
        enableCountdown(false);
    }

    @Override
    public View getCountdownView() {
        return findViewById(R.id.countdown_container);
    }

    @Override
    public Context getActivityContext() {
        // DreamService is not an Activity.
        return null;
    }

    private void initializeDreamView() {
        mVillage = new DaydreamVillage(this);
        mCountDown = new LaunchCountdown(this);
        mWavingAnim = AnimationUtils.loadAnimation(this, R.anim.santa_wave);

        VillageView villageView = (VillageView) findViewById(R.id.villageView);
        villageView.setVillage(mVillage);

        View countDownView = findViewById(R.id.countdown_container);
        countDownView.setVisibility(View.VISIBLE);

        mWavingHandsHandler = new Handler();
    }

    private void enableCountdown(boolean enable) {

        if (enable) {
            final long takeoffTime = getResources().getInteger(R.integer.santa_takeoff) * MS;
            final long currTime = System.currentTimeMillis();
            mCountDown.startTimer(takeoffTime - currTime);
        } else {
            mCountDown.cancel();
        }
    }


    private void enableSantaAnimation(boolean enable) {
        // remove any remaining mWavingAnimRunnable instance first.
        mWavingHandsHandler.removeCallbacks(mWavingAnimRunnable);

        if (enable) {
            mWavingHandsHandler.post(mWavingAnimRunnable);
        }
    }

    private void waveSantaHand() {
        findViewById(R.id.santa_arm).startAnimation(mWavingAnim);
    }

    private long getRandomIntervalTime() {
        return (5 + mRandom.nextInt(MAX_SANTA_ANI_INTERVAL_IN_SEC -5)) * MS;
    }
}