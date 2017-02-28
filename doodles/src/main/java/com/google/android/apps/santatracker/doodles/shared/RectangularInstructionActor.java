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
import com.google.android.apps.santatracker.doodles.shared.AnimatedSprite.AnimatedSpriteListener;

/**
 * An actor that shows instructions for a game.
 */
public class RectangularInstructionActor extends Actor {

  private static final int RECTANGLE_CENTER_X = 257;
  private static final int RECTANGLE_CENTER_Y = 343;

  private static final int FRAME_BUBBLE_APPEARS = 2;

  public AnimatedSprite rectangle;
  public AnimatedSprite diagram;
  private float diagramScale = 1;
  private float diagramAlpha = 1;
  private boolean animationIsReversed = false;

  /**
   * @param diagram Animated sprite showing instructions in a loop.
   */
  public RectangularInstructionActor(Resources resources, AnimatedSprite diagram) {
    this.rectangle = AnimatedSprite.fromFrames(resources, Sprites.tutoappear_new);
    this.diagram = diagram;

    // Off-center anchor point lets us match the rectangle's animation with a simple scale.
    diagram.setAnchor(diagram.frameWidth / 2, diagram.frameHeight / 2);

    rectangle.setLoop(false);
    rectangle.addListener(new AnimatedSpriteListener() {
      @Override
      public void onFrame(int index) {
        // Scale (and fade) the diagram to match the rectangle.
        int maxFrame = rectangle.getNumFrames() - 1;
        index = animationIsReversed ? maxFrame - index : index;
        float percent = maxFrame == 0 ? 1 : (float) index / maxFrame;
        if (index < FRAME_BUBBLE_APPEARS) {
          percent = 0;
        }
        diagramScale = percent;
        diagramAlpha = percent;
      }

      @Override
      public void onFinished() {
        if (animationIsReversed) {
          hidden = true;
        }
      }
    });
  }

  public void show() {
    if (animationIsReversed) {
      reverseAnimation();
    }
    hidden = false;
    rectangle.setFrameIndex(0);
    diagramAlpha = 0;
    diagramScale = 0;
    update(0);
  }

  public void hide() {
    if (!animationIsReversed) {
      reverseAnimation();
    }
    rectangle.setFrameIndex(0);
    update(0);
  }

  private void reverseAnimation() {
    rectangle.reverseFrames();
    animationIsReversed = !animationIsReversed;
  }

  public void setDiagram(AnimatedSprite diagram) {
    diagram.setAnchor(diagram.frameWidth / 2, diagram.frameHeight / 2);
    this.diagram = diagram;
  }

  public float getScaledWidth() {
    return rectangle.frameWidth * scale;
  }

  public float getScaledHeight() {
    return rectangle.frameHeight * scale;
  }

  @Override
  public void update(float deltaMs) {
    super.update(deltaMs);

    rectangle.update(deltaMs);
    rectangle.setPosition(position.x, position.y);
    rectangle.setRotation(rotation);
    rectangle.setHidden(hidden);
    rectangle.setAlpha(alpha);
    rectangle.setScale(scale, scale);

    diagram.update(deltaMs);
    // Center diagram in rectangle.
    diagram.setPosition(position.x + RECTANGLE_CENTER_X * scale,
        position.y + RECTANGLE_CENTER_Y * scale);
    diagram.setRotation(rotation);
    diagram.setHidden(hidden);
    // Diagram has to take both this.alpha and diagramAlpha into account.
    diagram.setAlpha(alpha * diagramAlpha);
    // Same with scale.
    diagram.setScale(scale * diagramScale * 1.3f, scale * diagramScale * 1.3f);
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    if (!hidden) {
      rectangle.draw(canvas);
      diagram.draw(canvas);
    }
  }
}
