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

package com.google.android.apps.santatracker.games.gumball;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.games.common.PlayGamesActivity;
import com.google.android.apps.santatracker.games.matching.CircleView;
import com.google.android.apps.santatracker.games.matching.LevelTextView;
import com.google.android.apps.santatracker.games.matching.MatchingGameConstants;
import com.google.android.apps.santatracker.invites.AppInvitesFragment;
import com.google.android.apps.santatracker.util.ImmersiveModeHelper;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.contacts.Contact;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Gumball game fragment.
 */
public class TiltGameFragment extends Fragment implements
        SensorEventListener, ContactListener, AnimationListener,
        OnClickListener {

    /**
     * Bounce rate of objects in the physics world.
     */
    public static final float WORLD_OBJECT_BOUNCE = 0.2f;
    /**
     * Density of objects in the physics world.
     */
    public static final float WORLD_OBJECT_DENSITY = 185.77f;
    /**
     * Friction of objects in the physics world.
     */
    public static final float WORLD_OBJECT_FRICTION = 0.2f;
    /**
     * Friction of floor objects in the physics world.
     */
    public static final float WORLD_FLOOR_FRICTION = 0.8f;
    /**
     * Initial X position of the floor and pipes in the physics world.
     */
    public static final float WORLD_FLOOR_X = 3.37f;
    /*
     * Initial Y position of the floor and pipes in the physics world.
     */
    public static final float WORLD_FLOOR_Y = 0f;

    /** View that contains the main game. */
    private TiltGameView mGameView;

    /**
     * Box2D physics world for this game.
     */
    private PhysicsWorld mWorld;

    /**
     * Current rotation of the device. Used to adjust sensor readings if the screen is rotate in
     * portrait or landscape.
     *
     * @see android.view.Display#getRotation()
     */
    private int mRotation;

    /**
     * Main game thread.
     */
    private Runnable mGameThread;

    /**
     * Previous value of the sensor's Y reading. Used to calculate the rotational offset between
     * sensor events.
     */
    private float mPreviousSensorY = 0f;

    /**
     * MediaPlayer that plays the background music.
     */
    private MediaPlayer mBackgroundMusic;

    /**
     * Index of loaded sound effect in sound pool for small bounce.
     */
    private int mSoundBounceSmall = -1;

    /**
     * Index of loaded sound effect in sound pool for medium bounce.
     */
    private int mSoundBounceMed = -1;

    /**
     * Index of loaded sound effect in sound pool for large bounce.
     */
    private int mSoundBounceLarge = -1;

    /**
     * Index of loaded sound effect in sound pool for ball in machine.
     */
    private int mSoundBallInMachine = -1;

    /**
     * Index of loaded sound effect in sound pool for failed ball.
     */
    private int mSoundBallFail = -1;

    /**
     * Index of loaded sound effect in sound pool for dropped ball.
     */
    private int mSoundBallDrop = -1;

    /**
     * Index of loaded sound effect in sound pool for game over.
     */
    private int mSoundGameOver = -1;

    /**
     * Scale down animation for level.
     */
    private Animation mAnimationScaleLevelDown;

    /**
     * Fading out animation for level.
     */
    private Animation mAnimationLevelFadeOut;

    /**
     * Scaling up animation for level.
     */
    private Animation mAnimationLevelScaleUp;

    /**
     * Outlet animation for balls.
     */
    private Animation mAnimationOutlet;

    /**
     * Alpha animation for timer updates.
     */
    private Animation mAnimationTimerAlpha;

    /**
     * View for end of level circle overlay.
     */
    private CircleView mEndLevelCircle;

    /**
     * View that shows the current level number.
     */
    private LevelTextView mLevelNumberText;

    /**
     * Sound pool from which all sounds are played back.
     */
    private SoundPool mSoundPool;

    /**
     * Holder for sound pool id to handle playbacks, connects and disconnects.
     */
    private final HashMap<UUID, Boolean> mSoundPoolId = new HashMap<>();

    /**
     * Number of balls left in the game.
     */
    private int mGameBallsLeft = 2;

    /**
     * Current play level. Zero indexed, first level is 0.
     */
    private int mCurrentLevelNum = 0;

    /**
     * View for the ball outlet at the top of the screen.
     */
    private View mGameOutlet;

    /**
     * Root view of the game layout.
     */
    private View mRootView;

    /**
     * Gumballs that are queued to be dropped through the outlet.
     */
    private Queue<Gumball> mGumballQueue;

    /**
     * The current, active gumball on screen.
     */
    private Gumball mCurrentGumball;

    /**
     * X position of outlet in the last animation.
     */
    private float mOutletPreviousXPos = 0;

    /**
     * Array of the ball indicator views at the bottom of the screen.
     */
    private ImageView mViewIndicators[] = new ImageView[6];

    /**
     * Number of gumballs collected in the current game.
     */
    private int mNumberCollected = 0;

    /**
     * Refresh rate for the game countdown timer.
     *
     * @see com.google.android.apps.santatracker.games.gumball.TiltGameFragment.GameCountdown
     */
    private int mFramesPerSecond = 60;

    /**
     * Time left in the current game. Value in milliseconds.
     */
    private long mTimeLeftInMillis = MatchingGameConstants.GUMBALL_INIT_TIME;

    /**
     * Countdown timer for the current game.
     */
    private GameCountdown mCountDownTimer = null;

    /**
     * Countdown timer text.
     */
    private TextView mViewCountdown;

    /**
     * Score text.
     */
    private TextView mViewScore;

    /**
     * Total score of current game.
     */
    private int mMatchScore = 0;

    /**
     * Number of balls that have respawned in the current level. Used to calculate the total
     * game score.
     */
    private int mCountLevelBallRespawns = 0;

    /**
     * Flag indicating if the game is paused.
     */
    private boolean wasPaused = false;

    private ImageView mViewPlayButton;

    private ImageView mViewPauseButton;

    private ImageButton mViewBigPlayButton;

    private ImageView mViewCancelBar;

    private ImageView mViewInviteButton;

    private View mViewMatchPauseOverlay;

    private View mViewPlayAgainBackground;

    private View mViewPlayAgainMain;

    private Button mViewPlayAgainButton;

    private TextView mViewPlayAgainScore;

    private TextView mViewPlayAgainLevel;

    private Animation mAnimationPlayAgainBackground;

    private Animation mAnimationPlayAgainMain;

    /**
     * Display offset on X axis for outlet in pixels.
     */
    private int mOutletOffset;

    /**
     * View that displays the instructions from {@link #mDrawableTransition}
     */
    private ImageView mViewInstructions;

    /**
     * Drawable that contains all images for the instructions.
     */
    private AnimationDrawable mDrawableTransition;

    private SharedPreferences mSharedPreferences;

    private ImageView mViewGPlusSignIn;

    private View mViewGPlusLayout;

    private ImageButton mViewMainMenuButton;

    private AppInvitesFragment mInvitesFragment;

    /**
     * Gets an instance of this fragment
     */
    public static TiltGameFragment newInstance() {
        TiltGameFragment fragment = new TiltGameFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_gumball, container, false);
        mRootView.setKeepScreenOn(true);

        // Use a lower resolution background image to conserve memory below ICS
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            View matchScoreLayout = mRootView.findViewById(R.id.tilt_score_layout);
            matchScoreLayout.setBackgroundResource(R.drawable.score_background_gingerbread);
        }

        mViewPlayAgainScore = (TextView) mRootView.findViewById(R.id.play_again_score);
        mViewPlayAgainScore.setText(String.valueOf(mMatchScore));
        mViewPlayAgainLevel = (TextView) mRootView.findViewById(R.id.play_again_level);
        mViewPlayAgainLevel.setText(String.valueOf(mCurrentLevelNum));
        mViewPlayAgainBackground = mRootView.findViewById(R.id.play_again_bkgrd);
        mViewPlayAgainMain = mRootView.findViewById(R.id.play_again_main);
        mViewPlayAgainButton = (Button) mRootView.findViewById(R.id.play_again_btn);
        mViewPlayAgainButton.setOnClickListener(this);

        mViewGPlusSignIn = (ImageView) mRootView.findViewById(R.id.gplus_button);
        mViewGPlusSignIn.setOnClickListener(this);
        mViewGPlusLayout = mRootView.findViewById(R.id.play_again_gplus);
        mViewGPlusLayout.setVisibility(View.GONE);

        // Initialise all animations
        // Construct an animation to blink the timer indefinitely
        mAnimationTimerAlpha = new AlphaAnimation(0.0f, 1.0f);
        mAnimationTimerAlpha.setDuration(1000);
        mAnimationTimerAlpha.setRepeatMode(Animation.REVERSE);
        mAnimationTimerAlpha.setRepeatCount(Animation.INFINITE);

        // Load all other animations
        mAnimationPlayAgainBackground = AnimationUtils
                .loadAnimation(getActivity(), R.anim.play_again_bkgrd_anim);
        mAnimationPlayAgainBackground.setFillAfter(true);
        mAnimationPlayAgainBackground.setAnimationListener(this);
        mAnimationPlayAgainMain = AnimationUtils
                .loadAnimation(getActivity(), R.anim.play_again_main_anim);
        mAnimationPlayAgainMain.setFillAfter(true);
        mAnimationPlayAgainMain.setAnimationListener(this);
        mAnimationScaleLevelDown = AnimationUtils
                .loadAnimation(getActivity(), R.anim.scale_level_anim_down);
        mAnimationScaleLevelDown.setAnimationListener(this);
        mAnimationLevelFadeOut = AnimationUtils
                .loadAnimation(getActivity(), R.anim.level_fade_out_anim);
        mAnimationLevelFadeOut.setAnimationListener(this);
        mAnimationLevelScaleUp = AnimationUtils
                .loadAnimation(getActivity(), R.anim.scale_up_level_anim);
        mAnimationLevelScaleUp.setAnimationListener(this);

        mViewMainMenuButton = (ImageButton) mRootView.findViewById(R.id.main_menu_button);
        mViewMainMenuButton.setVisibility(View.GONE);
        mViewMainMenuButton.setOnClickListener(this);

        // App Invites Button
        mViewInviteButton = (ImageView) mRootView.findViewById(R.id.invite_button);
        mViewInviteButton.setVisibility(View.GONE);
        mViewInviteButton.setOnClickListener(this);

        mGameOutlet = mRootView.findViewById(R.id.tiltGameOutlet);
        mOutletOffset = getResources().getInteger(R.integer.outlet_offset);

        mViewIndicators[0] = (ImageView) mRootView.findViewById(R.id.indicator1);
        mViewIndicators[1] = (ImageView) mRootView.findViewById(R.id.indicator2);
        mViewIndicators[2] = (ImageView) mRootView.findViewById(R.id.indicator3);
        mViewIndicators[3] = (ImageView) mRootView.findViewById(R.id.indicator4);
        mViewIndicators[4] = (ImageView) mRootView.findViewById(R.id.indicator5);
        mViewIndicators[5] = (ImageView) mRootView.findViewById(R.id.indicator6);
        mViewCountdown = (TextView) mRootView.findViewById(R.id.tiltTimer);

        mLevelNumberText = (LevelTextView) mRootView.findViewById(R.id.tilt_end_level_number);
        mLevelNumberText.setVisibility(View.GONE);
        mEndLevelCircle = (CircleView) mRootView.findViewById(R.id.tilt_end_level_circle);
        mEndLevelCircle.setVisibility(View.GONE);

        mViewPlayButton = (ImageView) mRootView.findViewById(R.id.tilt_play_button);
        mViewPlayButton.setOnClickListener(this);
        mViewPlayButton.setVisibility(View.GONE);
        mViewPauseButton = (ImageView) mRootView.findViewById(R.id.tilt_pause_button);
        mViewPauseButton.setOnClickListener(this);
        mViewPauseButton.setVisibility(View.VISIBLE);
        mViewMatchPauseOverlay = mRootView.findViewById(R.id.tilt_pause_overlay);
        mViewMatchPauseOverlay.setVisibility(View.GONE);
        mViewBigPlayButton = (ImageButton) mRootView.findViewById(R.id.tilt_big_play_button);
        mViewBigPlayButton.setOnClickListener(this);
        mViewCancelBar = (ImageView) mRootView.findViewById(R.id.tilt_cancel_bar);
        mViewCancelBar.setOnClickListener(this);
        mViewCancelBar.setVisibility(View.GONE);

        mViewScore = (TextView) mRootView.findViewById(R.id.tilt_score);
        mViewScore.setText(String.valueOf(mMatchScore));

        mGameView = (TiltGameView) mRootView.findViewById(R.id.tiltGameView);

        // Create the Box2D physics world.
        mWorld = new PhysicsWorld();
        Vec2 gravity = new Vec2(0.0f, 0.0f);
        mWorld.create(gravity);
        mGameView.setModel(mWorld);
        mWorld.getWorld().setContactListener(this);

        mGumballQueue = new LinkedList<>();

        // Initialise the sound pool and audio playback
        mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        mSoundBounceSmall = mSoundPool.load(getActivity(), R.raw.gbg_ball_bounce_1, 1);
        mSoundBounceMed = mSoundPool.load(getActivity(), R.raw.gbg_ball_bounce_2, 1);
        mSoundBounceLarge = mSoundPool.load(getActivity(), R.raw.gbg_ball_bounce_3, 1);
        mSoundBallInMachine = mSoundPool.load(getActivity(), R.raw.gbg_ball_into_machine, 1);
        mSoundBallFail = mSoundPool.load(getActivity(), R.raw.gbg_ball_fall_out, 1);
        mSoundBallDrop = mSoundPool.load(getActivity(), R.raw.gbg_new_ball_bounce_drop, 1);
        mSoundGameOver = mSoundPool.load(getActivity(), R.raw.gameover, 1);


        // Display the instructions if they haven't been seen before
        mSharedPreferences = getActivity().getSharedPreferences(MatchingGameConstants.PREFERENCES_FILENAME, getActivity().MODE_PRIVATE);
        if (!mSharedPreferences.getBoolean(MatchingGameConstants.GUMBALL_INSTRUCTIONS_VIEWED, false)) {
            mDrawableTransition = new AnimationDrawable();

            mDrawableTransition.addFrame(VectorDrawableCompat.create(getResources(), R.drawable.instructions_shake_1, null), 300);
            mDrawableTransition.addFrame(VectorDrawableCompat.create(getResources(), R.drawable.instructions_shake_2, null), 300);
            mDrawableTransition.addFrame(VectorDrawableCompat.create(getResources(), R.drawable.instructions_shake_3, null), 300);
            mDrawableTransition.setOneShot(false);
            mViewInstructions = (ImageView) mRootView.findViewById(R.id.instructions);

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mViewInstructions.setImageResource(R.drawable.instructions_shake_1);
            } else {
                mViewInstructions.setImageDrawable(mDrawableTransition);
                mViewInstructions.post(new Runnable() {
                    public void run() {
                        mDrawableTransition.start();
                    }
                });
            }

            // Hide the instructions after 2 seconds
            mViewInstructions.postDelayed(new HideInstructionsRunnable(), 2200);
        }
        
        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mInvitesFragment = AppInvitesFragment.getInstance(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        // Resume the game play if the game was not paused
        if (!wasPaused) {
            mRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            SensorManager sensorManager = (SensorManager) getActivity()
                    .getSystemService(Activity.SENSOR_SERVICE);
            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (sensor != null) {
                sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
            }
            mCountDownTimer = new GameCountdown(mFramesPerSecond, mTimeLeftInMillis);
            mCountDownTimer.start();
            mGameView.setGameCountDown(mCountDownTimer);
        }

        // Start the game loop if it is not initialised yet
        if (mGameThread == null) {
            mGameThread = new Runnable() {
                public void run() {
                    synchronized (mWorld) {
                        if (!wasPaused) {
                            if (mCurrentLevelNum == 0) {
                                mCurrentLevelNum++;
                                loadLevel(mCurrentLevelNum);
                            }
                            mWorld.update();
                            mGameView.invalidate();
                        }
                    }
                    getActivity().getWindow().getDecorView().postDelayed(mGameThread, 10);
                }
            };
        }
        getActivity().getWindow().getDecorView().postDelayed(mGameThread, 1000);

        loadBackgroundMusic();
        updateSignInButtonVisibility();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseGame();
        if (mBackgroundMusic != null) {
            mBackgroundMusic.stop();
            mBackgroundMusic.release();
        }
        getActivity().getWindow().getDecorView().removeCallbacks(mGameThread);
    }

    private void loadBackgroundMusic() {
        mBackgroundMusic = MediaPlayer.create(getActivity(), R.raw.santatracker_musicloop);
        mBackgroundMusic.setLooping(true);
        mBackgroundMusic.setVolume(.2f, .2f);
        mBackgroundMusic.start();
    }

    /**
     * Hide the sign in button if sign in was successful.
     */
    public void onSignInSucceeded() {
        setSignInButtonVisibility(false);
    }

    public void onSignInFailed() {
    }

    @Override
    public void onClick(View view) {
        if (view.equals(mViewPauseButton)) {
            // Pause the game
            pauseGame();
        } else if (view.equals(mViewPlayButton) || view.equals(mViewBigPlayButton)) {
            // Continue the game
            unPauseGame();
        } else if (view.equals(mViewPlayAgainButton)) {
            // Reload the background music for a new game
            if (mBackgroundMusic != null) {
                mBackgroundMusic.stop();
                mBackgroundMusic.release();
            }
            loadBackgroundMusic();

            // Reset the game variables
            mCurrentLevelNum = 0;
            mTimeLeftInMillis = MatchingGameConstants.GUMBALL_INIT_TIME;
            mMatchScore = 0;
            mViewScore.setText(String.valueOf(mMatchScore));
            wasPaused = false;

            // Hide the pause screen
            mViewPlayAgainBackground.clearAnimation();
            mViewPlayAgainMain.clearAnimation();
            mViewPlayAgainBackground.setVisibility(View.GONE);
            mViewPlayAgainMain.setVisibility(View.GONE);
            mViewGPlusLayout.setVisibility(View.GONE);
            mViewMainMenuButton.setVisibility(View.GONE);
            mViewInviteButton.setVisibility(View.GONE);
        } else if (view.equals(mViewGPlusSignIn)) {
            // Start sign-in flow.
            PlayGamesActivity act = Utils.getPlayGamesActivity(this);
            if (act != null) {
                act.startSignIn();
            }
        } else if (view.equals(mViewCancelBar) || (view.equals(mViewMainMenuButton))) {
            // Exit and return to previous Activity.
            returnToBackClass();
        } else if (view.equals(mViewInviteButton)) {
            // Send App Invite
            mInvitesFragment.sendGameInvite(
                    getString(R.string.gumball),
                    getString(R.string.gumball_game_id),
                    mMatchScore);
        }

    }

    private void returnToBackClass() {
        getActivity().finish();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x, y;
        if (getActivity() != null) {
            // Store the current screen rotation (used to offset the readings of the sensor).
            mRotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        }

        // Handle screen rotations by interpreting the sensor readings here
        if (mRotation == Surface.ROTATION_0) {
            x = -event.values[0];
            y = -event.values[1];
        } else if (mRotation == Surface.ROTATION_90) {
            x = event.values[1];
            y = -event.values[0];
        } else if (mRotation == Surface.ROTATION_180) {
            x = event.values[0];
            y = event.values[1];
        } else {
            x = -event.values[1];
            y = event.values[0];
        }
        // keep y low to simulate gravity
        if (mPreviousSensorY == 0f) {
            mPreviousSensorY = -9;
        } else if (mPreviousSensorY > y) {
            mPreviousSensorY = y;
        }
        // restrict x to ~+-45 degrees
        x *= 0.5;
        mWorld.getWorld().setGravity(new Vec2(x, mPreviousSensorY));
    }

    @Override
    public void beginContact(Contact contact) {

    }

    /**
     * Handle contact with objects in the Box 2D world.
     * Here the main game logic is implemented: When a ball hits the bottom pipe, it is removed
     * and the next level or ball is started.
     * When the ball goes over the edge, it is removed and a new ball is dropped from the pipe
     * again.
     */
    @Override
    public void endContact(Contact contact) {
        // If the gumball goes in the pipe, remove it from the scene (Case 1/2)
        if (contact.getFixtureA().getBody().getUserData() != null && !(contact.getFixtureA()
                .getBody().getUserData() instanceof Gumball) && (
                contact.getFixtureA().getBody().getUserData().equals(TiltGameView.PIPE_BOTTOM)
                        || contact.getFixtureA().getBody().getUserData()
                        .equals(TiltGameView.PIPE_SIDES))) {
            mWorld.mBodiesToBeRemoved.add(contact.getFixtureB().getBody());
            mSoundPoolId
                    .remove(((Gumball) contact.getFixtureB().getBody().getUserData()).mSoundPoolId);
            onBallInPipe();
        } else if (contact.getFixtureB().getBody().getUserData() != null && !(contact.getFixtureB()
                .getBody().getUserData() instanceof Gumball) && (
                // If the gumball goes in the pipe, remove it from the scene (Case 2/2)
                contact.getFixtureA().getBody().getUserData().equals(TiltGameView.PIPE_BOTTOM)
                        || contact.getFixtureA().getBody().getUserData()
                        .equals(TiltGameView.PIPE_SIDES))) {
            mWorld.mBodiesToBeRemoved.add(contact.getFixtureA().getBody());
            mSoundPoolId
                    .remove(((Gumball) contact.getFixtureA().getBody().getUserData()).mSoundPoolId);
            onBallInPipe();
        } else if (contact.getFixtureA().getBody().getUserData() != null && !(contact.getFixtureA()
                .getBody().getUserData() instanceof Gumball) && contact.getFixtureA().getBody()
                .getUserData().equals(TiltGameView.GAME_FLOOR)) {
            // If the gumball goes over the edge, remove it and respawn (Case 1/2)
            Gumball gumball = ((Gumball) contact.getFixtureB().getBody().getUserData());
            mWorld.mBodiesToBeRemoved.add(contact.getFixtureB().getBody());
            mSoundPoolId.remove(gumball.mSoundPoolId);
            mSoundPool.play(mSoundBallFail, 1, 1, 0, 0, 1.0f);
            mWorld.getWorld().step(1.0f / 60.0f, 10, 10);
            moveOutlet((mCurrentGumball.mXInitPos));
            mCountLevelBallRespawns++;
        } else if (contact.getFixtureB().getBody().getUserData() != null && !(contact.getFixtureB()
                .getBody().getUserData() instanceof Gumball) && contact.getFixtureB().getBody()
                .getUserData().equals(TiltGameView.GAME_FLOOR)) {
            // If the gumball goes over the edge, remove it and respawn (Case 2/2)
            Gumball gumball = ((Gumball) contact.getFixtureB().getBody().getUserData());
            mWorld.mBodiesToBeRemoved.add(contact.getFixtureA().getBody());
            mSoundPoolId.remove(gumball.mSoundPoolId);
            mSoundPool.play(mSoundBallFail, 1, 1, 0, 0, 1.0f);
            mWorld.getWorld().step(1.0f / 60.0f, 10, 10);
            moveOutlet((mCurrentGumball.mXInitPos));
            mCountLevelBallRespawns++;
        }
    }

    /**
     * Successfully dropped a ball in the pipe.
     * Add the next ball and go to the next level if no balls are left in this level.
     */
    private void onBallInPipe() {
        mSoundPool.play(mSoundBallInMachine, 1, 1, 0, 0, 1.0f);
        mGameBallsLeft--;
        mNumberCollected++;
        changeIndicator();
        mMatchScore += 50 * Math.max(1f, (mCurrentLevelNum - mCountLevelBallRespawns));
        mViewScore.setText(String.valueOf(mMatchScore));
        if (mGameBallsLeft == 0 && mViewPlayAgainBackground.getVisibility() != View.VISIBLE) {
            // No balls are left in this level, go to the next one
            mCurrentLevelNum++;
            mLevelNumberText.setLevelNumber(mCurrentLevelNum);
            mLevelNumberText.startAnimation(mAnimationLevelScaleUp);
            mEndLevelCircle.startAnimation(mAnimationScaleLevelDown);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jbox2d.callbacks.ContactListener#postSolve(org.jbox2d.dynamics.contacts
     * .Contact, org.jbox2d.callbacks.ContactImpulse)
     */

    /**
     * Play a sound on impact (when a ball is dropped).
     * The sound depends on the severity of the impact.
     *
     * @see #playBounceSound(float)
     */
    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        // Get both collision objects
        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();

        // Check if one of the objects is NOT a gumball, but a candy cane.
        boolean hitCane = false;
        if (dataA != null && !(dataA instanceof Gumball)
                && (Integer) dataA > TiltGameView.GUMBALL_PURPLE) {
            hitCane = true;
        } else if (dataB != null && !(dataB instanceof Gumball)
                && (Integer) dataB > TiltGameView.GUMBALL_PURPLE) {
            hitCane = true;
        }

        if (hitCane && impulse.normalImpulses[0] > 80) {
            playBounceSound(impulse.normalImpulses[0]);
        }
    }

    /**
     * Plays a 'bounce' sound through the sound pool, depending on the impulse.
     */
    private void playBounceSound(float impulse) {
        if (impulse > 80) {
            mSoundPool.play(mSoundBounceLarge, 1, 1, 0, 0, 1.0f);
        } else if (impulse > 60) {
            mSoundPool.play(mSoundBounceMed, 1, 1, 0, 0, 1.0f);
        } else if (impulse > 30) {
            mSoundPool.play(mSoundBounceSmall, 1, 1, 0, 0, 1.0f);
        }
    }


    @Override
    public void preSolve(Contact contact, Manifold arg1) {

    }

    /**
     * Add a gumball to the game and play the ball drop sound.
     */
    private void addGumball(float xPos, float yPos) {
        Gumball gumball = new Gumball();
        gumball.mXInitPos = xPos;
        gumball.mYInitPos = yPos;
        gumball.mSoundPoolId = UUID.randomUUID();
        mSoundPoolId.put(gumball.mSoundPoolId, false);
        mGameView.addGumball(gumball);
        mSoundPool.play(mSoundBallDrop, 1, 1, 0, 0, 1);
    }

    private JSONObject readLevelFile(int levelNumber) throws IOException, JSONException {
        // load the appropriate levels file from a raw resource.
        InputStream is = getResources()
                .openRawResource(Utils.getLevelRawFile(mCurrentLevelNum));
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        String json = new String(buffer, "UTF-8");
        JSONObject level = new JSONObject(json);

        return level;
    }
    /**
     * Loads a level from the levels json file and sets up the game world.
     */
    private void loadLevel(int levelNumber) {

        // Reset the current game state
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        mCountLevelBallRespawns = 0;
        mNumberCollected = 0;
        mViewPlayAgainLevel.setText(String.valueOf(levelNumber));
        Body body = mWorld.getWorld().getBodyList();
        while (body != null) {
            if (body.m_userData == null) {
                body = body.getNext();
                continue;
            }
            mWorld.mBodiesToBeRemoved.add(body);
            body = body.getNext();
        }
        mWorld.getWorld().step(1.0f / 60.0f, 10, 10);

        try {
            // Read the level file and extract the candy cane positions
            JSONObject level = readLevelFile(levelNumber);
            JSONArray canes = level.getJSONArray("candycanes");

            for (int i = 0; i < canes.length(); i++) {
                JSONObject canePart = canes.getJSONObject(i);
                int type = canePart.getInt("type");
                float xPos = (float) canePart.getDouble("xPos");
                float yPos = (float) canePart.getDouble("yPos");
                // Add the candy cane to the game world, the values represent the
                mWorld.addItem(xPos, yPos, Edges.getEdges(type), WORLD_OBJECT_BOUNCE, type,
                        WORLD_OBJECT_DENSITY, WORLD_OBJECT_FRICTION,
                        BodyType.STATIC);
            }

            // Add the sides and floor to the game world to catch dropped balls.
            // Note that the WORLD_FRICTION is used as the bounce rate of the floors.
            mWorld.addItem(WORLD_FLOOR_X, WORLD_FLOOR_Y, Edges.getPipeSideEdges(),
                    WORLD_OBJECT_BOUNCE, TiltGameView.PIPE_SIDES,
                    WORLD_OBJECT_DENSITY, WORLD_OBJECT_FRICTION, BodyType.STATIC);
            mWorld.addFloor(WORLD_FLOOR_X, WORLD_FLOOR_Y, TiltGameView.GAME_FLOOR,
                    WORLD_OBJECT_DENSITY, WORLD_OBJECT_FRICTION, WORLD_FLOOR_FRICTION,
                    BodyType.STATIC);
            mWorld.addPipeBottom(WORLD_FLOOR_X, WORLD_FLOOR_Y, TiltGameView.PIPE_BOTTOM,
                    WORLD_OBJECT_DENSITY, WORLD_OBJECT_FRICTION, WORLD_FLOOR_FRICTION,
                    BodyType.STATIC);

            // Add the gumballs
            JSONArray gumballs = level.getJSONArray("gumballs");
            mGameBallsLeft = gumballs.length();
            setIndicators(mGameBallsLeft);
            for (int j = 0; j < gumballs.length(); j++) {
                JSONObject gumball = gumballs.getJSONObject(j);
                float xPos = (float) gumball.getDouble("xPos");
                float yPos = (float) gumball.getDouble("yPos");
                Gumball gumballObject = new Gumball();
                gumballObject.mXInitPos = xPos;
                gumballObject.mYInitPos = yPos;
                mGumballQueue.add(gumballObject);
            }
            mCurrentGumball = mGumballQueue.poll();

            // Start the timer
            if (mCurrentGumball != null) {
                if (mCurrentLevelNum > 1) {
                    // Do not include gumball dropping time in countdown calculation.
                    mTimeLeftInMillis += MatchingGameConstants.GUMBALL_ADDED_TIME;
                }
                mCountDownTimer = new GameCountdown(mFramesPerSecond, mTimeLeftInMillis);
                mCountDownTimer.start();
                mGameView.setGameCountDown(mCountDownTimer);

                // Move the outlet to its initial position
                moveOutlet((mCurrentGumball.mXInitPos));
            }
        } catch (IOException e) {
        } catch (JSONException e) {
        }

    }

    /**
     * Update the state of the indicators at the bottom of the screen to the number of balls
     * collected.
     */
    private void setIndicators(int numGumballs) {
        for(int i=0; i < mViewIndicators.length ; i++){
            int stateResource = R.drawable.gbg_gumball_indicator_collected_disabled;
            if(i+1 <= numGumballs){
               stateResource = R.drawable.gbg_gumball_indicator_pending;
            }
            mViewIndicators[i].setImageResource(stateResource);
        }
    }


    /**
     * Mark the last indicator for which a ball was collected in the 'collected' state.
     */
    private void changeIndicator() {
        mViewIndicators[mNumberCollected - 1].setImageResource(
                R.drawable.gbg_gumball_indicator_collected);
    }


    @Override
    public void onAnimationEnd(Animation animation) {
        if (animation == mAnimationScaleLevelDown) {
            // After the level scale down animation, fade out the level number and end circle
            mLevelNumberText.startAnimation(mAnimationLevelFadeOut);
            mEndLevelCircle.startAnimation(mAnimationLevelFadeOut);
        } else if (animation == mAnimationLevelFadeOut) {
            // After the level fade out animation reset and hide all other end level views
            mEndLevelCircle.clearAnimation();
            mLevelNumberText.clearAnimation();
            mLevelNumberText.setVisibility(View.GONE);
            mEndLevelCircle.setVisibility(View.GONE);
        } else if (animation == mAnimationOutlet) {
            // After the outlet has moved to the correct position, add gumball
            addGumball(mCurrentGumball.mXInitPos, mCurrentGumball.mYInitPos);
            if (mGumballQueue.peek() != null) {
                // Move it to the next position if there is a gumball left in the queue
                mCurrentGumball = mGumballQueue.poll();
                moveOutlet(mCurrentGumball.mXInitPos);
            }
        }
    }

    @Override
    public void onAnimationRepeat(Animation arg0) {
        // do nothing

    }

    @Override
    public void onAnimationStart(Animation animation) {
        if (animation == mAnimationScaleLevelDown) {
            // Show the circle level end and level text views when the animation starts
            mEndLevelCircle.setVisibility(View.VISIBLE);
            mLevelNumberText.setVisibility(View.VISIBLE);
        } else if (animation == mAnimationLevelFadeOut) {
            // Load the next level after the end level animation is over
            loadLevel(mCurrentLevelNum);
        } else if (animation == mAnimationPlayAgainBackground) {
            // Show the 'play again' screen when the animation starts and cancel the timer
            mViewPlayAgainBackground.setVisibility(View.VISIBLE);
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
            }
        } else if (animation == mAnimationPlayAgainMain) {
            mViewPlayAgainMain.setVisibility(View.VISIBLE);
            setSignInButtonVisibility(true);
        }
    }

    /**
     * Set the visibility of the sign in button if the user is not already signed in.
     */
    private void setSignInButtonVisibility(boolean show) {
        mViewGPlusLayout.setVisibility(show && !Utils.isSignedIn(this) ? View.VISIBLE : View.GONE);
    }

    /**
     * Hide the sign in button when the user signs in and the button is still visible on screen.
     */
    private void updateSignInButtonVisibility() {
        if (mViewGPlusLayout.getVisibility() == View.VISIBLE && Utils.isSignedIn(this)) {
            setSignInButtonVisibility(false);
        }
    }

    /**
     * Start an animation to move the outlet to the x position in pixels.
     */
    private void moveOutlet(float xPos) {
        float scale = mRootView.getWidth() / 10.0f;
        mAnimationOutlet = new TranslateAnimation(mOutletPreviousXPos, (scale * xPos) - mOutletOffset, 0, 0);
        mAnimationOutlet.setDuration(700);
        mAnimationOutlet.setFillAfter(true);
        mAnimationOutlet.setStartOffset(400);
        mAnimationOutlet.setAnimationListener(this);
        mGameOutlet.startAnimation(mAnimationOutlet);
        mOutletPreviousXPos = (scale * xPos) - mOutletOffset;
    }

    /**
     * Countdown for the main game.
     * Updates the countdown on screen and stops the game when the timer runs out.
     */
    public class GameCountdown {

        private Boolean animationStarted = false;

        private final long mMillisDuration;

        private final long mMillisTickDuration;

        private long mTicksLeft;

        private boolean mStarted = false;

        private long mSecondsTextValue = -1;

        /**
         * @param framesPerSecond assumed frame rate
         * @param millisInFuture  duration of game at this frame rate
         */
        public GameCountdown(int framesPerSecond, long millisInFuture) {
            mMillisDuration = millisInFuture;
            mMillisTickDuration = 1000 / framesPerSecond;
            mTicksLeft = mMillisDuration / mMillisTickDuration;
        }

        /**
         * Stop the timer.
         */
        public void cancel() {
            mTicksLeft = 0;
            mStarted = false;
        }

        /**
         * Starts the timer.
         */
        public void start() {
            mStarted = true;
            mSecondsTextValue = -1;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(mTicksLeft * mMillisTickDuration);
            if (seconds >= 6) {
                animationStarted = false;
                mViewCountdown.clearAnimation();
                mViewCountdown.setTextColor(Color.WHITE);
                mViewCountdown.setTypeface(Typeface.DEFAULT);
            }
        }

        /**
         * Update the displayed timer.
         * When the timer is below 6s the text color changes to red.
         */
        public void tick() {
            if (mStarted) {
                --mTicksLeft;
                mTimeLeftInMillis = mTicksLeft * mMillisTickDuration;
                if (mTimeLeftInMillis < 6000 && !animationStarted) {
                    animationStarted = true;
                    mViewCountdown.setTextColor(Color.RED);
                    mViewCountdown.setTypeface(Typeface.DEFAULT_BOLD);
                    mViewCountdown.clearAnimation();
                    mViewCountdown.startAnimation(mAnimationTimerAlpha);
                }
                if (mSecondsTextValue != mTimeLeftInMillis / 1000) {
                    mViewCountdown.setText(
                            String.format("%d:%02d",
                                    TimeUnit.MILLISECONDS.toMinutes(mTimeLeftInMillis),
                                    TimeUnit.MILLISECONDS.toSeconds(mTimeLeftInMillis)));
                    mSecondsTextValue = mTimeLeftInMillis / 1000;
                }
                if (mTimeLeftInMillis == 0) {
                    finished();
                }
            }
        }

        /**
         * Shut down the count down timer.
         * Cancel all pending animations and display the 'play again' screen.
         */
        private void finished() {
            mViewCountdown.clearAnimation();
            animationStarted = false;
            mViewCountdown.setTextColor(Color.WHITE);
            mViewCountdown.setTypeface(Typeface.DEFAULT);
            if (mViewPlayAgainBackground.getVisibility() != View.VISIBLE && !wasPaused) {
                wasPaused = true;
                submitScore(MatchingGameConstants.LEADERBOARDS_GUMBALL, mMatchScore);
                if (mBackgroundMusic != null) {
                    mBackgroundMusic.stop();
                    mBackgroundMusic.release();
                    mBackgroundMusic = null;
                }
                mViewPlayAgainScore.setText(String.valueOf(mMatchScore));
                mViewPlayAgainBackground.startAnimation(mAnimationPlayAgainBackground);
                mViewPlayAgainMain.startAnimation(mAnimationPlayAgainMain);
                mViewPlayAgainBackground.setVisibility(View.VISIBLE);
                mViewPlayAgainMain.setVisibility(View.VISIBLE);
                mViewMainMenuButton.setVisibility(View.VISIBLE);
                mViewInviteButton.setVisibility(View.VISIBLE);
                setSignInButtonVisibility(true);
                mSoundPool.play(mSoundGameOver, .2f, .2f, 0, 0, 1.0f);
            }

            cancel();
        }
    }

    /**
     * Pause the game when the back key is pressed.
     */
    public void onBackKeyPressed() {
        if (mViewPlayAgainMain.getVisibility() == View.VISIBLE) {
            returnToBackClass();
        } else {
            if (mViewPauseButton.getVisibility() != View.GONE) {// check if already handled
                pauseGame();
            } else {
                // Exit and return to previous Activity.
                returnToBackClass();
            }
        }
    }

    /**
     * Pause the game and display the pause game screen.
     */
    private void pauseGame() {
        mViewPauseButton.setVisibility(View.GONE);
        mViewPlayButton.setVisibility(View.VISIBLE);
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            wasPaused = true;
        }
        mViewMatchPauseOverlay.setVisibility(View.VISIBLE);
        mViewCancelBar.setVisibility(View.VISIBLE);

        SensorManager sensorManager = (SensorManager) getActivity()
                .getSystemService(Activity.SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
        if (Utils.hasKitKat()) {
            ImmersiveModeHelper.setImmersiveStickyWithActionBar(getActivity().getWindow());
        }
    }

    /**
     * Continue the paused game.
     * Restart the countdown timer and hide the pause game screen.
     */
    private void unPauseGame() {
        mViewPauseButton.setVisibility(View.VISIBLE);
        mViewPlayButton.setVisibility(View.GONE);
        mViewMatchPauseOverlay.setVisibility(View.GONE);
        mViewCancelBar.setVisibility(View.GONE);
        mCountDownTimer = new GameCountdown(mFramesPerSecond, mTimeLeftInMillis);
        mCountDownTimer.start();
        mGameView.setGameCountDown(mCountDownTimer);
        wasPaused = false;
        SensorManager sensorManager = (SensorManager) getActivity()
                .getSystemService(Activity.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
        }
        if (Utils.hasKitKat()) {
            ImmersiveModeHelper.setImmersiveSticky(getActivity().getWindow());
        }
    }

    /**
     * Submit score to play games services
     */
    private void submitScore(int resId, int score) {
        PlayGamesActivity act = Utils.getPlayGamesActivity(this);
        if (act != null) {
            act.postSubmitScore(resId, score);
        }
    }

    /**
     * Hide the instructions and mark them as viewed.
     */
    private class HideInstructionsRunnable implements Runnable {

        @Override
        public void run() {
            mDrawableTransition.stop();
            wasPaused = false;
            mViewInstructions.setVisibility(View.GONE);
            Editor edit = mSharedPreferences.edit();
            edit.putBoolean(MatchingGameConstants.GUMBALL_INSTRUCTIONS_VIEWED, true);
            edit.apply();
        }

    }
}
