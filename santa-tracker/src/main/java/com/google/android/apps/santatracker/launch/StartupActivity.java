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

package com.google.android.apps.santatracker.launch;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.apps.santatracker.SantaNotificationBuilder;
import com.google.android.apps.santatracker.cast.CastUtil;
import com.google.android.apps.santatracker.cast.LoggingCastSessionListener;
import com.google.android.apps.santatracker.cast.LoggingCastStateListener;
import com.google.android.apps.santatracker.common.NotificationConstants;
import com.google.android.apps.santatracker.data.GameDisabledState;
import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.apps.santatracker.games.PlayGamesFragment;
import com.google.android.apps.santatracker.games.SignInListener;
import com.google.android.apps.santatracker.invites.AppInvitesFragment;
import com.google.android.apps.santatracker.service.SantaService;
import com.google.android.apps.santatracker.service.SantaServiceMessages;
import com.google.android.apps.santatracker.util.AccessibilityUtil;
import com.google.android.apps.santatracker.util.AnalyticsManager;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.apps.santatracker.util.PlayServicesUtil;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.apps.santatracker.village.SnowFlakeView;
import com.google.android.apps.santatracker.village.Village;
import com.google.android.apps.santatracker.village.VillageView;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.lang.ref.WeakReference;
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

    public static final String EXTRA_DISABLE_ANIMATIONS = "disable_animations";

    private static final String VILLAGE_TAG = "VillageFragment";
    private static final String INTENT_HANDLED = "intent_handled";
    private static final String KEY_RECYCLER_POSITION = "key_recycler_position";
    private static final String KEY_CATEGORY_SELECTED = "key_category_selected";

    public static final int NUM_CATEGORIES = 3;
    public static final int SANTA_CATEGORY = 0;
    public static final int GAMES_CATEGORY = 1;
    public static final int MOVIES_CATEGORY = 2;

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

    private ImageView mSantaIcon;
    private ImageView mGamesIcon;
    private ImageView mMoviesIcon;

    // RecyclerView and associated bits
    private SantaCardAdapter mSantaCardAdapter;
    private GamesCardAdapter mGamesCardAdapter;
    private MoviesCardAdapter mMoviesCardAdapter;
    private CardAdapter[] mCardAdapters;

    private CardLayoutManager[] mCardLayoutManagers;
    private int[] mScrollPositions;
    private StickyScrollListener[] mScrollListeners;

    private int mCategory;

    private TextView mStatusText;
    private LaunchCountdown mCountdown;

    private ViewPager mCardsViewPager;
    private CardListPagerAdapter mCardListPagerAdapter;

    // Load these values from resources when an instance of this activity is initialised.
    private static long OFFLINE_SANTA_DEPARTURE;
    private static long OFFLINE_SANTA_FINALARRIVAL;
    private static long UNLOCK_GUMBALL;
    private static long UNLOCK_JETPACK;
    private static long UNLOCK_MEMORY;
    private static long UNLOCK_ROCKET;
    private static long UNLOCK_DANCER;
    private static long UNLOCK_SNOWDOWN;
    private static long UNLOCK_CITY_QUIZ;
    private static long UNLOCK_VIDEO_1;
    private static long UNLOCK_VIDEO_15;
    private static long UNLOCK_VIDEO_23;

    // Server controlled flags
    private long mOffset = 0;
    private boolean mFlagSwitchOff = false;
    private boolean mFlagDisableCast = false;

    // Game flags
    private GameDisabledState mGameDisabledState = new GameDisabledState();

    private String[] mVideoList = new String[]{null, null, null};

    // Check if we have Google Play Services
    private boolean mHaveGooglePlayServices = false;

    private long mFirstDeparture;
    private long mFinalArrival;

    // Handler for scheduled UI updates
    private Handler mHandler = new Handler();

    // Waiting for data from the API (no data or data is outdated)
    private boolean mWaitingForApi = true;

    // Launching a child activity (another launch request should be blocked)
    private boolean mLaunchingChild = false;

    // Service integration
    private Messenger mService = null;

    private boolean mIsBound = false;
    private Messenger mMessenger;

    // request code for games Activities
    private final int RC_STARTUP = 1111;
    private final int RC_GAMES = 9999;

    // Permission request codes
    private final int RC_DEBUG_PERMS = 1;

    private FirebaseAnalytics mMeasurement;

    // Cast
    private MenuItem mMediaRouteMenuItem;
    private SessionManagerListener mCastListener;
    private CastStateListener mCastStateListener;

    // Flag used to disable animations to make testing more reliable
    // Never to be used outside of testing
    private boolean mAnimationDisabled = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionsIfDebugModeEnabled();

        // Record Firebase properties
        MeasurementManager.recordDeviceProperties(this);

        // Glide's pretty aggressive at caching images, so get the 8888 preference in early.
        if (!Glide.isSetup()) {
            Glide.setup(new GlideBuilder(getActivityContext())
                    .setDecodeFormat(DecodeFormat.PREFER_ARGB_8888));
        }

        mMessenger = new Messenger(new IncomingHandler(this));

        setContentView(R.layout.layout_startup);

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
        mSantaCollapsing.setSnowFlakeView((SnowFlakeView)findViewById(R.id.snowFlakeView));
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

        mSantaIcon = (ImageView) findViewById(R.id.santa_icon);
        mGamesIcon = (ImageView) findViewById(R.id.arcade_icon);
        mMoviesIcon = (ImageView) findViewById(R.id.theatre_icon);

        mWavingSanta = findViewById(R.id.santa_waving);
        mWavingSantaArm = (ImageView) findViewById(R.id.santa_arm);
        mWavingSanta.setOnClickListener(this);

        mCardLayoutManagers = new CardLayoutManager[NUM_CATEGORIES];
        mScrollListeners = new StickyScrollListener[NUM_CATEGORIES];
        mScrollPositions = new int[NUM_CATEGORIES];
        for(int i = 0; i < mScrollPositions.length; i++) {
            mScrollPositions[i] = RecyclerView.NO_POSITION;
        }
        mCardsViewPager = (ViewPager)findViewById(R.id.cards_view_pager);
        mCardListPagerAdapter = new CardListPagerAdapter(this);
        mCardsViewPager.setAdapter(mCardListPagerAdapter);
        mCardsViewPager.addOnPageChangeListener(mCardListPagerAdapter);
        initialiseViews();
        // Restore saved instance state
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }

        // Check if we have Google Play Services
        mHaveGooglePlayServices = PlayServicesUtil.hasPlayServices(getApplicationContext());

        findViewById(R.id.games_button).setOnClickListener(this);
        findViewById(R.id.movies_button).setOnClickListener(this);
        findViewById(R.id.santa_button).setOnClickListener(this);

        // initialize our connection to Google Play Games
        mGamesFragment = PlayGamesFragment.getInstance(this, this);
        ViewCompat.setElevation(findViewById(R.id.category_picker_bar), 4);
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

        // See if it was a voice action which triggered this activity and handle it
        onNewIntent(getIntent());

        mCastListener = new LoggingCastSessionListener(this,
                R.string.analytics_cast_session_launch);
        mCastStateListener = new LoggingCastStateListener(this,
                R.string.analytics_cast_statechange_launch);

        // FOR TESTING -- Check if we should disable animations
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_DISABLE_ANIMATIONS, false)) {
            disableAnimations();
        }

        // set the initial states
        resetLauncherStates();
    }

    @Override
    protected void onDestroy() {
        mMessenger = null;
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save RecyclerView scroll position
        if (mCardLayoutManagers[mCategory] != null) {
            mScrollPositions[mCategory] = mCardLayoutManagers[mCategory].findFirstCompletelyVisibleItemPosition();
        }
        outState.putInt(KEY_CATEGORY_SELECTED, mCategory);
        outState.putInt(KEY_RECYCLER_POSITION, mScrollPositions[mCategory]);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mCategory = savedInstanceState.getInt(KEY_CATEGORY_SELECTED);
        // Restore RecyclerView scroll position
        mScrollPositions[mCategory] = savedInstanceState.getInt(KEY_RECYCLER_POSITION,
                RecyclerView.NO_POSITION);
    }

    @VisibleForTesting
    protected void disableAnimations() {
        // Disable animations in general
        mAnimationDisabled = true;

        // Disable snow
        ((SnowFlakeView) findViewById(R.id.snowFlakeView)).disableAnimation();

        // Disable village view
        mVillageView.disableAnimation();
    }

    private void updateCategory() {
        switch(mCategory) {
            case GAMES_CATEGORY:
                setGamesCategory();
                break;
            case SANTA_CATEGORY:
                setSantaCategory();
                break;
            case MOVIES_CATEGORY:
                setMoviesCategory();
                break;
        }
    }

    private void setGamesCategory() {
        mCategory = GAMES_CATEGORY;
        mCardAdapters[GAMES_CATEGORY].refreshData();

        mGamesIcon.setImageResource(R.drawable.filter_games);
        mSantaIcon.setImageResource(R.drawable.filter_compass_inactive);
        mMoviesIcon.setImageResource(R.drawable.filter_videos_inactive);
    }

    private void setMoviesCategory() {
        mCategory = MOVIES_CATEGORY;
        mCardAdapters[MOVIES_CATEGORY].refreshData();

        mGamesIcon.setImageResource(R.drawable.filter_games_inactive);
        mSantaIcon.setImageResource(R.drawable.filter_compass_inactive);
        mMoviesIcon.setImageResource(R.drawable.filter_videos);
    }

    private void setSantaCategory() {
        mCategory = SANTA_CATEGORY;
        mCardAdapters[SANTA_CATEGORY].refreshData();

        mGamesIcon.setImageResource(R.drawable.filter_games_inactive);
        mSantaIcon.setImageResource(R.drawable.filter_compass);
        mMoviesIcon.setImageResource(R.drawable.filter_videos_inactive);;
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
        UNLOCK_CITY_QUIZ = res.getInteger(R.integer.unlock_city_quiz) * ms;

        // Video unlock
        UNLOCK_VIDEO_1 = res.getInteger(R.integer.unlock_video1) * ms;
        UNLOCK_VIDEO_15 = res.getInteger(R.integer.unlock_video15) * ms;
        UNLOCK_VIDEO_23 = res.getInteger(R.integer.unlock_video23) * ms;
    }

    void initialiseViews() {
        mVillageView.setVillage(mVillage);

        mCardAdapters = new CardAdapter[NUM_CATEGORIES];

        // Initialize the RecyclerView
        int numColumns = getResources().getInteger(R.integer.village_columns);
        mCardLayoutManagers[SANTA_CATEGORY] = new CardLayoutManager(this, numColumns);
        mSantaCardAdapter = new SantaCardAdapter(this);
        mScrollListeners[SANTA_CATEGORY] = new StickyScrollListener(mCardLayoutManagers[SANTA_CATEGORY], numColumns);

        mCardLayoutManagers[GAMES_CATEGORY] = new CardLayoutManager(this, numColumns);
        mGamesCardAdapter = new GamesCardAdapter(this);
        mScrollListeners[GAMES_CATEGORY] = new StickyScrollListener(mCardLayoutManagers[GAMES_CATEGORY], numColumns);

        mCardLayoutManagers[MOVIES_CATEGORY] = new CardLayoutManager(this, numColumns);
        mMoviesCardAdapter = new MoviesCardAdapter(this);
        mScrollListeners[MOVIES_CATEGORY] = new StickyScrollListener(mCardLayoutManagers[MOVIES_CATEGORY], numColumns);

        mCardAdapters[SANTA_CATEGORY] = mSantaCardAdapter;
        mCardAdapters[GAMES_CATEGORY] = mGamesCardAdapter;
        mCardAdapters[MOVIES_CATEGORY] = mMoviesCardAdapter;
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
        AbstractLaunch[] pins = mCardAdapters[GAMES_CATEGORY].getLaunchers();
        List<AbstractLaunch> games = new ArrayList<>(pins.length);
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
                AbstractLaunch[] pins = mCardAdapters[SANTA_CATEGORY].getLaunchers();
                // try sending the voice command to all launchers, the first one that handles it wins
                for (AbstractLaunch launch : pins) {
                    if (handled = launch.handleVoiceAction(intent)) {
                        // App Measurement
                        MeasurementManager.recordCustomEvent(mMeasurement,
                                getString(R.string.analytics_event_category_launch),
                                getString(R.string.analytics_launch_action_voice),
                                launch.mContentDescription);

                        // [ANALYTICS EVENT]: Launch Voice
                        AnalyticsManager.sendEvent(R.string.analytics_event_category_launch,
                                R.string.analytics_launch_action_voice,
                                launch.mContentDescription);
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

        if (mCardLayoutManagers[mCategory] != null) {
            mCardLayoutManagers[mCategory].scrollToPosition(mScrollPositions[mCategory]);
        }

        registerCastListeners();

        updateCategory();
        mResumed = true;

    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
        mAudioPlayer.stopAll();

        CastUtil.removeCastListener(this, mCastListener);
        CastUtil.removeCastStateListener(this, mCastStateListener);

        cancelUIUpdate();

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

        MenuItem menuItemLegal = menu.findItem(R.id.legal);
        menuItemLegal.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final Resources resources = getResources();
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(StartupActivity.this);
                dialogBuilder.setItems(resources.getStringArray(R.array.legal_privacy),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                String url;
                                switch (which) {
                                    case 1:
                                        // Privacy
                                        url = resources.getString(R.string.url_privacy);
                                        break;
                                    case 2:
                                        // Terms of Service
                                        url = resources.getString(R.string.url_tos);
                                        break;
                                    case 3:
                                        // Open source licenses
                                        LicenseDialogFragment.newInstance()
                                                .show(getSupportFragmentManager(), "dialog");
                                        return;
                                    case 4:
                                        // Open source licenses for Google Play Services
                                        GmsLicenseDialogFragment.newInstance()
                                                .show(getSupportFragmentManager(), "dialog");
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

        menu.findItem(R.id.sync_config)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (mService != null) {
                            Message msg = Message.obtain(null,
                                    SantaServiceMessages.MSG_SERVICE_FORCE_SYNC);
                            try {
                                mService.send(msg);
                            } catch (RemoteException e) {
                                Log.e(TAG, "sendMessage:FORCE_SYNC", e);
                            }
                        }

                        return true;
                    }
                });

        menu.findItem(R.id.notification_takeoff)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        SantaNotificationBuilder
                                .createSantaNotification(StartupActivity.this,
                                        R.string.notification_takeoff);
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean castDisabled = getCastDisabled();

        // Add cast button
        if (!castDisabled && mMediaRouteMenuItem == null) {
            mMediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(
                    getApplicationContext(), menu, R.id.media_route_menu_item);
        }

        // Toggle cast visibility
        if (mMediaRouteMenuItem != null) {
            mMediaRouteMenuItem.setVisible(!castDisabled);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        menu.findItem(R.id.sign_out).setVisible(mSignedIn);
        return super.onPrepareOptionsPanel(view, menu);
    }

    private void registerCastListeners() {
        CastUtil.registerCastListener(this, mCastListener);
        CastUtil.registerCastStateListener(this, mCastStateListener);
    }

    private boolean getCastDisabled() {
        // Cast should be disabled if we don't have the proper version of Google Play Services
        // (to avoid a crash) or if we choose to disable it from the server.
        return (!mHaveGooglePlayServices || mFlagDisableCast);
    }

    private void setCastDisabled(boolean disableCast) {
        if (!mHaveGooglePlayServices) {
            return;
        }

        if (disableCast) {
            // If cast was previously enabled and we are disabling it, try to stop casting
            CastUtil.stopCasting(this);
        } else {
            // If cast was disabled and is becoming enabled, register listeners
            registerCastListeners();
        }

        // Update state
        mFlagDisableCast = disableCast;

        // Update options menu
        supportInvalidateOptionsMenu();
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
        if (!CastUtil.isCasting(this)) {
            setCastDisabled(true);
        }


        AbstractLaunch launchSanta = mSantaCardAdapter.getLauncher(SantaCardAdapter.KEY_SANTA_CARD);
        int prevSantaState = launchSanta.getState();

        // Note that in the "no data" state, this may or may not include the TIME_OFFSET, depending
        // on whether we've had a successful API call and still have the data. We can't use
        // System.currentTimeMillis() as it *will* ignore TIME_OFFSET.
        final long time = SantaPreferences.getCurrentTime();
        boolean takenOff = (time >= OFFLINE_SANTA_DEPARTURE);
        boolean isFlying = takenOff && (time < OFFLINE_SANTA_FINALARRIVAL);

        if (!takenOff) {
            // Santa hasn't departed yet, show countdown
            launchSanta.setState(AbstractLaunch.STATE_LOCKED);
            startCountdown(OFFLINE_SANTA_DEPARTURE);
            final long notificationTime = SantaPreferences
                    .getAdjustedTime(OFFLINE_SANTA_DEPARTURE);
            SantaNotificationBuilder
                    .scheduleSantaNotification(getApplicationContext(), notificationTime,
                            NotificationConstants.NOTIFICATION_TAKEOFF);

        } else if (isFlying) {
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

        // Santa's state changed, reload the Santa card adapter
        if (prevSantaState != launchSanta.getState()) {
            Log.d(TAG, "data:santaLaunchState: " + prevSantaState + " --> " + launchSanta.getState());
            mSantaCardAdapter.changeState(this, isFlying);
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

        AbstractLaunch launchSanta = mSantaCardAdapter.getLauncher(SantaCardAdapter.KEY_SANTA_CARD);
        int prevSantaState = launchSanta.getState();

        final long time = SantaPreferences.getCurrentTime();
        boolean takenOff = (time >= OFFLINE_SANTA_DEPARTURE);
        boolean isFlying = takenOff && (time < OFFLINE_SANTA_FINALARRIVAL);

        if (isFlying) {
            // Santa should be travelling, enable map and hide countdown
            enableTrackerMode(true);

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
        } else if (!takenOff) {
            // Santa hasn't taken off yet, start count-down and schedule
            // notification to first departure, hide buttons
            final long notificationTime = SantaPreferences
                    .getAdjustedTime(mFirstDeparture);
            SantaNotificationBuilder.scheduleSantaNotification(getApplicationContext(),
                    notificationTime,
                    NotificationConstants.NOTIFICATION_TAKEOFF);

            startCountdown(mFirstDeparture);
            launchSanta.setState(AbstractLaunch.STATE_LOCKED);
        } else {
            // Post Christmas, hide countdown and buttons
            launchSanta.setState(AbstractLaunch.STATE_FINISHED);
            stopCountdown();
            enableTrackerMode(false);
        }


        // Santa's state changed, reload the Santa card adapter
        if (prevSantaState != launchSanta.getState()) {
            Log.d(TAG, "nodata:santaLaunchState: " + prevSantaState + " --> " + launchSanta.getState());
            mSantaCardAdapter.changeState(this, isFlying);
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

        // Change the color of the status bar on SDK 21+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(
                    ContextCompat.getColor(this, R.color.villageStatusBarDark));
        }
    }

    public void startCountdown(long time) {
        if (mAnimationDisabled) {
            return;
        }

        mCountdown.startTimer(time - SantaPreferences.getCurrentTime());
        mVillageBackdrop.setImageResource(R.drawable.village_bg_countdown);
        mVillage.setPlaneEnabled(true);
        mLaunchButton.setVisibility(View.GONE);
        mSantaCollapsing.setOverlayColor(R.color.villageToolbarLight);
        mCountdownView.setVisibility(View.VISIBLE);
        mWavingSanta.setVisibility(View.VISIBLE);
        mOrnament.setVisibility(View.VISIBLE);

        // Change the color of the status bar on SDK 21+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(
                    ContextCompat.getColor(this, R.color.villageStatusBarLight));
        }
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
        mCardAdapters[GAMES_CATEGORY].getLauncher(GamesCardAdapter.GUMBALL_CARD).setState(
                getGamePinState(mGameDisabledState.disableGumballGame, UNLOCK_GUMBALL));
        mCardAdapters[GAMES_CATEGORY].getLauncher(GamesCardAdapter.MEMORY_CARD).setState(
                getGamePinState(mGameDisabledState.disableMemoryGame, UNLOCK_MEMORY));
        mCardAdapters[GAMES_CATEGORY].getLauncher(GamesCardAdapter.ROCKET_CARD).setState(
                getGamePinState(mGameDisabledState.disableRocketGame, UNLOCK_ROCKET));
        mCardAdapters[GAMES_CATEGORY].getLauncher(GamesCardAdapter.DANCER_CARD).setState(
                getGamePinState(mGameDisabledState.disableDancerGame, UNLOCK_DANCER));
        mCardAdapters[GAMES_CATEGORY].getLauncher(GamesCardAdapter.SNOWDOWN_CARD).setState(
                getGamePinState(mGameDisabledState.disableSnowdownGame, UNLOCK_SNOWDOWN));
        mCardAdapters[GAMES_CATEGORY].getLauncher(GamesCardAdapter.CITY_QUIZ_CARD).setState(
                getGamePinState(mGameDisabledState.disableCityQuizGame, UNLOCK_CITY_QUIZ));

        // Minigames
        // TODO: Decide if we want time-based unlocks for doodles games
        mCardAdapters[GAMES_CATEGORY].getLauncher(GamesCardAdapter.MINIGAME_SWIMMING_CARD).setState(
                getGamePinState(mGameDisabledState.disableSwimmingGame, 0L));
        mCardAdapters[GAMES_CATEGORY].getLauncher(GamesCardAdapter.MINIGAME_RUNNING_CARD).setState(
                getGamePinState(mGameDisabledState.disableRunningGame, 0L));

        // Movies
        ((LaunchVideo) mCardAdapters[MOVIES_CATEGORY].getLauncher(MoviesCardAdapter.VIDEO01_CARD)).setVideo(
                mVideoList[0], UNLOCK_VIDEO_1);
        ((LaunchVideo) mCardAdapters[MOVIES_CATEGORY].getLauncher(MoviesCardAdapter.VIDEO15_CARD)).setVideo(
                mVideoList[1], UNLOCK_VIDEO_15);
        ((LaunchVideo) mCardAdapters[MOVIES_CATEGORY].getLauncher(MoviesCardAdapter.VIDEO23_CARD)).setVideo(
                mVideoList[2], UNLOCK_VIDEO_23);

        // Present Quest
        mSantaCardAdapter.getLauncher(SantaCardAdapter.KEY_PRESENT_QUEST_CARD)
                .setState(getGamePinState(mGameDisabledState.disablePresentQuest, 0L));

        // reinitialise action bar
        supportInvalidateOptionsMenu();
    }

    private int getGamePinState(boolean disabledFlag, long unlockTime) {
        if (disabledFlag) {
            return AbstractLaunch.STATE_HIDDEN;
        } else if (SantaPreferences.getCurrentTime() < unlockTime) {
            return AbstractLaunch.STATE_LOCKED;
        } else {
            return AbstractLaunch.STATE_READY;
        }
    }

    /*
     * Status Message
     */

    private void showStatus(int i) {
        int state = mSantaCardAdapter.getLauncher(SantaCardAdapter.KEY_SANTA_CARD).getState();
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
        // TODO: If doodle games get unlock times, include them here
        final long[] delays = new long[]{
                mGameDisabledState.disableGumballGame ? Long.MAX_VALUE : UNLOCK_GUMBALL - time,
                mGameDisabledState.disableJetpackGame ? Long.MAX_VALUE : UNLOCK_JETPACK - time,
                mGameDisabledState.disableMemoryGame ? Long.MAX_VALUE : UNLOCK_MEMORY - time,
                mGameDisabledState.disableRocketGame ? Long.MAX_VALUE : UNLOCK_ROCKET - time,
                mGameDisabledState.disableDancerGame ? Long.MAX_VALUE : UNLOCK_DANCER - time,
                mGameDisabledState.disableSnowdownGame ? Long.MAX_VALUE : UNLOCK_SNOWDOWN - time,
                mGameDisabledState.disableCityQuizGame ? Long.MAX_VALUE : UNLOCK_CITY_QUIZ - time,
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
                MeasurementManager.recordVillageSantaClick(mMeasurement);
                animateSanta();
                break;
            case R.id.fab_achievement:
                showAchievements();
                break;
            case R.id.fab_leaderboard:
                showLeaderboards();
                break;
            case R.id.games_button:
                MeasurementManager.recordVillageTabClick(mMeasurement, "games");
                mCardsViewPager.setCurrentItem(GAMES_CATEGORY);
                break;
            case R.id.santa_button:
                MeasurementManager.recordVillageTabClick(mMeasurement, "santa");
                mCardsViewPager.setCurrentItem(SANTA_CATEGORY);
                break;
            case R.id.movies_button:
                MeasurementManager.recordVillageTabClick(mMeasurement, "movies");
                mCardsViewPager.setCurrentItem(MOVIES_CATEGORY);
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
        if (show) {
            fab.setVisibility(View.VISIBLE);
        } else {
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
        AbstractLaunch launch = mSantaCardAdapter.getLauncher(SantaCardAdapter.KEY_SANTA_CARD);
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

    static class IncomingHandler extends Handler {

        private WeakReference<StartupActivity> mActivityRef;

        IncomingHandler(StartupActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        /*
        Order in which messages are received: Data updates, State of Service [Idle, Error]
         */
        @Override
        public void handleMessage(Message msg) {
            StartupActivity activity = mActivityRef.get();
            if (activity == null) {
                return;
            }
            SantaLog.d(TAG, "message=" + msg.what);
            switch (msg.what) {
                case SantaServiceMessages.MSG_SERVICE_STATUS:
                    // Current state of service, received once when connecting
                    activity.onSantaServiceStateUpdate(msg.arg1);
                    break;
                case SantaServiceMessages.MSG_INPROGRESS_UPDATE_ROUTE:
                    // route is about to be updated
                    activity.onRouteUpdateStart();
                    break;
                case SantaServiceMessages.MSG_UPDATED_ROUTE:
                    // route data has been updated
                    activity.onRouteDataUpdateFinished();
                    break;
                case SantaServiceMessages.MSG_UPDATED_ONOFF:
                    activity.mFlagSwitchOff = (msg.arg1 == SantaServiceMessages.SWITCH_OFF);
                    activity.onDataUpdate();
                    break;
                case SantaServiceMessages.MSG_UPDATED_TIMES:
                    Bundle b = (Bundle) msg.obj;
                    activity.mOffset = b.getLong(SantaServiceMessages.BUNDLE_OFFSET);
                    SantaPreferences.cacheOffset(activity.mOffset);
                    activity.mFinalArrival = b.getLong(SantaServiceMessages.BUNDLE_FINAL_ARRIVAL);
                    activity.mFirstDeparture = b.getLong(SantaServiceMessages.BUNDLE_FIRST_DEPARTURE);
                    activity.onDataUpdate();
                    break;
                case SantaServiceMessages.MSG_UPDATED_CASTDISABLED:
                    activity.mFlagDisableCast = (msg.arg1 == SantaServiceMessages.DISABLED);
                    activity.onDataUpdate();
                    break;
                case SantaServiceMessages.MSG_UPDATED_GAMES:
                    final int arg = msg.arg1;
                    activity.mGameDisabledState = new GameDisabledState(arg);
                    activity.onDataUpdate();
                    break;
                case SantaServiceMessages.MSG_UPDATED_VIDEOS:
                    Bundle data = msg.getData();
                    activity.mVideoList = data.getStringArray(SantaServiceMessages.BUNDLE_VIDEOS);
                    activity.onDataUpdate();
                    break;
                case SantaServiceMessages.MSG_ERROR:
                    // Error accessing the API, ignore because there is data.
                    activity.onApiSuccess();
                    break;
                case SantaServiceMessages.MSG_ERROR_NODATA:
                    activity.stateNoData();
                    break;
                case SantaServiceMessages.MSG_SUCCESS:
                    activity.onApiSuccess();
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

            SantaNotificationBuilder.dismissNotifications(getApplicationContext());

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivityForResult(intent, RC_STARTUP);
                    mLaunchingChild = false;
                }
            }, delayMs);
        }
    }

    private class CardListPagerAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener {
        private Context mContext;
        private RecyclerView[] recyclerViews;

        CardListPagerAdapter(Context ctx) {
            mContext = ctx;
            recyclerViews = new RecyclerView[NUM_CATEGORIES];
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            if(recyclerViews[position] != null) {
                mCategory = position;
                updateCategory();
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }

        @Override
        public int getCount() {
            return NUM_CATEGORIES;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            if (recyclerViews[position] == null) {
                recyclerViews[position] = (RecyclerView) inflater.inflate(
                        R.layout.card_list, container, false);
                recyclerViews[position].setAdapter(mCardAdapters[position]);
                recyclerViews[position].setLayoutManager(mCardLayoutManagers[position]);
                recyclerViews[position].addOnScrollListener(mScrollListeners[position]);

                // Set tag key for testing
                recyclerViews[position].setTag(position);
            }
            container.removeView(recyclerViews[position]);
            container.addView(recyclerViews[position]);

            return recyclerViews[position];
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return super.getPageTitle(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

    }
}
