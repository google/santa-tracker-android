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
package com.google.android.apps.santatracker.doodles.tilt;

import com.google.android.apps.santatracker.doodles.shared.Actor;

import java.util.ArrayList;
import java.util.List;

/**
 * A basic tilt model to be used for testing.
 */
public class TestTiltModel implements TiltModel {

  private List<Actor> actors;

  public TestTiltModel() {
    actors = new ArrayList<>();
  }

  @Override
  public List<Actor> getActors() {
    return actors;
  }

  @Override
  public void addActor(Actor actor) {
    actors.add(actor);
  }

  @Override
  public void setLevelName(String levelName) {
    // Do nothing.
  }
}
