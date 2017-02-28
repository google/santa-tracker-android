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

package com.google.android.apps.santatracker.rocketsleigh;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.android.apps.santatracker.invites.AppInvitesFragment;
import com.google.android.apps.santatracker.util.AnalyticsManager;
import com.google.android.apps.santatracker.util.ImmersiveModeHelper;
import com.google.android.apps.santatracker.util.MeasurementManager;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class RocketSleighActivity extends FragmentActivity
        implements View.OnTouchListener,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        OnClickListener,
        SoundPool.OnLoadCompleteListener,
        MediaPlayer.OnCompletionListener {

    private ImageView mElf;
    private ImageView mThrust;
    private Bitmap mElfBitmap;
    private Bitmap mBurnBitmap;
    private Bitmap mThrustBitmap;
    private Bitmap mSmokeBitmpap;
    private Bitmap mCurrentTrailBitmap;
    private LinearLayout mElfLayout;
    private int mElfState = 0;  // 0 - 100% ... 3 - 25% 4 - Parachute elf.
    private boolean mElfIsHit = false;
    private long mElfHitTime = 0;
    private float mElfPosX = 100; // Pixels from the left edge
    private float mElfPosY = 200; // Pixels from the top edge
    private float mElfVelX = 0.3f; // Horizontal speed.
    private float mElfVelY = 0.0f; // Vertical speed.
    private float mElfAccelY = 0.0f; // Acceleration due to thrust if user is touching screen.
    private float mThrustAccelY; // Vertical acceleration in pixel velocity per second of thrust.
    private float mGravityAccelY; // Vertical acceleration due to gravity in pixel velocity per second.
    private long mLastTime;
    private float mElfScale;

    private CountDownTimer mCountDownTimer;

    // Achievments
    private boolean mHit = false;
    private boolean mHitLevel = false;
    private boolean mCleanLevel = false;
    private boolean mPresentBonus = false;

    private boolean mRainingPresents = false;
    private ImageView mPlus100;
    private ImageView mPlus500;
    private AlphaAnimation m100Anim;
    private AlphaAnimation m500Anim;

    private LinearLayout mObstacleLayout;
    private HorizontalScrollView mObstacleScroll;
    private int mSlotWidth;
            // This is the width of an ornament "slot".  Obstacles can span multiple slots.
    private Random mRandom;
    private int mLastTopHeight = 0;
    private int mLastBottomHeight = 0;

    private LinearLayout mBackgroundLayout;
    private HorizontalScrollView mBackgroundScroll;
    private LinearLayout mForegroundLayout;
    private HorizontalScrollView mForegroundScroll;
    private float mScaleY = 1.0f;
    private float mScaleX = 1.0f;

    private TextView mScoreText;
    private String mScoreLabel;
    private ImageView mPlayPauseButton;

    private int mScreenHeight;
    private int mScreenWidth;

    private View mControlView;
    private GestureDetector mGestureDetector;
    private MotionEvent mDownEvent;

    private TextView mCountdown;

    private boolean mIsTv = true;
    private boolean mIsPlaying = false;
    private boolean mMoviePlaying = false;
    private boolean mCountdownStarted = false;
    private int mLevel = 0; // There are six levels.
    private long mScore = 0;
    private int mPresentCount = 0; // 5 in a row gets a bonus...
    private int mBackgroundCount = 0; // 5 copies of backgrounds per level
    private int mTransitionImagesCount = 0; // Some level transitions have transition images.

    private LayoutInflater mInflater;

    private long mLastFrameTime = 0;

    private Vibrator mVibrator;

    private Handler mHandler;

    private VideoView mIntroVideo;
    private View mIntroControl;
    private MediaPlayer mBackgroundPlayer;

    // For sound effects
    private SoundPool mSoundPool;
    private int mCrashSound1;
    private int mCrashSound2;
    private int mCrashSound3;
    private int mGameOverSound;
    private int mJetThrustSound;
    private int mLevelUpSound;
    private int mScoreBigSound;
    private int mScoreSmallSound;
    private int mJetThrustStream;

    private View mBigPlayButtonLayout;
    private ImageButton mBigPlayButton;

    private ImageView mExit;

    private FirebaseAnalytics mMeasurement;
    private AppInvitesFragment mInvitesFragment;

    private static final String LOG_TAG = RocketSleighActivity.class.getSimpleName();
    private static final int SLOTS_PER_SCREEN = 10;

    private static final int[] BACKGROUNDS = {
            R.drawable.bg_jet_pack_1,
            R.drawable.bg_jet_pack_2,
            R.drawable.bg_jet_pack_3,
            R.drawable.bg_jet_pack_4,
            R.drawable.bg_jet_pack_5,
            R.drawable.bg_jet_pack_6
    };

    private Bitmap[] mBackgrounds;
    private Bitmap[] mBackgrounds2;

    private static final int[] FOREGROUNDS = {
            R.drawable.img_snow_ground_tiles,
            R.drawable.img_snow_ground_tiles,
            R.drawable.img_snow_ground_tiles,
            R.drawable.img_snow_ground_tiles,
            -1,
            -1
    };

    private static final int[] EXIT_TRANSITIONS = {
            -1,
            -1,
            -1,
            R.drawable.bg_transition_2,
            -1,
            R.drawable.bg_transition_4,
    };

    private Bitmap[] mExitTransitions;

    private static final int[] ENTRY_TRANSITIONS = {
            -1,
            -1,
            R.drawable.bg_transition_1,
            -1,
            R.drawable.bg_transition_3,
            -1
    };

    private Bitmap[] mEntryTransitions;

    private static final int[] ELF_IMAGES = {
            R.drawable.img_jetelf_100,
            R.drawable.img_jetelf_75,
            R.drawable.img_jetelf_50,
            R.drawable.img_jetelf_25,
            R.drawable.img_jetelf_0
    };

    private Bitmap[] mElfImages;

    private static final int[] ELF_HIT_IMAGES = {
            R.drawable.img_jetelf_100_hit,
            R.drawable.img_jetelf_75_hit,
            R.drawable.img_jetelf_50_hit,
            R.drawable.img_jetelf_25_hit,
    };

    private Bitmap[] mElfHitImages;

    private static final int[] ELF_BURN_IMAGES = {
            R.drawable.img_jet_burn_100,
            R.drawable.img_jet_burn_75,
            R.drawable.img_jet_burn_50,
            R.drawable.img_jet_burn_25
    };

    private Bitmap[] mElfBurnImages;

    private static final int[] ELF_THRUST_IMAGES = {
            R.drawable.img_jet_thrust_100,
            R.drawable.img_jet_thrust_75,
            R.drawable.img_jet_thrust_50,
            R.drawable.img_jet_thrust_25
    };

    private Bitmap[] mElfThrustImages;

    private static final int[] ELF_SMOKE_IMAGES = {
            R.drawable.img_jet_smoke_100_hit,
            R.drawable.img_jet_smoke_75_hit,
            R.drawable.img_jet_smoke_50_hit,
            R.drawable.img_jet_smoke_25_hit
    };

    private Bitmap[] mElfSmokeImages;

    private static final int[] GIFT_BOXES = {
            R.drawable.img_gift_blue_jp,
            R.drawable.img_gift_green_jp,
            R.drawable.img_gift_yellow_jp,
            R.drawable.img_gift_purple_jp,
            R.drawable.img_gift_red_jp
    };

    private Bitmap[] mGiftBoxes;

    // Top, Bottom, Background
    private static final int[] WOOD_OBSTACLES = {
            -1, R.drawable.img_pine_1_bottom, R.drawable.img_pine_0,
            -1, R.drawable.img_pine_2_bottom, R.drawable.img_pine_0,
            -1, R.drawable.img_pine_3_bottom, R.drawable.img_pine_0,
            -1, R.drawable.img_pine_4_bottom, R.drawable.img_pine_0,
            R.drawable.img_pine_1_top, -1, R.drawable.img_pine_0,
            R.drawable.img_pine_2_top, -1, R.drawable.img_pine_0,
            R.drawable.img_pine_3_top, -1, R.drawable.img_pine_0,
            R.drawable.img_pine_4_top, -1, R.drawable.img_pine_0,
            -1, R.drawable.img_birch_1_bottom, R.drawable.img_birch_0,
            -1, R.drawable.img_birch_2_bottom, R.drawable.img_birch_0,
            -1, R.drawable.img_birch_3_bottom, R.drawable.img_birch_0,
            -1, R.drawable.img_birch_4_bottom, R.drawable.img_birch_0,
            R.drawable.img_birch_1_top, -1, R.drawable.img_birch_0,
            R.drawable.img_birch_2_top, -1, R.drawable.img_birch_0,
            R.drawable.img_birch_3_top, -1, R.drawable.img_birch_0,
            R.drawable.img_birch_4_top, -1, R.drawable.img_birch_0,
            -1, R.drawable.img_tree_1_bottom, R.drawable.img_birch_0,
            -1, R.drawable.img_tree_2_bottom, R.drawable.img_birch_0,
            -1, R.drawable.img_tree_3_bottom, R.drawable.img_birch_0,
            -1, R.drawable.img_tree_4_bottom, R.drawable.img_birch_0,
            -1, R.drawable.img_tree_5_bottom, R.drawable.img_birch_0,
            -1, R.drawable.img_tree_6_bottom, R.drawable.img_birch_0,
            R.drawable.img_tree_1_top, -1, R.drawable.img_birch_0,
            R.drawable.img_tree_2_top, -1, R.drawable.img_birch_0,
            R.drawable.img_tree_3_top, -1, R.drawable.img_birch_0,
            R.drawable.img_tree_4_top, -1, R.drawable.img_birch_0,
            R.drawable.img_tree_5_top, -1, R.drawable.img_birch_0,
            R.drawable.img_tree_6_top, -1, R.drawable.img_birch_0,
            -1, R.drawable.img_owl, -1,
            -1, R.drawable.img_log_elf, -1,
            -1, R.drawable.img_bear_big,
            -1, R.drawable.img_bear_little
    };

    private TreeMap<Integer, Bitmap> mWoodObstacles;
    private ArrayList<Integer> mWoodObstacleList;
    private int mWoodObstacleIndex = 0;

    // Top and bottom, no backgrounds
    private static final int[] CAVE_OBSTACLES = {
            R.drawable.img_icicle_small_3, -1,
            R.drawable.img_icicle_small_4, -1,
            R.drawable.img_icicle_med_3, -1,
            R.drawable.img_icicle_med_4, -1,
            R.drawable.img_icicle_lrg_2, -1,
            -1, R.drawable.img_icicle_small_1,
            -1, R.drawable.img_icicle_small_2,
            -1, R.drawable.img_icicle_med_1,
            -1, R.drawable.img_icicle_med_2,
            -1, R.drawable.img_icicle_lrg_1,
            R.drawable.img_icicle_small_3, R.drawable.img_icicle_small_1,
            R.drawable.img_icicle_small_3, R.drawable.img_icicle_small_2,
            R.drawable.img_icicle_small_4, R.drawable.img_icicle_small_1,
            R.drawable.img_icicle_small_4, R.drawable.img_icicle_small_2,
            R.drawable.img_2_bats, -1,
            R.drawable.img_3_bats, -1,
            R.drawable.img_4_bats, -1,
            R.drawable.img_5_bats, -1,
            -1, R.drawable.img_yeti,
            -1, R.drawable.img_mammoth,
            -1, R.drawable.img_snow_kiss,
            -1, R.drawable.img_snowman
    };

    private TreeMap<Integer, Bitmap> mCaveObstacles;
    private ArrayList<Integer> mCaveObstacleList;
    private int mCaveObstacleIndex = 0;

    private final static int[] FACTORY_OBSTACLES = {
            R.drawable.img_icecream_drop, R.drawable.img_icecream_0,
            R.drawable.img_icecream_drop, R.drawable.img_icecream_1,
            R.drawable.img_mint_drop_top, R.drawable.img_mint_drop_bottom,
            R.drawable.img_mint_stack_top, R.drawable.img_mint_stack_bottom,
            -1, R.drawable.img_candy_cane_0,
            R.drawable.img_candy_cane_1, -1,
            -1, R.drawable.img_lollipops,
            -1, R.drawable.img_choco_fountn,
            -1, R.drawable.img_candy_buttons,
            -1, R.drawable.img_mint_gondola,
            -1, R.drawable.img_candy_cane_0,
            R.drawable.img_candy_cane_1, -1,
            -1, R.drawable.img_lollipops,
            -1, R.drawable.img_choco_fountn,
            -1, R.drawable.img_candy_buttons,
            -1, R.drawable.img_mint_gondola,
            -1, R.drawable.img_candy_cane_0,
            R.drawable.img_candy_cane_1, -1,
            -1, R.drawable.img_lollipops,
            -1, R.drawable.img_choco_fountn,
            -1, R.drawable.img_candy_buttons,
            -1, R.drawable.img_mint_gondola
    };

    private TreeMap<Integer, Bitmap> mFactoryObstacles;
    private ArrayList<Integer> mFactoryObstacleList;
    private int mFactoryObstacleIndex = 0;

    private Runnable mGameLoop = new Runnable() {
        @Override
        public void run() {
            processFrame();
        }
    };

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "onCreate() : " + savedInstanceState);

        setContentView(R.layout.activity_jet_pack_elf);

        // App Invites
        mInvitesFragment = AppInvitesFragment.getInstance(this);

        // App Measurement
        mMeasurement = FirebaseAnalytics.getInstance(this);
        MeasurementManager.recordScreenView(mMeasurement,
                getString(R.string.analytics_screen_rocket));

        // [ANALYTICS SCREEN]: Rocket Sleigh
        AnalyticsManager.initializeAnalyticsTracker(this);
        AnalyticsManager.sendScreenView(R.string.analytics_screen_rocket);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ImmersiveModeHelper.setImmersiveSticky(getWindow());
            ImmersiveModeHelper.installSystemUiVisibilityChangeListener(getWindow());
        }

        mIntroVideo = (VideoView) findViewById(R.id.intro_view);
        mIntroControl = findViewById(R.id.intro_control_view);
        if (savedInstanceState == null) {
            String path = "android.resource://" + getPackageName() + "/" + R.raw.jp_background;
            mBackgroundPlayer = new MediaPlayer();
            try {
                mBackgroundPlayer.setDataSource(this, Uri.parse(path));
                mBackgroundPlayer.setLooping(true);
                mBackgroundPlayer.prepare();
                mBackgroundPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            boolean nomovie = false;
            if (getIntent().getBooleanExtra("nomovie", false)) {
                nomovie = true;
            } else if (Build.MANUFACTURER.toUpperCase().contains("SAMSUNG")) {
//                nomovie = true;
            }
            if (!nomovie) {
                mIntroControl.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        endIntro();
                    }
                });
                path = "android.resource://" + getPackageName() + "/" + R.raw.intro_wipe;
                mIntroVideo.setVideoURI(Uri.parse(path));
                mIntroVideo.setOnCompletionListener(this);
                mIntroVideo.start();
                mMoviePlaying = true;
            } else {
                mIntroControl.setOnClickListener(null);
                mIntroControl.setVisibility(View.GONE);
                mIntroVideo.setVisibility(View.GONE);
            }
        } else {
            mIntroControl.setOnClickListener(null);
            mIntroControl.setVisibility(View.GONE);
            mIntroVideo.setVisibility(View.GONE);
        }

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);  // For hit indication.

        mHandler = new Handler();  // Get the main UI handler for posting update events

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        Log.d(LOG_TAG, "Width: " + dm.widthPixels + " Height: " + dm.heightPixels + " Density: "
                + dm.density);

        mScreenHeight = dm.heightPixels;
        mScreenWidth = dm.widthPixels;
        mSlotWidth = mScreenWidth / SLOTS_PER_SCREEN;

        // Setup the random number generator
        mRandom = new Random();
        mRandom.setSeed(
                System.currentTimeMillis());  // This is ok.  We are not looking for cryptographically secure random here!

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Setup the background/foreground
        mBackgroundLayout = (LinearLayout) findViewById(R.id.background_layout);
        mBackgroundScroll = (HorizontalScrollView) findViewById(R.id.background_scroll);
        mForegroundLayout = (LinearLayout) findViewById(R.id.foreground_layout);
        mForegroundScroll = (HorizontalScrollView) findViewById(R.id.foreground_scroll);

        mBackgrounds = new Bitmap[6];
        mBackgrounds2 = new Bitmap[6];
        mExitTransitions = new Bitmap[6];
        mEntryTransitions = new Bitmap[6];

        // Need to vertically scale background to fit the screen.  Checkthe image size
        // compared to screen size and scale appropriately.  We will also use the matrix to translate
        // as we move through the level.
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), BACKGROUNDS[0]);
        Log.d(LOG_TAG, "Bitmap Width: " + bmp.getWidth() + " Height: " + bmp.getHeight()
                + " Screen Width: " + dm.widthPixels + " Height: " + dm.heightPixels);
        mScaleY = (float) dm.heightPixels / (float) bmp.getHeight();
        mScaleX = (float) (dm.widthPixels * 2) / (float) bmp
                .getWidth();  // Ensure that a single bitmap is 2 screens worth of time.  (Stock xxhdpi image is 3840x1080)

        if ((mScaleX != 1.0f) || (mScaleY != 1.0f)) {
            Bitmap tmp = Bitmap.createScaledBitmap(bmp, mScreenWidth * 2, mScreenHeight, false);
            if (tmp != bmp) {
                bmp.recycle();
                bmp = tmp;
            }
        }
        BackgroundLoadTask.createTwoBitmaps(bmp, mBackgrounds, mBackgrounds2, 0);

        // Load the initial background view
        addNextImages(0);
        addNextImages(0);

        mWoodObstacles = new TreeMap<Integer, Bitmap>();
        mWoodObstacleList = new ArrayList<Integer>();
        mWoodObstacleIndex = 0;
        // We need the bitmaps, so we do pre-load here synchronously.
        initObstaclesAndPreLoad(WOOD_OBSTACLES, 3, mWoodObstacles, mWoodObstacleList);

        mCaveObstacles = new TreeMap<Integer, Bitmap>();
        mCaveObstacleList = new ArrayList<Integer>();
        mCaveObstacleIndex = 0;
        initObstacles(CAVE_OBSTACLES, 2, mCaveObstacleList);

        mFactoryObstacles = new TreeMap<Integer, Bitmap>();
        mFactoryObstacleList = new ArrayList<Integer>();
        mFactoryObstacleIndex = 0;
        initObstacles(FACTORY_OBSTACLES, 2, mFactoryObstacleList);

        // Setup the elf
        mElf = (ImageView) findViewById(R.id.elf_image);
        mThrust = (ImageView) findViewById(R.id.thrust_image);
        mElfLayout = (LinearLayout) findViewById(R.id.elf_container);
        loadElfImages();
        updateElf(false);
        // Elf should be the same height relative to the height of the screen on any platform.
        Matrix scaleMatrix = new Matrix();
        mElfScale = ((float) dm.heightPixels * 0.123f) / (float) mElfBitmap
                .getHeight();  // On a 1920x1080 xxhdpi screen, this makes the elf 133 pixels which is the height of the drawable.
        scaleMatrix.preScale(mElfScale, mElfScale);
        mElf.setImageMatrix(scaleMatrix);
        mThrust.setImageMatrix(scaleMatrix);
        mElfPosX = (dm.widthPixels * 15) / 100; // 15% Into the screen
        mElfPosY = (dm.heightPixels - ((float) mElfBitmap.getHeight() * mElfScale))
                / 2; // About 1/2 way down.
        mElfVelX = (float) dm.widthPixels
                / 3000.0f; // We start at 3 seconds for a full screen to scroll.
        mGravityAccelY = (float) (2 * dm.heightPixels) / (float) Math.pow((1.2 * 1000.0),
                2.0);  // a = 2*d/t^2 Where d = height in pixels and t = 1.2 seconds
        mThrustAccelY = (float) (2 * dm.heightPixels) / (float) Math.pow((0.7 * 1000.0),
                2.0);  // a = 2*d/t^2 Where d = height in pixels and t = 0.7 seconds

        // Setup the control view
        mControlView = findViewById(R.id.control_view);
        mGestureDetector = new GestureDetector(this, this);
        mGestureDetector.setIsLongpressEnabled(true);
        mGestureDetector.setOnDoubleTapListener(this);

        mScoreLabel = getString(R.string.score);
        mScoreText = (TextView) findViewById(R.id.score_text);
        mScoreText.setText("0");

        mPlayPauseButton = (ImageView) findViewById(R.id.play_pause_button);
        mExit = (ImageView) findViewById(R.id.exit);

        // Is Tv?
        mIsTv = TvUtil.isTv(this);
        if (mIsTv) {
            mScoreText.setText(mScoreLabel + ": 0");
            mPlayPauseButton.setVisibility(View.GONE);
            mExit.setVisibility(View.GONE);
            // move scoreLayout position to the Top-Right corner.
            View scoreLayout = findViewById(R.id.score_layout);
            RelativeLayout.LayoutParams params
                    = (RelativeLayout.LayoutParams) scoreLayout.getLayoutParams();
            params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

            final int marginTop
                    = getResources().getDimensionPixelOffset(R.dimen.overscan_margin_top);
            final int marginLeft
                    = getResources().getDimensionPixelOffset(R.dimen.overscan_margin_left);

            params.setMargins(marginLeft, marginTop, 0, 0);
            scoreLayout.setLayoutParams(params);
            scoreLayout.setBackground(null);
            scoreLayout.findViewById(R.id.score_text_seperator).setVisibility(View.GONE);
        } else {
            mPlayPauseButton.setEnabled(false);
            mPlayPauseButton.setOnClickListener(this);
            mExit.setOnClickListener(this);
        }

        mBigPlayButtonLayout = findViewById(R.id.big_play_button_layout);
        mBigPlayButtonLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // No interaction with the screen below this one.
                return true;
            }
        });
        mBigPlayButton = (ImageButton) findViewById(R.id.big_play_button);
        mBigPlayButton.setOnClickListener(this);

        // For showing points when getting presents.
        mPlus100 = (ImageView) findViewById(R.id.plus_100);
        m100Anim = new AlphaAnimation(1.0f, 0.0f);
        m100Anim.setDuration(1000);
        m100Anim.setFillBefore(true);
        m100Anim.setFillAfter(true);
        mPlus500 = (ImageView) findViewById(R.id.plus_500);
        m500Anim = new AlphaAnimation(1.0f, 0.0f);
        m500Anim.setDuration(1000);
        m500Anim.setFillBefore(true);
        m500Anim.setFillAfter(true);

        // Get the obstacle layouts ready.  No obstacles on the first screen of a level.
        // Prime with a screen full of obstacles.
        mObstacleLayout = (LinearLayout) findViewById(R.id.obstacles_layout);
        mObstacleScroll = (HorizontalScrollView) findViewById(R.id.obstacles_scroll);

        // Initialize the present bitmaps.  These are used repeatedly so we keep them loaded.
        mGiftBoxes = new Bitmap[GIFT_BOXES.length];
        for (int i = 0; i < GIFT_BOXES.length; i++) {
            mGiftBoxes[i] = BitmapFactory.decodeResource(getResources(), GIFT_BOXES[i]);
        }

        // Add starting obstacles.  First screen has presents.  Next 3 get obstacles.
        addFirstScreenPresents();
