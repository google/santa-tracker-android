/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.apps.santatracker.doodles.presenttoss;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import com.google.android.apps.santatracker.doodles.shared.actor.Actor;

/** Handles rendering for the water polo game. Copy and pasted from JumpingView. */
public class PresentTossView extends View {
    private static final String TAG = PresentTossView.class.getSimpleName();

    private static final int COLOR_FLOOR = 0xFFA6FFFF;

    private PresentTossModel model;
    private float currentScale;
    private float currentOffsetX = 0; // In game units
    private float currentOffsetY = 0;

    public PresentTossView(Context context) {
        this(context, null);
    }

    public PresentTossView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PresentTossView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setModel(PresentTossModel model) {
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
            currentScale =
                    Math.min(
                            canvas.getWidth() / (float) PresentTossModel.WATER_POLO_WIDTH,
                            canvas.getHeight() / (float) PresentTossModel.WATER_POLO_HEIGHT);

            currentOffsetX =
                    (canvas.getWidth() / currentScale - PresentTossModel.WATER_POLO_WIDTH) / 2;
            currentOffsetY =
                    (canvas.getHeight() / currentScale - PresentTossModel.WATER_POLO_HEIGHT) / 2;

            model.moveSlide(currentOffsetX);

            canvas.scale(currentScale, currentScale);
            canvas.translate(
                    currentOffsetX - model.cameraShake.position.x,
                    currentOffsetY - model.cameraShake.position.y);

            synchronized (model.actors) {
                for (int i = 0; i < model.actors.size(); i++) {
                    Actor actor = model.actors.get(i);
                    if (!actor.hidden) {
                        actor.draw(canvas);
                    }
                }
            }
            canvas.restore();
        }
    }
}
