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
import android.graphics.Canvas;

/**
 * The base watermelon actor used by both the golf and running game.
 */
public class WatermelonBaseActor extends Actor {

  public AnimatedSprite bodySprite;
  public AnimatedSprite shadowSprite;

  public WatermelonBaseActor(Resources resources) {
    bodySprite = AnimatedSprite.fromFrames(resources, Sprites.snowball);
    shadowSprite = AnimatedSprite.fromFrames(resources, Sprites.melon_shadow);

    bodySprite.setAnchor(bodySprite.frameWidth / 2, bodySprite.frameHeight / 2);
    shadowSprite.setAnchor(shadowSprite.frameWidth * 0.5f, shadowSprite.frameHeight * 0.35f);
  }

  @Override
  public void update(float deltaMs) {
    super.update(deltaMs);

    bodySprite.update(deltaMs);
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    drawSprite(shadowSprite, canvas);
    drawSprite(bodySprite, canvas);
  }

  private void drawSprite(AnimatedSprite sprite, Canvas canvas) {
    sprite.setPosition(position.x, position.y);
    sprite.setScale(scale, scale);
    sprite.draw(canvas);
  }

  private void drawSpriteYInverted(AnimatedSprite sprite, Canvas canvas) {
    sprite.setPosition(position.x, position.y);
    sprite.setScale(scale, -scale);
    sprite.draw(canvas);
  }
}
