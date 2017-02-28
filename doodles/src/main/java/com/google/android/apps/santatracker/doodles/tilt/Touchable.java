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
 * An actor which can be touched in the level editor.
 */
public interface Touchable {
  boolean canHandleTouchAt(Vector2D worldCoords, float cameraScale);
  void startTouchAt(Vector2D worldCoords, float cameraScale);

  /**
   * Handle a move event internally.
   * @param delta the movement vector
   * @return true if the move event has been handled, false otherwise.
   */
  boolean handleMoveEvent(Vector2D delta);

  /**
   * Handle a long press internally.
   * @return true if the long press has been handled, false otherwise.
   */
  boolean handleLongPress();
}
