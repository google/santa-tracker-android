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
package com.google.android.apps.santatracker.doodles.shared;

import android.support.annotation.VisibleForTesting;

/**
 * A helper for timing log events in the Pineapple 2016 games.
 */
public class PineappleLogTimer {
  private static PineappleLogTimer instance;

  public static PineappleLogTimer getInstance() {
    if (instance == null) {
      instance = new PineappleLogTimer();
    }
    return instance;
  }

  private long startTimeMs;
  private long pauseTimeMs;
  private boolean isPaused;
  private LogClock clock;

  private PineappleLogTimer() {
    this(new LogClock());
  }

  @VisibleForTesting
  PineappleLogTimer(LogClock clock) {
    this.clock = clock;
    startTimeMs = clock.currentTimeMillis();
  }

  public void reset() {
    startTimeMs = clock.currentTimeMillis();
    pauseTimeMs = clock.currentTimeMillis();
    unpause();
  }

  public long timeElapsedMs() {
    if (isPaused) {
      return pauseTimeMs - startTimeMs;
    }
    return clock.currentTimeMillis() - startTimeMs;
  }

  public void pause() {
    if (!isPaused) {
      pauseTimeMs = clock.currentTimeMillis();
      isPaused = true;
    }
  }

  public void unpause() {
    if (isPaused) {
      long pauseDurationMs = clock.currentTimeMillis() - pauseTimeMs;
      startTimeMs += pauseDurationMs;
      isPaused = false;
    }
  }

  /**
   * Wrapper around System.currentTimeMillis so that we can test PineappleLogTimer.
   */
  @VisibleForTesting
  static class LogClock {
    public long currentTimeMillis() {
      return System.currentTimeMillis();
    }
  }
}
