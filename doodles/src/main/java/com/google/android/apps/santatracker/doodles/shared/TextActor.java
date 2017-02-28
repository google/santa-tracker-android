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

import android.content.res.AssetManager;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;

/**
 * Draws text on the screen.
 */
public class TextActor extends Actor {
  private static final String TAG = TextActor.class.getSimpleName();

  private String text;
  private Paint paint;
  private Rect bounds = new Rect();
  private float previousAlpha;

  public boolean hidden;
  private boolean centeredVertically;

  public TextActor(String text) {
    this.paint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
    // Set text size using px instead of dip so that the scale of text needed to match a bitmap
    // sprite stays the same across devices.
    paint.setTextSize(12);
    setColor(Color.BLACK);
    setText(text);
  }

  public void setText(String text) {
    this.text = text;
    paint.getTextBounds(text, 0, text.length(), bounds);
  }

  public String getText() {
    return text;
  }

  /**
   * @return The scaled width of the text.
   */
  public float getWidth() {
    return bounds.width() * scale;
  }

  /**
   * @return The scaled height of the text.
   */
  public float getHeight() {
    return bounds.height() * scale;
  }

  public void scaleToFitScreen(int screenWidth, int screenHeight) {
    scale = Math.min(
        screenWidth / bounds.width(),
        screenHeight / bounds.height());
  }

  public void setColor(int color) {
    paint.setColor(color);
    previousAlpha = paint.getAlpha() / 255f;
  }

  public void setFont(AssetManager assetManager, String fontPath) {
    Typeface typeface = Typeface.createFromAsset(assetManager, fontPath);
    paint.setTypeface(typeface);
  }

  public void enableBlur(float blurRadius) {
    paint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));
  }

  public void alignCenter() {
    paint.setTextAlign(Align.CENTER);
  }

  public void setBold(boolean bold) {
    paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
  }

  public void centerVertically(boolean center) {
    this.centeredVertically = center;
  }

  @Override
  public void draw(Canvas canvas) {
    if (previousAlpha != alpha) {
      previousAlpha = alpha;
      paint.setAlpha(Math.round(alpha * 255));
    }
    if (hidden) {
      return;
    }
    float yOffset = centeredVertically ? -getHeight() / 2 : 0;
    canvas.save();
    canvas.scale(scale, scale);
    // 1. drawText has y=0 at the baseline of the text. To make this work like other actors, y=0
    // should be the top of the bounding box, so add bounds.top to y.
    // 2. drawText also has rounding error on the (x, y) coordinates, which makes text jitter
    // around if you are tweening scale, so use canvas.translate() to position instead.
    canvas.translate(position.x / scale, (position.y + yOffset) / scale - bounds.top);
    canvas.drawText(text, 0, 0, paint);
    canvas.restore();

  }
}
