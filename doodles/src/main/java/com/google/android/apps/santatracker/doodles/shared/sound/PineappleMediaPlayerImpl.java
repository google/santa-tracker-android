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
package com.google.android.apps.santatracker.doodles.shared.sound;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

/**
 * A wrapper around a MediaPlayer so that we can define our own LoopingMediaPlayer and have it
 * use a common interface.
 */
public class PineappleMediaPlayerImpl implements PineappleMediaPlayer {
  private static final String TAG = PineappleMediaPlayerImpl.class.getSimpleName();

  private MediaPlayer mediaPlayer;
  private float volume;

  public static PineappleMediaPlayerImpl create(Context context, int resId) {
    return new PineappleMediaPlayerImpl(context, resId);
  }

  private PineappleMediaPlayerImpl(Context context, int resId) {
    mediaPlayer = MediaPlayer.create(context, resId);
  }

  @Override
  public void start() {
    try {
      mediaPlayer.start();
    } catch (IllegalStateException e) {
      Log.w(TAG, "start() failed: " + e.toString());
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
      Log.w(TAG, "pause() failed: " + e.toString());
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
      Log.w(TAG, "setNextMediaPlayer() failed: " + e.toString());
    }
  }

  @Override
  public void setVolume(float volume) {
    this.volume = volume;
    try {
      mediaPlayer.setVolume(volume, volume);
    } catch (IllegalStateException e) {
      Log.w(TAG, "setVolume() failed: " + e.toString());
    }
  }

  @Override
  public void mute() {
    try {
      mediaPlayer.setVolume(0, 0);
    } catch (IllegalStateException e) {
      Log.w(TAG, "mute() failed: " + e.toString());
    }
  }

  @Override
  public void unmute() {
    try {
      mediaPlayer.setVolume(volume, volume);
    } catch (IllegalStateException e) {
      Log.w(TAG, "unmute() failed: " + e.toString());
    }
  }

  @Override
  public void release() {
    try {
      mediaPlayer.release();
    } catch (IllegalStateException e) {
      Log.w(TAG, "release() failed: " + e.toString());
    }
  }

}
