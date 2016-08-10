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

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.google.android.apps.santatracker.AudioPlayer;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.SantaApplication;
import com.google.android.apps.santatracker.SantaNotificationBuilder;
import com.google.android.apps.santatracker.cast.NotificationDataCastManager;
import com.google.android.apps.santatracker.common.NotificationConstants;
import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.apps.santatracker.games.PlayGamesFragment;
import com.google.android.apps.santatracker.games.SignInListener;
import com.google.android.apps.santatracker.invites.AppInvitesFragment;
import com.google.android.apps.santatracker.service.SantaService;
import com.google.android.apps.santatracker.service.SantaServiceMessages;
import com.google.android.apps.santatracker.util.AccessibilityUtil;
import com.google.android.apps.santatracker.util.AnalyticsManager;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.apps.santatracker.village.Village;
import com.google.android.apps.santatracker.village.VillageView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Launch activity for the app. Handles loading of the village, the state of the markers (based on
 * the date/time) and incoming voice intents.
 */
public class StartupActivity extends AppCompatActivity implements
        View.OnClickListener, Village.VillageListener,
        VoiceAction.VoiceActionHandler, SignInListener, SantaContext, LaunchCountdown.LaunchCountdownContext {

    protected static final String TAG = "SantaStart";
    private static final String VILLAGE_TAG = "VillageFragment";
    private static final String INTENT_HANDLED = "intent_handled";

    private PlayGamesFragment mGamesFragment;
    private AppInvitesFragment mInvitesFragment;
    private AudioPlayer mAudioPlayer;

    private boolean mResumed = false;
    private boolean mSignedIn = false;

    private Village mVillage;
    private VillageView mVillageView;
    private ImageView mVillageBackdrop;
    private View mLaunchButton;
    private SantaCollapsingToolbarLayout mSantaCollapsing;
    private View mCountdownView;
    private View mWavingSanta;
    private ImageView mWavingSantaArm;
    private Animation mWavingAnim;
    private View mOrnament;

    // private MarkerManager mMarkerManager;
    private CardAdapter mCardAdapter;
    private CardLayoutManager mCardLayoutManager;
    private StickyScrollListener mScrollListener;
    private RecyclerView mMarkers;

    private TextView mStatusText;
    private LaunchCountdown mCountdown;

    // Load these values from resources when an instance of this activity is initialised.
    private static long OFFLINE_SANTA_DEPARTURE;
    private static long OFFLINE_SANTA_FINALARRIVAL;
    private static long UNLOCK_GUMBALL;
    private static long UNLOCK_JETPACK;
    private static long UNLOCK_MEMORY;
    private static long UNLOCK_ROCKET;
    private static long UNLOCK_DANCER;
    private static long UNLOCK_SNOWDOWN;
    private static long UNLOCK_VIDEO_1;
    private static long UNLOCK_VIDEO_15;
    private static long UNLOCK_VIDEO_23;

    // Server controlled flags
    private long mOffset = 0;
    private boolean mFlagSwitchOff = false;
    private boolean mFlagDisableCast = false;
    private boolean mFlagDisableGumball = false;
    private boolean mFlagDisableJetpack = false;
    private boolean mFlagDisableMemory = false;
    private boolean mFlagDisableRocket = false;
    private boolean mFlagDisableDancer = false;
    private boolean mFlagDisableSnowdown = false;

    private String[] mVideoList = new String[]{null, null, null};


    private boolean mHaveGooglePlayServices = false;
    private long mFirstDeparture;
    private long mFinalArrival;

    private MenuItem mMenuItemLegal;

    // Handler for scheduled UI updates
    private Handler mHandler = new Handler();

    // Waiting for data from the API (no data or data is outdated)
    private boolean mWaitingForApi = true;

    // Launching a child activity (another launch request should be blocked)
    private boolean mLaunchingChild = false;

    // Service integration
    private Messenger mService = null;

    private boolean mIsBound = false;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    // request code for games Activities
    private final int RC_STARTUP = 1111;
    private final int RC_GAMES = 9999;

    // Permission request codes
    private final int RC_DEBUG_PERMS = 1;

    private FirebaseAnalytics mMeasurement;

    private NotificationDataCastManager mCastManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionsIfDebugModeEnabled();

        // Glide's pretty aggressive at caching images, so get the 8888 preference in early.
        if (!Glide.isSetup()) {
            Glide.setup(new GlideBuilder(getActivityContext())
                    .setDecodeFormat(DecodeFormat.PREFER_ARGB_8888));
        }

        // TODO: rename temp 'layout_startup_2015' layout when its implementation is completed.
        setContentView(R.layout.layout_startup_2015);

        loadResourceFields(getResources());

        mCountdown = new LaunchCountdown(this);
        mCountdownView = findViewById(R.id.countdown_container);
        mAudioPlayer = new AudioPlayer(getApplicationContext());

        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        if (toolBar != null) {
            setSupportActionBar(toolBar);
        }

        // Set up collapsing
        mSantaCollapsing = (SantaCollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        mSantaCollapsing.setToolbarContentView(findViewById(R.id.toolbar_content));
        mSantaCollapsing.setOverlayView(findViewById(R.id.view_color_overlay));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        mStatusText = (TextView) findViewById(R.id.statusText);

        mVillageView = (VillageView) findViewById(R.id.villageView);
        mVillage = (Village) getSupportFragmentManager().findFragmentByTag(VILLAGE_TAG);
        if (mVillage == null) {
            mVillage = new Village();
            getSupportFragmentManager().beginTransaction().add(mVillage, VILLAGE_TAG).commit();
        }
        mVillageBackdrop = (ImageView) findViewById(R.id.villageBackground);
        mLaunchButton = findViewById(R.id.launch_button);
        mLaunchButton.setOnClickListener(this);
        mOrnament = findViewById(R.id.countdown_ornament);

        mWavingSanta = findViewById(R.id.santa_waving);
        mWavingSantaArm = (ImageView) findViewById(R.id.santa_arm);
        mWavingSanta.setOnClickListener(this);

        mMarkers = (RecyclerView) findViewById(R.id.markers);
        initialiseViews();

        mHaveGooglePlayServices = NotificationDataCastManager.checkGooglePlayServices(this);

        // initialize our connection to Google Play Games
        mGamesFragment = PlayGamesFragment.getInstance(this, this);

        // App invites
        mInvitesFragment = AppInvitesFragment.getInstance(this);

        // set up click listeners for our buttons
        findViewById(R.id.fab_achievement).setOnClickListener(this);
        findViewById(R.id.fab_leaderboard).setOnClickListener(this);

        // Initialize measurement
        mMeasurement = FirebaseAnalytics.getInstance(this);
        MeasurementManager.recordScreenView(mMeasurement,
                getString(R.string.analytics_screen_village));

        // [ANALYTICS SCREEN]: Village
        AnalyticsManager.sendScreenView(R.string.analytics_screen_village);
        // set the initial states
        resetLauncherStates();
        // See if it was a voice action which triggered this activity and handle it
        onNewIntent(getIntent());

        if (mHaveGooglePlayServices) {
            mCastManager = SantaApplication.getCastManager(this);
        }
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
        UNLOCK_ROCKET = res.getInteger(R.integer.unlock_rocket) * ms;
        UNLOCK_DANCER = res.getInteger(R.integer.unlock_dancer) * ms;
        UNLOCK_SNOWDOWN = res.getInteger(R.integer.unlock_snowdown) * ms;

        // Video unlock
        UNLOCK_VIDEO_1 = res.getInteger(R.integer.unlock_video1) * ms;
        UNLOCK_VIDEO_15 = res.getInteger(R.integer.unlock_video15) * ms;
        UNLOCK_VIDEO_23 = res.getInteger(R.integer.unlock_video23) * ms;
    }

    void initialiseViews() {
        mVillageView.setVillage(mVillage);

        // Initialize the RecyclerView
        int numColumns = getResources().getInteger(R.integer.village_columns);
        mCardLayoutManager = new CardLayoutManager(this, numColumns);
        mCardAdapter = new CardAdapter(this);
        mScrollListener = new StickyScrollListener(mCardLayoutManager, numColumns);

        mMarkers.setAdapter(mCardAdapter);
        mMarkers.setLayoutManager(mCardLayoutManager);
        mMarkers.addOnScrollListener(mScrollListener);
    }

    private void requestPermissionsIfDebugModeEnabled() {
        // If debug mode is enabled in debug_settings.xml, and we don't yet have storage perms, ask.
        if (getResources().getBoolean(R.bool.prompt_for_sdcard_perms)
                && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    RC_DEBUG_PERMS);
        }
    }

    // see http://stackoverflow.com/questions/25884954/deep-linking-and-multiple-app-instances/
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleVoiceActions();

        Bundle extras = intent.getExtras();
        if (extras != null
                && extras.getString(NotificationConstants.KEY_NOTIFICATION_TYPE) != null) {

            // App Measurement
            MeasurementManager.recordCustomEvent(mMeasurement,
                    getString(R.string.analytics_event_category_launch),
                    getString(R.string.analytics_launch_action_notification),
                    extras.getString(NotificationConstants.KEY_NOTIFICATION_TYPE));

            // [ANALYTICS EVENT]: Launch Notification
            AnalyticsManager.sendEvent(R.string.analytics_event_category_launch,
                    R.string.analytics_launch_action_notification,
                    extras.getString(NotificationConstants.KEY_NOTIFICATION_TYPE));
            SantaLog.d(TAG, "launched from notification");
        }
    }

    @Override
    public boolean handleVoiceAction(Intent intent) {
        if (VoiceAction.ACTION_PLAY_RANDOM_GAME.equals(intent.getAction())) {
            Log.d(TAG, String.format("Voice command: [%s]", VoiceAction.ACTION_PLAY_RANDOM_GAME));
            return startRandomGame();
        } else {
            return false;
        }
    }

    /**
     * Pick a game at random from the available games in STATE_READY state
     * @return true if a game was launched
     */
    private boolean startRandomGame() {
        // find out all the games that are ready to play
        AbstractLaunch[] pins = mCardAdapter.getLaunchers();
        List<AbstractLaunch> games = new ArrayList<AbstractLaunch>(pins.length);
        for (AbstractLaunch pin : pins) {
            if (pin.isGame()) {
                if (pin.mState == AbstractLaunch.STATE_READY) {
                    games.add(pin);
                }
            }
        }
        // now pick one of the games from games and launch it
        if (games.size() > 0) {
            Random r = new Random();
            int index = r.nextInt(games.size());
            AbstractLaunch game = games.get(index);
            Log.d(TAG, String.format("Picked a game at random [%s]",
                    game.mContentDescription));
            // launch the game by simulating a click
            game.onClick(game.getClickTarget());

            // App Measurement
            MeasurementManager.recordCustomEvent(mMeasurement,
                    getString(R.string.analytics_event_category_launch),
                    getString(R.string.analytics_launch_action_voice),
                    game.mContentDescription);

            // [ANALYTICS EVENT]: Launch Voice
            AnalyticsManager.sendEvent(R.string.analytics_event_category_launch,
                    R.string.analytics_launch_action_voice,
                    game.mContentDescription);
            return true;
        } else {
            return false;
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
            boolean handled;
            // first check if *this* activity can handle the voice action
            handled = handleVoiceAction(intent);
            // next check all the pins
            if (!handled) {
                AbstractLaunch[] pins = mCardAdapter.getLaunchers();
                // try sending the voice command to all launchers, the first one that handles it wins
                for (AbstractLaunch l : pins) {
                    if (handled = l.handleVoiceAction(intent)) {
                        // App Measurement
                        MeasurementManager.recordCustomEvent(mMeasurement,
                                getString(R.string.analytics_event_category_launch),
                                getString(R.string.analytics_launch_action_voice),
                                l.mContentDescription);

                        // [ANALYTICS EVENT]: Launch Voice
                        AnalyticsManager.sendEvent(R.string.analytics_event_category_launch,
                                R.string.analytics_launch_action_voice,
                                l.mContentDescription);
                        break;
                    }
                }
            }
            if (!handled) {
                Toast.makeText(this, getResources().getText(R.string.voice_command_unhandled),
                        Toast.LENGTH_SHORT)
                        .show();

                // App Measurement
                MeasurementManager.recordCustomEvent(mMeasurement,
                        getString(R.string.analytics_event_category_launch),
                        getString(R.string.analytics_launch_action_voice),
                        getString(R.string.analytics_launch_voice_unhandled));

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

        if (mHaveGooglePlayServices) {
            mCastManager = SantaApplication.getCastManager(this);
            mCastManager.incrementUiCounter();
        }

        mResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
        mAudioPlayer.stopAll();

        cancelUIUpdate();

        if (mCastManager != null) {
            mCastManager.decrementUiCounter();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerWithService();

        // Check for App Invites
        mInvitesFragment.getInvite(new AppInvitesFragment.GetInvitationCallback() {
            @Override
            public void onInvitation(String invitationId, String deepLink) {
                Log.d(TAG, "onInvitation: " + deepLink);
            }
        }, true);

        initialiseViews();
        resetLauncherStates();
    }

    private void resetLauncherStates() {
        // Start only if play services are available
        if (mHaveGooglePlayServices) {
            stateNoData();
        } else {
            hideStatus();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterFromService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mGamesFragment.onActivityResult(requestCode, resultCode, data);
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

        // Add cast button
        if (mCastManager != null) {
            mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        }

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
                                        // TOS
                                        url = resources.getString(R.string.url_seismic);
                                        break;
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

        menu.findItem(R.id.menu_app_invite)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mInvitesFragment.sendGenericInvite();
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

        menu.findItem(R.id.github_santa)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.url_github_santa))));
                        return true;
                    }
                });

        menu.findItem(R.id.github_pienoon)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse(getString(R.string.url_github_pienoon))));
                        return true;
                    }
                });

        menu.findItem(R.id.sign_out)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Games.signOut(mGamesFragment.getGamesApiClient());
                        updateSignInState(false);
                        return true;
                    }
                });

        // TODO Temp menu items for testing notifications
        menu.findItem(R.id.notification_nearby)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        SantaNotificationBuilder.CreateSantaNotification(StartupActivity.this,
                                R.string.notification_nearby);
                        /*
                        Intent wearIntent = new Intent(StartupActivity.this,
                                PhoneNotificationService.class);
                        wearIntent.setAction(NotificationConstants.ACTION_SEND);
                        wearIntent.putExtra(NotificationConstants.KEY_CONTENT,
                                StartupActivity.this.getResources()
                                        .getString(R.string.notification_nearby));
                        StartupActivity.this.startService(wearIntent);
                        */
                        return true;
                    }
                });
        menu.findItem(R.id.notification_takeoff)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        SantaNotificationBuilder
                                .CreateSantaNotification(StartupActivity.this,
                                        R.string.notification_takeoff);
                        /*
                        Intent wearIntent = new Intent(StartupActivity.this,
                                PhoneNotificationService.class);
                        wearIntent.setAction(NotificationConstants.ACTION_SEND);
                        wearIntent.putExtra(NotificationConstants.KEY_CONTENT,
                                StartupActivity.this.getResources()
                                        .getString(R.string.notification_takeoff));
                        StartupActivity.this.startService(wearIntent);
                        */
                        return true;
                    }
                });
        menu.findItem(R.id.notification_location)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        /*
                        SantaNotificationBuilder
                                .CreateTriviaNotification(StartupActivity.this, "location",
                                        "photoUrl", "mapUrl", "fact");
                        */
                        SantaNotificationBuilder
                                .CreateInfoNotification(StartupActivity.this, "Title", "text",
                                        "https://www.google.com/images/srpr/logo11w.png");
                        return true;
                    }
                });

        menu.findItem(R.id.launch_mode)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        enableTrackerMode(true);
                        return true;
                    }
                });

        menu.findItem(R.id.countdown_mode)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        startCountdown(OFFLINE_SANTA_DEPARTURE);
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

    private void setCastDisabled(boolean disableCast) {
        if (mCastManager == null || !mHaveGooglePlayServices) {
            return;
        }
        // Enable or disable casting
        SantaApplication.toogleCast(this, disableCast);
    }

    /**
     * Move to 'no valid data' state ("offline"). No further locations, rely on local offline data
     * only.
     */
    private void stateNoData() {
        Log.d(TAG, "Santa is offline.");

        // Enable/disable pins and nav drawer
        updateNavigation();

        // Schedule UI Updates
        scheduleUIUpdate();

        // Disable cast only if not already casting
        if (mHaveGooglePlayServices && mCastManager != null && !mCastManager.isConnected()) {
            setCastDisabled(true);
        }

        // Note that in the "no data" state, this may or may not include the TIME_OFFSET, depending
        // on whether we've had a successful API call and still have the data. We can't use
        // System.currentTimeMillis() as it *will* ignore TIME_OFFSET.
        final long time = SantaPreferences.getCurrentTime();

        AbstractLaunch launchSanta = mCardAdapter.getLauncher(CardAdapter.SANTA);

        if (time < OFFLINE_SANTA_DEPARTURE) {
            // Santa hasn't departed yet, show countdown
            launchSanta.setState(AbstractLaunch.STATE_LOCKED);
            startCountdown(OFFLINE_SANTA_DEPARTURE);
            final long notificationTime = SantaPreferences
                    .getAdjustedTime(OFFLINE_SANTA_DEPARTURE);
            SantaNotificationBuilder
                    .ScheduleSantaNotification(getApplicationContext(), notificationTime,
                            NotificationConstants.NOTIFICATION_TAKEOFF);

        } else if (time >= OFFLINE_SANTA_DEPARTURE && time < OFFLINE_SANTA_FINALARRIVAL) {
            // Santa should have already left, but no data yet, hide countdown and show message
            stopCountdown();
            enableTrackerMode(false);
            launchSanta.setState(AbstractLaunch.STATE_DISABLED);
            showStatus(R.string.contacting_santa);
        } else {
            // Post Christmas
            stopCountdown();
            enableTrackerMode(false);
            launchSanta.setState(AbstractLaunch.STATE_FINISHED);
        }
    }


    /**
     * Move to 'data' (online) state.
     */
    private void stateData() {
        Log.d(TAG, "Santa is online.");

        // Enable/disable pins and nav drawer
        updateNavigation();

        // Schedule next UI update
        scheduleUIUpdate();

        // hide status
        hideStatus();

        // Set cast state
        setCastDisabled(mFlagDisableCast);

        long time = SantaPreferences.getCurrentTime();

        AbstractLaunch launchSanta = mCardAdapter.getLauncher(CardAdapter.SANTA);
        // Is Santa finished?
        if (time > mFirstDeparture && time < OFFLINE_SANTA_FINALARRIVAL) {
            // Santa should be travelling, enable map and hide countdown
            enableTrackerMode(true);

            // Schedule stream notifications
            SantaNotificationBuilder.ScheduleNotificationNotification(this);

            if (mFlagSwitchOff) {
                // Kill-switch triggered, disable button
                launchSanta.setState(AbstractLaunch.STATE_DISABLED);
            } else if (time > mFinalArrival) {
                // No data
                launchSanta.setState(AbstractLaunch.STATE_DISABLED);
                showStatus(R.string.still_trying_to_reach_santa);
            } else {
                launchSanta.setState(AbstractLaunch.STATE_READY);
            }

        } else if (time < mFirstDeparture) {
            // Santa hasn't taken off yet, start count-down and schedule
            // notification to first departure, hide buttons
            final long notificationTime = SantaPreferences
                    .getAdjustedTime(mFirstDeparture);
            SantaNotificationBuilder.ScheduleSantaNotification(getApplicationContext(),
                    notificationTime,
                    NotificationConstants.NOTIFICATION_TAKEOFF);
            // Schedule stream notifications
            SantaNotificationBuilder.ScheduleNotificationNotification(this);

            startCountdown(mFirstDeparture);
            launchSanta.setState(AbstractLaunch.STATE_LOCKED);

        } else {
            // Post Christmas, hide countdown and buttons
            launchSanta.setState(AbstractLaunch.STATE_FINISHED);
            stopCountdown();
            enableTrackerMode(false);
        }

    }

    public void enableTrackerMode(boolean showLaunchButton) {
        mCountdown.cancel();
        mVillageBackdrop.setImageResource(R.drawable.village_bg_launch);
        mVillage.setPlaneEnabled(false);
        mLaunchButton.setVisibility(showLaunchButton ? View.VISIBLE : View.GONE);
        mSantaCollapsing.setOverlayColor(R.color.villageToolbarDark);
        mCountdownView.setVisibility(View.GONE);
        mWavingSanta.setVisibility(View.GONE);
        mOrnament.setVisibility(View.GONE);
    }

    public void startCountdown(long time) {
        mCountdown.startTimer(time - SantaPreferences.getCurrentTime());
        mVillageBackdrop.setImageResource(R.drawable.village_bg_countdown);
        mVillage.setPlaneEnabled(true);
        mLaunchButton.setVisibility(View.GONE);
        mSantaCollapsing.setOverlayColor(R.color.villageToolbarLight);
        mCountdownView.setVisibility(View.VISIBLE);
        mWavingSanta.setVisibility(View.VISIBLE);
        mOrnament.setVisibility(View.VISIBLE);
    }

    public void stopCountdown() {
        mCountdown.cancel();
        mCountdownView.setVisibility(View.GONE);
    }

    /*
     * Village Markers
     */
    private void updateNavigation() {
        // Games
        mCardAdapter.getLauncher(CardAdapter.GUMBALL).setState(
                getGamePinState(mFlagDisableGumball, UNLOCK_GUMBALL));
        mCardAdapter.getLauncher(CardAdapter.MEMORY).setState(
                getGamePinState(mFlagDisableMemory, UNLOCK_MEMORY));
        mCardAdapter.getLauncher(CardAdapter.JETPACK)
                .setState(getGamePinState(mFlagDisableJetpack, UNLOCK_JETPACK));
        mCardAdapter.getLauncher(CardAdapter.ROCKET).setState(
                getGamePinState(mFlagDisableRocket, UNLOCK_ROCKET));
        mCardAdapter.getLauncher(CardAdapter.DANCER).setState(
                getGamePinState(mFlagDisableDancer, UNLOCK_DANCER));
        mCardAdapter.getLauncher(CardAdapter.SNOWDOWN).setState(
                getGamePinState(mFlagDisableSnowdown, UNLOCK_SNOWDOWN));

        ((LaunchVideo) mCardAdapter.getLauncher(CardAdapter.VIDEO01)).setVideo(
                mVideoList[0], UNLOCK_VIDEO_1);
        ((LaunchVideo) mCardAdapter.getLauncher(CardAdapter.VIDEO15)).setVideo(
                mVideoList[1], UNLOCK_VIDEO_15);
        ((LaunchVideo) mCardAdapter.getLauncher(CardAdapter.VIDEO23)).setVideo(
                mVideoList[2], UNLOCK_VIDEO_23);

        // reinitialise action bar
        supportInvalidateOptionsMenu();
    }

    private int getGamePinState(boolean disabledFlag, long unlockTime) {
        if (disabledFlag) {
            return AbstractLaunch.STATE_HIDDEN;
        } else if (!disabledFlag && SantaPreferences.getCurrentTime() < unlockTime) {
            return AbstractLaunch.STATE_LOCKED;
        } else {
            return AbstractLaunch.STATE_READY;
        }
    }

    /*
     * Status Message
     */

    private void showStatus(int i) {
        int state = mCardAdapter.getLauncher(CardAdapter.SANTA).getState();
        if (state == AbstractLaunch.STATE_DISABLED || state == AbstractLaunch.STATE_LOCKED) {

            mStatusText.setVisibility(View.VISIBLE);
            mStatusText.setText(i);
            mStatusText.setContentDescription(mStatusText.getText());
        }
    }

    private void hideStatus() {
        mStatusText.setVisibility(View.GONE);
    }

    /*
     * Scheduled UI update
     */

    /**
     * Schedule a call to {@link #stateData()} or {@link #stateNoData()} at the next time at which
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

        final long time = SantaPreferences.getCurrentTime();

        final long departureDelay = mFirstDeparture - time;
        final long arrivalDelay = mFinalArrival - time;

        // if disable flag is toggled, exclude from calculation
        final long[] delays = new long[]{
                mFlagDisableGumball ? Long.MAX_VALUE : UNLOCK_GUMBALL - time,
                mFlagDisableJetpack ? Long.MAX_VALUE : UNLOCK_JETPACK - time,
                mFlagDisableMemory ? Long.MAX_VALUE : UNLOCK_MEMORY - time,
                mFlagDisableRocket ? Long.MAX_VALUE : UNLOCK_ROCKET - time,
                mFlagDisableDancer ? Long.MAX_VALUE : UNLOCK_DANCER - time,
                mFlagDisableSnowdown ? Long.MAX_VALUE : UNLOCK_SNOWDOWN - time,
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
            if (!mWaitingForApi) {
                stateData();
            } else {
                stateNoData();
            }
        }
    };

    private void updateSignInState(boolean signedIn) {
        mSignedIn = signedIn;
        setFabVisibility((FloatingActionButton) findViewById(R.id.fab_leaderboard), signedIn);
        setFabVisibility((FloatingActionButton) findViewById(R.id.fab_achievement), signedIn);
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
            case R.id.launch_button:
                launchTracker();
                break;
            case R.id.santa_waving:
                animateSanta();
                break;
            case R.id.fab_achievement:
                showAchievements();
                break;
            case R.id.fab_leaderboard:
                showLeaderboards();
                break;
        }
    }

    private void animateSanta() {
        boolean play = false;
        if (mWavingAnim == null) {
            mWavingAnim = AnimationUtils.loadAnimation(this, R.anim.santa_wave);
            play = true;
        } else if (mWavingAnim.hasEnded()) {
            play = true;
        }

        if (play) {
            playSoundOnce(R.raw.ho_ho_ho);
            mWavingSantaArm.startAnimation(mWavingAnim);
        }
    }

    private void showAchievements() {
        GoogleApiClient apiClient = mGamesFragment.getGamesApiClient();
        if (apiClient != null && apiClient.isConnected()) {
            startActivityForResult(
                    Games.Achievements.getAchievementsIntent(apiClient),
                    RC_GAMES);
        }
    }

    private void showLeaderboards() {
        GoogleApiClient apiClient = mGamesFragment.getGamesApiClient();
        if (apiClient != null && apiClient.isConnected()) {
            startActivityForResult(
                    Games.Leaderboards.getAllLeaderboardsIntent(apiClient),
                    RC_GAMES);
        }
    }

    /**
     * Shows/hides one of the FloatingActionButtons. This sets both the visibility and the
     * appropriate anchor, which is required to keep the FAB hidden.
     */
    private void setFabVisibility(FloatingActionButton fab, boolean show) {
        CoordinatorLayout.LayoutParams p = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
        if (show) {
            p.setAnchorId(R.id.appbar);
            fab.setLayoutParams(p);
            fab.setVisibility(View.VISIBLE);
        } else {
            p.setAnchorId(View.NO_ID);
            fab.setLayoutParams(p);
            fab.setVisibility(View.GONE);
        }
    }

    @Override
    public void playSoundOnce(int resSoundId) {
        mAudioPlayer.playTrack(resSoundId, false);
    }

    @Override
    public Context getActivityContext() {
        return this;
    }

    @Override
    public void launchActivity(Intent intent) {
        launchActivityInternal(intent, 0);
    }

    @Override
    public void launchActivityDelayed(final Intent intent, View v) {
        launchActivityInternal(intent, 200);
    }

    @Override
    public View getCountdownView() {
        return findViewById(R.id.countdown_container);
    }

    @Override
    public void onCountdownFinished() {
        if (!mWaitingForApi) {
            stateData();
        } else {
            stateNoData();
        }
    }

    /** Attempt to launch the tracker, if available. */
    public void launchTracker() {
        AbstractLaunch launch = mCardAdapter.getLauncher(CardAdapter.SANTA);
        if (launch instanceof LaunchSanta) {
            LaunchSanta tracker = (LaunchSanta) launch;

            AnalyticsManager.sendEvent(R.string.analytics_event_category_launch,
                    R.string.analytics_launch_action_village);

            // App Measurement
            MeasurementManager.recordCustomEvent(mMeasurement,
                    getString(R.string.analytics_event_category_launch),
                    getString(R.string.analytics_launch_action_village));

            tracker.onClick(tracker.getClickTarget());
        }
    }

    /*
     * Service communication
     */

    class IncomingHandler extends Handler {

        /*
        Order in which messages are received: Data updates, State of Service [Idle, Error]
         */
        @Override
        public void handleMessage(Message msg) {
            SantaLog.d(TAG, "message=" + msg.what);
            switch (msg.what) {
                case SantaServiceMessages.MSG_SERVICE_STATUS:
                    // Current state of service, received once when connecting
                    onSantaServiceStateUpdate(msg.arg1);
                    break;
                case SantaServiceMessages.MSG_INPROGRESS_UPDATE_ROUTE:
                    // route is about to be updated
                    onRouteUpdateStart();
                    break;
                case SantaServiceMessages.MSG_UPDATED_ROUTE:
                    // route data has been updated
                    onRouteDataUpdateFinished();
                    break;
                case SantaServiceMessages.MSG_UPDATED_ONOFF:
                    mFlagSwitchOff = (msg.arg1 == SantaServiceMessages.SWITCH_OFF);
                    onDataUpdate();
                    break;
                case SantaServiceMessages.MSG_UPDATED_TIMES:
                    Bundle b = (Bundle) msg.obj;
                    mOffset = b.getLong(SantaServiceMessages.BUNDLE_OFFSET);
                    SantaPreferences.cacheOffset(mOffset);
                    mFinalArrival = b.getLong(SantaServiceMessages.BUNDLE_FINAL_ARRIVAL);
                    mFirstDeparture = b.getLong(SantaServiceMessages.BUNDLE_FIRST_DEPARTURE);
                    onDataUpdate();
                    break;
                case SantaServiceMessages.MSG_UPDATED_CASTDISABLED:
                    mFlagDisableCast = (msg.arg1 == SantaServiceMessages.DISABLED);
                    onDataUpdate();
                    break;
                case SantaServiceMessages.MSG_UPDATED_GAMES:
                    final int arg = msg.arg1;
                    mFlagDisableGumball = (arg & SantaServiceMessages.MSG_FLAG_GAME_GUMBALL)
                            == SantaServiceMessages.MSG_FLAG_GAME_GUMBALL;
                    mFlagDisableJetpack = (arg & SantaServiceMessages.MSG_FLAG_GAME_JETPACK)
                            == SantaServiceMessages.MSG_FLAG_GAME_JETPACK;
                    mFlagDisableMemory = (arg & SantaServiceMessages.MSG_FLAG_GAME_MEMORY)
                            == SantaServiceMessages.MSG_FLAG_GAME_MEMORY;
                    mFlagDisableRocket = (arg & SantaServiceMessages.MSG_FLAG_GAME_ROCKET)
                            == SantaServiceMessages.MSG_FLAG_GAME_ROCKET;
                    mFlagDisableDancer = (arg & SantaServiceMessages.MSG_FLAG_GAME_DANCER)
                            == SantaServiceMessages.MSG_FLAG_GAME_DANCER;
                    mFlagDisableSnowdown = (arg & SantaServiceMessages.MSG_FLAG_GAME_SNOWDOWN)
                            == SantaServiceMessages.MSG_FLAG_GAME_SNOWDOWN;
                    onDataUpdate();
                    break;
                case SantaServiceMessages.MSG_UPDATED_VIDEOS:
                    Bundle data = msg.getData();
                    mVideoList = data.getStringArray(SantaServiceMessages.BUNDLE_VIDEOS);
                    onDataUpdate();
                    break;
                case SantaServiceMessages.MSG_ERROR:
                    // Error accessing the API, ignore because there is data.
                    onApiSuccess();
                    break;
                case SantaServiceMessages.MSG_ERROR_NODATA:
                    stateNoData();
                    break;
                case SantaServiceMessages.MSG_SUCCESS:
                    onApiSuccess();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }

        }
    }

    /**
     * Handle the state of the SantaService when first connecting to it.
     */
    private void onSantaServiceStateUpdate(int state) {
        switch (state) {
            case SantaServiceMessages.STATUS_IDLE:
                // Service is idle, data should be uptodate
                mWaitingForApi = false;
                stateData();
                break;
            case SantaServiceMessages.STATUS_IDLE_NODATA:
                mWaitingForApi = true;
                stateNoData();
                break;
            case SantaServiceMessages.STATUS_ERROR_NODATA:
                // Service is in error state and there is no valid data
                mWaitingForApi = true;
                stateNoData();
            case SantaServiceMessages.STATUS_ERROR:
                // Service is in error state and waiting for another attempt to access API
                mWaitingForApi = true;
                stateNoData();
            case SantaServiceMessages.STATUS_PROCESSING:
                // Service is busy processing an update, wait for success and ignore this state
                mWaitingForApi = true;
                break;

        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);

            //reply with local Messenger to establish bi-directional communication
            Message msg = Message.obtain(null, SantaServiceMessages.MSG_SERVICE_REGISTER_CLIENT);
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                // Could not connect to Service, connection will be terminated soon.
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mIsBound = false;
        }
    };


    private void onApiSuccess() {

        if (mWaitingForApi) {
            mWaitingForApi = false;
            stateData();
        }
    }

    private void onRouteDataUpdateFinished() {
        // switch to 'online' mode, data has been loaded
        if (!mWaitingForApi) {
            stateData();
        }
    }

    private void onRouteUpdateStart() {
        // temporarily switch back to offline mode until route update has finished
        if (!mWaitingForApi) {
            stateNoData();
        }
    }

    private void onDataUpdate() {
        if (!mWaitingForApi) {
            stateData();
        }
    }


    private void registerWithService() {
        bindService(new Intent(this, SantaService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void unregisterFromService() {
        if (mIsBound) {
            if (mService != null) {
                Message msg = Message
                        .obtain(null, SantaServiceMessages.MSG_SERVICE_UNREGISTER_CLIENT);
                msg.replyTo = mMessenger;
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    // ignore if service is not available
                }
            }
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private synchronized void launchActivityInternal(
            final Intent intent, long delayMs) {

        if (!mLaunchingChild) {
            mLaunchingChild = true;

            // stop timer
            if (mCountdown != null) {
                mCountdown.cancel();
            }

            SantaNotificationBuilder.DismissNotifications(getApplicationContext());

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivityForResult(intent, RC_STARTUP);
                    mLaunchingChild = false;
                }
            }, delayMs);
        }
    }
}
