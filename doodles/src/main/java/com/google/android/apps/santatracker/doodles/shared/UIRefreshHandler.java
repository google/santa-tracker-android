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

import android.os.Handler;
import android.os.Message;
import android.view.View;

/**
 * Handler subclass which handles refreshing the UI.
 */
public class UIRefreshHandler extends Handler {
  private static final int REFRESH_UI_MESSAGE = 0;
  // Refresh the UI at a higher rate so that we can keep the drawing pipeline filled.
  private static final int UI_INTERVAL_MS = 1000 / 120;

  // Toggled in start/stop, and used in handleMessage to conditionally schedule the next refresh.
  private volatile boolean running;

  private View view;

  public UIRefreshHandler() {
  }

  public void start(View view) {
    running = true;
    this.view = view;
    sendEmptyMessage(REFRESH_UI_MESSAGE);
  }

  public void stop() {
    running = false;
    view = null;
    removeMessages(REFRESH_UI_MESSAGE);
  }

  @Override
  public void handleMessage(Message msg) {
    if (running) {
      if (msg.what == REFRESH_UI_MESSAGE) {
        long timeBeforeDraw = System.currentTimeMillis();
        if (view != null) {
          // invalidate
          view.invalidate();
        }
        // Wait different amounts of time depending on how much time the draw took.
        // Wait at least 1ms to avoid a mysterious memory leak.
        long timeToDraw = System.currentTimeMillis() - timeBeforeDraw;
        sendEmptyMessageDelayed(REFRESH_UI_MESSAGE, Math.max(1, UI_INTERVAL_MS - timeToDraw));
      }
    }
  }
}
