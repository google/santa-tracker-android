/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

package com.google.android.apps.santatracker.games.jetpack.gamebase;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.common.PlayGamesActivity;
import com.google.android.apps.santatracker.games.simpleengine.Scene;
import com.google.android.apps.santatracker.games.simpleengine.SceneManager;

import android.os.Bundle;

public abstract class SceneActivity extends PlayGamesActivity {

    protected abstract Scene getGameScene();

    public SceneActivity(int layoutId, Class<?> backClass) {
        super(layoutId, backClass);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            SceneManager.getInstance().enableDebugLog(getResources().getBoolean(
                    R.bool.debug_logs_enabled));
            SceneManager.getInstance().requestNewScene(getGameScene());
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        SceneManager.getInstance().onFocusChanged(hasFocus);
    }

    @Override
    public void onPause() {
        super.onPause();
        SceneManager.getInstance().onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        SceneManager.getInstance().onResume(this);
    }

    @Override
    public void onSignInFailed() {
        super.onSignInFailed();

        // communicate to the BaseScene that we are no longer signed in
        Scene s = SceneManager.getInstance().getCurrentScene();
        if (s instanceof BaseScene) {
            ((BaseScene) s).setSignedIn(false);
        }
    }

    @Override
    public void onSignInSucceeded() {
        super.onSignInSucceeded();

        // communicate to the BaseScene that we are no longer signed in
        Scene s = SceneManager.getInstance().getCurrentScene();
        if (s instanceof BaseScene) {
            ((BaseScene) s).setSignedIn(true);
        }
    }

    public void postQuitGame() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                launchStartupActivity();
            }
        });
    }
}
