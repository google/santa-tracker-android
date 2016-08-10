/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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
public class LegacyPrefrences {

    static class CastFlags_1 {

        public static final String Airport = "Airport";
        public static final String SantaCallVideo = "SantaCallVideo";
        public static final String StreetView = "StreetView";
        public static final String Factory = "Factory";
        public static final String FerrisWheel = "FerrisWheel";
        public static final String SnowdomeExperiment = "SnowdomeExperiment";
        public static final String ChoirVideo = "ChoirVideo";
        public static final String Commandcentre = "Commandcentre";
        public static final String Mountain = "Mountain";
        public static final String Playground = "Playground";
        public static final String Rollercoaster = "Rollercoaster";
        public static final String Windtunnel = "Windtunnel";
        public static final String Workshop = "Workshop";
        public static final String FinalPrepVideo = "FinalPrepVideo";
        public static final String Tracker = "Tracker";
        public static final String Village = "Village";
        public static final String BriefingRoom = "BriefingRoom";


        public static final String EnableAudio = "EnableAudio";

        public static final String[] ALL_FLAGS = new String[]{Village, BriefingRoom, Airport,
                Windtunnel, StreetView, Factory,
                Rollercoaster, Commandcentre, SantaCallVideo, Mountain, Playground, FerrisWheel,
                SnowdomeExperiment, ChoirVideo, Workshop, FinalPrepVideo, Tracker, EnableAudio};

    }

    public static final String PREF_EARTH = "PREF_EARTHDISABLED";
    private static final String PREF_CASTROUTE = "PREF_CASTROUTE";

    private static final String PREF_CASTFTU = "PREF_CASTFTU";
    private static final String PREF_CASTTOUCHPAD = "PREF_CASTTOUCHPAD";

    public static final String[] PREFERENCES = new String[] {PREF_EARTH, PREF_CASTFTU, PREF_CASTROUTE,
            PREF_CASTTOUCHPAD};

}
