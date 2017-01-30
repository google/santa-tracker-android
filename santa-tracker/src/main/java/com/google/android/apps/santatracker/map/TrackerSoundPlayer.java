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

package com.google.android.apps.santatracker.map;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.apps.santatracker.R;


public class TrackerSoundPlayer {

    private static final String TAG = "TrackerSoundPlayer";

    private static final float VOLUME_MULTIPLIER = 0.25f;

    private Context mContext;

    private MediaPlayer mSleighBellsPlayer;

    private MediaPlayer mHoHoHoPlayer;

    private boolean muted;

    public TrackerSoundPlayer(@NonNull Context context) {
        mContext = context;
        if (context instanceof Activity) {
            ((Activity) context).setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
    }

    public void release() {
        if (mContext instanceof Activity) {
            ((Activity) mContext).setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
        }
        if (mSleighBellsPlayer != null) {
            mSleighBellsPlayer.stop();
            mSleighBellsPlayer.reset();
            mSleighBellsPlayer.release();
            mSleighBellsPlayer = null;
        }
        if (mHoHoHoPlayer != null) {
            mHoHoHoPlayer.stop();
            mHoHoHoPlayer.reset();
            mHoHoHoPlayer.release();
            mHoHoHoPlayer = null;
        }
    }

    public void pause() {
        if (mHoHoHoPlayer != null) {
            mHoHoHoPlayer.pause();
        }
        if (mSleighBellsPlayer != null) {
            mSleighBellsPlayer.pause();
        }
    }

    public void resume() {
        if (mHoHoHoPlayer != null) {
            mHoHoHoPlayer.start();
        }
        if (mSleighBellsPlayer != null) {
            mSleighBellsPlayer.start();
        }
    }

    public void mute() {
        if (muted) {
            return;
        }
        muted = true;
        if (mHoHoHoPlayer != null) {
            mHoHoHoPlayer.setVolume(0f, 0f);
        }
        if (mSleighBellsPlayer != null) {
            mSleighBellsPlayer.setVolume(0f, 0f);
        }
    }

    public void unmute() {
        if (!muted) {
            return;
        }
        muted = false;
        if (mHoHoHoPlayer != null) {
            mHoHoHoPlayer.setVolume(VOLUME_MULTIPLIER, VOLUME_MULTIPLIER);
        }
        if (mSleighBellsPlayer != null) {
            mSleighBellsPlayer.setVolume(VOLUME_MULTIPLIER, VOLUME_MULTIPLIER);
        }
    }

    public void sayHoHoHo() {
        if (mHoHoHoPlayer == null) {
            Log.d(TAG, "sayHoHoHo: not ready");
            mHoHoHoPlayer = MediaPlayer.create(mContext, R.raw.ho_ho_ho);
            mHoHoHoPlayer.setLooping(false);
            final float volume = muted ? 0f : VOLUME_MULTIPLIER;
            mHoHoHoPlayer.setVolume(volume, volume);
            mHoHoHoPlayer.start();
        } else if (!mHoHoHoPlayer.isPlaying()) {
            Log.d(TAG, "sayHoHoHo: ready");
            mHoHoHoPlayer.seekTo(0);
            mHoHoHoPlayer.start();
        }
    }

    public void startSleighBells() {
        if (mSleighBellsPlayer == null) {
            mSleighBellsPlayer = MediaPlayer.create(mContext, R.raw.sleighbells);
            mSleighBellsPlayer.setLooping(true);
            final float volume = muted ? 0f : VOLUME_MULTIPLIER;
            mSleighBellsPlayer.setVolume(volume, volume);
            mSleighBellsPlayer.start();
        } else if (mSleighBellsPlayer.isPlaying()) {
            Log.d(TAG, "startSleighBells: already playing");
        } else {
            mSleighBellsPlayer.seekTo(0);
            mSleighBellsPlayer.start();
        }
    }

    public void stopSleighBells() {
        if (mSleighBellsPlayer == null) {
            return;
        }
        mSleighBellsPlayer.pause();
    }

}
