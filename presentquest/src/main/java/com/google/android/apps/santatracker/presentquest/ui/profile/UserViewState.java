/*
 * Copyright (C) 2017 Google Inc. All Rights Reserved.
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

package com.google.android.apps.santatracker.presentquest.ui.profile;

class UserViewState {
    private boolean isSecondWorkshopUnlocked;
    private boolean isThirdWorkshopUnlocked;
    private boolean isMagnet1Visible;
    private boolean isMagnet2Visible;
    private boolean isMagnet3Visible;

    UserViewState() {}

    boolean isSecondWorkshopUnlocked() {
        return isSecondWorkshopUnlocked;
    }

    void setSecondWorkshopUnlocked(boolean secondWorkshopUnlocked) {
        isSecondWorkshopUnlocked = secondWorkshopUnlocked;
    }

    boolean isThirdWorkshopUnlocked() {
        return isThirdWorkshopUnlocked;
    }

    void setThirdWorkshopUnlocked(boolean thirdWorkshopUnlocked) {
        isThirdWorkshopUnlocked = thirdWorkshopUnlocked;
    }

    boolean isMagnet1Visible() {
        return isMagnet1Visible;
    }

    void setMagnet1Visible(boolean magnet1Visible) {
        isMagnet1Visible = magnet1Visible;
    }

    boolean isMagnet2Visible() {
        return isMagnet2Visible;
    }

    void setMagnet2Visible(boolean magnet2Visible) {
        isMagnet2Visible = magnet2Visible;
    }

    boolean isMagnet3Visible() {
        return isMagnet3Visible;
    }

    void setMagnet3Visible(boolean magnet3Visible) {
        isMagnet3Visible = magnet3Visible;
    }
}
