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
package com.google.android.apps.santatracker.doodles.waterpolo;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.apps.santatracker.doodles.shared.Actor;

/**
 * Handles rendering for the water polo game.
 * Copy and pasted from JumpingView.
 */
public class WaterPoloView extends View {
  private static final String TAG = WaterPoloView.class.getSimpleName();

  private static final int COLOR_FLOOR = 0xFFA6FFFF;

  private WaterPoloModel model;
  private float currentScale;
  private float currentOffsetX = 0;  // In game units
  private float currentOffsetY = 0;

  public WaterPoloView(Context context) {
    this(context, null);
  }

  public WaterPoloView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public WaterPoloView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void setModel(WaterPoloModel model) {
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
      canvas.drawColor(COLOR_FLOOR);

      // Fit-to-screen & center.
      currentScale = Math.min(canvas.getWidth() / (float) WaterPoloModel.WATER_POLO_WIDTH,
          canvas.getHeight() / (float) WaterPoloModel.WATER_POLO_HEIGHT);

      currentOffsetX = (canvas.getWidth() / currentScale - WaterPoloModel.WATER_POLO_WIDTH) / 2;
      currentOffsetY = (canvas.getHeight() / currentScale - WaterPoloModel.WATER_POLO_HEIGHT) / 2;

      model.moveSlide(currentOffsetX);

      canvas.scale(currentScale, currentScale);
      canvas.translate(currentOffsetX - model.cameraShake.position.x,
          currentOffsetY - model.cameraShake.position.y);

      for (int i = 0; i < model.actors.size(); i++) { // Avoiding iterator to avoid garbage.
        Actor actor = model.actors.get(i);
        if (!actor.hidden) {
          actor.draw(canvas);
        }
      }
      canvas.restore();
    }
  }
}
