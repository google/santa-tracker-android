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

package com.google.android.apps.santatracker.launch;

import android.content.Context;
import android.os.Build;
import android.os.CountDownTimer;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.android.apps.santatracker.R;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.GregorianCalendar;

public class LaunchCountdown {

    /**
     * Listener for LaunchCountdown and convenience method to draw CountDown UI itself.
     */
    public interface LaunchCountdownContext{
        void onCountdownFinished();
        View getCountdownView();
        Context getActivityContext();
        Context getApplicationContext();
    }

    private CountDownTimer mTimer;

    private TextView mTvDays;
    private TextView mTvDays2;
    private TextView mTvHours;
    private TextView mTvHours2;
    private TextView mTvMinutes;
    private TextView mTvMinutes2;
    private TextView mTvSeconds;
    private TextView mTvSeconds2;

    boolean mDaysPrimary = true;
    Animation mDaysIn, mDaysOut;
    boolean mHoursPrimary = true;
    Animation mHoursIn, mHoursOut;
    boolean mMinutesPrimary = true;
    Animation mMinutesIn, mMinutesOut;
    boolean mSecondsPrimary = true;
    Animation mSecondsIn, mSecondsOut;
    long mCountdownTime = -1;

    private WeakReference<LaunchCountdownContext> mLaunchContextRef;

    public LaunchCountdown(LaunchCountdownContext context) {
        mLaunchContextRef = new WeakReference<>(context);
        final View countdownView = context.getCountdownView();
        mTvDays = (TextView) countdownView.findViewById(R.id.countdown_days_1);
        mTvDays2 = (TextView) countdownView.findViewById(R.id.countdown_days_2);
        mTvHours = (TextView) countdownView.findViewById(R.id.countdown_hours_1);
        mTvHours2 = (TextView) countdownView.findViewById(R.id.countdown_hours_2);
        mTvMinutes = (TextView) countdownView.findViewById(R.id.countdown_minutes_1);
        mTvMinutes2 = (TextView) countdownView.findViewById(R.id.countdown_minutes_2);
        mTvSeconds = (TextView) countdownView.findViewById(R.id.countdown_seconds_1);
        mTvSeconds2 = (TextView) countdownView.findViewById(R.id.countdown_seconds_2);
    }

    /**
     * Starts the countdown timer.
     */
    public void startTimer(long timer) {
        final LaunchCountdownContext launchContext = mLaunchContextRef.get();
        if (timer < 0 || Math.abs(timer - mCountdownTime) < 1000 || launchContext == null) {
            return;
        }

        mCountdownTime = timer;
        // cancel timer if already running
        cancel();

        final Context context = launchContext.getApplicationContext();

        mDaysIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
        mDaysOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_top);
        mHoursIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
        mHoursOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_top);
        mMinutesIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
        mMinutesOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_top);
        mSecondsIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
        mSecondsOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_top);

        mTimer = new CountDownTimer(mCountdownTime, 1000) {
            private DecimalFormat mDF = new DecimalFormat("00");
            private boolean initialRound = true;

            @Override
            public void onTick(long millisUntilFinished) {

                int iDays = (int) Math.floor(millisUntilFinished / (24 * 60 * 60 * 1000));
                int iHours = (int) Math.floor(millisUntilFinished / (60 * 60 * 1000) % 24);
                int iMinutes = (int) Math.floor(millisUntilFinished / (60 * 1000) % 60);
                int iSeconds = (int) Math.floor(millisUntilFinished / (1000) % 60);

                GregorianCalendar calendar = new GregorianCalendar();
                calendar.setTimeInMillis(millisUntilFinished);

                String days = mDF.format(iDays);
                if (animateValue(days, mTvDays, mTvDays2, mDaysIn, mDaysOut, mDaysPrimary,
                        initialRound)) {
                    mDaysPrimary = !mDaysPrimary;
                }

                String hours = mDF.format(iHours);
                if (animateValue(hours, mTvHours, mTvHours2, mHoursIn, mHoursOut, mHoursPrimary,
                        initialRound)) {
                    mHoursPrimary = !mHoursPrimary;
                }

                String minutes = mDF.format(iMinutes);
                if (animateValue(minutes, mTvMinutes, mTvMinutes2, mMinutesIn, mMinutesOut,
                        mMinutesPrimary, initialRound)) {
                    mMinutesPrimary = !mMinutesPrimary;
                }

                String seconds = mDF.format(iSeconds);
                if (animateValue(seconds, mTvSeconds, mTvSeconds2, mSecondsIn, mSecondsOut,
                        mSecondsPrimary, initialRound)) {
                    mSecondsPrimary = !mSecondsPrimary;
                }

                if (initialRound) {
                    initialRound = false;
                }
            }

            // Returns true if animation was triggered.
            private boolean animateValue(String value, TextView viewOne, TextView viewTwo,
                    Animation animIn, Animation animOut, boolean usePrimary, boolean initialRound) {
                if (viewOne == null || viewTwo == null) {
                    return false;
                }
                boolean rc = false;

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    if (initialRound) {
                        viewOne.setText(value);
                        viewOne.setContentDescription(viewOne.getText());
                        viewTwo.setText(value);
                        viewTwo.setContentDescription(viewTwo.getText());
                        viewOne.startAnimation(animIn);
                        viewTwo.startAnimation(animOut);
                        //noinspection ConstantConditions
                        return rc;
                    }

                    String one = viewOne.getText().toString();
                    String two = viewTwo.getText().toString();
                    if (usePrimary) {
                        if (value.compareTo(two) != 0) {
                            viewOne.setText(value);
                            viewOne.setContentDescription(viewOne.getText());
                            viewOne.clearAnimation();
                            viewOne.startAnimation(animIn);

                            viewTwo.clearAnimation();
                            viewTwo.startAnimation(animOut);
                            rc = true;
                        }
                    } else {
                        if (value.compareTo(one) != 0) {
                            viewTwo.setText(value);
                            viewTwo.setContentDescription(viewTwo.getText());
                            viewTwo.clearAnimation();
                            viewTwo.startAnimation(animIn);

                            viewOne.clearAnimation();
                            viewOne.startAnimation(animOut);
                            rc = true;
                        }
                    }
                } else {
                    // Skip animations on ICS and below.
                    viewOne.setText(value);
                    viewOne.setContentDescription(viewOne.getText());
                    if (initialRound) {
                        viewOne.setVisibility(View.VISIBLE);
                    }
                }
                return rc;
            }

            @Override
            public void onFinish() {
                final LaunchCountdownContext launchContext = mLaunchContextRef.get();
                if (launchContext != null) {
                    launchContext.onCountdownFinished();
                }
            }
        };
        mTimer.start();
    }

    public void cancel() {
        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = null;
    }

}