//        addFinalPresentRun();  // This adds 2 screens of presents
//        addNextObstacles(0, 1);
        addNextObstacles(0, 3);

        // Setup the sound pool
        mSoundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);
        mSoundPool.setOnLoadCompleteListener(this);
        mCrashSound1 = mSoundPool.load(this, R.raw.jp_crash_1, 1);
        mCrashSound2 = mSoundPool.load(this, R.raw.jp_crash_2, 1);
        mCrashSound3 = mSoundPool.load(this, R.raw.jp_crash_3, 1);
        mGameOverSound = mSoundPool.load(this, R.raw.jp_game_over, 1);
        mJetThrustSound = mSoundPool.load(this, R.raw.jp_jet_thrust, 1);
        mLevelUpSound = mSoundPool.load(this, R.raw.jp_level_up, 1);
        mScoreBigSound = mSoundPool.load(this, R.raw.jp_score_big, 1);
        mScoreSmallSound = mSoundPool.load(this, R.raw.jp_score_small, 1);
        mJetThrustStream = 0;

        if (!mMoviePlaying) {
            doCountdown();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(LOG_TAG, "onResume()");
    }

    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause()");

        if (mMoviePlaying) {
            if (mIntroVideo != null) {
                // We are only here if home or lock is pressed or another app (phone)
                // interrupts.  We just go to the pause screen and start the game when
                // we come back.
                mIntroVideo.stopPlayback();
                mIntroVideo.setVisibility(View.GONE);
                mIntroControl.setOnClickListener(null);
                mIntroControl.setVisibility(View.GONE);
            }
            mMoviePlaying = false;
            mIsPlaying = true;  // this will make pause() show the pause button.
        } else if (mCountdownStarted) {
            mCountdown.setVisibility(View.GONE);
            mCountDownTimer.cancel();
            mCountdownStarted = false;
            mIsPlaying = true;  // this will make pause() show the pause button.
        }
        pause();

        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        mInvitesFragment.getInvite(new AppInvitesFragment.GetInvitationCallback() {
            @Override
            public void onInvitation(String invitationId, String deepLink) {
                Log.d(LOG_TAG, "onInvitation:" + deepLink);
            }
        }, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy()");
        releaseResources();
    }

    private void releaseResources() {
        if (mSoundPool != null) {
            if (mBackgroundPlayer != null) {
                mBackgroundPlayer.stop();
                mBackgroundPlayer.release();
                mBackgroundPlayer = null;
            }
            if (mJetThrustStream > 0) {
                mSoundPool.stop(mJetThrustStream);
                mJetThrustStream = 0;
            }
            mSoundPool.unload(mCrashSound1);
            mSoundPool.unload(mCrashSound2);
            mSoundPool.unload(mCrashSound3);
            mSoundPool.unload(mGameOverSound);
            mSoundPool.unload(mJetThrustSound);
            mSoundPool.unload(mLevelUpSound);
            mSoundPool.unload(mScoreBigSound);
            mSoundPool.unload(mScoreSmallSound);
            mSoundPool.release();
            mSoundPool = null;
        }

        // recylce big bitmaps as soon as possible.
        releaseBitmapArray(mBackgrounds);
        releaseBitmapArray(mBackgrounds2);
        releaseBitmapArray(mEntryTransitions);
        releaseBitmapArray(mExitTransitions);
        releaseBitmapArray(mGiftBoxes);
        releaseBitmapArray(mElfBurnImages);
        releaseBitmapArray(mElfImages);
        releaseBitmapArray(mElfThrustImages);
        releaseBitmapArray(mElfHitImages);
        releaseBitmapArray(mElfSmokeImages);

        releaseIntegerBitmapMap(mWoodObstacles);
        releaseIntegerBitmapMap(mCaveObstacles);
        releaseIntegerBitmapMap(mFactoryObstacles);
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        // We are eating the config changes so that we don't get destroyed/recreated and again
        // destroyed/recreated when the lock button is pressed!
        Log.e(LOG_TAG, "Config change: " + config);
        super.onConfigurationChanged(config);
    }

    @Override
    public void onBackPressed() {
        if (mMoviePlaying) {
            if (mIntroVideo != null) {
                mIntroVideo.stopPlayback();
                mIntroVideo.setVisibility(View.GONE);
                mIntroControl.setOnClickListener(null);
                mIntroControl.setVisibility(View.GONE);
            }
            mMoviePlaying = false;
            super.onBackPressed();
        } else if (mCountdownStarted) {
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
                mCountDownTimer = null;
            }
            mCountdownStarted = false;
            super.onBackPressed();
        } else {
            if (mIsPlaying) {
                pause();
            } else if (mIsTv) {
                finish();
            } else {
                play();
            }
        }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
                //fall through
            case KeyEvent.KEYCODE_BUTTON_A:
                if (mIsPlaying) {
                    mElfAccelY = mThrustAccelY;
                    if (!mElfIsHit) {
                        updateElfThrust(1);
                    }
                    mJetThrustStream = mSoundPool.play(mJetThrustSound, 1.0f, 1.0f, 1, -1, 1.0f);
                } else if (!mCountdownStarted && !mMoviePlaying){
                    //game is paused. resume it.
                    mBigPlayButton.setPressed(true);
                }
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
                //fall through
            case KeyEvent.KEYCODE_BUTTON_A:
                if (mIsPlaying) {
                    mElfAccelY = 0.0f;
                    if (!mElfIsHit) {
                        updateElfThrust(0);
                    }
                    if (mJetThrustStream > 0) {
                        mSoundPool.stop(mJetThrustStream);
                        mJetThrustStream = 0;
                    }
                } else if (mMoviePlaying) {
                    endIntro();
                } else if (mBigPlayButton.isPressed()){
                    mBigPlayButton.setPressed(false);
                    mBigPlayButton.performClick();
                }
                return true;
            case KeyEvent.KEYCODE_BUTTON_B:
                onBackPressed();
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mDownEvent = event;
            mElfAccelY = mThrustAccelY;
            if (!mElfIsHit) {
                updateElfThrust(1);
            }
            mJetThrustStream = mSoundPool.play(mJetThrustSound, 1.0f, 1.0f, 1, -1, 1.0f);
        } else if ((event.getActionMasked() == MotionEvent.ACTION_UP) || (event.getActionMasked()
                == MotionEvent.ACTION_CANCEL)) {
            mDownEvent = null;
            mElfAccelY = 0.0f;
            if (!mElfIsHit) {
                updateElfThrust(0);
            }
            if (mJetThrustStream > 0) {
                mSoundPool.stop(mJetThrustStream);
                mJetThrustStream = 0;
            }
        }

