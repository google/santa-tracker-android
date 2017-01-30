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
 * A tween that does nothing in its updateValues method. Can be used to insert pauses in chains
 * of tweens. The useful part of this is that it calls finishedCallback after durationSeconds,
 * and it fits into the existing Tween framework.
 */
public class EmptyTween extends Tween {

  public EmptyTween(float durationSeconds) {
    super(durationSeconds);
  }

  @Override
  protected void updateValues(float percentDone) {
    // Nothing to do, just waiting for the tween to end.
  }
}
