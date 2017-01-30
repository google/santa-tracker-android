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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;

/**
 * The game view for the swimming game.
 */
public class SwimmingView extends View {
  private static final String TAG = SwimmingView.class.getSimpleName();
  public static final int WATER_BLUE = 0xffa6ffff;
  public static final int LINES_BLUE = 0xff00d4d4;

  private static final int LANE_LINE_WIDTH = 10;
  public static final int NUM_LANES = 5;

  private Paint swimmingLinesPaint;

  private SwimmingModel model;
  private GestureDetector editorGestureDetector;
  private ScaleGestureDetector scaleDetector;
  private GestureListener gestureListener;

  public SwimmingView(Context context) {
    this(context, null);
  }

  public SwimmingView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public SwimmingView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    swimmingLinesPaint = new Paint();
    swimmingLinesPaint.setColor(LINES_BLUE);
    swimmingLinesPaint.setStyle(Style.FILL);

    gestureListener = new GestureListener();
    editorGestureDetector = new GestureDetector(context, gestureListener);
    scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
  }

  @Override
  public void onDraw(Canvas canvas) {
    if (model == null) {
      return;
    }
    synchronized (model) {
      // Draw background
      canvas.drawColor(WATER_BLUE);

      canvas.save();

      canvas.scale(model.camera.scale, model.camera.scale);
      canvas.translate(
          -model.camera.position.x + model.cameraShake.position.x,
          -model.camera.position.y + model.cameraShake.position.y);

      drawSwimmingLines(canvas);

      model.drawActors(canvas);

      canvas.restore();

      model.drawUiActors(canvas);
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean handledTouch = false;
    if (SwimmingFragment.editorMode) {
      // Handle touch events in the editor.

      // Let the ScaleGestureDetector inspect all events.
      handledTouch = scaleDetector.onTouchEvent(event);

      final int action = event.getActionMasked();
      switch (action) {
        case MotionEvent.ACTION_DOWN:
          int index = event.getActionIndex();
          // If the user touches the screen, check to see if they have selected a polygon vertex,
          // which they can then drag around the screen.
          Vector2D worldCoords = model.camera.getWorldCoords(event.getX(index), event.getY(index));
          // Reset selection.
          gestureListener.selectedActor = null;

          // model.actors will be sorted by z-index. Iterate over it backwards so that touches on
          // elements are handled in reverse z-index order (i.e., actors in front will be selected
          // before actors in back).
          for (int i = model.actors.size() - 1; i >= 0; i--) {
            Actor actor = model.actors.get(i);
            if (actor instanceof Touchable
                && ((Touchable) actor).canHandleTouchAt(worldCoords, model.camera.scale)) {
              if (model.collisionMode == (actor instanceof CollisionActor)) {
                // Only allow interactions with objects within the current selection mode (i.e.,
                // only collision objects in collision mode, only scenery objects in non-collision
                // mode.
                gestureListener.selectedActor = actor;
                ((Touchable) actor).startTouchAt(worldCoords, model.camera.scale);
                break;
              }
            }
          }
          break;
      }
      handledTouch = editorGestureDetector.onTouchEvent(event) || handledTouch;
    } else {
      // Handle in-game touch events.
      if (model != null) {
        final int action = event.getActionMasked();
        switch (action) {
          case MotionEvent.ACTION_DOWN:
            model.onTouchDown();
            handledTouch = true;
            break;
        }
      }
    }
    return handledTouch || super.onTouchEvent(event);
  }

  public void setModel(SwimmingModel model) {
    this.model = model;
  }

  private void drawSwimmingLines(Canvas canvas) {
    int laneWidth = SwimmingModel.LEVEL_WIDTH / NUM_LANES;
    for (int i = 1; i < NUM_LANES; i++) {
      canvas.drawRect(laneWidth * i - LANE_LINE_WIDTH / 2.0f, model.camera.yToWorld(0),
          laneWidth * i + LANE_LINE_WIDTH / 2.0f, model.camera.yToWorld(canvas.getHeight()),
          swimmingLinesPaint);
    }
  }

  private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      model.camera.scale *= detector.getScaleFactor();
      model.camera.scale = Math.max(0.1f, Math.min(model.camera.scale, 5.0f));
      return true;
    }
  }

  private class GestureListener extends SimpleOnGestureListener {
    public Actor selectedActor;

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
      Vector2D delta = Vector2D.get(distanceX / model.camera.scale, distanceY / model.camera.scale);
      if (selectedActor != null) {
        ((Touchable) selectedActor).handleMoveEvent(delta);
      } else {
        model.camera.position.add(delta);
      }
      delta.release();
      return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
      performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
      if (selectedActor != null) {
        boolean handled = ((Touchable) selectedActor).handleLongPress();
        if (!handled) {
          // If the selected actor doesn't handle the long press, the default behavior is to remove
          // it.
          model.actors.remove(selectedActor);
        }
      } else {
        // Long press is not on a touchable actor. Create a new object and place it
        // where the long press occurred.
        DialogFragment dialogFragment = new CreateObjectDialogFragment(
            model.camera.getWorldCoords(event.getX(), event.getY()));
        dialogFragment.show(((Activity) getContext()).getFragmentManager(), "create_object");
      }
    }
  }

  private class CreateObjectDialogFragment extends DialogFragment {
    private Vector2D center;

    public CreateObjectDialogFragment(Vector2D center) {
      this.center = center;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      final String[] items;
      items = new String[model.collisionObjectTypes.size()];
      model.collisionObjectTypes.toArray(items);
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setTitle(com.google.android.apps.santatracker.doodles.R.string.create_object)
          .setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              model.createActor(center, items[which], getResources());
            }
          });
      return builder.create();
    }
  }
}
