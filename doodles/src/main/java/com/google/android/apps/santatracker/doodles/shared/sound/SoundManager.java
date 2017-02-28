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
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A manager for all of the different sounds which will be played in the games.
 */
public class SoundManager {
  private static final String TAG = SoundManager.class.getSimpleName();
  private static final String PREFS_NAME = "PineappleSoundManager";
  private static final String IS_MUTED_PREF = "MutePref";
  private static SoundManager instance;
  public static boolean soundsAreMuted;

  private final Object mediaPlayerLock = new Object();
  private final Object soundPoolLock = new Object();

  // A map from resource ID to a media player which can play the sound clip.
  private Map<Integer, PineappleMediaPlayer> mediaPlayerMap;
  // A map from resource ID to a sound pool containing the sound clip.
  private Map<Integer, SoundPoolContainer> soundPoolMap;
  // A set of resource IDs for sounds which have been individually muted. These shouldn't be unmuted
  // when the mute option is toggled.
  private Set<Integer> mutedSounds;
  private SoundManager() {
    mediaPlayerMap = new HashMap<>();
    soundPoolMap = new HashMap<>();
    mutedSounds = new HashSet<>();
  }

  public static SoundManager getInstance() {
    if (instance == null) {
      instance = new SoundManager();
    }
    return instance;
  }

  public void loadLongSound(Context context, int resId, boolean looping, float volume) {
    if (mediaPlayerMap.containsKey(resId)) {
      return;
    }
    PineappleMediaPlayer mediaPlayer;
    if (looping) {
      mediaPlayer = LoopingMediaPlayer.create(context, resId);
    } else {
      mediaPlayer = PineappleMediaPlayerImpl.create(context, resId);
    }
    mediaPlayer.setVolume(volume);
    if (soundsAreMuted) {
      mediaPlayer.mute();
    }
    synchronized (mediaPlayerLock) {
      mediaPlayerMap.put(resId, mediaPlayer);
    }
  }

  public void loadLongSound(Context context, int resId, boolean looping) {
    // Make this quiet by default so that it doesn't overpower the in-game sounds.
    loadLongSound(context, resId, looping, 0.1f);
  }

  public void loadShortSound(final Context context, final int resId, final boolean looping,
      final float volume) {
    if (soundPoolMap.containsKey(resId)) {
      return;
    }
     new AsyncTask<Void, Void, Void> () {
      @Override
      protected Void doInBackground(Void... params) {
        SoundPoolContainer container = new SoundPoolContainer(context, resId, looping, volume);
        synchronized (soundPoolLock) {
          soundPoolMap.put(resId, container);
        }
        return null;
      }
    }.execute();
  }

  public void loadShortSound(Context context, int resId) {
    loadShortSound(context, resId, false, 1f);
  }

  public void play(int resId) {
    if (mediaPlayerMap.containsKey(resId)) {
      mediaPlayerMap.get(resId).start();
    } else if (soundPoolMap.containsKey(resId)) {
      SoundPoolContainer container = soundPoolMap.get(resId);
      float vol = soundsAreMuted ? 0 : container.volume;
      container.streamId =
          container.soundPool.play(container.soundId, vol, vol, 1, container.looping ? -1 : 0, 1);
    }
  }

  public void pauseAll() {
    synchronized (mediaPlayerLock) {
      for (int resId : mediaPlayerMap.keySet()) {
        pause(resId);
      }
    }
    synchronized (soundPoolLock) {
      for (int resId : soundPoolMap.keySet()) {
        pause(resId);
      }
    }
  }

  public void pause(int resId) {
    if (mediaPlayerMap.containsKey(resId)) {
      PineappleMediaPlayer mediaPlayer = mediaPlayerMap.get(resId);
      if (mediaPlayer.isPlaying()) {
        mediaPlayer.pause();
      }
    } else if (soundPoolMap.containsKey(resId)) {
      soundPoolMap.get(resId).soundPool.autoPause();
    }
  }

  public void pauseShortSounds() {
    synchronized (soundPoolLock) {
      for (SoundPoolContainer container : soundPoolMap.values()) {
        container.soundPool.autoPause();
      }
    }
  }

  public void resumeShortSounds() {
    synchronized (soundPoolLock) {
      for (SoundPoolContainer container : soundPoolMap.values()) {
        container.soundPool.autoResume();
      }
    }
  }

