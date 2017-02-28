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

package com.google.android.apps.santatracker.games;

/**
 * Interface for Activities that observe sign-in state, normally used with {@link GameActivity}.
 */
public interface SignInListener {

    /**
     * Called when sign-in fails. As a result, a "Sign-In" button can be shown to the user.
     * Not all calls to this method indicate an error, sign-in is expected to fail when the
     * user has never signed in before.
     */
    void onSignInFailed();

    /**
     * Called when sign-in succeeds and the application can begin to take action on behalf of
     * the user.
     */
    void onSignInSucceeded();

}
