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
package com.google.android.apps.santatracker.doodles.pursuit;

import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * PursuitLevel is a programmatic representation of a .mp file which contains rows of power ups.
 * Lanes devoid of power ups are represented by '.'.
 * A power up is represented by 1.
 * Example: '.1.' is equivalent to a row with a power up in the middle lane.
 */
public class PursuitLevel {
  private List<char[]> level;

  public PursuitLevel(int resId, Resources resources) {
    level = new ArrayList<>();
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(resources.openRawResource(resId)));
    try {
      String line = reader.readLine();
      while (line != null) {
        level.add(line.toCharArray());
        line = reader.readLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public char[] getRowArray(int row) {
    return level.get(row % level.size());
  }
}
