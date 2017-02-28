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

import android.graphics.Bitmap;
import android.util.Pair;

import java.util.HashMap;

/**
 * Cache of bitmaps (and sampleSizes), mapped by resource ID and frame number.
 *
 * <p>Note: This cache must be manually cleared in order to free up memory. This is under the
 * assumption that any bitmaps currently used by the app should be in the cache.</p>
 */
public class BitmapCache {
  public static final String TAG = BitmapCache.class.getSimpleName();

  private HashMap<String, Pair<Bitmap, Integer>> bitmapCache = new HashMap<>();

  public Pair<Bitmap, Integer> getBitmapFromCache(int id, int frame) {
    Pair<Bitmap, Integer> pair = bitmapCache.get(bitmapCacheKey(id, frame));
    return pair;
  }

  public void putBitmapInCache(Bitmap bitmap, int id, int frame, int sampleSize) {
    bitmapCache.put(bitmapCacheKey(id, frame), new Pair(bitmap, sampleSize));
  }

  public void clear() {
    bitmapCache.clear();
  }

  private static String bitmapCacheKey(int id, int frameNumber) {
    return id + ":" + frameNumber;
  }
}
