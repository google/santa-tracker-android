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
import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.FocusHighlight;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.google.android.apps.santatracker.AudioPlayer;
import com.google.android.apps.santatracker.BuildConfig;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.data.SantaPreferences;
import com.google.android.apps.santatracker.service.SantaService;
import com.google.android.apps.santatracker.service.SantaServiceMessages;
import com.google.android.apps.santatracker.util.AccessibilityUtil;
import com.google.android.apps.santatracker.util.AnalyticsManager;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.apps.santatracker.util.SantaLog;
import com.google.android.apps.santatracker.village.Village;
import com.google.android.apps.santatracker.village.VillageView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.lang.ref.WeakReference;

/**
 * Launch activity for the app. Handles loading of the village, the state of the markers (based on
 * the date/time) and incoming voice intents.
 */
public class TvStartupActivity extends FragmentActivity implements
        View.OnClickListener, Village.VillageListener,
        SantaContext, LaunchCountdown.LaunchCountdownContext {

    protected static final String TAG = "SantaStart";
    private static final String VILLAGE_TAG = "VillageFragment";

    private AudioPlayer mAudioPlayer;

    private boolean mResumed = false;

    private boolean mIsDebug = false;

    private Village mVillage;
    private VillageView mVillageView;
    private ImageView mVillageBackdrop;
    private View mLaunchButton;
    private View mCountdownView;

    private VerticalGridView mMarkers;
    private TvCardAdapter mCardAdapter;

    private LaunchCountdown mCountdown;

    // Load these values from resources when an instance of this activity is initialised.
    private static long OFFLINE_SANTA_DEPARTURE;
    private static long OFFLINE_SANTA_FINALARRIVAL;
    private static long UNLOCK_JETPACK;
    private static long UNLOCK_ROCKET;
    private static long UNLOCK_SNOWDOWN;
    private static long UNLOCK_VIDEO_1;
    private static long UNLOCK_VIDEO_15;
    private static long UNLOCK_VIDEO_23;

    // Server controlled flags
    private long mOffset = 0;
    private boolean mFlagSwitchOff = false;
    private boolean mFlagDisableJetpack = false;
    private boolean mFlagDisableRocket = false;
    private boolean mFlagDisableSnowdown = false;

    private String[] mVideoList = new String[]{null, null, null};


    private boolean mHaveGooglePlayServices = false;
    private long mFirstDeparture;
    private long mFinalArrival;

    // Handler for scheduled UI updates
    private Handler mHandler = new Handler();

    // Waiting for data from the API (no data or data is outdated)
    private boolean mWaitingForApi = true;

    // Service integration
    private Messenger mService = null;

    private boolean mIsBound = false;
    private Messenger mMessenger;

    // request code for games Activities
    private static final int RC_STARTUP = 1111;

    // Permission request codes
    private static final int RC_DEBUG_PERMS = 1;

    private FirebaseAnalytics mMeasurement;
    private boolean mLaunchingChild = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionsIfDebugModeEnabled();

        // Glide's pretty aggressive at caching images, so get the 8888 preference in early.
        if (!Glide.isSetup()) {
            Glide.setup(new GlideBuilder(getActivityContext())
                    .setDecodeFormat(DecodeFormat.PREFER_ARGB_8888));
        }

        setContentView(R.layout.layout_startup_tv);
        loadResourceFields(getResources());

        mIsDebug = BuildConfig.DEBUG;

        mMessenger= new Messenger(new IncomingHandler(this));

        mCountdown = new LaunchCountdown(this);
        mCountdownView = findViewById(R.id.countdown_container);
        mAudioPlayer = new AudioPlayer(getApplicationContext());

        mVillageView = (VillageView) findViewById(R.id.villageView);
        mVillage = (Village) getSupportFragmentManager().findFragmentByTag(VILLAGE_TAG);
        if (mVillage == null) {
            mVillage = new Village();
            getSupportFragmentManager().beginTransaction().add(mVillage, VILLAGE_TAG).commit();
        }
        mVillageBackdrop = (ImageView) findViewById(R.id.villageBackground);
        mLaunchButton = findViewById(R.id.launch_button);
        mLaunchButton.setOnClickListener(this);

        mMarkers = (VerticalGridView) findViewById(R.id.santa_markers);
        initialiseViews();

        mHaveGooglePlayServices = checkGooglePlayServicesAvailable();

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
    }

    private void loadResourceFields(Resources res) {
        final long ms = 1000L;
        OFFLINE_SANTA_DEPARTURE = res.getInteger(R.integer.santa_takeoff) * ms;
        OFFLINE_SANTA_FINALARRIVAL = res.getInteger(R.integer.santa_arrival) * ms;
        mFinalArrival = OFFLINE_SANTA_FINALARRIVAL;
        mFirstDeparture = OFFLINE_SANTA_DEPARTURE;

        // Game unlock
        UNLOCK_JETPACK = res.getInteger(R.integer.unlock_jetpack) * ms;
        UNLOCK_ROCKET = res.getInteger(R.integer.unlock_rocket) * ms;
        UNLOCK_SNOWDOWN = res.getInteger(R.integer.unlock_snowdown) * ms;

        // Video unlock
        UNLOCK_VIDEO_1 = res.getInteger(R.integer.unlock_video1) * ms;
        UNLOCK_VIDEO_15 = res.getInteger(R.integer.unlock_video15) * ms;
        UNLOCK_VIDEO_23 = res.getInteger(R.integer.unlock_video23) * ms;
    }

    void initialiseViews() {
        mVillageView.setVillage(mVillage);

        // Initialize ListRowPresenter
        ListRowPresenter listRowPresenter = new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL);
        listRowPresenter.setShadowEnabled(true);
        final int rowHeight
                = getResources().getDimensionPixelOffset(R.dimen.tv_marker_height_and_shadow);
        listRowPresenter.setRowHeight(rowHeight);

        // Initialize ListRow
        TvCardPresenter presenter = new TvCardPresenter(this);
        mCardAdapter = new TvCardAdapter(this, presenter);
        ListRow listRow = new ListRow(mCardAdapter);

        // Initialize ObjectAdapter for ListRow
        ArrayObjectAdapter arrayObjectAdapter = new ArrayObjectAdapter(listRowPresenter);
        arrayObjectAdapter.add(listRow);

        // Initialized Debug menus only for debug build.
        if (mIsDebug) {
            addDebugMenuListRaw(arrayObjectAdapter);
        }

        // set ItemBridgeAdapter to RecyclerView
        ItemBridgeAdapter adapter = new ItemBridgeAdapter(arrayObjectAdapter);
        mMarkers.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_HIGH_EDGE);
        mMarkers.setAdapter(adapter);
    }

    private void addDebugMenuListRaw(ArrayObjectAdapter objectAdapter) {

        mMarkers.setPadding(
                mMarkers.getPaddingLeft(), mMarkers.getPaddingTop() + 150,
                mMarkers.getPaddingRight(), mMarkers.getPaddingBottom());

        Presenter debugMenuPresenter = new Presenter() {
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent) {
                TextView tv = new TextView(parent.getContext());
                ViewGroup.MarginLayoutParams
                        params = new ViewGroup.MarginLayoutParams(200, 150);
                tv.setLayoutParams(params);
                tv.setGravity(Gravity.CENTER);
                tv.setBackgroundColor(getResources().getColor(R.color.SantaBlueDark));
                tv.setFocusableInTouchMode(false);
                tv.setFocusable(true);
                tv.setClickable(true);
                return new ViewHolder(tv);
            }

            @Override
            public void onBindViewHolder(ViewHolder viewHolder, Object item) {
                ((TextView)viewHolder.view).setText((String)item);
                viewHolder.view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String text = ((TextView)v).getText().toString();
                        if (text.contains("Enable Tracker")) {
                            enableTrackerMode(true);
                        } else if (text.contains("Enable CountDown")){
                            startCountdown(SantaPreferences.getCurrentTime());
                        } else {
                            mIsDebug = false;
                            initialiseViews();
                            resetLauncherStates();
                        }
                    }
                });
            }

            @Override
            public void onUnbindViewHolder(ViewHolder viewHolder) {

            }
        };

        ObjectAdapter debugMenuAdapter = new ObjectAdapter(debugMenuPresenter) {

            private final String[] mMenuString
                    = {"Enable Tracker", "Enable CountDown", "Hide DebugMenu"};
            @Override
            public int size() {
                return mMenuString.length;
            }

            @Override
            public Object get(int position) {
                return mMenuString[position];
            }
        };

        ListRow debugMenuListRow = new ListRow(debugMenuAdapter);
        objectAdapter.add(debugMenuListRow);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        showColorMask(false);
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
        registerWithService();

        initialiseViews();
        resetLauncherStates();
    }

    private void resetLauncherStates() {
        // Start only if play services are available
        if (mHaveGooglePlayServices) {
            stateNoData();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterFromService();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mResumed && hasFocus && !AccessibilityUtil.isTouchAccessiblityEnabled(this)) {
            mAudioPlayer.playTrackExclusive(R.raw.village_music, true);
        }
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

        // Note that in the "no data" state, this may or may not include the TIME_OFFSET, depending
        // on whether we've had a successful API call and still have the data. We can't use
        // System.currentTimeMillis() as it *will* ignore TIME_OFFSET.
        final long time = SantaPreferences.getCurrentTime();

        AbstractLaunch launchSanta = mCardAdapter.getLauncher(TvCardAdapter.SANTA);

        if (time < OFFLINE_SANTA_DEPARTURE) {
            // Santa hasn't departed yet, show countdown
            launchSanta.setState(AbstractLaunch.STATE_LOCKED);
            startCountdown(OFFLINE_SANTA_DEPARTURE);
        } else if (time >= OFFLINE_SANTA_DEPARTURE && time < OFFLINE_SANTA_FINALARRIVAL) {
            // Santa should have already left, but no data yet, hide countdown and show message
            stopCountdown();
            enableTrackerMode(false);
            launchSanta.setState(AbstractLaunch.STATE_DISABLED);
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

        long time = SantaPreferences.getCurrentTime();

        AbstractLaunch launchSanta = mCardAdapter.getLauncher(TvCardAdapter.SANTA);
        // Is Santa finished?
        if (time > mFirstDeparture && time < OFFLINE_SANTA_FINALARRIVAL) {
            // Santa should be travelling, enable map and hide countdown
            enableTrackerMode(true);

            if (mFlagSwitchOff) {
                // Kill-switch triggered, disable button
                launchSanta.setState(AbstractLaunch.STATE_DISABLED);
            } else if (time > mFinalArrival) {
                // No data
                launchSanta.setState(AbstractLaunch.STATE_DISABLED);
            } else {
                launchSanta.setState(AbstractLaunch.STATE_READY);
            }

        } else if (time < mFirstDeparture) {
            // Santa hasn't taken off yet, start count-down and schedule
            // notification to first departure, hide buttons

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
        mCountdownView.setVisibility(View.GONE);
    }

    public void startCountdown(long time) {
        mCountdown.startTimer(time - SantaPreferences.getCurrentTime());
        mVillageBackdrop.setImageResource(R.drawable.village_bg_countdown);
        mVillage.setPlaneEnabled(true);
        mLaunchButton.setVisibility(View.GONE);
        mCountdownView.setVisibility(View.VISIBLE);
    }

    public void stopCountdown() {
        mCountdown.cancel();
        mCountdownView.setVisibility(View.GONE);
    }

    /*
     * Village Markers
     */
    private void updateNavigation() {
        mCardAdapter.getLauncher(TvCardAdapter.ROCKET).setState(
                getGamePinState(mFlagDisableRocket, UNLOCK_ROCKET));
        mCardAdapter.getLauncher(TvCardAdapter.SNOWDOWN).setState(
                getGamePinState(mFlagDisableSnowdown, UNLOCK_SNOWDOWN));

        ((LaunchVideo) mCardAdapter.getLauncher(TvCardAdapter.VIDEO01)).setVideo(
                mVideoList[0], UNLOCK_VIDEO_1);
        ((LaunchVideo) mCardAdapter.getLauncher(TvCardAdapter.VIDEO15)).setVideo(
                mVideoList[1], UNLOCK_VIDEO_15);
        ((LaunchVideo) mCardAdapter.getLauncher(TvCardAdapter.VIDEO23)).setVideo(
                mVideoList[2], UNLOCK_VIDEO_23);
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
                mFlagDisableJetpack ? Long.MAX_VALUE : UNLOCK_JETPACK - time,
                mFlagDisableRocket ? Long.MAX_VALUE : UNLOCK_ROCKET - time,
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

    /*
     * Google Play Services - from
     * http://code.google.com/p/google-api-java-client/source/browse/tasks-android-sample/src/main/
     *     java/com/google/api/services/samples/tasks/android/TasksSample.java?repo=samples
     */

    /**
     * Check that Google Play services APK is installed and up to date.
     */
    private boolean checkGooglePlayServicesAvailable() {
        if (getPackageName().contains("debug")) {
            return true;
        }

        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = availability.isGooglePlayServicesAvailable(this);
        if (availability.isUserResolvableError(connectionStatusCode)) {
            Dialog dialog = availability.getErrorDialog(this, connectionStatusCode, 123);
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            });
            dialog.show();

            return false;
        }

        return (connectionStatusCode == ConnectionResult.SUCCESS);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.launch_button:
                launchTracker();
                break;
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
        launchActivityInternal(intent, null, 0);
    }

    @Override
    public void launchActivityDelayed(final Intent intent, final View v) {
        launchActivityInternal(intent, v, 200);
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
        AbstractLaunch launch = mCardAdapter.getLauncher(TvCardAdapter.SANTA);
        if (launch instanceof LaunchSanta) {
            LaunchSanta tracker = (LaunchSanta) launch;

            AnalyticsManager.sendEvent(R.string.analytics_event_category_launch,
                    R.string.analytics_launch_action_village);

            // App Measurement
            MeasurementManager.recordCustomEvent(mMeasurement,
                    getString(R.string.analytics_event_category_launch),
                    getString(R.string.analytics_launch_action_village));

            tracker.onClick(mLaunchButton);
        }
    }

    /*
     * Service communication
     */

    private static class IncomingHandler extends Handler {

        private final WeakReference<TvStartupActivity> mActivityRef;

        public IncomingHandler(TvStartupActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        /*
        Order in which messages are received: Data updates, State of Service [Idle, Error]
         */
        @Override
        public void handleMessage(Message msg) {
            SantaLog.d(TAG, "message=" + msg.what);
            final TvStartupActivity activity = mActivityRef.get();
            if (activity == null) {
                return;
            }

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
                case SantaServiceMessages.MSG_UPDATED_GAMES:
                    final int arg = msg.arg1;
                    activity.mFlagDisableJetpack = (arg & SantaServiceMessages.MSG_FLAG_GAME_JETPACK)
                            == SantaServiceMessages.MSG_FLAG_GAME_JETPACK;
                    activity.mFlagDisableRocket = (arg & SantaServiceMessages.MSG_FLAG_GAME_ROCKET)
                            == SantaServiceMessages.MSG_FLAG_GAME_ROCKET;
                    activity.mFlagDisableSnowdown = (arg & SantaServiceMessages.MSG_FLAG_GAME_SNOWDOWN)
                            == SantaServiceMessages.MSG_FLAG_GAME_SNOWDOWN;
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
            final Intent intent, View srcView, long delayMs) {

        if (!mLaunchingChild) {
            mLaunchingChild = true;

            // stop timer
            if (mCountdown != null) {
                mCountdown.cancel();
            }

            if (srcView != null) {
                playCircularRevealTransition(srcView);
            }

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivityForResult(intent, RC_STARTUP);
                    mLaunchingChild = false;
                }
            }, delayMs);
        }
    }

    final Rect mSrcRect = new Rect();
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void playCircularRevealTransition(View srcView) {

        showColorMask(true);
        View mask = findViewById(R.id.content_mask);
        srcView.getGlobalVisibleRect(mSrcRect);
        Animator anim = ViewAnimationUtils.createCircularReveal(mask,
                mSrcRect.centerX(), mSrcRect.centerY(), 0.f, mask.getWidth());
        anim.start();
    }

    private void showColorMask(boolean show) {
        int visibility = show ? View.VISIBLE: View.INVISIBLE;
        (findViewById(R.id.content_mask)).setVisibility(visibility);
    }
}
