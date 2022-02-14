/*
 * Copyright 2019. Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.santatracker.launch;

import static com.google.android.apps.santatracker.launch.CardKeys.CITY_QUIZ_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.DANCER_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.GUMBALL_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.HEADER_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.JETPACK_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.MEMORY_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.PRESENT_QUEST_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.PRESENT_THROW_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.ROCKET_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.RUNNING_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.SANTA_SNAP_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.SWIMMING_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.VIDEO01_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.VIDEO15_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.VIDEO23_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_AIRPORT_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_BOATLOAD_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_CLAUSDRAWS_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_CODEBOOGIE_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_CODELAB_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_ELFMAKER_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_ELFSKI_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_GUMBALL_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_JAMBAND_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_PENGUINDASH_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_PRESENTBOUNCE_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_PRESENTDROP_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_RACER_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_RUNNER_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_SANTASEARCH_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_SANTASELFIE_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_SEASONOFGIVING_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_SNOWBALL_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_SNOWFLAKE_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_SPEEDSKETCH_CARD;
import static com.google.android.apps.santatracker.launch.CardKeys.WEB_WRAPBATTLE_CARD;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.apps.santatracker.AppIndexingUpdateServiceKt;
import com.google.android.apps.santatracker.AudioPlayer;
import com.google.android.apps.santatracker.Intents;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.SantaNotificationBuilder;
import com.google.android.apps.santatracker.common.NotificationConstants;
import com.google.android.apps.santatracker.config.Config;
import com.google.android.apps.santatracker.customviews.SnowFlakeView;
import com.google.android.apps.santatracker.customviews.Village;
import com.google.android.apps.santatracker.customviews.VillageView;
import com.google.android.apps.santatracker.data.FlyingState;
import com.google.android.apps.santatracker.data.GameState;
import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.apps.santatracker.data.SantaTravelState;
import com.google.android.apps.santatracker.data.TakeoffLandingTimes;
import com.google.android.apps.santatracker.data.VideoState;
import com.google.android.apps.santatracker.data.WebSceneState;
import com.google.android.apps.santatracker.games.PlayGamesFragment;
import com.google.android.apps.santatracker.games.SignInListener;
import com.google.android.apps.santatracker.invites.AppInvitesFragment;
import com.google.android.apps.santatracker.launch.adapters.CardAdapter;
import com.google.android.apps.santatracker.launch.adapters.GameAdapter;
import com.google.android.apps.santatracker.launch.adapters.TrackerAdapter;
import com.google.android.apps.santatracker.messaging.SantaMessagingService;
import com.google.android.apps.santatracker.tracker.cast.CastUtil;
import com.google.android.apps.santatracker.tracker.cast.LoggingCastSessionListener;
import com.google.android.apps.santatracker.tracker.cast.LoggingCastStateListener;
import com.google.android.apps.santatracker.tracker.time.Clock;
import com.google.android.apps.santatracker.util.AccessibilityUtil;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.apps.santatracker.util.PlayServicesUtil;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.apps.santatracker.viewmodel.VillageViewModel;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import me.mvdw.recyclerviewmergeadapter.adapter.RecyclerViewMergeAdapter;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.inject.Inject;

/**
 * Launch activity for the app. Handles loading of the village, the state of the markers (based on
 * the date/time) and incoming voice intents.
 */
