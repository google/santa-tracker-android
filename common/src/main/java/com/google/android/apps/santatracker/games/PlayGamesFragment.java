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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.google.android.apps.santatracker.common.BuildConfig;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

/**
 * Non-visible fragment to encapsulate Google Play Game Services logic.
 */
public class PlayGamesFragment extends Fragment implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "PlayGamesFragment";
    private static final String FRAGMENT_TAG = "PlayGamesFragment_Tag";

    /** Key to store mIsResolving in SharedPreferences. **/
    private static final String KEY_IS_RESOLVING = "is_resolving";

    /** Key to store mShouldResolve in SharedPreferences. **/
    private static final String KEY_SHOULD_RESOLVE = "should_resolve";

    /** Request code used for resolving Games sign-in failures. **/
    private static final int RC_GAMES = 9001;

    /** Should debug-level log messages be printed? **/
    private boolean mDebugLogEnabled = false;

    /** GoogleApiClient used for interacting with Play Game Services. **/
    private GoogleApiClient mGamesApiClient;

    /** Is a resolution already in progress? **/
    private boolean mIsResolving = false;

    /** Should connection failures be automatically resolved? **/
    private boolean mShouldResolve = false;

    /** Listener for sign-in events. **/
    private SignInListener mListener;

    /**
     * Get or create an instance of the Fragment attached to an Activity.
     * @param activity FragmentActivity to host the Fragment.
     * @param listener SignInListener to respond to changes in sign-in state.
     * @return instance of PlayGamesFragment.
     */
    public static PlayGamesFragment getInstance(FragmentActivity activity,
                                                SignInListener listener) {

        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        PlayGamesFragment result = null;

        Fragment fragment = fm.findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            result = new PlayGamesFragment();
            ft.add(result, FRAGMENT_TAG).disallowAddToBackStack().commit();
        } else {
            result = (PlayGamesFragment) fragment;
        }

        result.setListener(listener);
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restore state of in-progress sign-in in the case of rotation or other
        // Activity recreation.
        if (savedInstanceState != null) {
            mIsResolving = savedInstanceState.getBoolean(KEY_IS_RESOLVING, false);
            mShouldResolve = savedInstanceState.getBoolean(KEY_SHOULD_RESOLVE, false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mGamesApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mGamesApiClient.disconnect();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Only log debug messages when enabled
        mDebugLogEnabled = BuildConfig.DEBUG;

        // Api client for interacting with Google Play Games
        mGamesApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API, Games.GamesOptions.builder().build())
                .addScope(Games.SCOPE_GAMES)
                .build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_GAMES) {
            debugLog("onActivityResult:RC_GAMES:" + resultCode + ":" + data);

            // If the error resolution was not successful we should not resolve further.
            if (resultCode != Activity.RESULT_OK) {
                mShouldResolve = false;
            }

            mIsResolving = false;
            mGamesApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        debugLog("onConnected:" + bundle);
        mShouldResolve = false;
        mListener.onSignInSucceeded();
    }

    @Override
    public void onConnectionSuspended(int i) {
        debugLog("onConnectionSuspended:" + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        debugLog("onConnectionFailed:" + connectionResult);
        if (!mIsResolving && mShouldResolve) {
            if (connectionResult.hasResolution()) {
                try {
                    connectionResult.startResolutionForResult(getActivity(), RC_GAMES);
                    mIsResolving = true;
                } catch (IntentSender.SendIntentException e) {
                    debugLog("onConnectionFailed:SendIntentException:" + e.getMessage());
                    mIsResolving = false;
                    mGamesApiClient.connect();
                }
            } else {
                // Could not resolve the connection result, show the user an
                // error dialog.
                showErrorDialog(connectionResult);
            }
        } else {
            // Show the signed-out UI
            mListener.onSignInFailed();
        }
    }

    /**
     * Show error dialog for Google Play Services errors that cannot be resolved.
     * @param connectionResult the connection result from onConnectionFailed.
     */
    private void showErrorDialog(ConnectionResult connectionResult) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(getActivity());

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(getActivity(), resultCode, RC_GAMES,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                mShouldResolve = false;
                                mListener.onSignInFailed();
                            }
                        }).show();
            } else {
                String errorString = apiAvailability.getErrorString(resultCode);
                debugLog("Google Play Services Error:" + connectionResult + ":" + errorString);;

                mShouldResolve = false;
                mListener.onSignInFailed();
            }
        }
    }

    public boolean isSignedIn() {
        return (mGamesApiClient != null && mGamesApiClient.isConnected());
    }

    public void beginUserInitiatedSignIn() {
        mShouldResolve = true;
        mGamesApiClient.connect();
    }

    public GoogleApiClient getGamesApiClient() {
        return mGamesApiClient;
    }

    private void debugLog(String message) {
        if (!mDebugLogEnabled) {
            return;
        }

        Log.d(TAG, message);
    }

    private void setListener(SignInListener listener) {
        mListener = listener;
    }

}
