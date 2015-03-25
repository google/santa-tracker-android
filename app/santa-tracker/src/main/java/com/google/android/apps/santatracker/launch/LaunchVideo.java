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

package com.google.android.apps.santatracker.launch;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.util.Intents;

import android.content.Intent;
import android.content.res.Resources;
import android.view.View;

/**
 * Launch a YouTube video.
 */
public class LaunchVideo extends AbstractLaunch {

    public static final String HIDDEN_VIDEO = "_disabled";

    private final int mLockedDrawableId;
    private final int mUnlockDate;
    private String mVideoId;

    /**
     * Constructs a video-launching marker.
     *
     * @param context          The SantaContext
     * @param lockedDrawableId The badge icon to use if this marker is locked (e.g. unlock date)
     * @param colorId          The resource ID of the color to fill the marker
     * @param unlockDate       The day in December to unlock this video (e.g. 05 for December 5)
     */
    public LaunchVideo(StartupActivity.SantaContext context, int lockedDrawableId, int colorId,
            int unlockDate) {
        super(context, R.string.video, R.drawable.marker_badge_video, colorId,
                R.dimen.markerVideoPaddingLeft, R.dimen.markerBadgeDefaultPadding,
                R.dimen.markerVideoPaddingRight);
        mLockedDrawableId = lockedDrawableId;
        mUnlockDate = unlockDate;
    }

    @Override
    public void attachToView(MarkerView image) {
        super.attachToView(image);
        Resources r = mContext.getContext().getResources();
        if (mLockedDrawableId > 0) {
            image.setLockedDrawable(r.getDrawable(mLockedDrawableId));
        } else {
            image.setLockedDrawable(null);
        }
    }

    @Override
    public void onClick(View v) {
        switch (mState) {
            case STATE_READY:
            case STATE_FINISHED:
                Intent intent = Intents.getYoutubeIntent(mContext.getContext(), mVideoId);
                mContext.getContext().startActivity(intent);
                break;
            case STATE_DISABLED:
                notify(mContext.getContext(), R.string.video_disabled);
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getContext(), R.string.video_disabled, mUnlockDate);
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch (mState) {
            case STATE_READY:
            case STATE_FINISHED:
                notify(mContext.getContext(), R.string.video);
                break;
            case STATE_DISABLED:
                notify(mContext.getContext(), R.string.video_disabled);
                break;
            case STATE_LOCKED:
            default:
                notify(mContext.getContext(), R.string.video_locked, mUnlockDate);
                break;
        }
        return true;
    }

    public void setVideo(String videoId, long unlockTime) {
        if (HIDDEN_VIDEO.equals(videoId)) {
            // video explicitly disabled
            mVideoId = null;
            setState(STATE_HIDDEN);
        } else if (videoId != null && !videoId.isEmpty() && !videoId.equals("null")) {
            // JSONObject.getString will coerce a null value into "null"
            // valid-looking video ID - unlock regardless of time
            mVideoId = videoId;
            setState(STATE_READY);
        } else if (System.currentTimeMillis() < unlockTime) {
            // video not-yet unlocked
            mVideoId = null;
            setState(STATE_LOCKED);
        } else {
            // video ID null or not present and video should be unlocked
            mVideoId = null;
            setState(STATE_DISABLED);
        }
    }

    @Override
    public boolean isGame() {
        return false;
    }
}
