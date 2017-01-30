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
package com.google.android.apps.santatracker.doodles.pursuit;

import android.content.res.Resources;
import android.graphics.Canvas;
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.AnimatedSprite;
import com.google.android.apps.santatracker.doodles.shared.Sprites;

/**
 * The breakable ribbon that acts as the finish line for the running game.
 */
public class RibbonActor extends Actor {

  private static final int Z_INDEX = 0;

  // Assets.
  private AnimatedSprite sprite;

  public RibbonActor(float x, float y, Resources resources) {
    zIndex = Z_INDEX;

    position.x = x;
    position.y = y;

    sprite = AnimatedSprite.fromFrames(resources, Sprites.running_finish_line);

    sprite.setAnchor(sprite.frameWidth / 2,
        sprite.frameHeight / 2);

    sprite.setPaused(true);
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    sprite.setPosition(position.x, position.y);
    sprite.setScale(scale, scale);
    sprite.draw(canvas);
  }
}
