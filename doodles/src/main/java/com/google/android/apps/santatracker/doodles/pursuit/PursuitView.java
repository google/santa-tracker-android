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
package com.google.android.apps.santatracker.doodles.pursuit;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import com.google.android.apps.santatracker.doodles.shared.Actor;

/**
 * Handles rendering for the second version of the running game.
 */
public class PursuitView extends View {
  private static final String TAG = PursuitView.class.getSimpleName();

  private PursuitModel model;
  private float currentScale;
  private float currentOffsetX = 0;  // In game units
  private float currentOffsetY = 0;

  public PursuitView(Context context) {
    this(context, null);
  }

  public PursuitView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public PursuitView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void setModel(PursuitModel model) {
    this.model = model;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (model == null) {
      return;
    }
    synchronized (model) {
      super.onDraw(canvas);
      canvas.save();

      // Fit-to-screen & center.
      currentScale = Math.min(canvas.getWidth() / (float) PursuitModel.WIDTH,
          canvas.getHeight() / (float) PursuitModel.HEIGHT);

      currentOffsetX = (canvas.getWidth() / currentScale - PursuitModel.WIDTH) / 2;
      currentOffsetY = (canvas.getHeight() / currentScale - PursuitModel.HEIGHT) / 2;

      canvas.scale(currentScale * model.camera.scale, currentScale * model.camera.scale);
      canvas.translate(currentOffsetX - model.cameraShake.position.x - model.camera.position.x,
          currentOffsetY - model.cameraShake.position.y - model.camera.position.y);

      // Draws the beach and the sidewalk.
      model.backgroundActor.draw(canvas);

      // Draws the actors
      synchronized (model.actors) {
        for (int i = 0; i < model.actors.size(); i++) {
          Actor actor = model.actors.get(i);
          if (!actor.hidden) {
            actor.draw(canvas);
          }
        }
      }

      // Draws tree, umbrellas and their shadows.
      model.backgroundActor.drawTop(canvas);

      canvas.restore();
      canvas.save();

      canvas.scale(currentScale * model.camera.scale, currentScale * model.camera.scale);
      canvas.translate(currentOffsetX, currentOffsetY);

      // Draws the UI
      for (int i = 0; i < model.ui.size(); i++) {
        Actor actor = model.ui.get(i);
        if (!actor.hidden) {
          actor.draw(canvas);
        }
      }

      canvas.restore();
    }
  }
}
