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

package com.google.android.apps.santatracker.games.simpleengine.ui;

import com.google.android.apps.santatracker.games.simpleengine.Renderer;

import java.util.ArrayList;

public class SimpleUI {

    Renderer mRenderer;
    ArrayList<Widget> mWidgets = new ArrayList<Widget>();

    public SimpleUI(Renderer renderer) {
        mRenderer = renderer;
    }

    public void add(Widget widget) {
        if (!mWidgets.contains(widget)) {
            mWidgets.add(widget);
        }
    }

    public void doFrame(float deltaT) {
        for (Widget w : mWidgets) {
            w.doFrame(deltaT);
        }
    }

    public void onPointerDown(int pointerId, float x, float y) {
        for (Widget w : mWidgets) {
            w.onPointerDown(pointerId, x, y);
        }
    }

    public void onPointerMove(int pointerId, float x, float y, float deltaX, float deltaY) {
        for (Widget w : mWidgets) {
            w.onPointerMove(pointerId, x, y, deltaX, deltaY);
        }
    }

    public void onPointerUp(int pointerId, float x, float y) {
        for (Widget w : mWidgets) {
            w.onPointerUp(pointerId, x, y);
        }
    }

    public void dispose() {
        for (Widget w : mWidgets) {
            w.dispose();
        }
        mWidgets.clear();
        mRenderer = null;
    }
}
