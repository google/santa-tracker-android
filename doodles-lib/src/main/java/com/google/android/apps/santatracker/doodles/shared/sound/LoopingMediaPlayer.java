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
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import com.google.android.apps.santatracker.util.SantaLog;

/** A wrapper around a MediaPlayer which allows for a gapless, looping track. */
public class LoopingMediaPlayer implements DoodleMediaPlayer {
    private static final String TAG = LoopingMediaPlayer.class.getSimpleName();

    private MediaPlayer currentPlayer;
    private MediaPlayer nextPlayer;
    private AssetFileDescriptor soundFileDescriptor;
    private float volume;

    private LoopingMediaPlayer(Context context, int resId) {
        soundFileDescriptor = context.getResources().openRawResourceFd(resId);

        currentPlayer = MediaPlayer.create(context, resId);
        nextPlayer = MediaPlayer.create(context, resId);
        currentPlayer.setNextMediaPlayer(nextPlayer);

        currentPlayer.setOnCompletionListener(
                new OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        try {
                            currentPlayer.reset();
                            currentPlayer.setDataSource(
                                    soundFileDescriptor.getFileDescriptor(),
                                    soundFileDescriptor.getStartOffset(),
                                    soundFileDescriptor.getLength());
                            currentPlayer.prepare();
                            nextPlayer.setNextMediaPlayer(currentPlayer);
                        } catch (Exception e) {
                            SantaLog.w(TAG, "onCompletion: unexpected exception", e);
                        }
                    }
                });
        nextPlayer.setOnCompletionListener(
                new OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        try {
                            nextPlayer.reset();
                            nextPlayer.setDataSource(
                                    soundFileDescriptor.getFileDescriptor(),
                                    soundFileDescriptor.getStartOffset(),
                                    soundFileDescriptor.getLength());
                            nextPlayer.prepare();
                            currentPlayer.setNextMediaPlayer(nextPlayer);
                        } catch (Exception e) {
                            SantaLog.w(TAG, "onCompletion: unexpected exception", e);
                        }
                    }
                });
    }

    public static LoopingMediaPlayer create(Context context, int resId) {
        return new LoopingMediaPlayer(context, resId);
    }

    @Override
    public void start() {
        try {
            currentPlayer.start();
        } catch (IllegalStateException e) {
            SantaLog.w(TAG, "start() failed: " + e.toString());
        }
    }

    @Override
    public boolean isPlaying() {
        return currentPlayer.isPlaying() || nextPlayer.isPlaying();
    }

    @Override
    public void pause() {
        try {
            currentPlayer.pause();
            nextPlayer.pause();
        } catch (Exception e) {
            SantaLog.w(TAG, "pause() failed: " + e.toString());
        }
    }

    @Override
    public MediaPlayer getMediaPlayer() {
        return currentPlayer;
    }

    @Override
    public void setNextMediaPlayer(MediaPlayer mediaPlayer) {
        try {
            currentPlayer.setNextMediaPlayer(mediaPlayer);
        } catch (Exception e) {
            SantaLog.w(TAG, "setNextMediaPlayer() failed: ", e);
        }
    }

    @Override
    public void setVolume(float volume) {
        this.volume = volume;
        try {
            currentPlayer.setVolume(volume, volume);
            nextPlayer.setVolume(volume, volume);
        } catch (Exception e) {
            SantaLog.w(TAG, "setVolume() failed: ", e);
        }
    }

    @Override
    public void mute() {
        try {
            currentPlayer.setVolume(0, 0);
            nextPlayer.setVolume(0, 0);
        } catch (Exception e) {
            SantaLog.w(TAG, "mute() failed: ", e);
        }
    }

    @Override
    public void unmute() {
        try {
            currentPlayer.setVolume(volume, volume);
            nextPlayer.setVolume(volume, volume);
        } catch (Exception e) {
            SantaLog.w(TAG, "unmute() failed: ", e);
        }
    }

    @Override
    public void release() {
        try {
            currentPlayer.release();
            nextPlayer.release();
            soundFileDescriptor.close();
        } catch (Exception e) {
            SantaLog.w(TAG, "release() failed: ", e);
        }
    }
}
