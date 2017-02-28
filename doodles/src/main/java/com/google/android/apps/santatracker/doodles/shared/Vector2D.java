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

import java.util.Stack;

/**
 * A basic 2D vector, with convenience functions to interact with it.
 */
public class Vector2D {
  private static final int MAX_POOL_SIZE = 50;
  private static final Stack<Vector2D> vectorPool = new Stack<>();

  public float x;
  public float y;

  private Vector2D(float x, float y) {
    this.x = x;
    this.y = y;
  }

  public static Vector2D get() {
    return get(0, 0);
  }

  public static synchronized  Vector2D get(float x, float y) {
    if (!vectorPool.isEmpty()) {
      Vector2D v = vectorPool.pop();
      v.set(x, y);
      return v;
    } else {
      return new Vector2D(x, y);
    }
  }

  public static Vector2D get(Vector2D other) {
    return get(other.x, other.y);
  }

  /**
   * Release this vector back into the vector pool. Note that, once this has been called, the
   * vector object may be re-used, and there is no guarantee that the released object will act as
   * expected.
   */
  public void release() {
    if (vectorPool.size() < MAX_POOL_SIZE) {
      vectorPool.push(this);
    }
  }

  public Vector2D normalize() {
    float length = getLength();
    if (length == 0) {
      set(0, 0);
    } else {
      set (x / length, y / length);
    }
    return this;
  }

  public Vector2D toNormal() {
    return set(y, -x).normalize();
  }

  public float getLength() {
    return getLength(x, y);
  }

  public static float getLength(float x, float y) {
    return (float) Math.sqrt(x * x + y * y);
  }

  public Vector2D add(Vector2D rhs) {
    set(this.x + rhs.x, this.y + rhs.y);
    return this;
  }

  public Vector2D add(float x, float y) {
    set(this.x + x, this.y + y);
    return this;
  }

  public Vector2D subtract(Vector2D rhs) {
    set(this.x - rhs.x, this.y - rhs.y);
    return this;
  }

  public Vector2D subtract(float x, float y) {
    set(this.x - x, this.y - y);
    return this;
  }

  public Vector2D scale(float factor) {
    set(this.x * factor, this.y * factor);
    return this;
  }

  public float dot(Vector2D rhs) {
    return x * rhs.x + y * rhs.y;
  }

  public Vector2D rotate(float radians) {
    double cos = Math.cos(radians);
    double sin = Math.sin(radians);
    set((float) (x * cos - y * sin), (float) (x * sin + y * cos));
    return this;
  }

  public Vector2D set(Vector2D other) {
    x = other.x;
    y = other.y;
    return this;
  }

  public Vector2D set(float x, float y) {
    this.x = x;
    this.y = y;
    return this;
  }

  @Override
  public String toString() {
    return "(" + x + ", " + y + ")";
  }

  public float distanceTo(Vector2D other) {
    float dx = x - other.x;
    float dy = y - other.y;
    return (float) Math.sqrt(dx * dx + dy * dy);
  }
}
