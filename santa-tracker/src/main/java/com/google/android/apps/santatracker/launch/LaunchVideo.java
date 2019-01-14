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

import android.content.Intent;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.google.android.apps.santatracker.Intents;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.tracker.time.Clock;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.firebase.analytics.FirebaseAnalytics;

/** Launch a YouTube video. */
public class LaunchVideo extends AbstractLaunch {

    public static final String HIDDEN_VIDEO = "_disabled";

    private final int mUnlockDate;
    private String mVideoId;
    private FirebaseAnalytics mMeasurement;
    private Clock mClock;
    @StringRes private int mTitleRes;

    /**
     * Constructs a video-launching marker.
     *
     * @param context The SantaContext
     * @param adapter
     * @param cardDrawable The card drawable to show in the village.
     * @param unlockDate The day in December to unlock this video (e.g. 05 for December 5)
     * @param clock The clock instance for providing time
     */
    public LaunchVideo(
            SantaContext context,
            LauncherDataChangedCallback adapter,
            @StringRes int title,
            @DrawableRes int cardDrawable,
            int unlockDate,
            Clock clock) {
        super(context, adapter, title, cardDrawable);
        mUnlockDate = unlockDate;
        mMeasurement = FirebaseAnalytics.getInstance(context.getApplicationContext());
        mClock = clock;
        mTitleRes = title;
    }

    public static int getId() {
        return R.string.rocket;
    }

    @Override
    public String getVerb() {
        return mContext.getResources().getString(R.string.watch);
    }

    @Override
    public String getTitle() {
        return mContext.getResources().getString(mTitleRes);
    }

    @Override
    public void onClick(View v) {
        switch (getState()) {
            case STATE_READY:
            case STATE_FINISHED:
                Intent intent =
                        Intents.getYoutubeIntent(mContext.getApplicationContext(), mVideoId);
                mContext.launchActivityDelayed(intent, v);

                // [ANALYTICS]
                MeasurementManager.recordScreenView(mMeasurement, "video_" + mVideoId);
                MeasurementManager.recordCustomEvent(
                        mMeasurement,
                        mContext.getResources().getString(R.string.analytics_event_category_launch),
                        mContext.getResources().getString(R.string.analytics_tracker_action_video),
                        mVideoId);
                break;
            case STATE_DISABLED:
                notify(mContext.getApplicationContext(), R.string.video_disabled);
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getApplicationContext(), R.string.video_locked, mUnlockDate);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (getState()) {
            case STATE_READY:
            case STATE_FINISHED:
                notify(mContext.getApplicationContext(), R.string.video);
                break;
            case STATE_DISABLED:
                notify(mContext.getApplicationContext(), R.string.video_disabled);
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getApplicationContext(), R.string.video_locked, mUnlockDate);
                break;
        }
        return true;
    }

    public void setVideo(String videoId, long unlockTime) {
        // TODO: enable video featuring.
        if (HIDDEN_VIDEO.equals(videoId)) {
            // video explicitly disabled
            mVideoId = null;
            setState(false, STATE_HIDDEN);
        } else if (mClock.nowMillis() < unlockTime) {
            // video not-yet unlocked
            mVideoId = null;
            setState(false, STATE_LOCKED);
        } else if (videoId != null && !videoId.isEmpty() && !videoId.equals("null")) {
            // JSONObject.getString will coerce a null value into "null"
            // valid-looking video ID - unlock regardless of time
            mVideoId = videoId;
            setState(false, STATE_READY);
        } else {
            // video ID null or not present and video should be unlocked
            mVideoId = null;
            setState(false, STATE_DISABLED);
        }
    }

    @Override
    public boolean isGame() {
        return false;
    }
}
