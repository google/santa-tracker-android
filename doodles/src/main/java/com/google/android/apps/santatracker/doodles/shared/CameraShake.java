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
 * Tracks a vibration which reduces over time, suitable for screen shake effects.
 * (Use position as the camera's offset when rendering)
 */
public class CameraShake extends Actor {

  private float frequency = 0;
  private float magnitude = 0;
  private float falloff = 0;
  private float msTillNextShake = 0;
  
  public void shake(float frequency, float magnitude, float falloff) {
    this.frequency = frequency;
    this.magnitude = magnitude;
    this.falloff = falloff;
    msTillNextShake = 1000 / frequency;
  }

  @Override
  public void update(float deltaMs) {
    if (magnitude == 0) {
      return;
    }
    
    msTillNextShake -= deltaMs;
    if (msTillNextShake < 0) {
      msTillNextShake = 1000 / frequency;
      magnitude *= falloff;
      // Tiny amounts of shake take too long to fall off, and they look bad, so just quickly
      // kill the shake once it falls below a low threshold.
      if (this.magnitude < 2) {
        this.magnitude = 0;
      }
    }
    position.x = (float) ((Math.random() - 0.5) * magnitude);
    position.y = (float) ((Math.random() - 0.5) * magnitude);
  }
}
