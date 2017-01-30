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

import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Class that holds the config we need for the doodle.
 */
public class DoodleConfig {

  /**
   * Function to call to actually go to the search results page.
   */
  public interface QueryRunner {
    public void runQuery();
  }

  // Public to be read, but final so we know it doesn't change.
  @Nullable
  public final Bundle extraData;
  @Nullable
  public final QueryRunner queryRunner;

  public DoodleConfig(@Nullable Bundle extraData, @Nullable QueryRunner queryRunner) {
    this.extraData = extraData;
    this.queryRunner = queryRunner;
  }

}
