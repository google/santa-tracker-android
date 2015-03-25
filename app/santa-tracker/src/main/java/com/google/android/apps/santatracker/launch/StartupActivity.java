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

package com.google.android.apps.santatracker.launch;

import com.google.android.apps.santatracker.util.AudioPlayer;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.common.NotificationConstants;
import com.google.android.apps.santatracker.notification.SantaNotificationBuilder;
import com.google.android.apps.santatracker.util.AccessibilityUtil;
import com.google.android.apps.santatracker.util.AnalyticsManager;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.apps.santatracker.village.Village;
import com.google.android.apps.santatracker.village.VillageView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.GameHelper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

/**
 * Launch activity for the app. Handles loading of the village, the state of the markers (based on
 * the date/time) and incoming voice intents.
 */
public class StartupActivity extends ActionBarActivity
        implements GameHelper.GameHelperListener, View.OnClickListener, Village.VillageListener {

    protected static final String TAG = "SantaStart";
    private static final String VILLAGE_TAG = "VillageFragment";
    private static final String INTENT_HANDLED = "intent_handled";

    private GameHelper mGameHelper;
    private AudioPlayer mAudioPlayer;

    private boolean mResumed = false;
    private boolean mSignedIn = false;

    private Village mVillage;
    private VillageView mVillageView;
    private SantaContext mSantaContext;

    private MarkerManager mMarkerManager;
    private RecyclerView mMarkers;

    // Load these values from resources when an instance of this activity is initialised.
    private static long OFFLINE_SANTA_DEPARTURE;
    private static long OFFLINE_SANTA_FINALARRIVAL;
    private static long UNLOCK_GUMBALL;
    private static long UNLOCK_JETPACK;
    private static long UNLOCK_MEMORY;
    private static long UNLOCK_VIDEO_1;
    private static long UNLOCK_VIDEO_15;
    private static long UNLOCK_VIDEO_23;

    // Server controlled flags
    private boolean mFlagDisableGumball = false;
    private boolean mFlagDisableJetpack = false;
    private boolean mFlagDisableMemory = false;

    private String[] mVideoList = new String[]{null, null, null};


    private boolean mHaveGooglePlayServices = false;
    private long mFirstDeparture;
    private long mFinalArrival;

    private MenuItem mMenuItemLegal;

    // Handler for scheduled UI updates
    private Handler mHandler = new Handler();

    // request code for games Activities
    private final int RC_STARTUP = 1111;
    private final int RC_GAMES = 9999;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_startup);

        loadResourceFields(getResources());

        mAudioPlayer = new AudioPlayer(getApplicationContext());

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));

        mSantaContext = new SantaContext();

        mVillageView = (VillageView) findViewById(R.id.villageView);
        mVillage = (Village) getSupportFragmentManager().findFragmentByTag(VILLAGE_TAG);
        if (mVillage == null) {
            mVillage = new Village();
            getSupportFragmentManager().beginTransaction().add(mVillage, VILLAGE_TAG).commit();
        }

        mMarkers = (RecyclerView) findViewById(R.id.markers);
        mMarkerManager = new MarkerManager();

        initialiseViews();

        mHaveGooglePlayServices = checkGooglePlayServicesAvailable();

        // initialize our connection to Google Play Games
        mGameHelper = new GameHelper(this, GameHelper.CLIENT_GAMES);
        if (getResources().getBoolean(R.bool.debug_logs_enabled)) {
            SantaLog.d("SantaTracker:GameHelper", "GameHelper debug logs are enabled.");
            mGameHelper.enableDebugLog(true, "SantaTracker:GameHelper");
        }
        // Max sign-in attempts of 0 forces BaseGameUtils into deferring sign-in until either a) the
        // user signs in via a manual trigger or b) login, but only if the user has already signed
        // in previously.
        mGameHelper.setMaxAutoSignInAttempts(0);
        mGameHelper.setup(this);

        // set up click listeners for our buttons
        findViewById(R.id.button_show_achievements).setOnClickListener(this);
        findViewById(R.id.button_show_leaderboards).setOnClickListener(this);

        // [ANALYTICS SCREEN]: Village
        AnalyticsManager.sendScreenView(R.string.analytics_screen_village);
        // set the initial states
        resetLauncherStates();
        // See if it was a voice action which triggered this activity and handle it
        onNewIntent(getIntent());
    }

    private void loadResourceFields(Resources res) {
        final long ms = 1000L;
        OFFLINE_SANTA_DEPARTURE = res.getInteger(R.integer.santa_takeoff) * ms;
        OFFLINE_SANTA_FINALARRIVAL = res.getInteger(R.integer.santa_arrival) * ms;
        mFinalArrival = OFFLINE_SANTA_FINALARRIVAL;
        mFirstDeparture = OFFLINE_SANTA_DEPARTURE;

        // Game unlock
        UNLOCK_GUMBALL = res.getInteger(R.integer.unlock_gumball) * ms;
        UNLOCK_JETPACK = res.getInteger(R.integer.unlock_jetpack) * ms;
        UNLOCK_MEMORY = res.getInteger(R.integer.unlock_memory) * ms;

        // Video unlock
        UNLOCK_VIDEO_1 = res.getInteger(R.integer.unlock_video1) * ms;
        UNLOCK_VIDEO_15 = res.getInteger(R.integer.unlock_video15) * ms;
        UNLOCK_VIDEO_23 = res.getInteger(R.integer.unlock_video23) * ms;
    }

    void initialiseViews() {
        mVillageView.setVillage(mVillage);
        mMarkerManager.initialise(mSantaContext, mMarkers);
    }

    // see http://stackoverflow.com/questions/25884954/deep-linking-and-multiple-app-instances/
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleVoiceActions();

        Bundle extras = intent.getExtras();
        if (extras != null
                && extras.getString(NotificationConstants.KEY_NOTIFICATION_TYPE) != null) {
            // [ANALYTICS EVENT]: Launch Notification
            AnalyticsManager.sendEvent(R.string.analytics_event_category_launch,
                    R.string.analytics_launch_action_notification,
                    extras.getString(NotificationConstants.KEY_NOTIFICATION_TYPE));
            SantaLog.d(TAG, "launched from notification");
        }
    }

    private void handleVoiceActions() {
        Intent intent = getIntent();
        if (VoiceAction.isVoiceAction(intent)) {
            if (isAlreadyHandled(intent)) {
                Log.d(TAG, String.format("Ignoring an already handled intent [%s]",
                        intent.getAction()));
                return; // already processed
            }
            boolean handled = false;
            // check all the pins
            AbstractLaunch[] pins = mMarkerManager.getLaunchers();
            // try sending the voice command to all launchers, the first one that handles it wins
            for (AbstractLaunch l : pins) {
                if (handled = l.handleVoiceAction(intent)) {
                    // [ANALYTICS EVENT]: Launch Voice
                    AnalyticsManager.sendEvent(R.string.analytics_event_category_launch,
                            R.string.analytics_launch_action_voice,
                            l.mContentDescription);
                    break;
                }
            }
            if (!handled) {
                Toast.makeText(this, getResources().getText(R.string.voice_command_unhandled),
                        Toast.LENGTH_SHORT)
                        .show();
                // [ANALYTICS EVENT]: Launch Voice Unhandled
                AnalyticsManager.sendEvent(R.string.analytics_event_category_launch,
                        R.string.analytics_launch_action_voice,
                        R.string.analytics_launch_voice_unhandled);
            } else {
                setAlreadyHandled(intent);
            }
        }
    }

    /**
     * This method is responsible for handling a corner case.
     * Upon orientation change, the Activity is re-created (onCreate is called)
     * and the same intent is (re)delivered to the app.
     * Fortunately the Intent is Parcelable so we can mark it and check for this condition.
     * Without this, if the phone is in portrait mode, and the user issues voice command to
     * start a game (or other forcing orientation change), the following happens:
     *
     * 1. com.google.android.apps.santatracker.PLAY_GAME is delivered to the app.
     * 2. Game is started and phone switches to landscape.
     * 3. User ends the game, rotates the phone back to portrait.
     * 4. onCreate is called again since StartupActivity is re-created.
     * 5. The voice action is re-executed
     * (since getIntent returns com.google.android.apps.santatracker.PLAY_GAME).
     *
     * We don't want #5 to take place.
     *
     * @param intent current intent
     */
    private void setAlreadyHandled(Intent intent) {
        intent.putExtra(INTENT_HANDLED, true);
    }

    /**
     * Checks to see if the intent (voice command) has already been processed
     *
     * @param intent current intent (voice command)
     * @return true if the intent (voice command) has already been processed
     */
    private boolean isAlreadyHandled(Intent intent) {
        return intent.getBooleanExtra(INTENT_HANDLED, false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
        mAudioPlayer.stopAll();

        cancelUIUpdate();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mGameHelper.onStart(this);
        initialiseViews();
        resetLauncherStates();
    }

    private void resetLauncherStates() {
        // Start only if play services are available
        if (mHaveGooglePlayServices) {
            updateVillage();
        } else {
            //TODO
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGameHelper.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mGameHelper.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mResumed && hasFocus && !AccessibilityUtil.isTouchAccessiblityEnabled(this)) {
            mAudioPlayer.playTrackExclusive(R.raw.village_music, true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.menu_startup, menu);

        mMenuItemLegal = menu.findItem(R.id.legal);
        mMenuItemLegal.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final Resources resources = getResources();
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(StartupActivity.this);
                dialogBuilder.setItems(resources.getStringArray(R.array.legal_privacy),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String url;
                                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                                        StartupActivity.this);
                                switch (which) {
                                    case 1:
                                        // Privacy
                                        url = resources.getString(R.string.url_privacy);
                                        break;
                                    case 2:
                                        // TOS
                                        url = resources.getString(R.string.url_tos);
                                        break;
                                    case 3:
                                        //Box 2D
                                        dialog.dismiss();
                                        dialogBuilder.setMessage(R.string.jbox2d_license).create()
                                                .show();
                                        dialogBuilder.setPositiveButton(android.R.string.ok,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog,
                                                            int which) {
                                                        dialog.dismiss();
                                                    }
                                                });
                                        return;
                                    case 4:
                                        // Show play services license text
                                        dialog.dismiss();
                                        dialogBuilder.setMessage(GooglePlayServicesUtil
                                                .getOpenSourceSoftwareLicenseInfo(
                                                        getApplicationContext())).create().show();
                                        dialogBuilder.setPositiveButton(android.R.string.ok,
                                                new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog,
                                                            int which) {
                                                        dialog.dismiss();
                                                    }
                                                });
                                        return;
                                    case 0:
                                    default:
                                        url = resources.getString(R.string.url_legal);
                                        break;
                                }
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                            }
                        });
                dialogBuilder.create().show();
                return true;
            }
        });

        menu.findItem(R.id.open_help)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.url_help))));
                        return true;
                    }
                });

        menu.findItem(R.id.sign_out)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        mGameHelper.signOut();
                        updateSignInState(false);
                        return true;
                    }
                });

        // For debugging and testing purposes, display notifications just as if they were scheduled
        // to be displayed.

        // The take-off notification with a fixed message text.
        menu.findItem(R.id.notification_takeoff)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        SantaNotificationBuilder
                                .CreateSantaNotification(StartupActivity.this,
                                        R.string.notification_takeoff);
                        return true;
                    }
                });

        // Info notification that contains a title, text and hardcoded image URL.
        menu.findItem(R.id.notification_info)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        SantaNotificationBuilder
                                .CreateInfoNotification(StartupActivity.this, "Title", "text",
                                        "https://www.google.com/images/srpr/logo11w.png");
                        return true;
                    }
                });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        menu.findItem(R.id.sign_out).setVisible(mSignedIn);
        return super.onPrepareOptionsPanel(view, menu);
    }

    /**
     * Update the UI elements on the village.
     * This resets the navigation elements (pins), schedules another update once additional
     * pins (un)lock and schedules the take-off notification.
     */
    private void updateVillage() {
        Log.d(TAG, "Santa is offline.");

        // Enable/disable pins
        updateNavigation();

        // Schedule UI Updates
        scheduleUIUpdate();

        // TODO: Set only one state here and remove references to timing
        // Note that in the "no data" state, this may or may not include the TIME_OFFSET, depending
        // on whether we've had a successful API call and still have the data. We can't use
        // System.currentTimeMillis() as it *will* ignore TIME_OFFSET.
        final long time = System.currentTimeMillis();

        AbstractLaunch launchSanta = mMarkerManager.getLauncher(MarkerManager.SANTA);

        if (time < OFFLINE_SANTA_DEPARTURE) {
            // Santa hasn't departed yet, show countdown
            launchSanta.setState(AbstractLaunch.STATE_LOCKED);
            final long notificationTime = OFFLINE_SANTA_DEPARTURE;
            SantaNotificationBuilder
                    .ScheduleSantaNotification(getApplicationContext(), notificationTime,
                            NotificationConstants.NOTIFICATION_TAKEOFF);

        } else if (time >= OFFLINE_SANTA_DEPARTURE && time < OFFLINE_SANTA_FINALARRIVAL) {
            // Santa should have already left, but no data yet, hide countdown and show message
            launchSanta.setState(AbstractLaunch.STATE_DISABLED);

        } else {
            // Post Christmas
            launchSanta.setState(AbstractLaunch.STATE_FINISHED);
        }
    }

    /*
     * Village Markers
     */
    private void updateNavigation() {
        // Games
        mMarkerManager.getLauncher(MarkerManager.GUMBALL).setState(
                getGamePinState(mFlagDisableGumball, UNLOCK_GUMBALL));
        mMarkerManager.getLauncher(MarkerManager.MEMORY).setState(
                getGamePinState(mFlagDisableMemory, UNLOCK_MEMORY));
        mMarkerManager.getLauncher(MarkerManager.JETPACK)
                .setState(getGamePinState(mFlagDisableJetpack, UNLOCK_JETPACK));

        ((LaunchVideo) mMarkerManager.getLauncher(MarkerManager.VIDEO01)).setVideo(
                mVideoList[0], UNLOCK_VIDEO_1);
        ((LaunchVideo) mMarkerManager.getLauncher(MarkerManager.VIDEO15)).setVideo(
                mVideoList[1], UNLOCK_VIDEO_15);
        ((LaunchVideo) mMarkerManager.getLauncher(MarkerManager.VIDEO23)).setVideo(
                mVideoList[2], UNLOCK_VIDEO_23);

        // reinitialise action bar
        supportInvalidateOptionsMenu();
    }

    private int getGamePinState(boolean disabledFlag, long unlockTime) {
        // TODO: update logic
        if (disabledFlag) {
            return AbstractLaunch.STATE_HIDDEN;
        } else if (!disabledFlag && System.currentTimeMillis() < unlockTime) {
            return AbstractLaunch.STATE_LOCKED;
        } else {
            return AbstractLaunch.STATE_READY;
        }
    }

    /*
     * Scheduled UI update
     */

    /**
     * Schedule a call to {@link #updateVillage()} at the next time at which
     * the UI should be updated (games become available, Santa takes off, Santa is finished).
     */
    private void scheduleUIUpdate() {
        // cancel scheduled update
        cancelUIUpdate();

        final long delay = calculateNextUiUpdateDelay();
        if (delay > 0 && delay < Long.MAX_VALUE) {
            // schedule if delay is in the future
            mHandler.postDelayed(mUpdateUiRunnable, delay);
        }
    }

    private long calculateNextUiUpdateDelay() {

        final long time = System.currentTimeMillis();

        final long departureDelay = mFirstDeparture - time;
        final long arrivalDelay = mFinalArrival - time;

        // if disable flag is toggled, exclude from calculation
        final long[] delays = new long[]{
                mFlagDisableGumball ? Long.MAX_VALUE : UNLOCK_GUMBALL - time,
                mFlagDisableJetpack ? Long.MAX_VALUE : UNLOCK_JETPACK - time,
                mFlagDisableMemory ? Long.MAX_VALUE : UNLOCK_MEMORY - time,
                departureDelay, arrivalDelay};

        // find lowest delay, but only count positive values or zero (ie. that are in the future)
        long delay = Long.MAX_VALUE;
        for (final long x : delays) {
            if (x >= 0) {
                delay = Math.min(delay, x);
            }
        }

        return delay;
    }

    private void cancelUIUpdate() {
        mHandler.removeCallbacksAndMessages(null);
    }

    private Runnable mUpdateUiRunnable = new Runnable() {
        @Override
        public void run() {
            updateVillage();
        }
    };


    /*
     * Google Play Services - from
     * http://code.google.com/p/google-api-java-client/source/browse/tasks-android-sample/src/main/
     *     java/com/google/api/services/samples/tasks/android/TasksSample.java?repo=samples
     */

    /**
     * Check that Google Play services APK is installed and up to date.
     */
    private boolean checkGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        }
        return (connectionStatusCode == ConnectionResult.SUCCESS);
    }

    private void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {

        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(connectionStatusCode, this, 0);
        dialog.show();
        dialog.setOnDismissListener(new Dialog.OnDismissListener() {

            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });

    }

    private void updateSignInState(boolean signedIn) {
        mSignedIn = signedIn;
        findViewById(R.id.games_buttons).setVisibility(signedIn ? View.VISIBLE : View.INVISIBLE);
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onSignInFailed() {
        updateSignInState(false);
    }

    @Override
    public void onSignInSucceeded() {
        updateSignInState(true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_show_achievements:
                showAchievements();
                break;
            case R.id.button_show_leaderboards:
                showLeaderboards();
                break;
        }
    }

    private void showAchievements() {
        GoogleApiClient apiClient = mGameHelper.getApiClient();
        if (apiClient != null && apiClient.isConnected()) {
            startActivityForResult(
                    Games.Achievements.getAchievementsIntent(apiClient),
                    RC_GAMES);
        }
    }

    private void showLeaderboards() {
        GoogleApiClient apiClient = mGameHelper.getApiClient();
        if (apiClient != null && apiClient.isConnected()) {
            startActivityForResult(
                    Games.Leaderboards.getAllLeaderboardsIntent(apiClient),
                    RC_GAMES);
        }
    }

    @Override
    public void playSoundOnce(int resSoundId) {
        mAudioPlayer.playTrack(resSoundId, false);
    }

    /**
     * Context holder and convenience methods for AbstractLaunch objects.
     */
    public class SantaContext {

        public Context getContext() {
            return StartupActivity.this.getApplicationContext();
        }

        public Resources getResources() {
            return StartupActivity.this.getResources();
        }

        // Launch the activity but do cleanup first.
        public void launchActivity(Class clss) {
            SantaNotificationBuilder.DismissNotifications(getApplicationContext());
            startActivityForResult(new Intent(getApplicationContext(), clss), RC_STARTUP);
        }

    }

    protected void enableAnimation(boolean animate) {
        mVillageView.enableAnimation(animate);
    }
}
