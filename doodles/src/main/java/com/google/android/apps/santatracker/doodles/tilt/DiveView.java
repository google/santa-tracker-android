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

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import com.google.android.apps.santatracker.doodles.R;
import com.google.android.apps.santatracker.doodles.shared.Interpolator;
import com.google.android.apps.santatracker.doodles.shared.PauseView;
import com.google.android.apps.santatracker.doodles.shared.Process;
import com.google.android.apps.santatracker.doodles.shared.ProcessChain;
import com.google.android.apps.santatracker.doodles.shared.Tween;
import com.google.android.apps.santatracker.doodles.shared.UIUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * The dive cooldown UI for the swimming game.
 */
public class DiveView extends ImageView {
  private static final int CLOCK_DURATION_MS =
      SwimmerActor.DIVE_DURATION_MS + SwimmerActor.DIVE_COOLDOWN_MS;
  private static final float BUMP_SCALE = 1.4f;
  private static final long BUMP_DURATION_MS = 200;

  private RectF viewBounds;
  private Paint paint;
  private Paint imagePaint;

  private float clockAngle;
  private Bitmap diveArrowBitmap;
  private Rect diveArrowBounds;
  private List<ProcessChain> processChains;

  public DiveView(Context context) {
    this(context, null);
  }

  public DiveView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public DiveView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(0xffffffff);
    imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    imagePaint.setXfermode(new PorterDuffXfermode(Mode.SRC_ATOP));
    viewBounds = new RectF(0, 0, getWidth(), getHeight());
    diveArrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.swimming_dive_arrow);
    diveArrowBounds = new Rect(0, 0, diveArrowBitmap.getWidth(), diveArrowBitmap.getHeight());
    processChains = new ArrayList<>();

    setLayerType(LAYER_TYPE_HARDWARE, null);
  }

  @Override
  public void onDraw(Canvas canvas) {
    float startAngle = -90;
    float sweepAngle = clockAngle;

    canvas.drawArc(viewBounds, startAngle, sweepAngle, true, paint);
    canvas.drawBitmap(diveArrowBitmap, diveArrowBounds, viewBounds, imagePaint);
  }

  @Override
  public void onSizeChanged(int w, int h, int oldw, int oldh) {
    viewBounds = new RectF(0, 0, getWidth(), getHeight());
  }

  public void update(float deltaMs) {
    ProcessChain.updateChains(processChains, deltaMs);
  }

  public void startCooldown() {
    setImageColorSaturation(0);
    show();
    Process cooldown = new Tween(CLOCK_DURATION_MS / 1000.0f) {
      @Override
      protected void updateValues(float percentDone) {
        clockAngle = Interpolator.LINEAR.getValue(percentDone, 0, 360);
        postInvalidate();
      }

      @Override
      protected void onFinish() {
        setImageColorSaturation(1);
        bump();
      }
    }.asProcess();
    processChains.add(new ProcessChain(cooldown));
  }

  public void hide() {
    UIUtil.fadeOutAndHide(this, PauseView.FADE_DURATION_MS);
  }

  public void show() {
    clockAngle = 0;
    UIUtil.showAndFadeIn(this, PauseView.FADE_DURATION_MS);
  }

  private void bump() {
    ValueAnimator bumpAnimation = UIUtil.animator(BUMP_DURATION_MS,
        new DecelerateInterpolator(),
        new AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            final float scale = (float) valueAnimator.getAnimatedValue("scale");
            post(new Runnable() {
              @Override
              public void run() {
                setScaleX(scale);
                setScaleY(scale);
              }
            });
          }
        },
        UIUtil.floatValue("scale", BUMP_SCALE, 1)
    );
    bumpAnimation.start();
  }

  private void setImageColorSaturation(float saturation) {
    ColorMatrix cm = new ColorMatrix();
    cm.setSaturation(saturation);
    imagePaint.setColorFilter(new ColorMatrixColorFilter(cm));
  }
}
