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
import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents ball in Tennis.
 */
public class BallActor extends Actor {

  private static final float MIN_SHADOW_SCALE = 0.4f;
  private static final float MAX_SHADOW_SCALE = 0.7f;
  // Highest the ball can be. Going higher than this may mess up scaling & shadow positioning.
  public static final int BALL_MAX_HEIGHT = 75;

  private static final int MAX_STREAK_LENGTH = 10; // Max # of frames back the streak can go.

  // Slightly less than the sprite's frameWidth because there are semi-transparent pixels.
  public static final int BALL_RADIUS = 14;
  private static final double STREAK_ALPHA = 0.25; // 1 = opaque, 0 = transparent
  private static final float BALL_MIN_SCALE = 0.5f; // Ball is scaled between BALL_MIN_SCALE and 1.

  private AnimatedSprite ball;
  private AnimatedSprite fireball;  // Flaming version of ball sprite (optional).
  private AnimatedSprite shadow;  // Might be null, if ball shouldn't have a shadow.
  private float height = 0;  // Height above the court.
  // Whether or not the ball should be scaled larger at higher heights (to aid illusion of ball
  // arcing through the air)
  public boolean shouldScaleWithHeight;
  private final Paint debugPaint;
  // The streak is drawn by storing a history of positions & scales going back streakLength
  // updates into the past.
  private List<Vector2D> streakPositions = new ArrayList<>();
  private List<Float> streakHeights = new ArrayList<>();
  private int streakIndex = 0;
  // This is the actual streak length. On iOS, it must be <= MAX_STREAK_LENGTH in order to fit in
  // the arrays, so we're keeping it <= here too.
  private int streakLength = 10;
  private boolean shouldDrawStreak = true;
  private Paint streakPaint = new Paint();
  private Paint streakDebugPaint = new Paint();

  // This is a field so that we don't have to create a new one every frame. Not threadsafe,
  // but draw() shouldn't be called from multiple threads so it should be ok.
  private Path path = new Path();

  // Fireball is optional.
  public BallActor(AnimatedSprite shadow, AnimatedSprite ball, AnimatedSprite fireball,
      int streakLength) {
    this.ball = ball;
    ball.setAnchor(ball.frameWidth / 2, ball.frameHeight);
    if (fireball != null) {
      fireball.setAnchor(fireball.frameWidth / 2, BALL_RADIUS);
      this.fireball = fireball;
    }

    this.shadow = shadow;
    this.streakLength = streakLength;
    if (streakLength > MAX_STREAK_LENGTH) {
      throw new IllegalArgumentException("Error: Streak length exceeds MAX_STREAK_LENGTH");
    }
    shouldScaleWithHeight = true;
    debugPaint = new Paint();
    debugPaint.setColor(0xff000000);

    streakPaint.setColor(android.graphics.Color.WHITE);
    streakPaint.setStyle(Paint.Style.FILL);
    streakPaint.setAlpha((int) (STREAK_ALPHA * 256));

    streakDebugPaint.setColor(android.graphics.Color.BLACK);
    streakDebugPaint.setStyle(Paint.Style.STROKE);

    clearStreak();
  }

  @Override
  public void update(float deltaMs) {
    super.update(deltaMs);
    ball.update(deltaMs);
    if (fireball != null) {
      fireball.update(deltaMs);
    }
    if (shadow != null) {
      shadow.update(deltaMs);
    }

    streakIndex++;
    streakPositions.get(streakIndex % streakLength).set(position);
    streakHeights.set(streakIndex % streakLength, height);

    // Prepare sprites for drawing.
    if (shadow != null) {
      float shadowScale = MIN_SHADOW_SCALE + (MAX_SHADOW_SCALE - MIN_SHADOW_SCALE)
          * (1 - Math.min(BALL_MAX_HEIGHT, height) / BALL_MAX_HEIGHT);
      if (!shouldScaleWithHeight) {
        shadowScale = MAX_SHADOW_SCALE * scale;
      }
      shadow.setScale(shadowScale, shadowScale);
      shadow.setPosition(position.x - shadowScale * shadow.frameWidth / 2,
          position.y - shadowScale * shadow.frameHeight / 2);
    }

    float ballScale = calculateScale(height);
    ball.setScale(ballScale, ballScale);
    ball.setRotation(rotation);
    ball.setPosition(position.x, position.y - height);
    if (fireball != null) {
      fireball.setScale(ballScale, ballScale);
      fireball.setRotation((float) (Math.atan2(velocity.y, velocity.x) + Math.PI / 2));
      fireball.setPosition(position.x, position.y - height - BALL_RADIUS);
    }

    // Streak. Start by working down the left side of the streak.
    path.rewind();
    for (int i = 0; i < streakLength; i++) {
      int index = (streakIndex - i) % streakLength;
      Vector2D pos = streakPositions.get(index);
      float ballRadius = BALL_RADIUS * calculateScale(streakHeights.get(index));
      float taperedRadius = ballRadius * (1 - i / (float) streakLength);
      float x = pos.x - taperedRadius;
      float y = pos.y - ballRadius - streakHeights.get(index);
      if (i == 0) {
        path.moveTo(x, y);
      } else {
        path.lineTo(x, y);
      }
    }

    // Now finish by going back up right side of streak.
    for (int i = streakLength - 1; i >= 0; i--) {
      int index = (streakIndex - i) % streakLength;
      Vector2D pos = streakPositions.get(index);
      float ballRadius = BALL_RADIUS * calculateScale(streakHeights.get(index));
      float taperedRadius = ballRadius * (1 - i / (float) streakLength);
      float x = pos.x + taperedRadius;
      float y = pos.y - ballRadius - streakHeights.get(index);
      path.lineTo(x, y);
    }
    path.close();
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    // Shadow, ball, and streak are all drawn together here, so nothing can be drawn above shadow
    // but below the ball.

    if (!hidden) {
      if (shadow != null) {
        shadow.draw(canvas);
      }
      ball.draw(canvas);
      if (fireball != null) {
        fireball.draw(canvas);
      }
      if (shouldDrawStreak) {
        canvas.drawPath(path, streakPaint);
      }

      if (Debug.DRAW_POSITIONS) {
        canvas.drawPath(path, streakDebugPaint);
        canvas.drawCircle(position.x, position.y, 2, debugPaint);
        canvas.drawCircle(position.x, position.y - height, 2, debugPaint);
      }
    }
  }

  public void clearStreak() {
    streakPositions.clear();
    streakHeights.clear();
    for (int i = 0; i < streakLength; i++) {
      streakPositions.add(Vector2D.get(position));
      streakHeights.add(height);
    }
    streakIndex = streakLength; // Start here so we never get negative indexes.
  }

  public void setHeight(float height) {
    this.height = height;
  }

  public float getHeight() {
    return height;
  }

  public void setColorForDebug(int color) {
    debugPaint.setColor(color);
  }

  public void showFireball(boolean shouldShowFireball) {
    if (fireball != null) {
      fireball.setHidden(!shouldShowFireball);
      ball.setHidden(shouldShowFireball);
      shouldDrawStreak = !shouldShowFireball;
    }
  }

  private float calculateScale(float height) {
    if (!shouldScaleWithHeight) {
      return scale;
    }
    return BALL_MIN_SCALE + (1 - BALL_MIN_SCALE) * height / BALL_MAX_HEIGHT;
  }
}
