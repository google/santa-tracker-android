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
package com.google.android.apps.santatracker.data;

import com.google.android.apps.santatracker.service.SantaServiceMessages;

/**
 * Disabled state of each game.
 */
public class GameDisabledState {

    public boolean disableGumballGame = false;
    public boolean disableJetpackGame = false;
    public boolean disableMemoryGame = false;
    public boolean disableRocketGame = false;
    public boolean disableDancerGame = false;
    public boolean disableSnowdownGame = false;
    public boolean disableSwimmingGame = false;
    public boolean disableBmxGame = false;
    public boolean disableRunningGame = false;
    public boolean disableTennisGame = false;
    public boolean disableWaterpoloGame = false;
    public boolean disableCityQuizGame = false;
    public boolean disablePresentQuest = false;

    public GameDisabledState() {
    }

    /**
     * Create from {@link SantaPreferences}.
     */
    public GameDisabledState(SantaPreferences preferences) {
        this.disableGumballGame = preferences.getGumballDisabled();
        this.disableJetpackGame = preferences.getJetpackDisabled();
        this.disableMemoryGame = preferences.getMemoryDisabled();
        this.disableRocketGame = preferences.getRocketDisabled();
        this.disableDancerGame = preferences.getDancerDisabled();

        this.disableSnowdownGame = preferences.getSnowdownDisabled();

        this.disableSwimmingGame = preferences.getSwimmingDisabled();
        this.disableBmxGame = preferences.getBmxDisabled();
        this.disableRunningGame = preferences.getRunningDisabled();
        this.disableTennisGame = preferences.getTennisDisabled();
        this.disableWaterpoloGame = preferences.getWaterpoloDisabled();

        this.disableCityQuizGame = preferences.getCityQuizDisabled();

        this.disablePresentQuest = preferences.getPresentQuestDisabled();
    }

    /**
     * Create from flags contained in {@link com.google.android.apps.santatracker.service.SantaServiceMessages}.
     */
    public GameDisabledState(int arg) {
        disableGumballGame = (arg & SantaServiceMessages.MSG_FLAG_GAME_GUMBALL)
                == SantaServiceMessages.MSG_FLAG_GAME_GUMBALL;
        disableJetpackGame = (arg & SantaServiceMessages.MSG_FLAG_GAME_JETPACK)
                == SantaServiceMessages.MSG_FLAG_GAME_JETPACK;
        disableMemoryGame = (arg & SantaServiceMessages.MSG_FLAG_GAME_MEMORY)
                == SantaServiceMessages.MSG_FLAG_GAME_MEMORY;
        disableRocketGame = (arg & SantaServiceMessages.MSG_FLAG_GAME_ROCKET)
                == SantaServiceMessages.MSG_FLAG_GAME_ROCKET;
        disableDancerGame = (arg & SantaServiceMessages.MSG_FLAG_GAME_DANCER)
                == SantaServiceMessages.MSG_FLAG_GAME_DANCER;

        disableSnowdownGame = (arg & SantaServiceMessages.MSG_FLAG_GAME_SNOWDOWN)
                == SantaServiceMessages.MSG_FLAG_GAME_SNOWDOWN;

        disableSwimmingGame = (arg & SantaServiceMessages.MSG_FLAG_GAME_SWIMMING)
                == SantaServiceMessages.MSG_FLAG_GAME_SWIMMING;
        disableBmxGame = (arg & SantaServiceMessages.MSG_FLAG_GAME_BMX)
                == SantaServiceMessages.MSG_FLAG_GAME_BMX;
        disableRunningGame = (arg & SantaServiceMessages.MSG_FLAG_GAME_RUNNING)
                == SantaServiceMessages.MSG_FLAG_GAME_RUNNING;
        disableTennisGame = (arg & SantaServiceMessages.MSG_FLAG_GAME_TENNIS)
                == SantaServiceMessages.MSG_FLAG_GAME_TENNIS;
        disableWaterpoloGame = (arg & SantaServiceMessages.MSG_FLAG_GAME_WATERPOLO)
                == SantaServiceMessages.MSG_FLAG_GAME_WATERPOLO;

        disableCityQuizGame = (arg & SantaServiceMessages.MSG_FLAG_GAME_CITY_QUIZ)
                == SantaServiceMessages.MSG_FLAG_GAME_CITY_QUIZ;

        disablePresentQuest = (arg & SantaServiceMessages.MSG_FLAG_GAME_PRESENTQUEST)
                == SantaServiceMessages.MSG_FLAG_GAME_PRESENTQUEST;
    }

}
