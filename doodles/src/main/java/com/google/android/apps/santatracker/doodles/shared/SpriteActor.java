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

import android.graphics.Canvas;

/**
 * A generic actor class for game objects.
 */
public class SpriteActor extends Actor {
  public static final String TYPE = "Sprite actor";

  public AnimatedSprite sprite;
  private final String type;
  // If true, then this.scale will be ignored. This would allow you to call sprite.setScale(x, y)
  // without the effect getting overwritten.
  public boolean ignoreScale = false;

  public SpriteActor(AnimatedSprite sprite, Vector2D position, Vector2D velocity) {
    this(sprite, position, velocity, TYPE);
  }

  public SpriteActor(AnimatedSprite sprite, Vector2D position, Vector2D velocity, String type) {
    super(position, velocity);
    this.sprite = sprite;
    this.type = type;
  }

  @Override
  public void update(float deltaMs) {
    super.update(deltaMs);
    sprite.update(deltaMs);
  }

  @Override
  public void draw(Canvas canvas) {
    draw(canvas, 0, 0, sprite.frameWidth * scale, sprite.frameHeight * scale);
  }

  public void draw(
        Canvas canvas, float xOffset, float yOffset, float width, float height) {
    if (!hidden) {
      sprite.setRotation(rotation);
      sprite.setPosition(position.x + xOffset, position.y + yOffset);
      if (!ignoreScale) {
        sprite.setScale(width / sprite.frameWidth, height / sprite.frameHeight);
      }
      sprite.setAlpha(alpha);
      sprite.draw(canvas);
    }
  }

  @Override
  public String getType() {
    return type;
  }
}