//		return mGestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public void onClick(View view) {
        if (view == mPlayPauseButton) {
            if (mIsPlaying) {
                pause();
            } else {
                play();
            }
        } else if (view == mBigPlayButton) {
            if (!mIsPlaying) {
                mBigPlayButtonLayout.setVisibility(View.GONE);
                doCountdown();
            }
        } else if (view == mExit) {
            finish();
        }
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        endIntro();
    }

    private void endIntro() {
        mMoviePlaying = false;
        mIntroControl.setOnClickListener(null);
        mIntroControl.setVisibility(View.GONE);
        mIntroVideo.setVisibility(View.GONE);
        doCountdown();
    }

    private void processFrame() {
        long newTime = System.currentTimeMillis();
        long time = newTime - mLastTime;

        boolean end = false;

        if (time > 60) {
            Log.e("LONG", "Frame time took too long! Time: " + time + " Last process frame: "
                    + mLastFrameTime + " Count: " + mBackgroundCount + " Level: " + mLevel);
        }

        // We don't want to jump too far so, if real time is > 60 treat it as 33.  On screen will seem to slow
        // down instaead of "jump"
        if (time > 60) {
            time = 33;
        }

        // Score is based on time + presents.  Right now 100 point per second played.  No presents yet
        if (mLevel < 6) {
            mScore += time;
        }

        if (mIsTv) {
            mScoreText.setText(mScoreLabel + ": "
                    + NumberFormat.getNumberInstance().format((mScore / 10)));
        } else {
            mScoreText.setText(NumberFormat.getNumberInstance().format((mScore / 10)));
        }

        float scroll = mElfVelX * time;

        // Do collision detection first...
        // The elf can't collide if it is within 2 seconds of colliding previously.
        if (mElfIsHit) {
            if ((newTime - mElfHitTime) > 2000) {
                // Move to next state.
                if (mElfState < 4) {
                    mElfState++;
                    AnalyticsManager.sendEvent(getString(R.string.analytics_screen_rocket),
                            getString(R.string.analytics_action_rocket_hit), null, mElfState);
                    if (mElfState == 4) {
                        mSoundPool.play(mGameOverSound, 1.0f, 1.0f, 2, 0, 1.0f);
                        // No more control...
                        mControlView.setOnTouchListener(null);
                        mElfAccelY = 0.0f;
                        if (mJetThrustStream != 0) {
                            mSoundPool.stop(mJetThrustStream);
                        }
                    }
                }
                updateElf(false);
                mElfIsHit = false;
            }
        } else if (mElfState == 4) {
            // Don't do any collision detection for parachute elf.  Just let him fall...
        } else {
            // Find the obstacle(s) we might be colliding with.  It can only be one of the first 3 obstacles.
            for (int i = 0; i < 3; i++) {
                View view = mObstacleLayout.getChildAt(i);
                if (view == null) {
                    // No more obstacles...
                    break;
                }

                int[] tmp = new int[2];
                view.getLocationOnScreen(tmp);

                // If the start of this view is past the center of the elf, we are done
                if (tmp[0] > mElfPosX) {
                    break;
                }

                if (RelativeLayout.class.isInstance(view)) {
                    // this is an obstacle layout.
                    View topView = view.findViewById(R.id.top_view);
                    View bottomView = view.findViewById(R.id.bottom_view);
                    if ((topView != null) && topView.getVisibility() == View.VISIBLE) {
                        topView.getLocationOnScreen(tmp);
                        Rect obsRect = new Rect(tmp[0], tmp[1], tmp[0] + topView.getWidth(),
                                tmp[1] + topView.getHeight());
                        if (obsRect.contains((int) mElfPosX,
                                (int) mElfPosY + mElfBitmap.getHeight() / 2)) {
                            handleCollision();
                        }
                    }
                    if (!mElfIsHit) {
                        if ((bottomView != null) && bottomView.getVisibility() == View.VISIBLE) {
                            bottomView.getLocationOnScreen(tmp);
                            Rect obsRect = new Rect(tmp[0], tmp[1], tmp[0] + bottomView.getWidth(),
                                    tmp[1] + bottomView.getHeight());
                            if (obsRect.contains((int) mElfPosX,
                                    (int) mElfPosY + mElfBitmap.getHeight() / 2)) {
                                // Special case for the mammoth obstacle...
                                if (bottomView.getTag() != null) {
                                    if (((mElfPosX - tmp[0]) / (float) bottomView.getWidth())
                                            > 0.25f) {
                                        // We are over the mammoth not the spike.  lower the top of the rect and test again.
                                        obsRect.top = (int) (tmp[1] + (
                                                (float) bottomView.getHeight() * 0.18f));
                                        if (obsRect.contains((int) mElfPosX,
                                                (int) mElfPosY + mElfBitmap.getHeight() / 2)) {
                                            handleCollision();
                                        }
                                    }
                                } else {
                                    handleCollision();
                                }
                            }
                        }
                    }
                } else if (FrameLayout.class.isInstance(view)) {
                    // Present view
                    FrameLayout frame = (FrameLayout) view;
                    if (frame.getChildCount() > 0) {
                        ImageView presentView = (ImageView) frame.getChildAt(0);
                        presentView.getLocationOnScreen(tmp);
                        Rect presentRect = new Rect(tmp[0], tmp[1], tmp[0] + presentView.getWidth(),
                                tmp[1] + presentView.getHeight());
                        mElfLayout.getLocationOnScreen(tmp);
                        Rect elfRect = new Rect(tmp[0], tmp[1], tmp[0] + mElfLayout.getWidth(),
                                tmp[1] + mElfLayout.getHeight());
                        if (elfRect.intersect(presentRect)) {
                            // We got a present!
                            mPresentCount++;
                            if (mPresentCount < 4) {
                                mSoundPool.play(mScoreSmallSound, 1.0f, 1.0f, 2, 0, 1.0f);
                                mScore += 1000;  // 100 points.  Score is 10x displayed score.
                                mPlus100.setVisibility(View.VISIBLE);
                                if (mElfPosY > (mScreenHeight / 2)) {
                                    mPlus100.setY(mElfPosY - (mElfLayout.getHeight() + mPlus100
                                            .getHeight()));
                                } else {
                                    mPlus100.setY(mElfPosY + mElfLayout.getHeight());
                                }
                                mPlus100.setX(mElfPosX);
                                if (m100Anim.hasStarted()) {
                                    m100Anim.reset();
                                }
                                mPlus100.startAnimation(m100Anim);
                            } else {
                                mSoundPool.play(mScoreBigSound, 1.0f, 1.0f, 2, 0, 1.0f);
                                mScore += 5000; // 500 points.  Score is 10x displayed score.
                                if (!mRainingPresents) {
                                    mPresentCount = 0;
                                }
                                mPlus500.setVisibility(View.VISIBLE);
                                if (mElfPosY > (mScreenHeight / 2)) {
                                    mPlus500.setY(mElfPosY - (mElfLayout.getHeight() + mPlus100
                                            .getHeight()));
                                } else {
                                    mPlus500.setY(mElfPosY + mElfLayout.getHeight());
                                }
                                mPlus500.setX(mElfPosX);
                                if (m500Anim.hasStarted()) {
                                    m500Anim.reset();
                                }
                                mPlus500.startAnimation(m500Anim);
                                mPresentBonus = true;
                            }
                            frame.removeView(presentView);
                        } else if (elfRect.left > presentRect.right) {
                            mPresentCount = 0;
                        }
                    }
                }
            }
        }

        if (mForegroundLayout.getChildCount() > 0) {
            int currentX = mForegroundScroll.getScrollX();
            View view = mForegroundLayout.getChildAt(0);
            int newX = currentX + (int) scroll;
            if (newX > view.getWidth()) {
                newX -= view.getWidth();
                mForegroundLayout.removeViewAt(0);
            }
            mForegroundScroll.setScrollX(newX);
        }

        // Scroll obstacle views
        if (mObstacleLayout.getChildCount() > 0) {
            int currentX = mObstacleScroll.getScrollX();
            View view = mObstacleLayout.getChildAt(0);
            int newX = currentX + (int) scroll;
            if (newX > view.getWidth()) {
                newX -= view.getWidth();
                mObstacleLayout.removeViewAt(0);
            }
            mObstacleScroll.setScrollX(newX);
        }

        // Scroll the background and foreground
        if (mBackgroundLayout.getChildCount() > 0) {
            int currentX = mBackgroundScroll.getScrollX();
            View view = mBackgroundLayout.getChildAt(0);
            int newX = currentX + (int) scroll;
            if (newX > view.getWidth()) {
                newX -= view.getWidth();
                mBackgroundLayout.removeViewAt(0);
                if (view.getTag() != null) {
                    Pair<Integer, Integer> pair = (Pair<Integer, Integer>) view.getTag();
                    int type = pair.first;
                    int level = pair.second;
                    if (type == 0) {
                        if (mBackgrounds[level] != null) {
                            mBackgrounds[level].recycle();
                            mBackgrounds[level] = null;
                        } else if (mBackgrounds2[level] != null) {
                            mBackgrounds2[level].recycle();
                            mBackgrounds2[level] = null;
                        }
                    } else if (type == 1) {
                        if (mExitTransitions[level] != null) {
                            mExitTransitions[level].recycle();
                            mExitTransitions[level] = null;
                        }
                    } else if (type == 2) {
                        if (mEntryTransitions[level] != null) {
                            mEntryTransitions[level].recycle();
                            mEntryTransitions[level] = null;
                        }
                    }
                }
                if (mBackgroundCount == 5) {
                    if (mLevel < 6) {
                        // Pre-fetch next levels backgrounds
                        // end level uses the index 1 background...
                        int level = (mLevel == 5) ? 1 : (mLevel + 1);
                        BackgroundLoadTask task = new BackgroundLoadTask(getResources(),
                                mLevel + 1,
                                BACKGROUNDS[level],
                                EXIT_TRANSITIONS[mLevel],
                                // Exit transitions are for the current level...
                                ENTRY_TRANSITIONS[level],
                                mScaleX,
                                mScaleY,
                                mBackgrounds,
                                mBackgrounds2,
                                mExitTransitions,
                                mEntryTransitions,
                                mScreenWidth,
                                mScreenHeight);
                        task.execute();
                        addNextImages(mLevel, true);
                        addNextObstacles(mLevel, 2);
                    }
                    // Fetch first set of obstacles if the next level changes from woods to cave or cave to factory
                    if (mLevel == 1) {
                        // Next level will be caves.  Get bitmaps for the first 20 obstacles.
                        ObstacleLoadTask task = new ObstacleLoadTask(getResources(),
                                CAVE_OBSTACLES,
                                mCaveObstacles,
                                mCaveObstacleList,
                                0,
                                2,
                                mScaleX,
                                mScaleY);
                        task.execute();
                    } else if (mLevel == 3) {
                        // Next level will be factory.  Get bitmaps for the first 20 obstacles.
                        ObstacleLoadTask task = new ObstacleLoadTask(getResources(),
                                FACTORY_OBSTACLES,
                                mFactoryObstacles,
                                mFactoryObstacleList,
                                0,
                                2,
                                mScaleX,
                                mScaleY);
                        task.execute();
                    }
                    mBackgroundCount++;
                } else if (mBackgroundCount == 7) {
                    // Add transitions and/or next level
                    if (mLevel < 5) {
                        addNextTransitionImages(mLevel + 1);
                        if (mTransitionImagesCount > 0) {
                            addNextObstacleSpacer(mTransitionImagesCount);
                        }
                        addNextImages(mLevel + 1);
                        // First screen of each new level has no obstacles
                        if ((mLevel % 2) == 1) {
                            addNextObstacleSpacer(1);
                            addNextObstacles(mLevel + 1, 1);
                        } else {
                            addNextObstacles(mLevel + 1, 2);
                        }
                    } else if (mLevel == 5) {
                        addNextTransitionImages(mLevel + 1);
                        if (mTransitionImagesCount > 0) {
                            addNextObstacleSpacer(mTransitionImagesCount);
                        }
                        addFinalImages();
                    }
                    mBackgroundCount++;
                } else if (mBackgroundCount == 9) {
                    // Either the transition or the next level is showing
                    if (this.mTransitionImagesCount > 0) {
                        mTransitionImagesCount--;
                    } else {
                        if (mLevel == 1) {
                            // Destroy the wood obstacle bitmaps
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    synchronized (mWoodObstacles) {
                                        for (Bitmap bmp : mWoodObstacles.values()) {
                                            bmp.recycle();
                                        }
                                        mWoodObstacles.clear();
                                    }
                                }
                            });
                            thread.start();
                        } else if (mLevel == 3) {
                            // Destroy the cave obstacle bitmaps
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    synchronized (mCaveObstacles) {
                                        for (Bitmap bmp : mCaveObstacles.values()) {
                                            bmp.recycle();
                                        }
                                        mCaveObstacles.clear();
                                    }
                                }
                            });
                            thread.start();
                        } else if (mLevel == 5) {
                            // Destroy the factory obstacle bitmaps
                            Thread thread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    synchronized (mFactoryObstacles) {
                                        for (Bitmap bmp : mFactoryObstacles.values()) {
                                            bmp.recycle();
                                        }
                                        mFactoryObstacles.clear();
                                    }
                                }
                            });
                            thread.start();
                        }
                        mLevel++;

                        // Add an event for clearing this level - note we don't increment mLevel as
                        // it's 0-based and we're tracking the previous level.
                        AnalyticsManager.sendEvent(getString(R.string.analytics_screen_rocket),
                                getString(R.string.analytics_action_rocket_level), null, mLevel);

                        // Achievements
                        if (!mHitLevel) {
                            mCleanLevel = true;
                        }
                        mHitLevel = false;
                        if (mLevel == 5) {
                            mPlus100.setSelected(true);
                            mPlus500.setSelected(true);
                        } else if (mLevel == 6) {
                            mPlus100.setSelected(false);
                            mPlus500.setSelected(false);
                        }
                        if (mLevel < 6) {
                            mSoundPool.play(mLevelUpSound, 1.0f, 1.0f, 2, 0, 1.0f);
                            addNextImages(mLevel);
                            addNextObstacles(mLevel, 2);
                        }
                        mBackgroundCount = 0;
                    }
                } else {
                    if ((mBackgroundCount % 2) == 1) {
                        if (mLevel < 6) {
                            addNextImages(mLevel);
                            addNextObstacles(mLevel, 2);
                        }
                    }
                    mBackgroundCount++;
                }
            }
            int current = mBackgroundScroll.getScrollX();
            mBackgroundScroll.setScrollX(newX);
            if ((mLevel == 6) && (mBackgroundScroll.getScrollX() == current)) {
                end = true;
            }
        }

        // Check on the elf
        boolean hitBottom = false;
        boolean hitTop = false;

        float deltaY = mElfVelY * time;
        mElfPosY = mElfLayout.getY() + deltaY;
        if (mElfPosY < 0.0f) {
            mElfPosY = 0.0f;
            mElfVelY = 0.0f;
            hitTop = true;
        } else if (mElfPosY > (mScreenHeight - mElfLayout.getHeight())) {
            mElfPosY = mScreenHeight - mElfLayout.getHeight();
            mElfVelY = 0.0f;
            hitBottom = true;
        } else {
            // Remember -Y is up!
            mElfVelY += (mGravityAccelY * time - mElfAccelY * time);
        }
        mElfLayout.setY(mElfPosY);

        // Rotate the elf to indicate thrust, dive.
        float rot = (float) (Math.atan(mElfVelY / mElfVelX) * 120.0 / Math.PI);
        mElfLayout.setRotation(rot);

        mElf.invalidate();

        // Update the time and spawn the next call to processFrame.
        mLastTime = newTime;
        mLastFrameTime = System.currentTimeMillis() - newTime;
        if (!end) {
            if ((mElfState < 4) || !hitBottom) {
                if (mLastFrameTime < 16) {
                    mHandler.postDelayed(mGameLoop, 16 - mLastFrameTime);
                } else {
                    mHandler.post(mGameLoop);
                }
            } else {
                endGame();
            }
        } else {
            // Whatever the final stuff is, do it here.
            mPlayPauseButton.setEnabled(false);
            mPlayPauseButton.setVisibility(View.INVISIBLE);
            endGame();
        }
    }

    private void handleCollision() {
        // Achievements
        mHit = true;
        mHitLevel = true;

        // Collision!
        mElfIsHit = true;
        mElfHitTime = System.currentTimeMillis();
        updateElf(true);
        mVibrator.vibrate(500);
        if (mElfState == 0) {
            mSoundPool.play(mCrashSound1, 1.0f, 1.0f, 2, 0, 1.0f);
        } else if (mElfState == 1) {
            mSoundPool.play(mCrashSound2, 1.0f, 1.0f, 2, 0, 1.0f);
        } else if (mElfState == 2) {
            mSoundPool.play(mCrashSound3, 1.0f, 1.0f, 2, 0, 1.0f);
        } else if (mElfState == 3) {
            mSoundPool.play(mCrashSound3, 1.0f, 1.0f, 2, 0, 1.0f);
        }
    }

    private void doCountdown() {
        mCountdownStarted = true;
        mPlayPauseButton.setEnabled(false);
        // Start the countdown
        if (mCountdown == null){
            mCountdown = (TextView) findViewById(R.id.countdown_text);
        }
        mCountdown.setVisibility(View.VISIBLE);
        mCountdown.setTextColor(Color.WHITE);
        mCountdown.setText("3");
        mCountDownTimer = new CountDownTimer(3500, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                int time = (int) ((millisUntilFinished + 500) / 1000);
                if (time == 3) {
                    mCountdown.setText("3");
                } else if (time == 2) {
                    mCountdown.setText("2");
                } else if (time == 1) {
                    mCountdown.setText("1");
                } else if (time == 0) {
                    mCountdown.setText("Go!");
                }
            }

            @Override
            public void onFinish() {
                mCountdownStarted = false;
                mPlayPauseButton.setEnabled(true);
                play();
                mCountdown.setVisibility(View.GONE);
            }
        };
        mCountDownTimer.start();
    }

    private int mLastObstacle = 0;
            // 0 - spacer, 1 - upper obstacle, 2 - lower obstacle.  These are flags so top + bottom is 3.

    private void addFirstScreenPresents() {
        // First 4 slots have no nothing.
        for (int i = 0; i < Math.min(4, SLOTS_PER_SCREEN); i++) {
            View view = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mSlotWidth, mScreenHeight);
            mObstacleLayout.addView(view, lp);
        }

        // Generate a SIN like pattern;
        float center = (float) ((mScreenHeight - mGiftBoxes[0].getHeight()) / 2);
        float presentHeight = (float) mGiftBoxes[0].getHeight();
        float[] heights = new float[]{
                center,
                center - presentHeight,
                center - (1.5f * presentHeight),
                center - presentHeight,
                center,
                center + presentHeight,
                center + (1.5f * presentHeight),
                center + presentHeight,
                center
        };
        // Add presents to the end
        if (SLOTS_PER_SCREEN > 4) {
            for (int i = 0; i < (SLOTS_PER_SCREEN - 4); i++) {
                // Which one?
                Bitmap bmp = mGiftBoxes[mRandom.nextInt(mGiftBoxes.length)];
                ImageView iv = new ImageView(this);
                iv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                iv.setImageBitmap(bmp);

                // Position the present
                float left = (mSlotWidth - bmp.getWidth()) / 2;
                float top = heights[(i % heights.length)];

                FrameLayout frame = new FrameLayout(this);
                LayoutParams flp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT);
                frame.addView(iv, flp);
                iv.setTranslationX(left);
                iv.setTranslationY(top);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mSlotWidth,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                mObstacleLayout.addView(frame, lp);
            }
        }

        // Account for rounding errors in mSlotWidth
        int extra = (mScreenWidth - (SLOTS_PER_SCREEN * mSlotWidth));
        if (extra > 0) {
            // Add filler to ensure sync with background/foreground scrolls!
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(extra,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            View view = new View(this);
            mObstacleLayout.addView(view, lp);
        }

        mLastObstacle = 0;
    }

    private void addFinalPresentRun() {
        // Two spacers at the begining.
        View view = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mSlotWidth, mScreenHeight);
        mObstacleLayout.addView(view, lp);
        view = new View(this);
        mObstacleLayout.addView(view, lp);

        // All of these presents are 500 points (but only if you're awesome)
        if (mElfState == 0) {
            mRainingPresents = true;
        }

        // SIN wave of presents in the middle
        float center = (float) (mScreenHeight / 2);
        float amplitude = (float) (mScreenHeight / 4);

        int count = (3 * SLOTS_PER_SCREEN) - 4;

        for (int i = 0; i < count; i++) {
            float x = (float) ((mSlotWidth - mGiftBoxes[0].getWidth()) / 2);
            float y = center + (amplitude * (float) Math
                    .sin(2.0 * Math.PI * (double) i / (double) count));
            Bitmap bmp = mGiftBoxes[mRandom.nextInt(mGiftBoxes.length)];
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(bmp);
            iv.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            FrameLayout frame = new FrameLayout(this);
            LayoutParams flp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
            frame.addView(iv, flp);
            iv.setTranslationX(x);
            iv.setTranslationY(y);
            mObstacleLayout.addView(frame, lp);
        }

        // Two spacers at the end.
        view = new View(this);
        mObstacleLayout.addView(view, lp);
        view = new View(this);
        mObstacleLayout.addView(view, lp);

        // Account for rounding errors in mSlotWidth
        int extra = ((3 * mScreenWidth) - (3 * SLOTS_PER_SCREEN * mSlotWidth));
        if (extra > 0) {
            // Add filler to ensure sync with background/foreground scrolls!
            lp = new LinearLayout.LayoutParams(extra, LinearLayout.LayoutParams.MATCH_PARENT);
            view = new View(this);
            mObstacleLayout.addView(view, lp);
        }
    }

    // Pre-populate the random list of obstacles so we can background fetch them.
    private void initObstacles(int[] resources, int stride, ArrayList<Integer> list) {
        // Select 200 obstacles randomly.  This should be enough for each type of obstacles.
        // We will wrap if we need more.
        for (int i = 0; i < 200; i++) {
            list.add(mRandom.nextInt(resources.length / stride));
        }
    }

    // Pre-populate the random list of obstacles and pre-load some of the bitmaps.
    private void initObstaclesAndPreLoad(int[] resources, int stride, TreeMap<Integer, Bitmap> map,
            ArrayList<Integer> list) {
        initObstacles(resources, stride, list);
        // Load the bitmaps for the first 20 obstacles.
        for (int i = 0; i < 20; i++) {
            int obstacle = list.get(i);
            for (int j = (obstacle * stride); j < ((obstacle + 1) * stride); j++) {
                // Check just in case something is wonky
                if (j < resources.length) {
                    int id = resources[j];
                    if (id != -1) {
                        // Only need to load it once...
                        if (!map.containsKey(id)) {
                            Bitmap bmp = BitmapFactory.decodeResource(getResources(), id);
                            if ((mScaleX != 1.0f) || (mScaleY != 1.0f)) {
                                Bitmap tmp = Bitmap.createScaledBitmap(bmp,
                                        (int) ((float) bmp.getWidth() * mScaleX),
                                        (int) ((float) bmp.getHeight() * mScaleY), false);
                                if (tmp != bmp) {
                                    bmp.recycle();
                                }
                                map.put(id, tmp);
                            } else {
                                map.put(id, bmp);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addNextObstacleSpacer(int screens) {
        if (screens > 0) {
            View view = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(mScreenWidth * screens,
                    mScreenHeight);
            mObstacleLayout.addView(view, lp);
            mLastObstacle = 0;
        }
    }

    private void addNextObstacles(int level, int screens) {
        if (level < 2) {
            addWoodObstacles(screens);
        } else if (level < 4) {
            addCaveObstacles(screens);
        } else {
            addFactoryObstacles(screens);
        }
    }

    private void addWoodObstacles(int screens) {
        int totalSlots = screens * SLOTS_PER_SCREEN;
        for (int i = 0; i < totalSlots; ) {
            // Any given "slot" has a 1 in 3 chance of having an obstacle
            if (mRandom.nextInt(3) == 0) {
                View view = mInflater.inflate(R.layout.obstacle_layout, null);
                ImageView top = (ImageView) view.findViewById(R.id.top_view);
                ImageView bottom = (ImageView) view.findViewById(R.id.bottom_view);
                ImageView back = (ImageView) view.findViewById(R.id.back_view);

                // Which obstacle?
                int width = 0;
//				int obstacle = mRandom.nextInt((WOOD_OBSTACLES.length/3));
                if ((mWoodObstacleIndex % 20) == 0) {
                    ObstacleLoadTask task = new ObstacleLoadTask(getResources(),
                            WOOD_OBSTACLES,
                            mWoodObstacles,
                            mWoodObstacleList,
                            mWoodObstacleIndex + 20,
                            3,
                            mScaleX,
                            mScaleY);
                    task.execute();
                }
                int obstacle = mWoodObstacleList.get(mWoodObstacleIndex++);
                if (mWoodObstacleIndex >= mWoodObstacleList.size()) {
                    mWoodObstacleIndex = 0;
                }
                int topIndex = obstacle * 3;
                int bottomIndex = topIndex + 1;
                int backIndex = topIndex + 2;
                if (WOOD_OBSTACLES[backIndex] != -1) {
                    Bitmap bmp = null;
                    synchronized (mWoodObstacles) {
                        bmp = mWoodObstacles.get(WOOD_OBSTACLES[backIndex]);
                    }
                    while (bmp == null) {
                        synchronized (mWoodObstacles) {
                            bmp = mWoodObstacles.get(WOOD_OBSTACLES[backIndex]);
                            if (bmp == null) {
                                try {
                                    mWoodObstacles.wait();
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    }
                    width = bmp.getWidth();
                    back.setImageBitmap(bmp);
                } else {
                    back.setVisibility(View.GONE);
                }

                int currentObstacle = 0;  // Same values as mLastObstacle
                if (WOOD_OBSTACLES[topIndex] != -1) {
                    currentObstacle |= 1;
                    Bitmap bmp = null;
                    synchronized (mWoodObstacles) {
                        bmp = mWoodObstacles.get(WOOD_OBSTACLES[topIndex]);
                    }
                    while (bmp == null) {
                        synchronized (mWoodObstacles) {
                            bmp = mWoodObstacles.get(WOOD_OBSTACLES[topIndex]);
                            if (bmp == null) {
                                try {
                                    mWoodObstacles.wait();
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    }
                    width = bmp.getWidth();
                    top.setImageBitmap(bmp);
                } else {
                    top.setVisibility(View.GONE);
                }

                if (WOOD_OBSTACLES[bottomIndex] != -1) {
                    currentObstacle |= 2;
                    Bitmap bmp = null;
                    synchronized (mWoodObstacles) {
                        bmp = mWoodObstacles.get(WOOD_OBSTACLES[bottomIndex]);
                    }
                    while (bmp == null) {
                        synchronized (mWoodObstacles) {
                            bmp = mWoodObstacles.get(WOOD_OBSTACLES[bottomIndex]);
                            if (bmp == null) {
                                try {
                                    mWoodObstacles.wait();
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    }
                    if (bmp.getWidth() > width) {
                        width = bmp.getWidth();
                    }
                    bottom.setImageBitmap(bmp);
                } else {
                    bottom.setVisibility(View.GONE);
                }
                int slots = (width / mSlotWidth) + 2;

                // If last obstacle had a top and this is a bottom or vice versa, insert a space
                if ((mLastObstacle & 0x1) > 0) {
                    if ((currentObstacle & 0x2) > 0) {
                        addSpaceOrPresent(mSlotWidth);
                        i++;
                    }
                } else if ((mLastObstacle & 0x2) > 0) {
                    if ((currentObstacle & 0x1) > 0) {
                        addSpaceOrPresent(mSlotWidth);
                        i++;
                    }
                }

                // If the new obstacle is too wide for the remaining space, skip it and fill spacer instead
                if ((i + slots) > totalSlots) {
                    addSpaceOrPresent(mSlotWidth * (totalSlots - i));
                    i = totalSlots;
                } else {
                    mLastObstacle = currentObstacle;
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(slots * mSlotWidth,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    view.setLayoutParams(lp);
                    mObstacleLayout.addView(view);
                    i += slots;
                }
            } else {
                addSpaceOrPresent(mSlotWidth);
                i++;
            }
        }

        // Account for rounding errors in mSlotWidth
        int extra = ((screens * mScreenWidth) - (totalSlots * mSlotWidth));
        if (extra > 0) {
            // Add filler to ensure sync with background/foreground scrolls!
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(extra,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            View view = new View(this);
            mObstacleLayout.addView(view, lp);
        }
    }

    private void addCaveObstacles(int screens) {
        int totalSlots = screens * SLOTS_PER_SCREEN;
        for (int i = 0; i < totalSlots; ) {
            // Any given "slot" has a 1 in 3 chance of having an obstacle
            if (mRandom.nextInt(2) == 0) {
                View view = mInflater.inflate(R.layout.obstacle_layout, null);
                ImageView top = (ImageView) view.findViewById(R.id.top_view);
                ImageView bottom = (ImageView) view.findViewById(R.id.bottom_view);
                ImageView back = (ImageView) view.findViewById(R.id.back_view);

                // Which obstacle?
                int width = 0;
                if ((mCaveObstacleIndex % 20) == 0) {
                    ObstacleLoadTask task = new ObstacleLoadTask(getResources(),
                            CAVE_OBSTACLES,
                            mCaveObstacles,
                            mCaveObstacleList,
                            mCaveObstacleIndex + 20,
                            2,
                            mScaleX,
                            mScaleY);
                    task.execute();
                }
                int obstacle = mCaveObstacleList.get(mCaveObstacleIndex++);
                if (mCaveObstacleIndex >= mCaveObstacleList.size()) {
                    mCaveObstacleIndex = 0;
                }
//                int obstacle = mRandom.nextInt((CAVE_OBSTACLES.length/2));
                int topIndex = obstacle * 2;
                int bottomIndex = topIndex + 1;
                back.setVisibility(View.GONE);

                int currentObstacle = 0;  // Same values as mLastObstacle
                if (CAVE_OBSTACLES[topIndex] != -1) {
                    currentObstacle |= 1;
                    Bitmap bmp = null;
                    synchronized (mCaveObstacles) {
                        bmp = mCaveObstacles.get(CAVE_OBSTACLES[topIndex]);
                    }
                    while (bmp == null) {
                        synchronized (mCaveObstacles) {
                            bmp = mCaveObstacles.get(CAVE_OBSTACLES[topIndex]);
                            if (bmp == null) {
                                try {
                                    mCaveObstacles.wait();
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    }
                    width = bmp.getWidth();
                    top.setImageBitmap(bmp);
                } else {
                    top.setVisibility(View.GONE);
                }

                if (CAVE_OBSTACLES[bottomIndex] != -1) {
                    currentObstacle |= 2;
                    Bitmap bmp = null;
                    synchronized (mCaveObstacles) {
                        bmp = mCaveObstacles.get(CAVE_OBSTACLES[bottomIndex]);
                    }
                    while (bmp == null) {
                        synchronized (mCaveObstacles) {
                            bmp = mCaveObstacles.get(CAVE_OBSTACLES[bottomIndex]);
                            if (bmp == null) {
                                try {
                                    mCaveObstacles.wait();
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    }
                    if (bmp.getWidth() > width) {
                        width = bmp.getWidth();
                    }
                    bottom.setImageBitmap(bmp);
                    if (CAVE_OBSTACLES[bottomIndex] == R.drawable.img_mammoth) {
                        // Special case...
                        bottom.setTag(true);
                    }
                } else {
                    bottom.setVisibility(View.GONE);
                }
                int slots = (width / mSlotWidth);
                slots += 2;

                // If last obstacle had a top and this is a bottom or vice versa, insert a space
                if ((mLastObstacle & 0x1) > 0) {
                    if ((currentObstacle & 0x2) > 0) {
                        addSpaceOrPresent(mSlotWidth);
                        i++;
                    }
                } else if ((mLastObstacle & 0x2) > 0) {
                    if ((currentObstacle & 0x1) > 0) {
                        addSpaceOrPresent(mSlotWidth);
                        i++;
                    }
                }

                // If the new obstacle is too wide for the remaining space, skip it and fill spacer instead
                if ((i + slots) > totalSlots) {
                    addSpaceOrPresent(mSlotWidth * (totalSlots - i));
                    i = totalSlots;
                } else {
                    mLastObstacle = currentObstacle;
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(slots * mSlotWidth,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    view.setLayoutParams(lp);
                    mObstacleLayout.addView(view);
                    i += slots;
                }
            } else {
                addSpaceOrPresent(mSlotWidth);
                i++;
            }
        }

        // Account for rounding errors in mSlotWidth
        int extra = ((screens * mScreenWidth) - (totalSlots * mSlotWidth));
        if (extra > 0) {
            // Add filler to ensure sync with background/foreground scrolls!
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(extra,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            View view = new View(this);
            mObstacleLayout.addView(view, lp);
        }
    }

    private void addFactoryObstacles(int screens) {
        int totalSlots = screens * SLOTS_PER_SCREEN;
        for (int i = 0; i < totalSlots; ) {
            // Any given "slot" has a 1 in 3 chance of having an obstacle
            if (mRandom.nextInt(2) == 0) {
                View view = mInflater.inflate(R.layout.obstacle_layout, null);
                ImageView top = (ImageView) view.findViewById(R.id.top_view);
                ImageView bottom = (ImageView) view.findViewById(R.id.bottom_view);
                ImageView back = (ImageView) view.findViewById(R.id.back_view);

                // Which obstacle?
                int width = 0;
//                int obstacle = mRandom.nextInt((FACTORY_OBSTACLES.length/2));
                if ((mFactoryObstacleIndex % 20) == 0) {
                    ObstacleLoadTask task = new ObstacleLoadTask(getResources(),
                            FACTORY_OBSTACLES,
                            mFactoryObstacles,
                            mFactoryObstacleList,
                            mFactoryObstacleIndex + 20,
                            2,
                            mScaleX,
                            mScaleY);
                    task.execute();
                }
                int obstacle = mFactoryObstacleList.get(mFactoryObstacleIndex++);
                if (mFactoryObstacleIndex >= mFactoryObstacleList.size()) {
                    mFactoryObstacleIndex = 0;
                }
                int topIndex = obstacle * 2;
                int bottomIndex = topIndex + 1;
                back.setVisibility(View.GONE);

                int currentObstacle = 0;  // Same values as mLastObstacle
                if (FACTORY_OBSTACLES[topIndex] != -1) {
                    currentObstacle |= 1;
                    Bitmap bmp = null;
                    synchronized (mFactoryObstacles) {
                        bmp = mFactoryObstacles.get(FACTORY_OBSTACLES[topIndex]);
                    }
                    while (bmp == null) {
                        synchronized (mFactoryObstacles) {
                            bmp = mFactoryObstacles.get(FACTORY_OBSTACLES[topIndex]);
                            if (bmp == null) {
                                try {
                                    mFactoryObstacles.wait();
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    }
                    width = bmp.getWidth();
                    top.setImageBitmap(bmp);
                } else {
                    top.setVisibility(View.GONE);
                }

                if (FACTORY_OBSTACLES[bottomIndex] != -1) {
                    currentObstacle |= 2;
                    Bitmap bmp = null;
                    synchronized (mFactoryObstacles) {
                        bmp = mFactoryObstacles.get(FACTORY_OBSTACLES[bottomIndex]);
                    }
                    while (bmp == null) {
                        synchronized (mFactoryObstacles) {
                            bmp = mFactoryObstacles.get(FACTORY_OBSTACLES[bottomIndex]);
                            if (bmp == null) {
                                try {
                                    mFactoryObstacles.wait();
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    }
                    if (bmp.getWidth() > width) {
                        width = bmp.getWidth();
                    }
                    bottom.setImageBitmap(bmp);
                } else {
                    bottom.setVisibility(View.GONE);
                }
                int slots = (width / mSlotWidth);
                if ((width % mSlotWidth) != 0) {
                    slots++;
                }

                // If last obstacle had a top and this is a bottom or vice versa, insert a space
                if ((mLastObstacle & 0x1) > 0) {
                    if ((currentObstacle & 0x2) > 0) {
                        addSpaceOrPresent(mSlotWidth);
                        i++;
                    }
                } else if ((mLastObstacle & 0x2) > 0) {
                    if ((currentObstacle & 0x1) > 0) {
                        addSpaceOrPresent(mSlotWidth);
                        i++;
                    }
                }

                // If the new obstacle is too wide for the remaining space, skip it and fill spacer instead
                if ((i + slots) > totalSlots) {
                    addSpaceOrPresent(mSlotWidth * (totalSlots - i));
                    i = totalSlots;
                } else {
                    mLastObstacle = currentObstacle;
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(slots * mSlotWidth,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    view.setLayoutParams(lp);
                    mObstacleLayout.addView(view);
                    i += slots;
                }
            } else {
                addSpaceOrPresent(mSlotWidth);
                i++;
            }
        }

        // Account for rounding errors in mSlotWidth
        int extra = ((screens * mScreenWidth) - (totalSlots * mSlotWidth));
        if (extra > 0) {
            // Add filler to ensure sync with background/foreground scrolls!
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(extra,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            View view = new View(this);
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mObstacleLayout.addView(view, lp);
        }
    }

    private void addSpaceOrPresent(int width) {
        if (width > 0) {
            mLastObstacle = 0;
            // 1/3 chance of a present.
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            if (mRandom.nextInt(3) == 0) {
                // Present!

                // Which one?
                Bitmap bmp = mGiftBoxes[mRandom.nextInt(mGiftBoxes.length)];
                ImageView iv = new ImageView(this);
                iv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                iv.setImageBitmap(bmp);

                // Position the present
                int left = mRandom.nextInt(width / 2) + (width / 4) - (
                        (int) ((float) bmp.getWidth() * mScaleX) / 2);
                int top = mRandom.nextInt(mScreenHeight / 2) + (mScreenHeight / 4) - (
                        (int) ((float) bmp.getHeight() * mScaleY) / 2);

                FrameLayout frame = new FrameLayout(this);
                LayoutParams flp = new LayoutParams(LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT);
                frame.addView(iv, flp);
                iv.setTranslationX(left);
                iv.setTranslationY(top);

                mObstacleLayout.addView(frame, lp);
            } else {
                // Space
                View view = new View(this);
                mObstacleLayout.addView(view, lp);
            }
        }
    }

    private void addNextImages(int level) {
        addNextImages(level, false);
    }

    private void addNextImages(int level, boolean recycle) {
        if (level < BACKGROUNDS.length) {
            // Add the background image
            ImageView iv = new ImageView(this);
            iv.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            // This is being background loaded.  Should already be loaded, but if not, wait.
            while (mBackgrounds[level] == null) {
                synchronized (mBackgrounds) {
                    if (mBackgrounds[level] == null) {
                        try {
                            mBackgrounds.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
            iv.setImageBitmap(mBackgrounds[level]);
            if (recycle) {
                iv.setTag(new Pair<Integer, Integer>(0, level));
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            mBackgroundLayout.addView(iv, lp);
            iv = new ImageView(this);
            iv.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            if (recycle) {
                iv.setTag(new Pair<Integer, Integer>(0, level));
            }
            iv.setImageBitmap(mBackgrounds2[level]);
            mBackgroundLayout.addView(iv, lp);

            // Add the foreground image
            if (FOREGROUNDS[level] == -1) {
                View view = new View(this);
                lp = new LinearLayout.LayoutParams(mScreenWidth * 2, 10);
                mForegroundLayout.addView(view, lp);
            } else {
                iv = new ImageView(this);
                iv.setBackgroundResource(R.drawable.img_snow_ground_tiles);
                if (recycle) {
                    iv.setTag(level);
                }
                lp = new LinearLayout.LayoutParams(mScreenWidth * 2,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                mForegroundLayout.addView(iv, lp);
                iv = new ImageView(this);
                if (recycle) {
                    iv.setTag(level);
                }
                iv.setBackgroundResource(R.drawable.img_snow_ground_tiles);
                mForegroundLayout.addView(iv, lp);
            }
        }
    }

    // This is the level we are moving TO.
    private void addNextTransitionImages(int level) {
        mTransitionImagesCount = 0;
        if ((level > 0) && ((level - 1) < EXIT_TRANSITIONS.length)) {
            if (EXIT_TRANSITIONS[level - 1] != -1) {
                // Add the exit transition image
                ImageView iv = new ImageView(this);
                iv.setTag(new Pair<Integer, Integer>(1, (level - 1)));
                // This is being background loaded.  Should already be loaded, but if not, wait.
                while (mExitTransitions[level - 1] == null) {
                    synchronized (mExitTransitions) {
                        if (mExitTransitions[level - 1] == null) {
                            try {
                                mExitTransitions.wait();
                            } catch (InterruptedException e) {
                                continue;
                            }
                        }
                    }
                }
                iv.setImageBitmap(mExitTransitions[level - 1]);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                mBackgroundLayout.addView(iv, lp);

                // No foreground on transistions.  Transition images are a single screen long
                View view = new View(this);
                lp = new LinearLayout.LayoutParams(mScreenWidth, 10);
                mForegroundLayout.addView(view, lp);
                mTransitionImagesCount++;
            }
        }
        if ((level > 0) && (level < ENTRY_TRANSITIONS.length)) {
            if (ENTRY_TRANSITIONS[level] != -1) {
                // Add the exit transition image
                ImageView iv = new ImageView(this);
                iv.setTag(new Pair<Integer, Integer>(2, level));
                // This is being background loaded.  Should already be loaded, but if not, wait.
                while (mEntryTransitions[level] == null) {
                    synchronized (mEntryTransitions) {
                        if (mEntryTransitions[level] == null) {
                            try {
                                mEntryTransitions.wait();
                            } catch (InterruptedException e) {
                                continue;
                            }
                        }
                    }
                }
                iv.setImageBitmap(mEntryTransitions[level]);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                mBackgroundLayout.addView(iv, lp);
                // No foreground on transistions.  Transition images are a single screen long
                View view = new View(this);
                lp = new LinearLayout.LayoutParams(mScreenWidth, 10);
                mForegroundLayout.addView(view, lp);
                mTransitionImagesCount++;
            }
        }
    }

    private void addFinalImages() {
        addNextImages(1);
        addNextImages(1);

        // Add presents
        addFinalPresentRun();

        // Add final screen.  This is a two screen background.
        ImageView iv = new ImageView(this);
        iv.setTag(true);
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.bg_finish);
        if ((mScaleX != 1.0f) || (mScaleY != 1.0f)) {
            Bitmap tmp = Bitmap.createScaledBitmap(bmp, mScreenWidth * 2, mScreenHeight, false);
            if (bmp != tmp) {
                bmp.recycle();
            }
            bmp = tmp;
        }
        iv.setImageBitmap(bmp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mBackgroundLayout.addView(iv, lp);
        View view = new View(this);
        lp = new LinearLayout.LayoutParams(mScreenWidth * 2, 10);
        mForegroundLayout.addView(view, lp);
        addNextObstacleSpacer(2);
    }

    // Load the level 1 images right now since we need to display them.
    // Load the rest on a thread.
    // We preload all of these because the transitions can be quick.
    // They are not very big, relatively speaking.
    private void loadElfImages() {
        mElfImages = new Bitmap[ELF_IMAGES.length];
        mElfHitImages = new Bitmap[ELF_HIT_IMAGES.length];
        mElfBurnImages = new Bitmap[ELF_BURN_IMAGES.length];
        mElfThrustImages = new Bitmap[ELF_THRUST_IMAGES.length];
        mElfSmokeImages = new Bitmap[ELF_SMOKE_IMAGES.length];
        mElfImages[0] = BitmapFactory.decodeResource(getResources(), ELF_IMAGES[0]);
        mElfHitImages[0] = BitmapFactory.decodeResource(getResources(), ELF_HIT_IMAGES[0]);
        mElfBurnImages[0] = BitmapFactory.decodeResource(getResources(), ELF_BURN_IMAGES[0]);
        mElfThrustImages[0] = BitmapFactory.decodeResource(getResources(), ELF_THRUST_IMAGES[0]);
        mElfSmokeImages[0] = BitmapFactory.decodeResource(getResources(), ELF_SMOKE_IMAGES[0]);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 1; i < ELF_IMAGES.length; i++) {
                    mElfImages[i] = BitmapFactory.decodeResource(getResources(), ELF_IMAGES[i]);
                }
                for (int i = 1; i < ELF_HIT_IMAGES.length; i++) {
                    mElfHitImages[i] = BitmapFactory
                            .decodeResource(getResources(), ELF_HIT_IMAGES[i]);
                }
                for (int i = 1; i < ELF_BURN_IMAGES.length; i++) {
                    mElfBurnImages[i] = BitmapFactory
                            .decodeResource(getResources(), ELF_BURN_IMAGES[i]);
                }
                for (int i = 1; i < ELF_THRUST_IMAGES.length; i++) {
                    mElfThrustImages[i] = BitmapFactory
                            .decodeResource(getResources(), ELF_THRUST_IMAGES[i]);
                }
                for (int i = 1; i < ELF_SMOKE_IMAGES.length; i++) {
                    mElfSmokeImages[i] = BitmapFactory
                            .decodeResource(getResources(), ELF_SMOKE_IMAGES[i]);
                }
            }
        });
        thread.start();
    }

    private void updateElf(boolean hit) {
        float thrustWidth = 0.0f;
        if (hit) {
            // Just update the elf drawable
            mElf.setImageDrawable(null);
            mElfBitmap.recycle();
            mElfBitmap = mElfHitImages[mElfState];
            mElf.setImageBitmap(mElfBitmap);
            updateElfThrust(2);
            thrustWidth = (float) mCurrentTrailBitmap.getWidth() * mElfScale;
        } else {
            // New state for elf recycle, reload and reset.
            mElf.setImageDrawable(null);
            if (mElfBitmap != null) {
                mElfBitmap.recycle();
            }
            mThrust.setImageDrawable(null);
            if (mBurnBitmap != null) {
                mBurnBitmap.recycle();
            }
            if (mThrustBitmap != null) {
                mThrustBitmap.recycle();
            }
            if (mSmokeBitmpap != null) {
                mSmokeBitmpap.recycle();
            }
            if (mElfState < 4) {
                mBurnBitmap = mElfBurnImages[mElfState];
                mThrustBitmap = mElfThrustImages[mElfState];
                mSmokeBitmpap = mElfSmokeImages[mElfState];
                mElfBitmap = mElfImages[mElfState];
                if (mElfAccelY > 0.0f) {
                    updateElfThrust(1);
                } else {
                    updateElfThrust(0);
                }
                thrustWidth = (float) mCurrentTrailBitmap.getWidth() * mElfScale;
            } else {
                mElfBitmap = mElfImages[4];
                mThrust.setVisibility(View.GONE);
            }
            mElf.setImageBitmap(mElfBitmap);
        }
        float offset = thrustWidth + ((float) mElfBitmap.getWidth() / 2.0f);
        mElfLayout.setX(mElfPosX - offset);
        mElfLayout.setY(mElfPosY);
        mElfLayout.setPivotX(offset);
        mElfLayout.setPivotY((float) mElfBitmap.getHeight() * 3.0f / 4.0f);
        float rot = (float) (Math.atan(mElfVelY / mElfVelX) * 120.0 / Math.PI);
        mElfLayout.setRotation(rot);
        mElfLayout.invalidate();
    }

    // 0 - burn, 1 - thrust, 2 - smoke, 3 - gone
    private void updateElfThrust(int type) {
        switch (type) {
            case 0:
                mCurrentTrailBitmap = mBurnBitmap;
                break;
            case 1:
                mCurrentTrailBitmap = mThrustBitmap;
                break;
            case 2:
                mCurrentTrailBitmap = mSmokeBitmpap;
                break;
            case 3:
            default:
                mCurrentTrailBitmap = null;
                break;
        }
        if (mCurrentTrailBitmap != null) {
            mThrust.setImageBitmap(mCurrentTrailBitmap);
        }
    }

    private void pause() {

        if (mIsPlaying) {
            mBigPlayButtonLayout.setVisibility(View.VISIBLE);
            if (!mIsTv) {
                mExit.setVisibility(View.VISIBLE);
            }
        }

        mIsPlaying = false;
        mHandler.removeCallbacks(mGameLoop);
        mControlView.setOnTouchListener(null);
        mPlayPauseButton.setImageResource(R.drawable.play_button_jp);

        if (mJetThrustStream > 0) {
            mSoundPool.stop(mJetThrustStream);
            mJetThrustStream = 0;
        }
        if (mBackgroundPlayer != null) {
            mBackgroundPlayer.pause();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ImmersiveModeHelper.setImmersiveStickyWithActionBar(getWindow());
        }
    }

    private void play() {
        mIsPlaying = true;
        mBigPlayButtonLayout.setVisibility(View.GONE);
        mLastTime = System.currentTimeMillis();
        mControlView.setOnTouchListener(this);
        mHandler.post(mGameLoop);
        mPlayPauseButton.setImageResource(R.drawable.pause_button_jp);
        mExit.setVisibility(View.GONE);
        if (mBackgroundPlayer != null) {
            mBackgroundPlayer.start();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ImmersiveModeHelper.setImmersiveSticky(getWindow());
        }
    }

    private void endGame() {
        mIsPlaying = false;
        Intent intent = new Intent(this.getApplicationContext(), EndGameActivity.class);
        intent.putExtra("score", (long) (mScore / 10));

        AnalyticsManager.sendEvent(getString(R.string.analytics_screen_rocket),
                getString(R.string.analytics_action_rocket_final_score), null, mScore / 10);

        if (mCleanLevel) {
            intent.putExtra(getString(R.string.achievement_safe_tapper), true);
        }
        if (!mHit) {
            intent.putExtra(getString(R.string.achievement_untouchable), true);
        }
        if (mPresentBonus) {
            intent.putExtra(getString(R.string.achievement_hidden_presents), true);
        }
        if ((mScore / 10) > 10000) {
            intent.putExtra(getString(R.string.achievement_rocket_junior_score_10000), true);
        }
        if ((mScore / 10) > 30000) {
            intent.putExtra(getString(R.string.achievement_rocket_intermediate_score_30000), true);
        }
        if ((mScore / 10) > 50000) {
            intent.putExtra(getString(R.string.achievement_rocket_pro_score_50000), true);
        }
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void releaseIntegerBitmapMap(Map<Integer, Bitmap> map) {

        Iterator<Map.Entry<Integer, Bitmap>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Bitmap> entry = it.next();
            Bitmap bitmap = entry.getValue();
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    private void releaseBitmapArray(Bitmap[] bitmapArray) {
        if (bitmapArray != null) {
            for (Bitmap bitmap : bitmapArray) {
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        }
    }
}
