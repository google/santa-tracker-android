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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.physics.Polygon;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An emitter of watermelons which can run over the golf ball.
 */
public class WatermelonEmitter extends CollisionActor {
  public static final String TYPE = "Watermelon emitter";

  public static final int NUM_WATERMELONS = 3;
  public static final float WATERMELON_VELOCITY = 750.0f;
  public static final long LIFETIME_MS = 7500;

  private List<WatermelonActor> watermelons = new ArrayList<>();
  private long elapsedMs;

  public WatermelonEmitter(Polygon collisionBody, Resources resources) {
    super(collisionBody);
    this.zIndex = 1;
    collisionBody.setPaintColors(0xffff6385, 0xff6cfc9b, 0x6400ff00);

    for (int i = 0; i < NUM_WATERMELONS; i++) {
      WatermelonActor watermelon = WatermelonActor.create(collisionBody.min, resources);
      watermelon.setVelocity(Vector2D.get(0, WATERMELON_VELOCITY));
      watermelon.delayMs = (i + 1) * LIFETIME_MS / NUM_WATERMELONS;
      watermelons.add(watermelon);
    }
  }

  @Override
  public void update(float deltaMs) {
    elapsedMs += deltaMs;
    for (int i = 0; i < watermelons.size(); i++) {
      WatermelonActor watermelon = watermelons.get(i);
      if (elapsedMs > watermelon.delayMs) {
        watermelon.update(deltaMs);
        if (watermelon.ageMs > LIFETIME_MS) {
          reset(watermelon);
        }
      }
    }
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);
    for (int i = 0; i < watermelons.size(); i++) {
      WatermelonActor watermelon = watermelons.get(i);
      watermelon.draw(canvas);
    }
  }

  @Override
  public boolean resolveCollision(Actor other, float deltaMs) {
    for (int i = 0; i < watermelons.size(); i++) {
      WatermelonActor watermelon = watermelons.get(i);
      if (watermelon.resolveCollision(other, deltaMs)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String getType() {
    return WatermelonEmitter.TYPE;
  }

  private void reset(WatermelonActor actor) {
    actor.setPosition(collisionBody.min);
    actor.ageMs = 0;
  }

  public static WatermelonEmitter fromJSON(JSONObject json, Context context) throws JSONException {
    return new WatermelonEmitter(
        Polygon.fromJSON(json.getJSONArray(POLYGON_KEY)), context.getResources());
  }
}
