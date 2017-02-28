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

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a list of tweens, taking care of removing them when they are done, and adding them (even
 * in the middle of iterating over the list of tweens).
 */
public class TweenManager {
  private final List<Tween> tweens = new ArrayList<>();
  private final List<Tween> incomingTweens = new ArrayList<>();
  private boolean shouldRemoveAll = false;

  public void update(float deltaMs) {
    // First, check whether removeAll was called since the last update.
    if (shouldRemoveAll) {
      finishRemovingAll();
      return;
    }
    try {
      // Move everything from incomingTweens to tweens (before iterating over tweens)
      for (int i = 0; i < incomingTweens.size(); i++) { // Avoiding iterator to avoid garbage.
        tweens.add(incomingTweens.get(i));
      }
      incomingTweens.clear();

      // Now iterate through tweens.
      for (int i = tweens.size() - 1; i >= 0; i--) { // Avoiding iterator to avoid garbage.
        Tween tween = tweens.get(i);
        boolean finished = tween == null || !tween.update(deltaMs);
        if (shouldRemoveAll) {
          finishRemovingAll();
          return;
        }
        if (finished) {
          tweens.remove(i);
        }
      }
    } catch (Exception e) { // do nothing
    }
  }

  public void add(Tween tween) {
    if (tween.durationSeconds() < 0) {
      throw new IllegalArgumentException("Tween duration should not be negative");
    }
    // Don't add to main list of tweens directly, to avoid ConcurrentModificationException.
    incomingTweens.add(tween);
  }

  /**
   * Removes all the tweens at the next possible opportunity. This isn't synchronous, but will
   * happen before any more tween.update calls occur.
   */
  public void removeAll() {
    // Remove incoming tweens immediately. No risk of removing while iterating for these, and we
    // shouldn't clear this later in finishRemovingAll in case more have been added since then,
    // so clear immediately.
    incomingTweens.clear();

    // There is a risk of removing while iterating for tweens though, so set a flag instead of
    // immediately clearing it.
    shouldRemoveAll = true;
  }

  private void finishRemovingAll() {
    // Don't clear incomingTweens here, on the assumption that A) removeAll already cleared it
    // and B) there's a possibility more have been added since then, and they *shouldn't* get
    // cleared.
    tweens.clear();
    shouldRemoveAll = false;
  }

}
