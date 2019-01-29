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
package com.google.android.apps.santatracker.doodles.shared.sound;

import android.content.Context;
import android.media.MediaPlayer;
import com.google.android.apps.santatracker.util.SantaLog;

/**
 * A wrapper around a MediaPlayer so that we can define our own LoopingMediaPlayer and have it use a
 * common interface.
 */
public class DoodleMediaPlayerImpl implements DoodleMediaPlayer {
    private static final String TAG = DoodleMediaPlayerImpl.class.getSimpleName();

    private MediaPlayer mediaPlayer;
    private float volume;

    private DoodleMediaPlayerImpl(Context context, int resId) {
        mediaPlayer = MediaPlayer.create(context, resId);
    }

    public static DoodleMediaPlayerImpl create(Context context, int resId) {
        return new DoodleMediaPlayerImpl(context, resId);
    }

    @Override
    public void start() {
        try {
            mediaPlayer.start();
        } catch (IllegalStateException e) {
            SantaLog.w(TAG, "start() failed: " + e.toString());
        }
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    @Override
    public void pause() {
        try {
            mediaPlayer.pause();
        } catch (IllegalStateException e) {
            SantaLog.w(TAG, "pause() failed: " + e.toString());
        }
    }

    @Override
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void setNextMediaPlayer(MediaPlayer next) {
        try {
            mediaPlayer.setNextMediaPlayer(next);
        } catch (IllegalStateException e) {
            SantaLog.w(TAG, "setNextMediaPlayer() failed: " + e.toString());
        }
    }

    @Override
    public void setVolume(float volume) {
        this.volume = volume;
        try {
            mediaPlayer.setVolume(volume, volume);
        } catch (IllegalStateException e) {
            SantaLog.w(TAG, "setVolume() failed: " + e.toString());
        }
    }

    @Override
    public void mute() {
        try {
            mediaPlayer.setVolume(0, 0);
        } catch (IllegalStateException e) {
            SantaLog.w(TAG, "mute() failed: " + e.toString());
        }
    }

    @Override
    public void unmute() {
        try {
            mediaPlayer.setVolume(volume, volume);
        } catch (IllegalStateException e) {
            SantaLog.w(TAG, "unmute() failed: " + e.toString());
        }
    }

    @Override
    public void release() {
        try {
            mediaPlayer.release();
        } catch (IllegalStateException e) {
            SantaLog.w(TAG, "release() failed: " + e.toString());
        }
    }
}
