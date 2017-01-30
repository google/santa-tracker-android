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

import static com.google.android.apps.santatracker.doodles.shared.Interpolator.EASE_IN_AND_OUT;

/**
 * A camera class to contain the scale, translation, and rotation of the world. Note that the camera
 * is defined to be positioned at the top-left corner of the screen.
 */
public class Camera extends Actor {

  public int screenWidth;
  public int screenHeight;

  public Camera(int screenWidth, int screenHeight) {
    this.position = Vector2D.get();
    scale = 1.0f;

    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;
  }

  /**
    * Get the world coordinates for the screen coordinates specified.
    *
    * @param x The x value in screen space.
    * @param y The y value in screen space.
    */
  public Vector2D getWorldCoords(float x, float y) {
    return Vector2D.get(xToWorld(x), yToWorld(y));
  }

  public float xToWorld(float x) {
    return position.x + x / scale;
  }

  public float yToWorld(float y) {
    return position.y + y / scale;
  }

  /**
   * Converts a length from screen scale to world space.
   *
   * @param dimension: the length in screen space.
   * @return the length in world space.
   */
  public float toWorldScale(float dimension) {
    return dimension / scale;
  }

  /**
   * Move the center of the camera's viewport to the specified position.
   *
   * @param position The position to center the camera on.
   */
  public void focusOn(Vector2D position) {
    this.position.set(getPositionToFocusOn(position, scale));
  }

  /**
   * Get the camera position needed to focus on the specified position at the specified scale.
   *
   * @param position The position to center on.
   * @param scale The scale at which to focus.
   */
  private Vector2D getPositionToFocusOn(Vector2D position, float scale) {
    return Vector2D.get(position).subtract((screenWidth / 2) / scale, (screenHeight / 2) / scale);
  }

  /**
   * Move the camer immediately so that it can see the bounding box specified by the min and max
   * position vectors.
   *
   * @param levelMinPosition The desired minimum visible portion of the level.
   * @param levelMaxPosition The desired maximum visible portion of the level.
   */
  public void moveImmediatelyTo(Vector2D levelMinPosition, Vector2D levelMaxPosition) {
    Vector2D levelDimens = Vector2D.get(levelMaxPosition).subtract(levelMinPosition);

    float pannedScale = Math.min(screenWidth / levelDimens.x, screenHeight / levelDimens.y);
    Vector2D screenDimensInWorldCoords = Vector2D.get(screenWidth, screenHeight)
        .scale(1 / pannedScale);

    // pannedPosition = levelMinPosition - (screenDimensInWorldCoords - levelDimens) / 2
    Vector2D pannedPosition = Vector2D.get(levelMinPosition).subtract(
        (screenDimensInWorldCoords.x - levelDimens.x) * 0.5f,
        (screenDimensInWorldCoords.y - levelDimens.y) * 0.5f);

    position.set(pannedPosition);
    scale = pannedScale;

    screenDimensInWorldCoords.release();
    levelDimens.release();
    pannedPosition.release();
  }

  /**
   * Pan to the specified position over the specified duration.
   *
   * @param levelMinPosition The desired minimum visible portion of the level.
   * @param levelMaxPosition The desired maximum visible portion of the level.
   * @param duration How many seconds the pan should take.
   * @return The tween to pan the camera.
   */
  public Tween panTo(
      final Vector2D levelMinPosition, final Vector2D levelMaxPosition, float duration) {

    final Vector2D startMin = Vector2D.get(position);
    final Vector2D startMax = getMaxVisiblePosition();

    Tween panTween = new Tween(duration) {
      @Override
      protected void updateValues(float percentDone) {

        float xMin = EASE_IN_AND_OUT.getValue(percentDone, startMin.x, levelMinPosition.x);
        float xMax = EASE_IN_AND_OUT.getValue(percentDone, startMax.x, levelMaxPosition.x);
        float yMin = EASE_IN_AND_OUT.getValue(percentDone, startMin.y, levelMinPosition.y);
        float yMax = EASE_IN_AND_OUT.getValue(percentDone, startMax.y, levelMaxPosition.y);

        Vector2D min = Vector2D.get(xMin, yMin);
        Vector2D max = Vector2D.get(xMax, yMax);
        moveImmediatelyTo(min, max);
        min.release();
        max.release();
      }

      @Override
      protected void onFinish() {
        startMin.release();
        startMax.release();
      }
    };
    return panTween;
  }

  private Vector2D getMaxVisiblePosition() {
    return Vector2D.get(position.x + screenWidth / scale, position.y + screenHeight / scale);
  }
}
