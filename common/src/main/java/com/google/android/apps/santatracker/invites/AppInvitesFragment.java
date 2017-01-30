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

package com.google.android.apps.santatracker.invites;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.google.android.apps.santatracker.common.R;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.appinvite.AppInviteInvitationResult;
import com.google.android.gms.appinvite.AppInviteReferral;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.firebase.analytics.FirebaseAnalytics;

public class AppInvitesFragment extends Fragment implements
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "AppInvitesFragment";
    private static final String FRAGMENT_TAG = "AppInvitesFragment";
    private static final int AUTOMANAGE_ID = 107;
    private static final int RC_INVITE = 9007;

    public static final Uri BASE_URI = Uri.parse("https://google.com/santatracker/android/");

    private GoogleApiClient mGoogleApiClient;
    private FirebaseAnalytics mMeasurement;

    public interface GetInvitationCallback {
        void onInvitation(String invitationId, String deepLink);
    }

    public static AppInvitesFragment getInstance(FragmentActivity activity) {

        FragmentManager fm = activity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        AppInvitesFragment result = null;

        Fragment fragment = fm.findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null) {
            result = new AppInvitesFragment();
            ft.add(result, FRAGMENT_TAG).disallowAddToBackStack().commit();
        } else {
            result = (AppInvitesFragment) fragment;
        }

        return result;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize app measurement.
        mMeasurement = FirebaseAnalytics.getInstance(getActivity());

        // Api client for AppInvites.
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addOnConnectionFailedListener(this)
                .enableAutoManage(getActivity(), AUTOMANAGE_ID, this)
                .addApi(AppInvite.API)
                .build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_INVITE) {
            String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
            Log.d(TAG, "onActivityResult:" + ids);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed:" + connectionResult);
    }

    /**
     * Send an invite with a deep link into a game.
     * @param gameName a human-readable name for the game, to be displayed in the invitation UI.
     * @param gameId an identifier for the game to be appended to
     *               http://google.com/santatracker/android/. The game should be a registered
     *               handler for this URL in the Android Manifest.
     * @param score the inviting user's game score, which will be pre-populated in the
     *              invitation message.
     */
    public void sendGameInvite(String gameName, String gameId, int score) {
        Uri uri = BASE_URI.buildUpon()
                .appendPath(gameId)
                .appendQueryParameter("score", Integer.toString(score))
                .build();

        sendInvite(getString(R.string.invite_message_game_fmt, score, gameName), uri);
        MeasurementManager.recordInvitationSent(mMeasurement, "game", uri.toString());

    }

    public void sendGenericInvite() {
        Uri uri = BASE_URI;
        sendInvite(getString(R.string.invite_message_generic), uri);
        MeasurementManager.recordInvitationSent(mMeasurement, "generic", uri.toString());
    }

    public void sendInvite(String message, Uri uri) {
        // If the message is too long, just cut it short and add ellipses. This is something that
        // only occurs in some translations and we do not have a better mitigation method.  The
        // alternative is an ugly IllegalArgumentException from the builder.
        int maxLength = AppInviteInvitation.IntentBuilder.MAX_MESSAGE_LENGTH;
        if (message.length() > maxLength) {
            String suffix = "...";
            String prefix = message.substring(0, maxLength - suffix.length());

            message = prefix + suffix;
        }

        Intent inviteIntent = new AppInviteInvitation.IntentBuilder(getString(R.string.invite_title))
                .setMessage(message)
                .setDeepLink(uri)
                .build();

        startActivityForResult(inviteIntent, RC_INVITE);
    }

    public void getInvite(final GetInvitationCallback callback, final boolean launchDeepLink) {
        // Using "null, false" as arguments here to avoid a known memory leak issue in
        // AppInvites. Should be fixed in Google Play services v10.4.0.
        final Activity activity = getActivity();
        AppInvite.AppInviteApi.getInvitation(mGoogleApiClient, null, false)
                .setResultCallback(new ResultCallback<AppInviteInvitationResult>() {
                    @Override
                    public void onResult(AppInviteInvitationResult appInviteInvitationResult) {
                        Log.d(TAG, "getInvite:" + appInviteInvitationResult.getStatus());

                        if (callback != null && appInviteInvitationResult.getStatus().isSuccess()) {
                            // Report the callback.
                            Intent intent = appInviteInvitationResult.getInvitationIntent();
                            String invitiationId = AppInviteReferral.getInvitationId(intent);
                            String deepLink = AppInviteReferral.getDeepLink(intent);
                            callback.onInvitation(invitiationId, deepLink);

                            // Record invitation receipt event.
                            MeasurementManager.recordInvitationReceived(mMeasurement, deepLink);

                            // Launch the deep link (see above note on why we don't do this
                            // automatically)
                            if (launchDeepLink) {
                                try {
                                    activity.startActivity(intent);
                                } catch (ActivityNotFoundException e) {
                                    Log.w(TAG, "No handler for deep link", e);
                                }
                            }
                        }

                    }
                });
    }
}
