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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;

import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.physics.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * An actor class which represents an arbitrarily-colored rectangle.
 */
public class ColoredRectangleActor extends Actor implements Touchable {
  /* Default color is black*/
  public static final String UNSPECIFIED = "unspecified";

  /* Golf colors */
  public static final String TEE_GREEN = "tee";
  public static final String FAIRWAY_GREEN = "fairway";

  /* Swimming colors */
  public static final String DISTANCE_30M = "30m";
  public static final String DISTANCE_50M = "50m";
  public static final String DISTANCE_100M = "100m";
  public static final String DISTANCE_LEVEL_LENGTH = "level length";
  public static final String DISTANCE_PR = "pr";
  public static final String STARTING_BLOCK = "start";

  public static final String DIMENS_X_KEY = "dimens x";
  public static final String DIMENS_Y_KEY = "dimens y";


  public static final Map<String, Integer> TYPE_TO_COLOR_MAP;
  static {
    TYPE_TO_COLOR_MAP = new HashMap<>();
    /* Golf */
    TYPE_TO_COLOR_MAP.put(TEE_GREEN, Constants.LIGHT_GREEN);
    TYPE_TO_COLOR_MAP.put(FAIRWAY_GREEN, Constants.DARK_GREEN);
    /* Swimming */
    TYPE_TO_COLOR_MAP.put(DISTANCE_30M, 0x44cd7f32);
    TYPE_TO_COLOR_MAP.put(DISTANCE_50M, 0x44c0c0c0);
    TYPE_TO_COLOR_MAP.put(DISTANCE_100M, 0x44ffd700);
    TYPE_TO_COLOR_MAP.put(DISTANCE_LEVEL_LENGTH, 0x44ffffff);
    TYPE_TO_COLOR_MAP.put(DISTANCE_PR, 0x4400cc00);
    TYPE_TO_COLOR_MAP.put(STARTING_BLOCK, 0xff4993a4);
    TYPE_TO_COLOR_MAP.put(UNSPECIFIED, 0xff000000);
  }

  /**
   * A direction used to pull the boundaries of the colored rectangle.
   */
  private enum Direction {
    NONE,
    UP,
    DOWN,
    LEFT,
    RIGHT,
  }
  private Direction selectedDirection;

  public String type;
  public Vector2D dimens;
  private Paint paint;
  private Paint midpointPaint;

  private Vector2D upMidpoint = Vector2D.get();
  private Vector2D downMidpoint = Vector2D.get();
  private Vector2D leftMidpoint = Vector2D.get();
  private Vector2D rightMidpoint = Vector2D.get();

  public ColoredRectangleActor(Vector2D position, Vector2D dimens) {
    this(position, dimens, UNSPECIFIED);
  }

  public ColoredRectangleActor(Vector2D position, Vector2D dimens, String type) {
    super(position, Vector2D.get());

    this.dimens = dimens;
    this.type = type;
    this.zIndex = -1;

    if (type.equals(FAIRWAY_GREEN)) {
      this.zIndex = -2;
    }

    selectedDirection = Direction.NONE;
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(TYPE_TO_COLOR_MAP.get(type));
    paint.setStyle(Style.FILL);

    midpointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    midpointPaint.setColor(Color.WHITE);

    updateExtents();
  }

  @Override
  public void draw(Canvas canvas) {
    canvas.drawRect(position.x, position.y, position.x + dimens.x, position.y + dimens.y, paint);
  }

  public void setStyle(Style style) {
    paint.setStyle(style);
  }

  public void setStrokeWidth(float width) {
    paint.setStrokeWidth(width);
    paint.setStrokeCap(Cap.ROUND);
  }

  public void setColor(int color) {
    paint.setColor(color);
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public boolean canHandleTouchAt(Vector2D worldCoords, float cameraScale) {
    Vector2D lowerRight = Vector2D.get(position).add(dimens);
    boolean retVal = Util.pointIsWithinBounds(position, lowerRight, worldCoords)
        || worldCoords.distanceTo(upMidpoint) < Constants.SELECTION_RADIUS
        || worldCoords.distanceTo(downMidpoint) < Constants.SELECTION_RADIUS
        || worldCoords.distanceTo(leftMidpoint) < Constants.SELECTION_RADIUS
        || worldCoords.distanceTo(rightMidpoint) < Constants.SELECTION_RADIUS;

    lowerRight.release();
    return retVal;
  }

  @Override
  public void startTouchAt(Vector2D worldCoords, float cameraScale) {
    if (worldCoords.distanceTo(upMidpoint) < Constants.SELECTION_RADIUS) {
      selectedDirection = Direction.UP;
    } else if (worldCoords.distanceTo(downMidpoint) < Constants.SELECTION_RADIUS) {
      selectedDirection = Direction.DOWN;
    } else if (worldCoords.distanceTo(leftMidpoint) < Constants.SELECTION_RADIUS) {
      selectedDirection = Direction.LEFT;
    } else if (worldCoords.distanceTo(rightMidpoint) < Constants.SELECTION_RADIUS) {
      selectedDirection = Direction.RIGHT;
    } else {
      selectedDirection = Direction.NONE;
    }
  }

  @Override
  public boolean handleMoveEvent(Vector2D delta) {
    if (selectedDirection == Direction.NONE) {
      position.subtract(delta);
    } else if (selectedDirection == Direction.UP) {
      position.y -= delta.y;
      dimens.y += delta.y;
    } else if (selectedDirection == Direction.DOWN) {
      dimens.y -= delta.y;
    } else if (selectedDirection == Direction.LEFT) {
      position.x -= delta.x;
      dimens.x += delta.x;
    } else {
      // Direction.RIGHT
      dimens.x -= delta.x;
    }
    updateExtents();
    return true;
  }

  @Override
  public boolean handleLongPress() {
    return false;
  }

  @Override
  public JSONObject toJSON() throws JSONException {
    JSONObject json = new JSONObject();
    json.put(TYPE_KEY, getType());
    json.put(X_KEY, position.x);
    json.put(Y_KEY, position.y);
    json.put(DIMENS_X_KEY, dimens.x);
    json.put(DIMENS_Y_KEY, dimens.y);
    return json;
  }

  private void updateExtents() {
    upMidpoint.set(position).add(dimens.x / 2, 0);
    downMidpoint.set(position).add(dimens.x / 2, dimens.y);
    leftMidpoint.set(position).add(0, dimens.y / 2);
    rightMidpoint.set(position).add(dimens.x, dimens.y / 2);
  }

  public static ColoredRectangleActor fromJSON(JSONObject json) throws JSONException {
    String type = json.getString(Actor.TYPE_KEY);
    Vector2D position = Vector2D.get(
        (float) json.optDouble(X_KEY, 0), (float) json.optDouble(Y_KEY, 0));
    Vector2D dimens = Vector2D.get(
        (float) json.optDouble(DIMENS_X_KEY, 0), (float) json.optDouble(DIMENS_Y_KEY, 0));
    return new ColoredRectangleActor(position, dimens, type);
  }
}
