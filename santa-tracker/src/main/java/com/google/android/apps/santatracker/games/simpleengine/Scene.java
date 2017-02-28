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

package com.google.android.apps.santatracker.games.simpleengine;

public class Scene {

    public void onScreenResized(int width, int height) {
    }

    public void doStandbyFrame(float deltaT) {
    }

    public void doFrame(float deltaT) {
    }

    public void onInstall() {
    }

    public void onUninstall() {
    }

    public void onPointerDown(int pointerId, float x, float y) {
    }

    public void onPointerMove(int pointerId, float x, float y, float deltaX, float deltaY) {
    }

    public void onPointerUp(int pointerId, float x, float y) {
    }

    public void onKeyDown(int keyCode, int repeatCount) {
    }

    public void onKeyUp(int keyCode) {
    }

    public void onSensorChanged(float x, float y, int accuracy) {
    }

    public boolean isGameEnded() {
        return false;
    }
}
