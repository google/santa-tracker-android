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

import android.content.res.Resources;
import com.google.android.apps.santatracker.doodles.tilt.ColoredRectangleActor;

/**
 * Debug flags (collected in one place).
 */
public class Debug {
  // Draw positions of things as they move around. Tennis only for now.
  public static final boolean DRAW_POSITIONS = false;

  // Draw the targets that the players hit towards. Tennis only for now.
  public static final boolean DRAW_TARGETS = false;

  // Draw the location of each hit. Tennis only for now.
  public static final boolean MARK_HITS = false;

  // Play without user input.  Tennis only for now.
  public static final boolean AUTO_PLAY = false;

  // Slow down or speed up everything (scales deltaMs).
  public static final float SPEED_MULTIPLIER = 1f;

  // Skip frames (slows game down without affecting deltaMs).
  public static final float FRAME_SKIP = 0;

  public static final boolean SHOW_SECONDARY_MENU_ICONS = false;

  // Return a SpriteActor of an "X" marker, centered over (x, y). For marking positions for
  // debugging.
  public static SpriteActor makeDebugMarkerX(Resources resources, float x, float y) {
    AnimatedSprite sprite = AnimatedSprite.fromFrames(resources, Sprites.debug_marker);
    return new SpriteActor(sprite,
        Vector2D.get(x - sprite.frameWidth / 2, y - sprite.frameHeight / 2),
        Vector2D.get(0, 0));
  }

  // Return a tiny rectangle actor, centered over (x, y). For marking positions for debugging.
  public static Actor makeDebugMarkerPoint(float x, float y) {
    float size = 3;
    return new ColoredRectangleActor(Vector2D.get(x - size / 2, y - size / 2),
        Vector2D.get(size, size), "fairway");
  }
}
