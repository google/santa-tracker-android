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

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.VerticalGridView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.apps.santatracker.AudioPlayer;
import com.google.android.apps.santatracker.BuildConfig;
import com.google.android.apps.santatracker.Intents;
import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.config.Config;
import com.google.android.apps.santatracker.customviews.Village;
import com.google.android.apps.santatracker.customviews.VillageView;
import com.google.android.apps.santatracker.data.GameState;
import com.google.android.apps.santatracker.data.SantaTravelState;
import com.google.android.apps.santatracker.data.VideoState;
import com.google.android.apps.santatracker.tracker.time.Clock;
import com.google.android.apps.santatracker.util.AccessibilityUtil;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.android.apps.santatracker.viewmodel.VillageViewModel;
import com.google.firebase.analytics.FirebaseAnalytics;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import javax.inject.Inject;

/**
 * Launch activity for the app. Handles loading of the village, the state of the markers (based on
 * the date/time) and incoming voice intents.
 */
public class TvStartupActivity extends FragmentActivity
        implements View.OnClickListener,
                Village.VillageListener,
                SantaContext,
                LaunchCountdown.LaunchCountdownContext,
                HasSupportFragmentInjector {

    private static final String VILLAGE_TAG = "VillageFragment";
    // request code for games Activities
    private static final int RC_STARTUP = 1111;
    final Rect mSrcRect = new Rect();
    @Inject DispatchingAndroidInjector<Fragment> mAndroidInjector;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject Config mConfig;
    @Inject Clock mClock;
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
    // Handler for scheduled UI updates
    private Handler mHandler = new Handler();
    private FirebaseAnalytics mMeasurement;
    private boolean mLaunchingChild = false;
    private VillageViewModel mVillageViewModel;
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

    public TvStartupActivity() {}

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVillageViewModel =
                ViewModelProviders.of(this, mViewModelFactory).get(VillageViewModel.class);

        setContentView(R.layout.layout_startup_tv);

        mIsDebug = BuildConfig.DEBUG;

        mCountdown = new LaunchCountdown(this);
        mCountdownView = findViewById(R.id.countdown_container);
        mAudioPlayer = new AudioPlayer(getApplicationContext());

        mVillageView = findViewById(R.id.villageView);
        mVillage = (Village) getSupportFragmentManager().findFragmentByTag(VILLAGE_TAG);
        if (mVillage == null) {
            mVillage = new Village();
            getSupportFragmentManager().beginTransaction().add(mVillage, VILLAGE_TAG).commit();
        }
        mVillageBackdrop = findViewById(R.id.villageBackground);
        mLaunchButton = findViewById(R.id.launch_button);
        mLaunchButton.setOnClickListener(this);

        mMarkers = findViewById(R.id.santa_markers);
        initialiseViews();

        // Initialize measurement
        mMeasurement = FirebaseAnalytics.getInstance(this);
        MeasurementManager.recordScreenView(
                mMeasurement, getString(R.string.analytics_screen_village));

        // See if it was a voice action which triggered this activity and handle it
        onNewIntent(getIntent());

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
                                onTimeUpdate(timeToTakeoff);
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
                                updateNavigation(launchFlags);
                            }
                        });

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(
                        mSyncConfigReceiver, new IntentFilter(Intents.SYNC_CONFIG_INTENT));
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mSyncRouteReceiver, new IntentFilter(Intents.SYNC_ROUTE_INTENT));
    }

    private void onSantaTravelStateChanged(SantaTravelState santaTravelState) {
        AbstractLaunch launchSanta = mCardAdapter.getLauncher(TvCardAdapter.SANTA);

        switch (santaTravelState.getFlyingState()) {
            case PRE_FLIGHT:
                // Santa hasn't taken off yet, start count-down and schedule
                // notification to first departure, hide buttons
                launchSanta.setState(false, AbstractLaunch.STATE_LOCKED);
                startCountdown();
                break;
            case FLYING:
                enableTrackerMode(true);
                launchSanta.setState(true, AbstractLaunch.STATE_READY);
                break;
            case POST_FLIGHT:
                launchSanta.setState(false, AbstractLaunch.STATE_FINISHED);
                stopCountdown();
                enableTrackerMode(false);
                break;
            case DISABLED:
                enableTrackerMode(false);
                launchSanta.setState(false, AbstractLaunch.STATE_DISABLED);
        }
    }

    /** Called continuously with the remaining time to take off. */
    private void onTimeUpdate(Long timeToTakeoff) {
        // Update the countdown
        mCountdown.setTimeRemaining(timeToTakeoff);
    }

    void initialiseViews() {
        mVillageView.setVillage(mVillage);

        // Initialize ListRowPresenter
        ListRowPresenter listRowPresenter = new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM);
        listRowPresenter.setShadowEnabled(false);
        final int rowHeight =
                getResources().getDimensionPixelOffset(R.dimen.tv_marker_height_and_shadow);
        listRowPresenter.setRowHeight(rowHeight);

        // Initialize ListRow
        TvCardPresenter presenter = new TvCardPresenter(this);
        mCardAdapter = new TvCardAdapter(this, presenter, mClock);
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
                mMarkers.getPaddingLeft(),
                mMarkers.getPaddingTop() + 150,
                mMarkers.getPaddingRight(),
                mMarkers.getPaddingBottom());

        Presenter debugMenuPresenter =
                new Presenter() {
                    @Override
                    public ViewHolder onCreateViewHolder(ViewGroup parent) {
                        TextView tv = new TextView(parent.getContext());
                        ViewGroup.MarginLayoutParams params =
                                new ViewGroup.MarginLayoutParams(200, 150);
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
                        ((TextView) viewHolder.view).setText((String) item);
                        viewHolder.view.setOnClickListener(
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        final String text = ((TextView) v).getText().toString();
                                        if (text.contains("Enable Tracker")) {
                                            enableTrackerMode(true);
                                        } else if (text.contains("Enable CountDown")) {
                                            startCountdown();
                                        } else {
                                            mIsDebug = false;
                                            initialiseViews();
                                        }
                                    }
                                });
                    }

                    @Override
                    public void onUnbindViewHolder(ViewHolder viewHolder) {}
                };

        ObjectAdapter debugMenuAdapter =
                new ObjectAdapter(debugMenuPresenter) {

                    private final String[] mMenuString = {
                        "Enable Tracker", "Enable CountDown", "Hide DebugMenu"
                    };

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

    // see http://stackoverflow.com/questions/25884954/deep-linking-and-multiple-app-instances/
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showColorMask(false);
        VillageViewModel.LaunchFlags launchFlags = mVillageViewModel.getLaunchFlags().getValue();
        if (launchFlags != null) {
            updateNavigation(launchFlags);
        }
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
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSyncConfigReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSyncRouteReceiver);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mResumed && hasFocus && !AccessibilityUtil.isTouchAccessiblityEnabled(this)) {
            mAudioPlayer.playTrackExclusive(R.raw.village_music, true);
        }
    }

    public void enableTrackerMode(boolean showLaunchButton) {
        mVillageBackdrop.setImageResource(R.drawable.village_bg_launch);
        mVillage.setPlaneEnabled(false);
        mLaunchButton.setVisibility(showLaunchButton ? View.VISIBLE : View.GONE);
        mCountdownView.setVisibility(View.GONE);
    }

    public void startCountdown() {
        mVillageBackdrop.setImageResource(R.drawable.village_bg_countdown);
        mVillage.setPlaneEnabled(true);
        mLaunchButton.setVisibility(View.GONE);
        mCountdownView.setVisibility(View.VISIBLE);
    }

    public void stopCountdown() {
        mCountdownView.setVisibility(View.GONE);
    }

    /*
     * Village Markers
     */
    private void updateNavigation(VillageViewModel.LaunchFlags launchFlags) {
        GameState gameState = launchFlags.getGameState();
        mCardAdapter
                .getLauncher(TvCardAdapter.ROCKET)
                .setState(
                        gameState.getFeatureRocketGame(),
                        getGamePinState(
                                gameState.getDisableRocketGame(),
                                mConfig.get(Config.UNLOCK_ROCKET)));

        VideoState videoState = launchFlags.getVideoState();
        ((LaunchVideo) mCardAdapter.getLauncher(TvCardAdapter.VIDEO01))
                .setVideo(videoState.getVideo1(), mConfig.get(Config.UNLOCK_VIDEO_1));
        ((LaunchVideo) mCardAdapter.getLauncher(TvCardAdapter.VIDEO15))
                .setVideo(videoState.getVideo15(), mConfig.get(Config.UNLOCK_VIDEO_15));
        ((LaunchVideo) mCardAdapter.getLauncher(TvCardAdapter.VIDEO23))
                .setVideo(videoState.getVideo23(), mConfig.get(Config.UNLOCK_VIDEO_23));
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

    private void cancelUIUpdate() {
        mHandler.removeCallbacksAndMessages(null);
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
    public void launchActivity(Intent intent, @NonNull ActivityOptionsCompat options) {
        launchActivityInternal(intent, null, 0);
    }

    @Override
    public void launchActivityDelayed(
            final Intent intent, final View v, @NonNull ActivityOptionsCompat options) {
        launchActivityInternal(intent, v, 200);
    }

    @Override
    public void launchActivity(Intent intent) {
        launchActivityInternal(intent, null, 0);
    }

    @Override
    public void launchActivityDelayed(Intent intent, View v) {
        launchActivityInternal(intent, v, 200);
    }

    @Override
    public View getCountdownView() {
        return findViewById(R.id.countdown_container);
    }

    /** Attempt to launch the tracker, if available. */
    public void launchTracker() {
        AbstractLaunch launch = mCardAdapter.getLauncher(TvCardAdapter.SANTA);
        if (launch instanceof LaunchSantaTracker) {
            LaunchSantaTracker tracker = (LaunchSantaTracker) launch;

            // App Measurement
            MeasurementManager.recordCustomEvent(
                    mMeasurement,
                    getString(R.string.analytics_event_category_launch),
                    getString(R.string.analytics_launch_action_village));

            tracker.onClick(mLaunchButton);
        }
    }

    private synchronized void launchActivityInternal(
            final Intent intent, View srcView, long delayMs) {

        if (!mLaunchingChild) {
            mLaunchingChild = true;

            if (srcView != null) {
                playCircularRevealTransition(srcView);
            }

            mHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            startActivityForResult(intent, RC_STARTUP);
                            mLaunchingChild = false;
                        }
                    },
                    delayMs);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void playCircularRevealTransition(View srcView) {
        showColorMask(true);
        View mask = findViewById(R.id.content_mask);
        srcView.getGlobalVisibleRect(mSrcRect);
        Animator anim =
                ViewAnimationUtils.createCircularReveal(
                        mask, mSrcRect.centerX(), mSrcRect.centerY(), 0.f, mask.getWidth());
        anim.start();
    }

    private void showColorMask(boolean show) {
        int visibility = show ? View.VISIBLE : View.INVISIBLE;
        (findViewById(R.id.content_mask)).setVisibility(visibility);
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return mAndroidInjector;
    }
}
