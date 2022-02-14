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

package com.google.android.apps.santatracker.launch;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import com.google.android.apps.santatracker.R;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.GregorianCalendar;

public class LaunchCountdown {

    private TextView mTvDays;
    private TextView mTvDays2;
    private TextView mTvHours;
    private TextView mTvHours2;
    private TextView mTvMinutes;
    private TextView mTvMinutes2;
    private TextView mTvSeconds;
    private TextView mTvSeconds2;
    private boolean mDaysPrimary = true;
    private boolean mHoursPrimary = true;
    private boolean mMinutesPrimary = true;
    private boolean mSecondsPrimary = true;
    private boolean mAnimationsLoaded = false;
    private Animation mDaysIn, mDaysOut;
    private Animation mHoursIn, mHoursOut;
    private Animation mMinutesIn, mMinutesOut;
    private Animation mSecondsIn, mSecondsOut;
    private WeakReference<LaunchCountdownContext> mLaunchContextRef;
    private DecimalFormat mDF = new DecimalFormat("00");
    private boolean initialRound = true;

    public LaunchCountdown(LaunchCountdownContext context) {
        mLaunchContextRef = new WeakReference<>(context);
        final View countdownView = context.getCountdownView();
        mTvDays = countdownView.findViewById(R.id.countdown_days_1);
        mTvDays2 = countdownView.findViewById(R.id.countdown_days_2);
        mTvHours = countdownView.findViewById(R.id.countdown_hours_1);
        mTvHours2 = countdownView.findViewById(R.id.countdown_hours_2);
        mTvMinutes = countdownView.findViewById(R.id.countdown_minutes_1);
        mTvMinutes2 = countdownView.findViewById(R.id.countdown_minutes_2);
        mTvSeconds = countdownView.findViewById(R.id.countdown_seconds_1);
        mTvSeconds2 = countdownView.findViewById(R.id.countdown_seconds_2);
    }

    private void loadAnimations() {
        if (mAnimationsLoaded) {
            return;
        }

        LaunchCountdownContext launchContext = mLaunchContextRef.get();
        if (launchContext == null) {
            return;
        }

        final Context context = launchContext.getApplicationContext();

        mDaysIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
        mDaysOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_top);
        mHoursIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
        mHoursOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_top);
        mMinutesIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
        mMinutesOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_top);
        mSecondsIn = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
        mSecondsOut = AnimationUtils.loadAnimation(context, R.anim.slide_out_top);

        mAnimationsLoaded = true;
    }

    public void setTimeRemaining(long millisUntilFinished) {
        if (millisUntilFinished < 0) {
            return;
        }

        // Load animations lazily (no-op if already loaded)
        loadAnimations();

        int iDays = (int) Math.floor(millisUntilFinished / (24 * 60 * 60 * 1000));
        int iHours = (int) Math.floor(millisUntilFinished / (60 * 60 * 1000) % 24);
        int iMinutes = (int) Math.floor(millisUntilFinished / (60 * 1000) % 60);
        int iSeconds = (int) Math.floor(millisUntilFinished / (1000) % 60);

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(millisUntilFinished);

        String days = mDF.format(iDays);
        if (animateValue(days, mTvDays, mTvDays2, mDaysIn, mDaysOut, mDaysPrimary, initialRound)) {
            mDaysPrimary = !mDaysPrimary;
        }

        String hours = mDF.format(iHours);
        if (animateValue(
                hours, mTvHours, mTvHours2, mHoursIn, mHoursOut, mHoursPrimary, initialRound)) {
            mHoursPrimary = !mHoursPrimary;
        }

        String minutes = mDF.format(iMinutes);
        if (animateValue(
                minutes,
                mTvMinutes,
                mTvMinutes2,
                mMinutesIn,
                mMinutesOut,
                mMinutesPrimary,
                initialRound)) {
            mMinutesPrimary = !mMinutesPrimary;
        }

        String seconds = mDF.format(iSeconds);
        if (animateValue(
                seconds,
                mTvSeconds,
                mTvSeconds2,
                mSecondsIn,
                mSecondsOut,
                mSecondsPrimary,
                initialRound)) {
            mSecondsPrimary = !mSecondsPrimary;
        }

        if (initialRound) {
            initialRound = false;
        }
    }

    // Returns true if animation was triggered.
    private boolean animateValue(
            String value,
            TextView viewOne,
            TextView viewTwo,
            Animation animIn,
            Animation animOut,
            boolean usePrimary,
            boolean initialRound) {
        if (viewOne == null || viewTwo == null) {
            return false;
        }
        boolean rc = false;

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

        return rc;
    }

    /** Listener for LaunchCountdown and convenience method to draw CountDown UI itself. */
    public interface LaunchCountdownContext {
        View getCountdownView();

        Context getActivityContext();

        Context getApplicationContext();
    }
}
