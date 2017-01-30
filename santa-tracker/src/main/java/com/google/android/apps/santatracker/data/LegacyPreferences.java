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

/**
 * Keeps track of unused preferences that can be removed.
 *
 * @see com.google.android.apps.santatracker.data.SantaPreferences#onUpgrade(int, int)
 */
class LegacyPreferences {

    static class CastFlags_1 {

        static final String Airport = "Airport";
        static final String SantaCallVideo = "SantaCallVideo";
        static final String StreetView = "StreetView";
        static final String Factory = "Factory";
        static final String FerrisWheel = "FerrisWheel";
        static final String SnowdomeExperiment = "SnowdomeExperiment";
        static final String ChoirVideo = "ChoirVideo";
        static final String Commandcentre = "Commandcentre";
        static final String Mountain = "Mountain";
        static final String Playground = "Playground";
        static final String Rollercoaster = "Rollercoaster";
        static final String Windtunnel = "Windtunnel";
        static final String Workshop = "Workshop";
        static final String FinalPrepVideo = "FinalPrepVideo";
        static final String Tracker = "Tracker";
        static final String Village = "Village";
        static final String BriefingRoom = "BriefingRoom";

        static final String EnableAudio = "EnableAudio";

        static final String[] ALL_FLAGS = new String[]{Village, BriefingRoom, Airport,
                Windtunnel, StreetView, Factory,
                Rollercoaster, Commandcentre, SantaCallVideo, Mountain, Playground, FerrisWheel,
                SnowdomeExperiment, ChoirVideo, Workshop, FinalPrepVideo, Tracker, EnableAudio};

    }

    private static final String PREF_EARTH = "PREF_EARTHDISABLED";
    private static final String PREF_CASTROUTE = "PREF_CASTROUTE";

    private static final String PREF_CASTFTU = "PREF_CASTFTU";
    private static final String PREF_CASTTOUCHPAD = "PREF_CASTTOUCHPAD";

    static final String[] PREFERENCES = new String[]{PREF_EARTH, PREF_CASTFTU, PREF_CASTROUTE,
            PREF_CASTTOUCHPAD};

}
