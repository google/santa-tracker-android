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

package com.google.android.apps.playgames.simpleengine;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import com.google.android.apps.santatracker.AudioConstants;
import com.google.android.apps.santatracker.data.SantaPreferences;

public class SoundManager
        implements MediaPlayer.OnPreparedListener, SoundPool.OnLoadCompleteListener {

    MediaPlayer mBgmMediaPlayer = null;
    boolean mBgmLoading = false;
    Context mAppContext;
    boolean mStoppedSound = false;
    boolean mWantBgm = true;

    SoundPool mSoundPool = null;
    int mSoundsLoading = 0; // how many sounds are loading in the SoundPool

    static final int MAX_STREAMS = 4;
    static final int STREAM_TYPE = AudioManager.STREAM_MUSIC;
    static final int SRC_QUALITY = 0;
    static final int DEFAULT_PRIORITY = 1;

    private final SantaPreferences mSantaPreferences;

    public SoundManager(Context ctx) {
        mAppContext = ctx.getApplicationContext();
        mSantaPreferences = new SantaPreferences(mAppContext);

        mSoundPool = new SoundPool(MAX_STREAMS, STREAM_TYPE, SRC_QUALITY);
        mSoundPool.setOnLoadCompleteListener(this);
    }

    public void requestBackgroundMusic(int resId) {
        mBgmMediaPlayer = MediaPlayer.create(mAppContext, resId);
        mBgmMediaPlayer.setOnPreparedListener(this);
        mBgmLoading = true;
    }

    public int requestSfx(int resId) {
        mSoundsLoading++;
        return mSoundPool.load(mAppContext, resId, DEFAULT_PRIORITY);
    }

    public void playSfx(int soundId) {
        if (!isMuted() && !mStoppedSound) {
            mSoundPool.play(
                    soundId,
                    AudioConstants.DEFAULT_SOUND_EFFECT_VOLUME,
                    AudioConstants.DEFAULT_SOUND_EFFECT_VOLUME,
                    DEFAULT_PRIORITY,
                    0,
                    1.0f);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mBgmLoading = false;
        mBgmMediaPlayer.setVolume(
                AudioConstants.DEFAULT_BACKGROUND_VOLUME, AudioConstants.DEFAULT_BACKGROUND_VOLUME);
        mBgmMediaPlayer.setLooping(true);
        updateBgm();
    }

    public boolean isReady() {
        return !mBgmLoading && mSoundsLoading <= 0;
    }

    private void updateBgm() {
        boolean shouldPlay = !isMuted() && mWantBgm && !mStoppedSound;
        final MediaPlayer bgmMediaPlayer = mBgmMediaPlayer;
        if (bgmMediaPlayer != null) {
            if (shouldPlay && !bgmMediaPlayer.isPlaying()) {
                bgmMediaPlayer.start();
            } else if (!shouldPlay && bgmMediaPlayer.isPlaying()) {
                bgmMediaPlayer.pause();
            }
        }
    }

    public void mute() {
        setMute(true);
    }

    public void unmute() {
        setMute(false);
    }

    public boolean isMuted() {
        return mSantaPreferences.isMuted();
    }

    public void setMute(boolean mute) {
        mSantaPreferences.setMuted(mute);
        updateBgm();
    }

    public void stopSound() {
        mStoppedSound = true;
        updateBgm();
    }

    public void resumeSound() {
        mStoppedSound = false;
        updateBgm();
    }

    public void enableBgm(boolean enable) {
        mWantBgm = enable;
        updateBgm();
    }

    public void reset() {
        if (mBgmMediaPlayer != null) {
            if (mBgmMediaPlayer.isPlaying()) {
                mBgmMediaPlayer.stop();
            }
            mBgmMediaPlayer = null;
        }
        mBgmLoading = false;
        mWantBgm = true;
    }

    public void dispose() {
        if (mBgmMediaPlayer != null && mBgmMediaPlayer.isPlaying()) {
            mBgmMediaPlayer.stop();
        }
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        mSoundsLoading--;
        if (status != 0) {
            Logger.e("Error loading SFX into SoundPool, sample " + sampleId + ", status " + status);
        }
    }
}
