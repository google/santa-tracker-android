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

package com.google.android.apps.santatracker.games.common;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.PlayGamesFragment;
import com.google.android.apps.santatracker.games.SignInListener;
import com.google.android.apps.santatracker.games.gumball.Utils;
import com.google.android.apps.santatracker.util.ImmersiveModeHelper;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

@SuppressLint("Registered")
public abstract class GameActivity extends AppCompatActivity implements
        SignInListener {

    private static final String TAG = "GameActivity";

    /** Non-visible fragment encapsulating Play Games logic. **/
    private PlayGamesFragment mGamesFragment;

    // we need a separate API client for App Indexing
    // so that view and ViewEnd events can be recorded even
    // when the user is not signed in
    GoogleApiClient mApiClient = null;
    boolean mSignedIn = false;

    // base URL for deep links required by App Indexing
    // Deep link intent filter entries in AndroidManifest.xml must match
    // the data part of this prefix
    private static Uri BASE_APP_URI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setBackgroundDrawable(new ColorDrawable(
                ContextCompat.getColor(this, android.R.color.transparent)));
        }

        mGamesFragment = PlayGamesFragment.getInstance(this, this);

        mApiClient = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();
        // add App Indexing API
        BASE_APP_URI = Uri.parse(
                "android-app://" + getApplicationContext().getPackageName() +
                        "/" + getResources().getString(R.string.santa_tracker_deep_link_prefix));

        if (Utils.hasKitKat()) {
            ImmersiveModeHelper.setImmersiveSticky(getWindow());
            ImmersiveModeHelper.installSystemUiVisibilityChangeListener(getWindow());
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (Utils.hasKitKat() && hasFocus) {
            ImmersiveModeHelper.setImmersiveSticky(getWindow());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        appIndexingRecordView();
    }

    @Override
    public void onStop() {
        super.onStop();
        appIndexingRecordViewEnd();
    }

    @Override
    public void onActivityResult(int req, int resp, Intent data) {
        super.onActivityResult(req, resp, data);
        mGamesFragment.onActivityResult(req, resp, data);
    }

    @Override
    public void onSignInFailed() {
        mSignedIn = false;
    }

    @Override
    public void onSignInSucceeded() {
        mSignedIn = true;
    }

    public boolean isSignedIn() {
        return mSignedIn;
    }

    public void beginUserInitiatedSignIn() {
        mGamesFragment.beginUserInitiatedSignIn();
    }

    public GoogleApiClient getGamesApiClient() {
        return mGamesFragment.getGamesApiClient();
    }

    /**
     * Connect the client, record the view using App Indexing API.
     * We're not connecting the client in the activity lifecycle method
     * to maintain symmetry with appIndexingRecordViewEnd which
     * disconnects the client once the view is recorded
     */
    private void appIndexingRecordView() {
        // connect the client
        mApiClient.connect();
        // Define a title for your current page, shown in autocompletion UI
        final String title = getGameTitle();
        final Uri appUri = getGameDeepLinkUri();

        // Call the App Indexing API view method
        PendingResult<Status> result = AppIndex.AppIndexApi.view(mApiClient, this,
                appUri, title, null, null);

        result.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    SantaLog.v(TAG, String.format("App Indexing API: Recorded ["
                            + title + "] view successfully."));
                } else {
                    Log.e(TAG, "App Indexing API: There was an error recording the view."
                            + status.toString());
                }
            }
        });
    }

    /**
     * Record the view end using the App Indexing API,
     * disconnect the client once the view is recorded.
     */
    private void appIndexingRecordViewEnd() {
        final Uri appUri = getGameDeepLinkUri();
        final String title = getGameTitle();
        PendingResult<Status> result = AppIndex.AppIndexApi.viewEnd(mApiClient, this,
                appUri);
        result.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                mApiClient.disconnect(); // disconnecting here because of a potential race
                if (status.isSuccess()) {
                    Log.v(TAG, "App Indexing API: Recorded ["
                            + title + "] view end successfully.");
                } else {
                    Log.e(TAG, "App Indexing API: There was an error recording the view end."
                            + status.toString());
                }
            }
        });
    }

    /**
     * See https://developers.google.com/app-indexing/webmasters/appindexingapi
     * @return deep link representing this game
     */
    public Uri getGameDeepLinkUri() {
        return BASE_APP_URI.buildUpon().appendPath(String.valueOf(getGameId())).build();
    }

    /**
     * This is the ID which becomes a part of the deep link.
     * For example:
     *  android-app://com.google.android.apps.santatracker/http/google.com/santatracker/gumball
     * This value need not be human-readable, it sent in the Intent back to the
     * Santa Tracker by the Google App.
     * When extending this class and providing new deep links, make sure
     * to update the corresponding intent-filter in AndroidManifest.xml.
     * @return deep link component to identify the game to app indexing API
     */
    public abstract String getGameId();

    /**
     * This name will be shown to users in the Google app for autocompletion
     * @return user-visible game title
     */
    public abstract String getGameTitle();

}
