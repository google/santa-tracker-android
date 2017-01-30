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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RadialGradient;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A text view with a radial gradient, used for the score display in game.
 */
public class RadialGradientTextView extends TextView {
  private static final int CENTER_COLOR = 0xfffeec51;
  private static final int EDGE_COLOR = 0xfffdbe38;

  private RadialGradient textGradient;

  public RadialGradientTextView(Context context) {
    this(context, null);
  }

  public RadialGradientTextView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public RadialGradientTextView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    // Draw the shadow.
    getPaint().setShadowLayer(getShadowRadius(), getShadowDx(), getShadowDy(), getShadowColor());
    getPaint().setShader(null);
    super.onDraw(canvas);

    // Draw the gradient filled text.
    getPaint().clearShadowLayer();
    getPaint().setShader(textGradient);
    super.onDraw(canvas);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (w > 0 && h > 0) {
      textGradient = new RadialGradient(
          w / 2, h / 2, Math.min(w, h) / 2, CENTER_COLOR, EDGE_COLOR, TileMode.CLAMP);
    }
  }
}
