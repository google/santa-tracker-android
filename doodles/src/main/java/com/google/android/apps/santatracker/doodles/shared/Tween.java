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

/**
 * Base class for tweens. Handles the basic bookkeeping, then delegates to subclasses via
 * updateValues() for updating specific values.
 */
public abstract class Tween {
  protected float durationSeconds = 0;
  private float elapsedSeconds = 0;
  private float percentDone = 0;

  public Tween(float durationSeconds) {
    this.durationSeconds = durationSeconds;
  }

  /**
   * @return true if update should continue to be called, false if tween is finished and should
   *     be removed.
   */
  public boolean update(double deltaMs) {
    boolean wasFinished = isFinished();
    if (wasFinished) {
      return false;
    }
    elapsedSeconds += deltaMs / 1000f;
    percentDone = elapsedSeconds / durationSeconds;
    if (percentDone > 1) {
      percentDone = 1;
    }
    updateValues(percentDone);
    if (!wasFinished && isFinished()) {
      onFinish();
    }
    return !isFinished();
  }

  /**
   * Subclasses should define this method to update their value(s) every frame. Suggested
   * implementation:
   *     currentValue = interpolator.getValue(percentDone, startValue, endValue);
   */
  protected abstract void updateValues(float percentDone);

  /**
   * Subclasses can override this to execute code when the Tween finishes.
   */
  protected void onFinish() {

  }

  // Cancels the tween. Doesn't reset values back to their starting value or the final value. Just
  // leaves state where it is and stops updating. onFinish will be called.
  public void cancel() {
    if (!isFinished()) {
      // Advance elapsedSeconds and percentDone to the end so future update calls won't do anything.
      elapsedSeconds = durationSeconds;
      percentDone = 1;
      onFinish();
    }
  }

  public boolean isFinished() {
    return percentDone >= 1;
  }

  public float durationSeconds() {
    return durationSeconds;
  }

  public Process asProcess() {
    return new Process() {
      @Override
      public void updateLogic(float deltaMs) {
        Tween.this.update(deltaMs);
      }

      @Override
      public boolean isFinished() {
        return Tween.this.isFinished();
      }
    };
  }
}
