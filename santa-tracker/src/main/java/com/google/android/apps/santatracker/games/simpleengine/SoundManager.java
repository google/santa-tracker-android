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

package com.google.android.apps.santatracker.games.simpleengine;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.io.IOException;

public class SoundManager implements MediaPlayer.OnPreparedListener,
        SoundPool.OnLoadCompleteListener {

    MediaPlayer mBgmMediaPlayer = null;
    AssetFileDescriptor mBgmFileDescriptor = null;
    boolean mBgmLoading = false;
    Context mAppContext;
    boolean mMuted = false;
    boolean mStoppedSound = false;
    boolean mWantBgm = true;

    SoundPool mSoundPool = null;
    int mSoundsLoading = 0;  // how many sounds are loading in the SoundPool

    static final int MAX_STREAMS = 4;
    static final int STREAM_TYPE = AudioManager.STREAM_MUSIC;
    static final int SRC_QUALITY = 0;
    static final int DEFAULT_PRIORITY = 1;
    static final float DEFAULT_VOLUME = 0.6f;
    static final float DEFAULT_BGM_VOLUME = 0.5f;

    public SoundManager(Context ctx) {
        mAppContext = ctx.getApplicationContext();
        mSoundPool = new SoundPool(MAX_STREAMS, STREAM_TYPE, SRC_QUALITY);
        mSoundPool.setOnLoadCompleteListener(this);
    }

    public void requestBackgroundMusic(String assetsFileName) {
        try {
            mBgmFileDescriptor = mAppContext.getAssets().openFd(assetsFileName);
            mBgmMediaPlayer = new MediaPlayer();
            mBgmMediaPlayer.setDataSource(mBgmFileDescriptor.getFileDescriptor(),
                    mBgmFileDescriptor.getStartOffset(),
                    mBgmFileDescriptor.getDeclaredLength());
            mBgmMediaPlayer.setOnPreparedListener(this);
            mBgmLoading = true;
            mBgmMediaPlayer.prepareAsync();
        } catch (IOException ex) {
            Logger.e("Error loading background music from asset file: " + assetsFileName);
            ex.printStackTrace();
            return;
        }
    }

    public int requestSfx(int resId) {
        mSoundsLoading++;
        return mSoundPool.load(mAppContext, resId, DEFAULT_PRIORITY);
    }

    public void playSfx(int soundId) {
        if(!mMuted && !mStoppedSound) {
            mSoundPool.play(soundId, DEFAULT_VOLUME, DEFAULT_VOLUME, DEFAULT_PRIORITY, 0, 1.0f);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mBgmFileDescriptor != null) {
            try {
                mBgmFileDescriptor.close();
            } catch (IOException ex) {
                Logger.e("Error closing bgm file descriptor:");
                ex.printStackTrace();
            }
            mBgmFileDescriptor = null;
        }
        mBgmLoading = false;
        mBgmMediaPlayer.setVolume(DEFAULT_BGM_VOLUME, DEFAULT_BGM_VOLUME);
        mBgmMediaPlayer.setLooping(true);
        updateBgm();
    }

    public boolean isReady() {
        return !mBgmLoading && mSoundsLoading <= 0;
    }

    private void updateBgm() {
        boolean shouldPlay = !mMuted && mWantBgm && !mStoppedSound;
        if (mBgmMediaPlayer != null) {
            if (shouldPlay && !mBgmMediaPlayer.isPlaying()) {
                mBgmMediaPlayer.start();
            } else if (!shouldPlay && mBgmMediaPlayer.isPlaying()) {
                mBgmMediaPlayer.pause();
            }
        }
    }

    public void mute() {
        mMuted = true;
        updateBgm();
    }

    public void unmute() {
        mMuted = false;
        updateBgm();
    }

    public boolean getMute() {
        return mMuted;
    }

    public void setMute(boolean mute) {
        mMuted = mute;
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
            Logger.e("Error loading SFX into SoundPool, sample " + sampleId +
                    ", status " + status);
        }
    }
}