public class StartupActivity extends AppCompatActivity
        implements View.OnClickListener,
                Village.VillageListener,
                VoiceAction.VoiceActionHandler,
                SignInListener,
                SantaContext,
                LaunchCountdown.LaunchCountdownContext,
                HasSupportFragmentInjector {

    public static final String EXTRA_DISABLE_ANIMATIONS = "disable_animations";
    protected static final String TAG = "SantaStart";
    private static final String VILLAGE_TAG = "VillageFragment";
    private static final String INTENT_HANDLED = "intent_handled";
    // request code for games Activities
    private final int RC_STARTUP = 1111;
    private final int RC_GAMES = 9999;
    @Inject Config mConfig;
    @Inject DispatchingAndroidInjector<Fragment> mAndroidInjector;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject Clock mClock;
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
    private View mLogoLayout;
    private View mCountdownView;
    private View mWavingSanta;
    private ImageView mWavingSantaArm;
    private Animation mWavingAnim;
    private View mOrnament;
    private LaunchCollection mGameLaunchCollection;
    private LaunchCollection mVideoLaunchCollection;
    private LaunchSantaTracker mLaunchSantaTracker;
    private FlexboxLayoutManager mFlexboxLayoutManager;

    private LaunchCountdown mCountdown;
    // Check if we have Google Play Services
    private boolean mHaveGooglePlayServices = false;
    // Handler for scheduled UI updates
    private Handler mHandler = new Handler();
    // Launching a child activity (another launch request should be blocked)
    private boolean mLaunchingChild = false;
    private FirebaseAnalytics mMeasurement;
    // Cast
    private MenuItem mMediaRouteMenuItem;
    private SessionManagerListener mCastListener;
    private CastStateListener mCastStateListener;
    // Flag used to disable animations to make testing more reliable
    // Never to be used outside of testing
    private boolean mAnimationDisabled = false;
    private VillageViewModel mVillageViewModel;
    private SantaPreferences mPreferences;
    // TODO: Remove this field once there is a good way to toggle the visibility of the cast button
    //       by observing the LaunchFlags LiveData
    private boolean isCastDisabled;
    private RecyclerView mRecyclerView;

    private BroadcastReceiver mSyncConfigReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mVillageViewModel.refresh();
                }
            };
    private BroadcastReceiver mSyncRouteReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mVillageViewModel.syncRoute();
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_startup);

        // Get view models
        mVillageViewModel =
                ViewModelProviders.of(this, mViewModelFactory).get(VillageViewModel.class);

        // Restore state
        mPreferences = new SantaPreferences(this);

        // Record Firebase properties
        MeasurementManager.recordDeviceProperties(this);

        mCountdown = new LaunchCountdown(this);
        mCountdownView = findViewById(R.id.countdown_container);
        mAudioPlayer = new AudioPlayer(getApplicationContext());

        Toolbar toolBar = findViewById(R.id.toolbar);
        if (toolBar != null) {
            setSupportActionBar(toolBar);
        }

        mRecyclerView = findViewById(R.id.card_list);

        // Set up collapsing
        mSantaCollapsing = findViewById(R.id.collapsing_toolbar);
        mSantaCollapsing.setSnowFlakeView((SnowFlakeView) findViewById(R.id.snowFlakeView));
        mSantaCollapsing.setToolbarContentView(findViewById(R.id.toolbar_content));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        mVillageView = findViewById(R.id.villageView);
        mVillage = (Village) getSupportFragmentManager().findFragmentByTag(VILLAGE_TAG);
        if (mVillage == null) {
            mVillage = new Village();
            getSupportFragmentManager().beginTransaction().add(mVillage, VILLAGE_TAG).commit();
        }
        mVillageBackdrop = findViewById(R.id.villageBackground);
        mLaunchButton = findViewById(R.id.launch_button);
        mLaunchButton.setOnClickListener(this);
        mOrnament = findViewById(R.id.countdown_ornament);
        mLogoLayout = findViewById(R.id.logo_layout);

        mWavingSanta = findViewById(R.id.santa_waving);
        mWavingSantaArm = findViewById(R.id.santa_arm);
        mWavingSanta.setOnClickListener(this);
        initialiseViews();
        // Restore saved instance state
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }

        // The first OnApplyWindowInsetsLisenter fixes the FrameLayout padding itself
        final FrameLayout backgroundViewGroup = findViewById(R.id.villageBackgroundLayout);
        ViewCompat.setOnApplyWindowInsetsListener(
                backgroundViewGroup,
                new OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(
                            View view, WindowInsetsCompat insets) {
                        return insets;
                    }
                });

        // Now we can listen for insets to handle things like cutouts
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                new OnApplyWindowInsetsListener() {
                    final ViewGroup.MarginLayoutParams mLogoMlp =
                            (ViewGroup.MarginLayoutParams) mLogoLayout.getLayoutParams();
                    final int mExtraMarginTop = mLogoMlp.topMargin;

                    final ViewGroup.MarginLayoutParams mOrnamentMlp =
                            (ViewGroup.MarginLayoutParams) mOrnament.getLayoutParams();

                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(
                            View view, WindowInsetsCompat insets) {
                        mLogoMlp.topMargin = insets.getSystemWindowInsetTop() + mExtraMarginTop;
                        mLogoLayout.requestLayout();

                        mOrnamentMlp.topMargin = insets.getSystemWindowInsetTop();
                        mOrnament.requestLayout();

                        return insets;
                    }
                });

        // Check if we have Google Play Services
        mHaveGooglePlayServices = PlayServicesUtil.hasPlayServices(getApplicationContext());

        // initialize our connection to Google Play Games
        mGamesFragment = PlayGamesFragment.getInstance(this, this);

        // App invites
        mInvitesFragment = AppInvitesFragment.getInstance(this);

        // set up click listeners for our buttons
        findViewById(R.id.fab_achievement).setOnClickListener(this);
        findViewById(R.id.fab_leaderboard).setOnClickListener(this);

        // Initialize measurement
        mMeasurement = FirebaseAnalytics.getInstance(this);
        MeasurementManager.recordScreenView(
                mMeasurement, getString(R.string.analytics_screen_village));

        // See if it was a voice action which triggered this activity and handle it
        onNewIntent(getIntent());

        mCastListener =
                new LoggingCastSessionListener(
                        this, R.string.analytics_cast_session_launch, mMeasurement);
        mCastStateListener =
                new LoggingCastStateListener(
                        this, R.string.analytics_cast_statechange_launch, mMeasurement);

        // FOR TESTING -- Check if we should disable animations
        if (getIntent() != null && getIntent().getBooleanExtra(EXTRA_DISABLE_ANIMATIONS, false)) {
            disableAnimations();
        }

        // Subscribe to FCM
        FirebaseMessaging.getInstance().subscribeToTopic(SantaMessagingService.TOPIC_SYNC);

        mVillageViewModel
                .getSantaTravelState()
                .observe(
                        this,
                        new Observer<SantaTravelState>() {
                            @Override
                            public void onChanged(@Nullable SantaTravelState santaTravelState) {
                                if (santaTravelState == null) {
                                    return;
                                }
                                onSantaTravelStateChanged(santaTravelState);
                            }
                        });
        mVillageViewModel
                .getTimeToTakeoff()
                .observe(
                        this,
                        new Observer<Long>() {
                            @Override
                            public void onChanged(@Nullable Long timeToTakeoff) {
                                if (timeToTakeoff == null) {
                                    return;
                                }
                                onTimeToTakeoffChanged(timeToTakeoff);
                            }
                        });
        mVillageViewModel
                .getLaunchFlags()
                .observe(
                        this,
                        new Observer<VillageViewModel.LaunchFlags>() {
                            @Override
                            public void onChanged(
                                    @Nullable VillageViewModel.LaunchFlags launchFlags) {
                                if (launchFlags == null) {
                                    return;
                                }
                                onLaunchFlagsChanged(launchFlags);
                            }
                        });
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(
                        mSyncConfigReceiver, new IntentFilter(Intents.SYNC_CONFIG_INTENT));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mSyncRouteReceiver, new IntentFilter(Intents.SYNC_ROUTE_INTENT));
        // set the initial states
        setInitialSantaLaunchState();
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    private void setInitialSantaLaunchState() {
        // TODO: Replace a launch collection with a launch collection for santa tracker
        mLaunchSantaTracker.setState(true, AbstractLaunch.STATE_LOCKED);
        showCountdownUI();
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

    /** Called continuously with the remaining time to take off. */
    private void onTimeToTakeoffChanged(Long timeToTakeoff) {
        // Update the countdown
        mCountdown.setTimeRemaining(timeToTakeoff);
    }

    /** Called when santa travel state ({@link FlyingState} or {@link TakeoffLandingTimes}) */
    private void onSantaTravelStateChanged(@NonNull SantaTravelState santaTravelState) {
        TakeoffLandingTimes takeoffLandingTimes = santaTravelState.getTakeoffLandingTimes();
        switch (santaTravelState.getFlyingState()) {
            case PRE_FLIGHT:
                // Santa hasn't taken off yet, start count-down and schedule
                // notification to first departure, hide buttons
                mLaunchSantaTracker.setState(true, AbstractLaunch.STATE_LOCKED);
                showCountdownUI();

                final long notificationTime =
                        mClock.adjustedTime(takeoffLandingTimes.getTakeoffTime());
                SantaNotificationBuilder.scheduleSantaNotification(
                        getApplicationContext(),
                        notificationTime,
                        NotificationConstants.NOTIFICATION_TAKEOFF);
                break;
            case FLYING:
                enableTrackerMode(true);
                mLaunchSantaTracker.setState(true, AbstractLaunch.STATE_READY);
                break;
            case POST_FLIGHT:
                // Post Christmas, hide countdown and buttons
                hideCountdownUI();
                enableTrackerMode(false);
                mLaunchSantaTracker.setState(true, AbstractLaunch.STATE_FINISHED);
                break;
            case DISABLED:
                enableTrackerMode(false);
                mLaunchSantaTracker.setState(true, AbstractLaunch.STATE_DISABLED);
                break;
        }
    }

    private void onLaunchFlagsChanged(VillageViewModel.LaunchFlags launchFlags) {
        // TODO: Remove the isCastDisabled field from Activity once there is a better way to
        // toggle the visibility of the cast button
        isCastDisabled = launchFlags.getFeatureState().getCastDisabled();

        // Enable/disable pins and nav drawer
        updateNavigation(launchFlags);

        if (!mHaveGooglePlayServices && !CastUtil.isCasting(this)) {
            setCastDisabled(true);
        } else {
            setCastDisabled(launchFlags.getFeatureState().getCastDisabled());
        }
        supportInvalidateOptionsMenu();
    }

    void initialiseViews() {
        mVillageView.setVillage(mVillage);

        RecyclerViewMergeAdapter mergeAdapter = new RecyclerViewMergeAdapter();
        GameAdapter gameAdapter = new GameAdapter(this, R.string.play);
        initializeGameLaunchers(gameAdapter);
        gameAdapter.setLaunchers(mGameLaunchCollection);

        CardAdapter videoAdapter = new CardAdapter(this, R.string.watch);
        initializeVideoLaunchers(videoAdapter);
        videoAdapter.setLaunchers(mVideoLaunchCollection);

        TrackerAdapter trackerAdapter = new TrackerAdapter();
        initializeSantaLauncher(trackerAdapter);
        trackerAdapter.setLauncher(mLaunchSantaTracker);

        mergeAdapter.addAdapter(gameAdapter);
        mergeAdapter.addAdapter(videoAdapter);
        mergeAdapter.addAdapter(trackerAdapter);

        mFlexboxLayoutManager = new FlexboxLayoutManager(this);
        mFlexboxLayoutManager.setFlexWrap(FlexWrap.WRAP);
        mFlexboxLayoutManager.setFlexDirection(FlexDirection.ROW);
        mFlexboxLayoutManager.setJustifyContent(JustifyContent.CENTER);
        mRecyclerView.setAdapter(mergeAdapter);
        mRecyclerView.setLayoutManager(mFlexboxLayoutManager);

        // TODO: Temporarily removing the scroll listener to prioritize the order of the cards first
    }

    private void initializeGameLaunchers(CardAdapter gameAdapter) {
        if (mGameLaunchCollection != null) {
            return;
        }
        mGameLaunchCollection = new LaunchCollection();
        // Adding a header as one of the launchers to keep the correct insert position of the rest
        // of the items.
        mGameLaunchCollection.add(HEADER_CARD, new LaunchHeader(this, gameAdapter));

        mGameLaunchCollection.add(PRESENT_QUEST_CARD, new LaunchPresentQuest(this, gameAdapter));
        mGameLaunchCollection.add(GUMBALL_CARD, new LaunchGumball(this, gameAdapter));
        mGameLaunchCollection.add(MEMORY_CARD, new LaunchMemory(this, gameAdapter));
        mGameLaunchCollection.add(ROCKET_CARD, new LaunchRocketSleigh(this, gameAdapter));
        mGameLaunchCollection.add(DANCER_CARD, new LaunchDasherDancer(this, gameAdapter));
        mGameLaunchCollection.add(SWIMMING_CARD, new LaunchPenguinSwim(this, gameAdapter));
        mGameLaunchCollection.add(RUNNING_CARD, new LaunchSnowballRun(this, gameAdapter));
        mGameLaunchCollection.add(CITY_QUIZ_CARD, new LaunchCityQuiz(this, gameAdapter));
        mGameLaunchCollection.add(SANTA_SNAP_CARD, new LaunchSantaSnap(this, gameAdapter));
        mGameLaunchCollection.add(PRESENT_THROW_CARD, new LaunchPresentThrow(this, gameAdapter));
        mGameLaunchCollection.add(JETPACK_CARD, new LaunchElfJetpack(this, gameAdapter));

        mGameLaunchCollection.add(
                WEB_AIRPORT_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_airport,
                        R.string.web_airport,
                        R.drawable.android_game_cards_airport,
                        R.color.airport_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_BOATLOAD_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_boatload,
                        R.string.web_boatload,
                        R.drawable.android_game_cards_gift_slingshot,
                        R.color.gift_slingshot_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_CLAUSDRAWS_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_clausdraws,
                        R.string.web_clausdraws,
                        R.drawable.android_game_cards_claus_draws,
                        R.color.claus_draws_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_CODEBOOGIE_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_codeboogie,
                        R.string.web_codeboogie,
                        R.drawable.android_game_cards_code_boogie,
                        R.color.code_boogie_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_CODELAB_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_codelab,
                        R.string.web_codelab,
                        R.drawable.android_game_cards_code_lab,
                        R.color.code_lab_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_GUMBALL_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_gumball,
                        R.string.web_gumball,
                        R.drawable.android_game_cards_gumball_tilt,
                        R.color.gumball_tilt_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_JAMBAND_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_jamband,
                        R.string.web_jamband,
                        R.drawable.android_game_cards_elf_jamband,
                        R.color.elf_jamband_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_PENGUINDASH_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_penguindash,
                        R.string.web_penguindash,
                        R.drawable.android_game_cards_penguin_dash,
                        R.color.penguin_dash_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_PRESENTBOUNCE_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_presentbounce,
                        R.string.web_presentbounce,
                        R.drawable.android_game_cards_present_bounce,
                        R.color.present_bounce_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_PRESENTDROP_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_presentdrop,
                        R.string.web_presentdrop,
                        R.drawable.android_game_cards_present_drop,
                        R.color.present_drop_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_RACER_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_racer,
                        R.string.web_racer,
                        R.drawable.android_game_cards_rudolph_racer,
                        R.color.rudolph_racer_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_RUNNER_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_runner,
                        R.string.web_runner,
                        R.drawable.android_game_cards_reigndeer_runner,
                        R.color.reindeer_runner_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_SANTASEARCH_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_santasearch,
                        R.string.web_santasearch,
                        R.drawable.android_game_cards_santa_search,
                        R.color.santa_search_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_SANTASELFIE_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_santaselfie,
                        R.string.web_santaselfie,
                        R.drawable.android_game_cards_santa_selfie,
                        R.color.santa_selfie_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_SEASONOFGIVING_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_seasonofgiving,
                        R.string.web_seasonofgiving,
                        R.drawable.android_game_cards_season_of_giving,
                        R.color.season_of_giving_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_ELFSKI_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_elfski,
                        R.string.web_elfski,
                        R.drawable.android_game_cards_elf_snowboarding,
                        R.color.elf_snowboarding_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_SNOWBALL_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_snowball,
                        R.string.web_snowball,
                        R.drawable.android_game_cards_snowball_storm,
                        R.color.snowball_storm_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_SNOWFLAKE_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_snowflake,
                        R.string.web_snowflake,
                        R.drawable.android_game_cards_code_lab,
                        R.color.code_lab_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_SPEEDSKETCH_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_speedsketch,
                        R.string.web_speedsketch,
                        R.drawable.android_game_cards_speed_sketch,
                        R.color.speed_sketch_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_WRAPBATTLE_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_wrapbattle,
                        R.string.web_wrapbattle,
                        R.drawable.android_game_cards_wrap_battle,
                        R.color.wrap_battle_splash_screen_background));
        mGameLaunchCollection.add(
                WEB_ELFMAKER_CARD,
                new LaunchWebScene(
                        this,
                        gameAdapter,
                        R.string.web_elfmaker,
                        R.string.web_elfmaker,
                        R.drawable.android_game_cards_elf_maker,
                        R.color.elf_maker_splash_screen_background));
    }

    private void initializeVideoLaunchers(CardAdapter videoAdapter) {
        if (mVideoLaunchCollection != null) {
            return;
        }
        mVideoLaunchCollection = new LaunchCollection();
        // Adding a header as one of the launchers to keep the correct insert position of the rest
        // of the items.
        mVideoLaunchCollection.add(HEADER_CARD, new LaunchHeader(this, videoAdapter));

        mVideoLaunchCollection.add(
                VIDEO01_CARD,
                new LaunchVideo(
                        this,
                        videoAdapter,
                        R.string.video_santa_is_back,
                        R.drawable.android_video_cards_santas_back,
                        1,
                        mClock));
        mVideoLaunchCollection.add(
                VIDEO15_CARD,
                new LaunchVideo(
                        this,
                        videoAdapter,
                        R.string.video_office_prank,
                        R.drawable.android_video_cards_office_prank,
                        15,
                        mClock));
        mVideoLaunchCollection.add(
                VIDEO23_CARD,
                new LaunchVideo(
                        this,
                        videoAdapter,
                        R.string.video_carpool,
                        R.drawable.android_video_cards_carpool,
                        23,
                        mClock));
    }

    private void initializeSantaLauncher(TrackerAdapter trackerAdapter) {
        if (mLaunchSantaTracker != null) {
            return;
        }
        mLaunchSantaTracker = new LaunchSantaTracker(this, trackerAdapter);
    }

    /** See http://stackoverflow.com/questions/25884954/deep-linking-and-multiple-app-instances/ */
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleVoiceActions();

        Bundle extras = intent.getExtras();
        if (extras != null
                && extras.getString(NotificationConstants.KEY_NOTIFICATION_TYPE) != null) {

            // App Measurement
            MeasurementManager.recordCustomEvent(
                    mMeasurement,
                    getString(R.string.analytics_event_category_launch),
                    getString(R.string.analytics_launch_action_notification),
                    extras.getString(NotificationConstants.KEY_NOTIFICATION_TYPE));

            SantaLog.d(TAG, "launched from notification");
        }
    }

    @Override
    public boolean handleVoiceAction(Intent intent) {
        if (VoiceAction.ACTION_PLAY_RANDOM_GAME.equals(intent.getAction())) {
            SantaLog.d(
                    TAG, String.format("Voice command: [%s]", VoiceAction.ACTION_PLAY_RANDOM_GAME));
            return startRandomGame();
        } else {
            return false;
        }
    }

    /**
     * Pick a game at random from the available games in STATE_READY state
     *
     * @return true if a game was launched
     */
    private boolean startRandomGame() {
        // find out all the games that are ready to play
        List<AbstractLaunch> games = mGameLaunchCollection.getReadyGames();

        // now pick one of the games from games and launch it
        if (games.size() > 0) {
            Random r = new Random();
            int index = r.nextInt(games.size());
            AbstractLaunch game = games.get(index);
            SantaLog.d(TAG, String.format("Picked a game at random [%s]", game.getTitle()));
            // launch the game by simulating a click
            game.onClick(game.getClickTarget());

            // App Measurement
            MeasurementManager.recordCustomEvent(
                    mMeasurement,
                    getString(R.string.analytics_event_category_launch),
                    getString(R.string.analytics_launch_action_voice),
                    game.getTitle());
            return true;
        } else {
            return false;
        }
    }

    private void handleVoiceActions() {
        Intent intent = getIntent();
        if (VoiceAction.isVoiceAction(intent)) {
            if (isAlreadyHandled(intent)) {
                SantaLog.d(
                        TAG,
                        String.format(
                                "Ignoring an already handled intent [%s]", intent.getAction()));
                return; // already processed
            }
            // first check if *this* activity can handle the voice action
            boolean handled = handleVoiceAction(intent);

            // next check all the pins
            if (!handled) {
                LaunchCollection pins = mGameLaunchCollection;
                // try sending the voice command to all launchers, the first one that handles it
                // wins
                for (AbstractLaunch launch : pins) {
                    if (handled = launch.handleVoiceAction(intent)) {
                        // App Measurement
                        MeasurementManager.recordCustomEvent(
                                mMeasurement,
                                getString(R.string.analytics_event_category_launch),
                                getString(R.string.analytics_launch_action_voice),
                                launch.getTitle());
                        break;
                    }
                }
            }
            if (!handled) {
                Toast.makeText(
                                this,
                                getResources().getText(R.string.voice_command_unhandled),
                                Toast.LENGTH_SHORT)
                        .show();

                // App Measurement
                MeasurementManager.recordCustomEvent(
                        mMeasurement,
                        getString(R.string.analytics_event_category_launch),
                        getString(R.string.analytics_launch_action_voice),
                        getString(R.string.analytics_launch_voice_unhandled));
            } else {
                setAlreadyHandled(intent);
            }
        }
    }

    /**
     * This method is responsible for handling a corner case. Upon orientation change, the Activity
     * is re-created (onCreate is called) and the same intent is (re)delivered to the app.
     * Fortunately the Intent is Parcelable so we can mark it and check for this condition. Without
     * this, if the phone is in portrait mode, and the user issues voice command to start a game (or
     * other forcing orientation change), the following happens:
     *
     * <p>1. com.google.android.apps.santatracker.PLAY_GAME is delivered to the app. 2. Game is
     * started and phone switches to landscape. 3. User ends the game, rotates the phone back to
     * portrait. 4. onCreate is called again since StartupActivity is re-created. 5. The voice
     * action is re-executed (since getIntent returns
     * com.google.android.apps.santatracker.PLAY_GAME).
     *
     * <p>We don't want #5 to take place.
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

        // Restore scroll position
        if (mFlexboxLayoutManager != null) {
            mFlexboxLayoutManager.scrollToPosition(mVillageViewModel.getScrollPosition());
        }
        registerCastListeners();
        SantaTravelState travelState = mVillageViewModel.getSantaTravelState().getValue();
        if (travelState != null) {
            onSantaTravelStateChanged(travelState);
        }
        mResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
        if (mAudioPlayer != null) {
            mAudioPlayer.stopAll();
        }

        CastUtil.removeCastListener(this, mCastListener);
        CastUtil.removeCastStateListener(this, mCastStateListener);

        mVillageViewModel.setScrollPosition(mFlexboxLayoutManager.findFirstVisibleItemPosition());
        // Save scroll positions
        if (mFlexboxLayoutManager != null) {
            mVillageViewModel.setScrollPosition(
                    mFlexboxLayoutManager.findFirstVisibleItemPosition());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check for App Invites
        mInvitesFragment.getInvite(
                new AppInvitesFragment.GetInvitationCallback() {
                    @Override
                    public void onInvitation(String invitationId, String deepLink) {
                        SantaLog.d(TAG, "onInvitation: " + deepLink);
                    }
                },
                true);
        VillageViewModel.LaunchFlags launchFlags = mVillageViewModel.getLaunchFlags().getValue();
        if (launchFlags != null) {
            onLaunchFlagsChanged(launchFlags);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSyncConfigReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSyncRouteReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mGamesFragment.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            playBackgroundMusic();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.menu_startup, menu);

        if (mMediaRouteMenuItem != null) {
            // If the item has been set up previously, reinitialise it here for the new menu entry
            mMediaRouteMenuItem =
                    CastButtonFactory.setUpMediaRouteButton(
                            getApplicationContext(), menu, R.id.media_route_menu_item);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean castDisabled = getCastDisabled();

        // Add cast button
        if (!castDisabled && mMediaRouteMenuItem == null) {
            mMediaRouteMenuItem =
                    CastButtonFactory.setUpMediaRouteButton(
                            getApplicationContext(), menu, R.id.media_route_menu_item);
        }

        // Toggle cast visibility
        if (mMediaRouteMenuItem != null) {
            mMediaRouteMenuItem.setVisible(!castDisabled);
        }

        // Only show sign out button when signed in
        MenuItem signOutItem = menu.findItem(R.id.sign_out);
        if (signOutItem != null) {
            signOutItem.setVisible(mSignedIn);
        }

        MenuItem muteButton = menu.findItem(R.id.mute_button);
        if (muteButton != null) {
            if (mPreferences.isMuted()) {
                muteButton.setTitle(R.string.mute);
                muteButton.setIcon(R.drawable.ic_volume_off_black_24dp);
            } else {
                muteButton.setTitle(R.string.unmute);
                muteButton.setIcon(R.drawable.ic_volume_up_black_24dp);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.legal_privacy:
                launchWebUrl(getString(R.string.url_privacy));
                return true;
            case R.id.legal_tos:
                launchWebUrl(getString(R.string.url_tos));
                return true;
            case R.id.legal_os:
                OssLicensesMenuActivity.setActivityTitle(getString(R.string.open_source_licenses));
                startActivity(new Intent(this, OssLicensesMenuActivity.class));
                return true;
            case R.id.legal_notice:
                launchWebUrl(getString(R.string.url_legal));
                return true;
            case R.id.menu_app_invite:
                mInvitesFragment.sendGenericInvite();
                return true;
            case R.id.open_help:
                launchWebUrl(getString(R.string.url_help));
                return true;
            case R.id.github_santa:
                launchWebUrl(getString(R.string.url_github_santa));
                return true;
            case R.id.sign_out:
                Games.signOut(mGamesFragment.getGamesApiClient());
                updateSignInState(false);
                return true;
            case R.id.sync_config:
                mConfig.syncConfigAsync(
                        new Config.ParamChangedCallback() {
                            @Override
                            public void onChanged(List<String> changedKeys) {
                                mVillageViewModel.refresh();
                            }
                        });
                return true;
            case R.id.notification_takeoff:
                SantaNotificationBuilder.createSantaNotification(
                        this, R.string.notification_takeoff);
                return true;
            case R.id.set_date:
                showDatePicker();
                return true;
            case R.id.sync_stickers:
                AppIndexingUpdateServiceKt.enqueueRefreshAppIndex(this);
                return true;
            case R.id.mute_button:
                mPreferences.toggleMuted();
                onMuteChanged(mPreferences.isMuted());
                invalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onMuteChanged(boolean isMuted) {
        if (isMuted) {
            mAudioPlayer.stopAll();
        } else {
            playBackgroundMusic();
        }
    }

    private void registerCastListeners() {
        CastUtil.registerCastListener(this, mCastListener);
        CastUtil.registerCastStateListener(this, mCastStateListener);
    }

    private boolean getCastDisabled() {
        // Cast should be disabled if we don't have the proper version of Google Play Services
        // (to avoid a crash) or if we choose to disable it from the server.
        return (!mHaveGooglePlayServices || isCastDisabled);
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

        // Update options menu
        supportInvalidateOptionsMenu();
    }

    public void enableTrackerMode(boolean showLaunchButton) {
        mVillageBackdrop.setImageResource(R.drawable.village_bg_launch);
        mVillage.setPlaneEnabled(false);
        mLaunchButton.setVisibility(showLaunchButton ? View.VISIBLE : View.GONE);
        mSantaCollapsing.setContentScrimColor(
                ContextCompat.getColor(this, R.color.villageToolbarDark));
        mSantaCollapsing.setStatusBarScrimColor(
                ContextCompat.getColor(this, R.color.villageStatusBarDark));
        mCountdownView.setVisibility(View.GONE);
        mWavingSanta.setVisibility(View.GONE);
        mOrnament.setVisibility(View.GONE);
    }

    /** Start the countdown to santa's departure. */
    public void showCountdownUI() {
        if (mAnimationDisabled) {
            return;
        }

        mVillageBackdrop.setImageResource(R.drawable.village_bg_countdown);
        mVillage.setPlaneEnabled(true);
        mLaunchButton.setVisibility(View.GONE);
        mSantaCollapsing.setContentScrimColor(
                ContextCompat.getColor(this, R.color.villageToolbarLight));
        mSantaCollapsing.setStatusBarScrimColor(
                ContextCompat.getColor(this, R.color.villageStatusBarLight));
        mCountdownView.setVisibility(View.VISIBLE);
        mWavingSanta.setVisibility(View.VISIBLE);
        mOrnament.setVisibility(View.VISIBLE);
    }

    public void hideCountdownUI() {
        mCountdownView.setVisibility(View.GONE);
    }

    /*
     * Village Markers
     */
    private void updateNavigation(VillageViewModel.LaunchFlags launchFlags) {
        GameState gameState = launchFlags.getGameState();
        // Games
        mGameLaunchCollection
                .getLauncherFromCardKey(CardKeys.GUMBALL_CARD)
                .setState(
                        gameState.getFeatureGumballGame(),
                        getGamePinState(
                                gameState.getDisableGumballGame(),
                                mConfig.get(Config.UNLOCK_GUMBALL)));
        mGameLaunchCollection
                .getLauncherFromCardKey(CardKeys.MEMORY_CARD)
                .setState(
                        gameState.getFeatureMemoryGame(),
                        getGamePinState(
                                gameState.getDisableMemoryGame(),
                                mConfig.get(Config.UNLOCK_MEMORY)));
        mGameLaunchCollection
                .getLauncherFromCardKey(CardKeys.ROCKET_CARD)
                .setState(
                        gameState.getFeatureRocketGame(),
                        getGamePinState(
                                gameState.getDisableRocketGame(),
                                mConfig.get(Config.UNLOCK_ROCKET)));
        mGameLaunchCollection
                .getLauncherFromCardKey(CardKeys.DANCER_CARD)
                .setState(
                        gameState.getFeatureDancerGame(),
                        getGamePinState(
                                gameState.getDisableDancerGame(),
                                mConfig.get(Config.UNLOCK_DANCER)));
        mGameLaunchCollection
                .getLauncherFromCardKey(CardKeys.CITY_QUIZ_CARD)
                .setState(
                        gameState.getFeatureCityQuizGame(),
                        getGamePinState(
                                gameState.getDisableCityQuizGame(),
                                mConfig.get(Config.UNLOCK_CITYQUIZ)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Enable the web scenes only on Android M and above.
            Map<String, WebSceneState.WebScene> scenes = launchFlags.getWebSceneState().getScenes();

            for (int sceneCard : CardKeys.WEB_SCENE_CARDS) {
                // For each web scene card, get the configuration name and set the data
                final String config = GameAdapter.WEB_CONFIG_MAPPING.get(sceneCard);
                ((LaunchWebScene) mGameLaunchCollection.getLauncherFromCardKey(sceneCard))
                        .setData(
                                scenes.get(config).getFeatured(),
                                scenes.get(config).getDisabled(),
                                scenes.get(config).getLandscape(),
                                scenes.get(config).getUrl(),
                                scenes.get(config).getCardImageUrl());
            }
        }

        // Minigames
        mGameLaunchCollection
                .getLauncherFromCardKey(CardKeys.SWIMMING_CARD)
                .setState(
                        gameState.getFeatureSwimmingGame(),
                        getGamePinState(gameState.getDisableSwimmingGame(), 0L));
        mGameLaunchCollection
                .getLauncherFromCardKey(CardKeys.RUNNING_CARD)
                .setState(
                        gameState.getFeatureRunningGame(),
                        getGamePinState(gameState.getDisableRunningGame(), 0L));
        mGameLaunchCollection
                .getLauncherFromCardKey(CardKeys.JETPACK_CARD)
                .setState(
                        gameState.getFeatureJetpackGame(),
                        getGamePinState(
                                gameState.getDisableJetpackGame(),
                                mConfig.get(Config.UNLOCK_JETPACK)));
        mGameLaunchCollection
                .getLauncherFromCardKey(CardKeys.PRESENT_THROW_CARD)
                .setState(
                        gameState.getFeaturePresentThrow(),
                        getGamePinState(
                                gameState.getDisablePresentThrow(),
                                mConfig.get(Config.UNLOCK_PRESENT_THROW)));

        mGameLaunchCollection
                .getLauncherFromCardKey(CardKeys.SANTA_SNAP_CARD)
                .setState(
                        gameState.getFeatureSantaSnap(),
                        getGamePinState(gameState.getDisableSantaSnap(), 0L));

        VideoState videoState = launchFlags.getVideoState();
        // Movies
        ((LaunchVideo) mVideoLaunchCollection.getLauncherFromCardKey(CardKeys.VIDEO01_CARD))
                .setVideo(videoState.getVideo1(), mConfig.get(Config.UNLOCK_VIDEO_1));
        ((LaunchVideo) mVideoLaunchCollection.getLauncherFromCardKey(CardKeys.VIDEO15_CARD))
                .setVideo(videoState.getVideo15(), mConfig.get(Config.UNLOCK_VIDEO_15));
        ((LaunchVideo) mVideoLaunchCollection.getLauncherFromCardKey(CardKeys.VIDEO23_CARD))
                .setVideo(videoState.getVideo23(), mConfig.get(Config.UNLOCK_VIDEO_23));

        // Present Quest
        mGameLaunchCollection
                .getLauncherFromCardKey(CardKeys.PRESENT_QUEST_CARD)
                .setState(
                        gameState.getFeaturePresentQuest(),
                        getGamePinState(gameState.getDisablePresentQuest(), 0L));

        // reinitialise action bar
        supportInvalidateOptionsMenu();
    }

    private int getGamePinState(boolean disabledFlag, long unlockTime) {
        if (disabledFlag) {
            return AbstractLaunch.STATE_HIDDEN;
        } else if (mClock.nowMillis() < unlockTime) {
            return AbstractLaunch.STATE_LOCKED;
        } else {
            return AbstractLaunch.STATE_READY;
        }
    }

    private void updateSignInState(boolean signedIn) {
        mSignedIn = signedIn;
        setFabVisibility(findViewById(R.id.fab_leaderboard), signedIn);
        setFabVisibility(findViewById(R.id.fab_achievement), signedIn);
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
            startActivityForResult(Games.Achievements.getAchievementsIntent(apiClient), RC_GAMES);
        }
    }

    private void showLeaderboards() {
        GoogleApiClient apiClient = mGamesFragment.getGamesApiClient();
        if (apiClient != null && apiClient.isConnected()) {
            startActivityForResult(
                    Games.Leaderboards.getAllLeaderboardsIntent(apiClient), RC_GAMES);
        }
    }

    /**
     * Shows/hides one of the FloatingActionButtons. This sets both the visibility and the
     * appropriate anchor, which is required to keep the FAB hidden.
     */
    private void setFabVisibility(View fab, boolean show) {
        if (show) {
            fab.setVisibility(View.VISIBLE);
        } else {
            fab.setVisibility(View.GONE);
        }
    }

    @Override
    public void playSoundOnce(int resSoundId) {
        if (!mPreferences.isMuted()) {
            mAudioPlayer.playTrack(resSoundId, false);
        }
    }

    @Override
    public Context getActivityContext() {
        return this;
    }

    @Override
    public void launchActivity(Intent intent, @NonNull ActivityOptionsCompat options) {
        launchActivityInternal(options, intent, 0);
    }

    @Override
    public void launchActivityDelayed(
            final Intent intent, View v, @NonNull ActivityOptionsCompat options) {
        launchActivityInternal(options, intent, 200);
    }

    @Override
    public void launchActivity(Intent intent) {
        launchActivityInternal(null, intent, 0);
    }

    @Override
    public void launchActivityDelayed(Intent intent, View v) {
        launchActivityInternal(null, intent, 0);
    }

    @Override
    public View getCountdownView() {
        return findViewById(R.id.countdown_container);
    }

    /** Attempt to launch the tracker, if available. */
    public void launchTracker() {
        // App Measurement
        MeasurementManager.recordCustomEvent(
                mMeasurement,
                getString(R.string.analytics_event_category_launch),
                getString(R.string.analytics_launch_action_village));

        mLaunchSantaTracker.onClick(mLaunchSantaTracker.getClickTarget());
    }

    public AndroidInjector<Fragment> supportFragmentInjector() {
        return mAndroidInjector;
    }

    /** Debugging only: show a picker to allow choosing the date (manipulates the time offset). */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(
                                    DatePicker datePicker, int year, int month, int day) {
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(year, month, day);

                                long timeNow = (new Date()).getTime();
                                long timePicked = calendar.getTimeInMillis();

                                long offset = timePicked - timeNow;
                                mPreferences.setOffset(offset);

                                mVillageViewModel.refresh();
                            }
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DATE));

        dialog.show();
    }

    private void launchWebUrl(String url) {
        new CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(getResources().getColor(R.color.SantaRedDark))
                .build()
                .launchUrl(this, Uri.parse(url));
    }

    private void playBackgroundMusic() {
        if (mResumed
                && !mPreferences.isMuted()
                && !AccessibilityUtil.isTouchAccessiblityEnabled(this)) {
            mAudioPlayer.playTrackExclusive(R.raw.village_music, true);
        }
    }

    private synchronized void launchActivityInternal(
            final ActivityOptionsCompat options, final Intent intent, long delayMs) {

        if (!mLaunchingChild) {
            mLaunchingChild = true;
            SantaNotificationBuilder.dismissNotifications(getApplicationContext());
            final Bundle bundleOptions = options != null ? options.toBundle() : null;
            mHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            ActivityCompat.startActivityForResult(
                                    StartupActivity.this, intent, RC_STARTUP, bundleOptions);
                            mLaunchingChild = false;
                        }
                    },
                    delayMs);
        }
    }

    public LaunchCollection getGameLaunchCollection() {
        return mGameLaunchCollection;
    }

    public LaunchCollection getVideoLaunchCollection() {
        return mVideoLaunchCollection;
    }
}
