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
 * Interpolates values for Tweens. Used to implement easing and repeated movement.
 */
public interface Interpolator {

  Interpolator LINEAR = new Interpolator() {
    @Override
    public float getValue(float percent, float initialValue, float finalValue) {
      return initialValue + (finalValue - initialValue) * percent;
    }
  };
  Interpolator EASE_IN = new Interpolator() {
    @Override
    public float getValue(float percent, float initialValue, float finalValue) {
      return initialValue + (finalValue - initialValue) * percent * percent;
    }
  };
  Interpolator EASE_OUT = new Interpolator() {
    @Override
    public float getValue(float percent, float initialValue, float finalValue) {
      return initialValue - ((finalValue - initialValue) * percent * (percent - 2));
    }
  };
  Interpolator EASE_IN_AND_OUT = new Interpolator() {
    @Override
    public float getValue(float percent, float initialValue, float finalValue) {
      // Simple sigmoid function: y = 3 * x^2 - 2 * x^3
      return LINEAR.getValue(3 * percent * percent - 2 * percent * percent * percent,
          initialValue, finalValue);
    }
  };
  Interpolator FAST_IN = new Interpolator() {
    @Override
    public float getValue(float percent, float initialValue, float finalValue) {
      return initialValue + (finalValue - initialValue) * (-percent * (percent - 2));
    }
  };
  Interpolator FAST_IN_AND_HOLD = new Interpolator() {
    @Override
    public float getValue(float percent, float initialValue, float finalValue) {
      percent *= 2;
      if (percent > 1) {
        percent = 1;
      }
      return initialValue + (finalValue - initialValue) * (-percent * (percent - 2));
    }
  };

  Interpolator OVERSHOOT = new Interpolator() {
    @Override
    public float getValue(float percent, float initialValue, float finalValue) {
      percent -= 1;
      percent = percent * percent * (3 * percent + 2) + 1;
      return initialValue + (finalValue - initialValue) * percent;
    }
  };

  float getValue(float percent, float initialValue, float finalValue);
}
