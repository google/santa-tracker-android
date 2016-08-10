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

package com.google.android.apps.santatracker.service;

import android.os.Bundle;
import android.os.Message;

public abstract class SantaServiceMessages {

    // IPC Messenger communication messages
    public static final int MSG_SERVICE_REGISTER_CLIENT = 1001;
    public static final int MSG_SERVICE_UNREGISTER_CLIENT = 1002;

    public static final int MSG_SERVICE_STATE_BEGIN = 9;
    public static final int MSG_SERVICE_STATUS = 10;

    public static final int MSG_INPROGRESS_UPDATE_ROUTE = 11;

    public static final int MSG_UPDATED_ROUTE = 20;
    public static final int MSG_UPDATED_ONOFF = 21;
    public static final int MSG_UPDATED_TIMES = 22;
    public static final int MSG_UPDATED_FINGERPRINT = 23;
    public static final int MSG_UPDATED_CASTDISABLED = 24;
    public static final int MSG_UPDATED_GAMES = 25;
    public static final int MSG_UPDATED_VIDEOS = 26;
    public static final int MSG_UPDATED_STREAM = 27;
    public static final int MSG_UPDATED_WEARSTREAM = 28;
    public static final int MSG_UPDATED_DESTINATIONPHOTO = 29;

    public static final int MSG_ERROR = 98;
    public static final int MSG_ERROR_NODATA = 99;
    public static final int MSG_SUCCESS = 100;

    public static final int MSG_FLAG_GAME_GUMBALL = 1;
    public static final int MSG_FLAG_GAME_JETPACK = 2;
    public static final int MSG_FLAG_GAME_MEMORY = 4;
    public static final int MSG_FLAG_GAME_ROCKET = 8;
    public static final int MSG_FLAG_GAME_DANCER = 16;
    public static final int MSG_FLAG_GAME_SNOWDOWN = 32;

    // Service status state
    public static final int STATUS_IDLE = 1;
    public static final int STATUS_IDLE_NODATA = 2;
    public static final int STATUS_PROCESSING = 3;
    public static final int STATUS_ERROR = 4;

    public static final int STATUS_ERROR_NODATA = 5;

    // Flags
    public static final int ENABLED = 1;
    public static final int DISABLED = 2;
    // Switchoff update
    public static final int SWITCH_ON = 1;
    public static final int SWITCH_OFF = 2;

    // Bundle keys for time/offset update
    public static final String BUNDLE_OFFSET = "OFFSET";
    public static final String BUNDLE_FIRST_DEPARTURE = "FIRST_DEPARTURE";
    public static final String BUNDLE_FINAL_ARRIVAL = "FINAL_ARRIVAL";
    public static final String BUNDLE_FINAL_DEPARTURE = "FINAL_DEPARTURE";

    // Bundle keys for video data
    public static final String BUNDLE_VIDEOS = "VIDEOS";

    private static Bundle timeBundle = new Bundle();

    public static Message getSwitchOffMessage(boolean isOff) {
        int status = isOff ? SantaServiceMessages.SWITCH_OFF : SantaServiceMessages.SWITCH_ON;
        return Message.obtain(null, SantaServiceMessages.MSG_UPDATED_ONOFF, status, 0);
    }

    public static Message getBeginFullStateMessage() {
        return Message.obtain(null, MSG_SERVICE_STATE_BEGIN);
    }

    public static Message getStateMessage(int state) {
        return Message.obtain(null, SantaServiceMessages.MSG_SERVICE_STATUS, state, 0);
    }

    public static Message getTimeUpdateMessage(long offset, long firstDeparture,
            long finalArrival, long finalDeparture) {
        timeBundle.clear();
        timeBundle.putLong(SantaServiceMessages.BUNDLE_OFFSET, offset);
        timeBundle.putLong(SantaServiceMessages.BUNDLE_FIRST_DEPARTURE, firstDeparture);
        timeBundle.putLong(SantaServiceMessages.BUNDLE_FINAL_ARRIVAL, finalArrival);
        timeBundle.putLong(SantaServiceMessages.BUNDLE_FINAL_DEPARTURE, finalDeparture);

        return Message.obtain(null, SantaServiceMessages.MSG_UPDATED_TIMES, timeBundle);
    }

    public static int getDisabledStatus(boolean isDisabled){
        return isDisabled ? SantaServiceMessages.DISABLED : SantaServiceMessages.ENABLED;
    }

    public static Message getCastDisabledMessage(boolean isDisabled) {
        return Message.obtain(null, SantaServiceMessages.MSG_UPDATED_CASTDISABLED,
                getDisabledStatus(isDisabled), 0);
    }

    public static Message getDestinationPhotoMessage(boolean isDisabled) {
        return Message.obtain(null, SantaServiceMessages.MSG_UPDATED_DESTINATIONPHOTO,
                getDisabledStatus(isDisabled), 0);
    }

    public static Message getGamesMessage(boolean disableGumball, boolean disableJetpack,
                                          boolean disableMemory, boolean disableRocket,
                                          boolean disableDancer, boolean disableSnowdown) {
        int status = 0;
        status += disableGumball ? MSG_FLAG_GAME_GUMBALL : 0;
        status += disableJetpack ? MSG_FLAG_GAME_JETPACK : 0;
        status += disableMemory ? MSG_FLAG_GAME_MEMORY : 0;
        status += disableRocket ? MSG_FLAG_GAME_ROCKET : 0;
        status += disableDancer ? MSG_FLAG_GAME_DANCER : 0;
        status += disableSnowdown ? MSG_FLAG_GAME_SNOWDOWN : 0;
        return Message.obtain(null, SantaServiceMessages.MSG_UPDATED_GAMES, status, 0);
    }

    public static Message getVideosMessage(String... videos) {
        Message message = Message.obtain(null, SantaServiceMessages.MSG_UPDATED_VIDEOS, 0, 0);
        Bundle bundle = new Bundle();
        bundle.putStringArray(BUNDLE_VIDEOS, videos);
        message.setData(bundle);
        return message;
    }
}
