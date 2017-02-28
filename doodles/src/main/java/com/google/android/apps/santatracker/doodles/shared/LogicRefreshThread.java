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

import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Thread subclass which handles refreshing the game logic.
 */
public class LogicRefreshThread extends Thread {
  private static final int REFRESH_MODEL = 0;

  // Wait at least this long between updates.
  // Update at 120 FPS so that stutters due to draw-loop synchronization are less noticeable.
  private static final int MODEL_INTERVAL_MS = 1000 / 60;

  private Handler handler;
  private final ConditionVariable handlerCreatedCV = new ConditionVariable();

  // Toggled in start/stop, and used in handleMessage to conditionally schedule the next refresh.
  private volatile boolean running;

  private GameLoop gameLoop;
  private long lastTick;
  private int framesSkippedSinceLastUpdate = 0;

  public LogicRefreshThread() {
    setPriority(Thread.MAX_PRIORITY);
  }

  @Override
  public void run() {
    Looper.prepare();

    handler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        if (running && gameLoop != null) {
          if (msg.what == REFRESH_MODEL) {
            float deltaMs = System.currentTimeMillis() - lastTick;
            // Cap deltaMs. Better for game to appear to slow down than have skips/jumps.
            deltaMs = Math.min(100, deltaMs);
            deltaMs *= Debug.SPEED_MULTIPLIER;
            lastTick = System.currentTimeMillis();

            framesSkippedSinceLastUpdate++;
            if (framesSkippedSinceLastUpdate >= Debug.FRAME_SKIP) {
              framesSkippedSinceLastUpdate = 0;
              if (gameLoop != null) {
                gameLoop.update(deltaMs);
              }
            }

            // Wait different amounts of time depending on how much time the game loop took.
            // Wait at least 1ms to avoid a mysterious memory leak.
            long timeToUpdate = System.currentTimeMillis() - lastTick;
            sendEmptyMessageDelayed(REFRESH_MODEL, Math.max(1, MODEL_INTERVAL_MS - timeToUpdate));
          }
        }
      }
    };
    handlerCreatedCV.open();

    Looper.loop();
  }

  public void startHandler(GameLoop gameLoop) {
    this.gameLoop = gameLoop;
    running = true;
    lastTick = System.currentTimeMillis();

    handlerCreatedCV.block();
    handler.sendEmptyMessage(REFRESH_MODEL);
  }

  public void stopHandler() {
    running = false;
    gameLoop = null;

    handlerCreatedCV.block();
    handler.removeMessages(REFRESH_MODEL);
  }
}