  public void playLongSoundsInSequence(int[] soundIds) {
    for (int i = 0; i < soundIds.length; i++) {
      if (isPlayingLongSound(soundIds[i])) {
        // Don't try to play long sounds which are already playing.
        return;
      }
    }

    try {
      PineappleMediaPlayer[] sounds = new PineappleMediaPlayer[soundIds.length];
      for (int i = 0; i < soundIds.length; i++) {
        sounds[i] = mediaPlayerMap.get(soundIds[i]);
        if (sounds[i] == null) {
          return;
        }
        if (i > 0) {
          sounds[i - 1].setNextMediaPlayer(sounds[i].getMediaPlayer());
        }
      }
      sounds[0].start();
    } catch (IllegalStateException e) {
      Log.d(TAG, "playLongSoundsInSequence() failed: " + e.toString());
    }
  }

  public boolean isPlayingLongSound(int resId) {
    if (mediaPlayerMap.containsKey(resId)) {
      return mediaPlayerMap.get(resId).isPlaying();
    }
    return false;
  }

  public void releaseAll() {
    synchronized (mediaPlayerLock) {
      for (PineappleMediaPlayer mediaPlayer : mediaPlayerMap.values()) {
        mediaPlayer.release();
      }
      mediaPlayerMap.clear();
    }
    synchronized (soundPoolLock) {
      for (SoundPoolContainer container : soundPoolMap.values()) {
        container.soundPool.release();
      }
      soundPoolMap.clear();
    }
  }

  public void release(int resId) {
    if (mediaPlayerMap.containsKey(resId)) {
      mediaPlayerMap.get(resId).release();
      mediaPlayerMap.remove(resId);
    }
    if (soundPoolMap.containsKey(resId)) {
      soundPoolMap.get(resId).soundPool.release();
      soundPoolMap.remove(resId);
    }
  }

  public void mute() {
    synchronized (mediaPlayerLock) {
      for (int resId : mediaPlayerMap.keySet()) {
        muteInternal(resId, false);
      }
    }
    synchronized (soundPoolLock) {
      for (int resId : soundPoolMap.keySet()) {
        muteInternal(resId, false);
      }
    }
    soundsAreMuted = true;
  }

  public void mute(int resId) {
    muteInternal(resId, true);
  }

  private void muteInternal(int resId, boolean addToMutedSounds) {
    if (mediaPlayerMap.containsKey(resId)) {
      mediaPlayerMap.get(resId).mute();
    }
    if (soundPoolMap.containsKey(resId)) {
      SoundPoolContainer container = soundPoolMap.get(resId);
      container.soundPool.setVolume(container.streamId, 0, 0);
    }
    if (addToMutedSounds) {
      mutedSounds.add(resId);
    }
  }

  public void unmute() {
    soundsAreMuted = false;
    synchronized (mediaPlayerLock) {
      for (int resId : mediaPlayerMap.keySet()) {
        if (!mutedSounds.contains(resId)) {
          unmuteInternal(resId, false);
        }
      }
    }
    synchronized (soundPoolLock) {
      for (int resId : soundPoolMap.keySet()) {
        if (!mutedSounds.contains(resId)) {
          unmuteInternal(resId, false);
        }
      }
    }
  }

  public void unmute(int resId) {
    unmuteInternal(resId, true);
  }

  private void unmuteInternal(int resId, boolean removeFromMutedSounds) {
    if (soundsAreMuted) {
      return;
    }
    if (mediaPlayerMap.containsKey(resId)) {
      mediaPlayerMap.get(resId).unmute();
    }
    if (soundPoolMap.containsKey(resId)) {
      SoundPoolContainer container = soundPoolMap.get(resId);
      container.soundPool.setVolume(container.streamId, container.volume, container.volume);
    }
    if (removeFromMutedSounds) {
      mutedSounds.remove(resId);
    }
  }

  public void storeMutePreference(final Context context) {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... voids) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(IS_MUTED_PREF, soundsAreMuted);
        editor.commit();
        return null;
      }
    }.execute();
  }

  public void loadMutePreference(final Context context) {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... voids) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, 0);
        if (sharedPreferences.getBoolean(IS_MUTED_PREF, false)) {
          mute();
        } else {
          unmute();
        }
        return null;
      }
    }.execute();
  }

  private static class SoundPoolContainer {
    public final SoundPool soundPool;
    public final int soundId;
    public final boolean looping;
    public final float volume;
    public int streamId;
    public SoundPoolContainer(Context context, int resId, boolean looping, float volume) {
      soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
      soundId = soundPool.load(context, resId, 1);
      this.looping = looping;
      this.volume = volume;
    }
  }
}
