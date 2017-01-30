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
 * An actor that looks like a button, but doesn't actually have any logic to detect or respond to
 * clicks. We use this to provide a UI affordance to the user. Even though all our games allow you
 * to click anywhere on the screen, having something that looks like a button helps the users
 * to know how to play the game.
 */
public class FakeButtonActor extends Actor {

  public final AnimatedSprite sprite; // Public so you can get to frameWidth/frameHeight.
  private final int lastFrameIndex;

  /**
   * The sprite for the button should conform to the following:
   * 1. The last frame of the animation will be the  "idle" state of the button.
   * 2. When the button is pressed, the animation will be played through, starting from frame 0
   *    and ending back on the last frame.
   * 3. The FPS of the sprite should be set to give the button press animation the desired duration.
   */
  public FakeButtonActor(AnimatedSprite sprite) {
    super();
    this.sprite = sprite;
    sprite.setLoop(false);
    lastFrameIndex = sprite.getNumFrames() - 1;
    sprite.setFrameIndex(lastFrameIndex);
  }

  public void press() {
    sprite.setFrameIndex(0);
  }

  public void pressAndHold() {
    sprite.setFrameIndex(0);
    sprite.setPaused(true);
  }

  public void release() {
    sprite.setPaused(false);
  }

  @Override
  public void update(float deltaMs) {
    super.update(deltaMs);
    sprite.update(deltaMs);
    sprite.setPosition(position.x, position.y);
    sprite.setRotation(rotation);
    sprite.setHidden(hidden);
    sprite.setAlpha(alpha);
    sprite.setScale(scale, scale);
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    sprite.draw(canvas);
  }
}
