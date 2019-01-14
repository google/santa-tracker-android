/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.apps.santatracker.doodles.shared.logging;

import androidx.annotation.Nullable;

/** A class used for constructing log events for the Pineapple 2016 games. */
public class DoodleLogEvent {
    public static final String DEFAULT_DOODLE_NAME = "doodle_game";

    public static final String MUTE_CLICKED = "clicked mute";
    public static final String UNMUTE_CLICKED = "clicked unmute";
    public static final String PAUSE_CLICKED = "clicked pause";
    public static final String UNPAUSE_CLICKED = "clicked unpause";
    public static final String REPLAY_CLICKED = "clicked replay";
    public static final String SHARE_CLICKED = "clicked share";
    public static final String HOME_CLICKED = "clicked home";
    public static final String LOADING_COMPLETE = "loading complete";
    public static final String DOODLE_LAUNCHED = "doodle launched";
    public static final String GAME_OVER = "game over";

    public static final String DISTINCT_GAMES_PLAYED = "distinct games";
    public static final String RUNNING_GAME_TYPE = "running";
    public static final String PRESENT_TOSS_GAME_TYPE = "tossing";
    public static final String SWIMMING_GAME_TYPE = "swimming";

    public final String doodleName;
    public final String eventName;
    @Nullable public final String eventSubType;
    @Nullable public final Float eventValue1;
    @Nullable public final Float eventValue2;
    @Nullable public final Long latencyMs;

    private DoodleLogEvent(
            String doodleName,
            String eventName,
            @Nullable String eventSubType,
            @Nullable Float eventValue1,
            @Nullable Float eventValue2,
            @Nullable Long latencyMs) {
        this.doodleName = doodleName;
        this.eventName = eventName;
        this.eventSubType = eventSubType;
        this.eventValue1 = eventValue1;
        this.eventValue2 = eventValue2;
        this.latencyMs = latencyMs;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DoodleLogEvent(" + doodleName);
        stringBuilder.append(", " + eventName);
        stringBuilder.append(", " + eventSubType);
        stringBuilder.append(", " + eventValue1);
        stringBuilder.append(", " + eventValue2);
        stringBuilder.append(", " + latencyMs + ")");
        return stringBuilder.toString();
    }

    /** A helper class to build PineappleLogEvents. */
    public static class Builder {
        private String doodleName;
        private String eventName;
        @Nullable private String eventSubType;
        @Nullable private Float eventValue1;
        @Nullable private Float eventValue2;
        @Nullable private Long latencyMs;

        public Builder(String doodleName, String eventName) {
            this.doodleName = doodleName;
            this.eventName = eventName;
            this.eventSubType = null;
            this.eventValue1 = null;
            this.eventValue2 = null;
            this.latencyMs = null;
        }

        public Builder withEventSubType(String eventSubType) {
            this.eventSubType = eventSubType;
            return this;
        }

        public Builder withEventValue1(float eventValue1) {
            this.eventValue1 = eventValue1;
            return this;
        }

        public Builder withEventValue2(float eventValue2) {
            this.eventValue2 = eventValue2;
            return this;
        }

        public Builder withLatencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }

        public DoodleLogEvent build() {
            return new DoodleLogEvent(
                    doodleName, eventName, eventSubType, eventValue1, eventValue2, latencyMs);
        }
    }
}
