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
package com.google.android.apps.santatracker.presentquest.model;

import com.orm.SugarRecord;

import java.util.List;


public class User extends SugarRecord {

    public static final int WORKSHOP_2_LEVEL = 2;
    public static final int WORKSHOP_3_LEVEL = 4;

    public int presentsCollected = 0;
    public int presentsReturned = 0;

    // Presents required on each level to proceed to next level.
    // If the user is on level i they will have collected somewhere between
    // PRESENTS_REQUIRED[i - 1] and PRESENTS_REQUIRED[i] presents
    //
    // A good game of present collecting returns around 50 presents, so getting
    // 100 points is two games and 1000 points is twenty games.
    private static final int[] PRESENTS_REQUIRED = {
            0,           // Level 0 (does not exist)
            1,           // Level 1
            50,          // Level 2
            200,         // Level 3
            500,         // Level 4
            750,         // Level 5
            1000,        // Level 6
            2000,        // Level 7
            5000,        // Level 8
            10000,       // Level 9
            15000,       // Level 10
            20000,       // Level 11

            // Level 12 is the last level, can't beat it
            Integer.MAX_VALUE
    };

    // Maximum presents that can be collected (held before returning) on each level.
    private static final int[] MAX_PRESENTS_COLLECTED = {
            0,           // Level 0 (does not exist)
            10,          // Level 1
            50,          // Level 2
            75,          // Level 3
            100,         // Level 4
            250,         // Level 5
            300,         // Level 6
            350,         // Level 7
            400,         // Level 8
            450,         // Level 9
            500,         // Level 10
            500,         // Level 11
            1000,        // Level 12
    };

    // Chance that a present bunch dropped is "large"
    private static final float[] LARGE_PRESENT_CHANCE = {
            0.00f,  // Level 0
            0.05f,  // Level 1
            0.10f,  // Level 2
            0.15f,  // Level 3
            0.20f,  // Level 4
            0.25f,  // Level 5
            0.30f,  // Level 6
            0.35f,  // Level 7
            0.40f,  // Level 8
            0.45f,  // Level 9
            0.55f,  // Level 10
            0.60f,  // Level 11
            0.65f,  // Level 12
    };

    // Returns the user object, or creates it.
    public static User get() {
        List<User> users = User.listAll(User.class);
        if (users.isEmpty()) {
            return new User();
        } else {
            return users.get(0);
        }
    }

    public User() {}

    // Returns current level.
    public int getLevel() {
        for (int i = 0; i < PRESENTS_REQUIRED.length; i++) {
            if (presentsReturned < PRESENTS_REQUIRED[i]) {
                return i;
            }
        }
        return 1;
    }

    // Returns avatar for current level.
    public int getAvatar() {
        return Avatars.AVATARS_UNLOCKED[getLevel() - 1];
    }

    // Returns progress percentage for current level.
    public int getLevelProgress() {
        int level = getLevel();

        // It's impossible to get past the last level, so always show full progress at level 12.
        if (level == (PRESENTS_REQUIRED.length - 1)) {
            return 100;
        }

        int requiredThisLevel = PRESENTS_REQUIRED[level];
        int requiredPreviousLevel = PRESENTS_REQUIRED[level - 1];

        int requiredForLevel = requiredThisLevel - requiredPreviousLevel;
        int returnedForLevel = presentsReturned - requiredPreviousLevel;
        int progress = (int) ((returnedForLevel * 100.0f) / requiredForLevel);
        return progress;
    }

    public void collectPresents(int num) {
        // Can't hold more than max capacity
        int maxCapacity = getMaxPresentsCollected();
        int newCollected = Math.min(maxCapacity, presentsCollected + num);

        presentsCollected = newCollected;
        save();
    }

    public void returnPresentsAndEmpty(int num) {
        // Can't return more than you've collected
        int maxReturn = Math.min(num, presentsCollected);

        // Increment presents returned, empty bag
        presentsReturned = presentsReturned + maxReturn;
        presentsCollected = 0;

        save();
    }

    public int getMaxWorkshops() {
        int level = getLevel();
        if (level >= WORKSHOP_3_LEVEL) {
            return 3;
        } else if (level >= WORKSHOP_2_LEVEL) {
            return 2;
        } else {
            return 1;
        }
    }

    public int getMaxPresentsCollected() {
        return MAX_PRESENTS_COLLECTED[getLevel()];
    }

    public int getPresentsCollectedAllTime() {
        return presentsReturned + presentsCollected;
    }

    public int getBagFillPercentage() {
        int percentFull =  (100 * presentsCollected) / (getMaxPresentsCollected());
        return Math.min(percentFull, 100);
    }

    public float getLargePresentChance() {
        return LARGE_PRESENT_CHANCE[getLevel()];
    }

    // FOR DEBUG ONLY -- Moves user down a level
    public void downlevel() {
        int currentLevel = getLevel();
        int requiredPreviousLevel = PRESENTS_REQUIRED[currentLevel - 1];

        int newPresentsReturned = (int) (requiredPreviousLevel / 2);
        presentsReturned = newPresentsReturned;
        presentsCollected = 0;

        save();
    }

}
