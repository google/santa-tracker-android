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

/**
 * Tweens properties of Actors (currently, position & scale).
 *
 * <p>If any of the from-values is left unset, the from value will be initialized on the first
 * update of the tween. This allows us to tween from an unspecified value, and have it do so
 * smoothly when the tween actually runs, regardless of the actual starting value.</p>
 */
public class ActorTween extends Tween {
  private static final String TAG = ActorTween.class.getSimpleName();

  private final Actor actor;
  private Interpolator interpolator;
  // Use Float objects set to null so that we can detect & support unspecified values.
  private Float xStart = null;
  private Float yStart = null;
  private Float xEnd = null;
  private Float yEnd = null;
  private Float scaleStart;
  private Float scaleEnd;
  private Callback finishedCallback = null;
  private Float rotationStart;
  private Float rotationEnd;
  private Float alphaStart;
  private Float alphaEnd;

  private boolean firstUpdate = true;

  public ActorTween(Actor actor) {
    super(0);
    this.actor = actor;
    interpolator = Interpolator.LINEAR;
  }

  public ActorTween from(float x, float y) {
    fromX(x);
    fromY(y);
    return this;
  }

  public ActorTween fromX(float x) {
    xStart = x;
    return this;
  }

  public ActorTween fromY(float y) {
    yStart = y;
    return this;
  }

  public ActorTween to(float x, float y) {
    toX(x);
    toY(y);
    return this;
  }

  public ActorTween toX(float x) {
    xEnd = x;
    return this;
  }

  public ActorTween toY(float y) {
    yEnd = y;
    return this;
  }

  public ActorTween scale(float fromScale, float toScale) {
    this.scaleStart = fromScale;
    this.scaleEnd = toScale;
    return this;
  }

  public ActorTween scale(float toScale) {
    this.scaleEnd = toScale;
    return this;
  }

  public ActorTween withRotation(float fromRadians, float toRadians) {
    this.rotationStart = fromRadians;
    this.rotationEnd = toRadians;
    return this;
  }

  public ActorTween withRotation(float toRadians) {
    this.rotationEnd = toRadians;
    return this;
  }

  public ActorTween withAlpha(float fromAlpha, float toAlpha) {
    this.alphaStart = fromAlpha;
    this.alphaEnd = toAlpha;
    return this;
  }

  public ActorTween withAlpha(float toAlpha) {
    this.alphaEnd = toAlpha;
    return this;
  }

  public ActorTween withDuration(float seconds) {
    this.durationSeconds = seconds;
    return this;
  }

  public ActorTween withInterpolator(Interpolator interpolator) {
    this.interpolator = interpolator;
    return this;
  }

  public ActorTween whenFinished(Callback c) {
    finishedCallback = c;
    return this;
  }

  protected void setInitialValues() {
    xStart = (xStart != null) ? xStart : actor.position.x;
    yStart = (yStart != null) ? yStart : actor.position.y;
    scaleStart = (scaleStart != null) ? scaleStart : actor.scale;
    rotationStart = (rotationStart != null) ? rotationStart : actor.rotation;
    alphaStart = (alphaStart != null) ? alphaStart : actor.alpha;
  }

  @Override
  protected void updateValues(float percentDone) {
    if (firstUpdate) {
      firstUpdate = false;
      setInitialValues();
    }
    // Perform null checks here so that we don't interpolate over unspecified fields.
    if (xEnd != null) {
      actor.position.x = interpolator.getValue(percentDone, xStart, xEnd);
    }
    if (yEnd != null) {
      actor.position.y = interpolator.getValue(percentDone, yStart, yEnd);
    }
    if (scaleEnd != null) {
      actor.scale = interpolator.getValue(percentDone, scaleStart, scaleEnd);
    }
    if (rotationEnd != null) {
      actor.rotation = interpolator.getValue(percentDone, rotationStart, rotationEnd);
    }
    if (alphaEnd != null) {
      actor.alpha = interpolator.getValue(percentDone, alphaStart, alphaEnd);
    }
  }

  @Override
  protected void onFinish() {
    if (finishedCallback != null) {
      finishedCallback.call();
    }
  }

  /**
   * Callback to be called at the end of a tween (can be used to chain tweens together, to hide
   * an actor when a tween finishes, etc.)
   */
  public interface Callback {
    void call();
  }
}
