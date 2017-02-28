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

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.KeyEvent;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GameView extends GLSurfaceView implements GLSurfaceView.Renderer {

    int mSurfWidth = 0;
    int mSurfHeight = 0;

    public GameView(Context ctx) {
        super(ctx);
        setEGLContextClientVersion(2);
        setRenderer(this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        SceneManager.getInstance().onGLSurfaceCreated(getContext().getApplicationContext());
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        mSurfWidth = width;
        mSurfHeight = height;
        SceneManager.getInstance().onGLSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        SceneManager.getInstance().onDrawFrame();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return SceneManager.getInstance().onTouchEvent(e);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return SceneManager.getInstance().onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return SceneManager.getInstance().onKeyUp(keyCode, event);
    }
}
