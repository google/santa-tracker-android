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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for different characters on the screen.
 */
public class Actor implements Comparable<Actor> {
  public static final float INFINITE_MASS = 0.0f;
  public static final String TYPE = "Actor";
  public static final String TYPE_KEY = "type";
  public static final String X_KEY = "x";
  public static final String Y_KEY = "y";

  public Vector2D positionBeforeFrame;
  // Assumes (0, 0) is upper-left corner of screen, with +y down and +x right.
  public Vector2D position;
  public Vector2D velocity;

  // Doesn't do anything yet (except for TextActors)
  public float scale = 1.0f;

  // The rotation of the actor in radians. Positive means clockwise, negative means anticlockwise.
  public float rotation = 0.0f;

  // Doesn't do anything yet (except for in tennis)
  public boolean hidden = false;

  // Specify z-index so that actors can be sorted before drawing. Higher is in front, lower in back.
  public int zIndex = 0;

  // 0: transparent, 1: opaque.
  public float alpha = 1;

  // Bounciness.
  public float restitution = 1.0f;
  public float inverseMass = INFINITE_MASS;

  public Actor() {
    this(Vector2D.get(0, 0), Vector2D.get(0, 0));
  }

  public Actor(Vector2D position, Vector2D velocity) {
    this.position = position;
    this.positionBeforeFrame = Vector2D.get(position);
    this.velocity = velocity;
  }

  public void update(float deltaMs) {
    positionBeforeFrame.set(this.position);
    float deltaSeconds = deltaMs / 1000.0f;
    this.position.x += velocity.x * deltaSeconds;
    this.position.y += velocity.y * deltaSeconds;
  }

  public void draw(Canvas canvas) {
    // Nothing to do for base class implementation.
  }

  @Override
  public int compareTo(Actor another) {
    int zDiff = zIndex - another.zIndex;
    if (zDiff != 0) {
      return zDiff;
    } else {
      // As a fallback, compare the y positions. Obstacles with smaller y values (i.e., higher on
      // the screen) should come first.
      float positionDiff = position.y - another.position.y;
      if (positionDiff > 0) {
        return 1;
      } else if (positionDiff < 0) {
        return -1;
      } else {
        return 0;
      }
    }
  }

  public JSONObject toJSON() throws JSONException {
    return null;
  }

  public String getType() {
    return TYPE;
  }
}
