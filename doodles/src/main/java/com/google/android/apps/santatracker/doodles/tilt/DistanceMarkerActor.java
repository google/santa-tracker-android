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
package com.google.android.apps.santatracker.doodles.tilt;

import com.google.android.apps.santatracker.doodles.shared.Vector2D;

/**
 * A colored rectangle actor which marks distance in the swimming game.
 */
public class DistanceMarkerActor extends ColoredRectangleActor {
  private static final int HEIGHT = 200;

  public DistanceMarkerActor(int positionInMeters, String type) {
    this(positionInMeters, type, HEIGHT);
  }

  public DistanceMarkerActor(int positionInMeters, String type, float height) {
    super(Vector2D.get(0, SwimmingModel.getWorldYFromMeters(positionInMeters)),
        Vector2D.get(SwimmingModel.LEVEL_WIDTH, height), type);
    zIndex = -2;
  }
}
