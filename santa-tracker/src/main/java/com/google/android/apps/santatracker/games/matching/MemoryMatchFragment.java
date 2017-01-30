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

package com.google.android.apps.santatracker.games.matching;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.apps.santatracker.R;
import com.google.android.apps.santatracker.util.ImmersiveModeHelper;
import com.google.android.apps.santatracker.games.common.PlayGamesActivity;
import com.google.android.apps.santatracker.games.gumball.Utils;
import com.google.android.apps.santatracker.invites.AppInvitesFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fragment that contains the memory match game.
 */
public class MemoryMatchFragment extends Fragment
        implements OnClickListener, AnimationListener, Callback {

    /**
     * Drawables for all card faces.
     */
    private static final Integer[] CARD_FACE_DRAWABLES = new Integer[]{
            R.drawable.mmg_card_ball, R.drawable.mmg_card_balloon, R.drawable.mmg_card_beachball,
            R.drawable.mmg_card_candle, R.drawable.mmg_card_globe, R.drawable.mmg_card_gumball,
            R.drawable.mmg_card_penguin, R.drawable.mmg_card_rabbit, R.drawable.mmg_card_reindeer,
            R.drawable.mmg_card_snowman, R.drawable.mmg_card_tree, R.drawable.mmg_card_trophy};
    /**
     * Drawables for all card face cloaks (background color).
     */
    private static final Integer[] CARD_CLOAK_DRAWABLES = new Integer[]{
            R.drawable.mmg_card_cloak_blue_dark, R.drawable.mmg_card_cloak_blue_light,
            R.drawable.mmg_card_cloak_orange, R.drawable.mmg_card_cloak_purple,
            R.drawable.mmg_card_cloak_red, R.drawable.mmg_card_cloak_orange};

    private static final int DOOR_CLOSE_DELAY_MILLIS = 1500;

    /**
     * Current game level.
     */
    private int mLevelNumber = 1;

    /**
     * Number of correct moves required for this level.
     */
    private int mCorrectMovesRequired = 0;

    /**
     * Count of correct moves in this level so far.
     */
    private int mCurrentCorrectMoves = 0;

    /**
     * Total score of the game so far.
     */
    private int mMatchScore = 0;

    /**
     * Count of the number of wrong selections in the level so far.
     */
    private int mWrongAnswers = 0;

    /**
     * First card that has been selected and is visible.
     */
    private View mVisibleCard1 = null;

    /**
     * Second card that has been selected and is visible.
     */
    private View mVisibleCard2 = null;

    /**
     * First card that was visible and is being animated to become hidden again.
     */
    private View mHiddenCard1;

    /**
     * Second card that was visible and is being animated to become hidden again.
     */
    private View mHiddenCard2;

    /**
     * Views that represent the cards (doors) on screen.
     */
    private View[] mViewCard = new View[12];

    /**
     * List of card faces.
     * This list is shuffled before each level.
     */
    private List<Integer> mCardFaceIds = Arrays.asList(CARD_FACE_DRAWABLES);

    /**
     * List of card cloaks (backgrounds).
     * This list is shuffled before each level.
     */
    private List<Integer> mCardCloakIds = Arrays.asList(CARD_CLOAK_DRAWABLES);

    /**
     * Time left in the game in milliseconds.
     */
    private long mTimeLeftInMillis = MatchingGameConstants.MATCH_INIT_TIME;
    /**
     * Countdown timer refresh interval in milliseconds.
     */
    private long mCountDownInterval = 1000;
    /**
     * Countdown timer that drives the game logic.
     */
    private GameCountdown mCountDownTimer = null;
    /**
     * Flag to indicate the state of this Fragment and stop the game correctly when the countdown
     * expires.
     */
    private boolean wasPaused = false;

    private Animation mAnimationRightPaneSlideOut;
    private Animation mAnimationLeftPaneSlideOut;
    private Animation mAnimationLeftPaneSlideIn;
    private Animation mAnimationRightPaneSlideIn;
    private Animation mAnimationScaleLevelDown;
    private Animation mAnimationLevelFadeOut;
    private Animation mAnimationLevelScaleUp;
    private Animation mAnimationPlayAgainBackground;
    private Animation mAnimationPlayAgainMain;
    private Animation mAnimationCardCover;
    private TranslateAnimation mAnimationSnowman;
    private Animation mAnimationTimerAlpha;
    private TranslateAnimation mAnimationSnowmanBack;
    private AnimationSet mAnimationSetSnowman;

    private CircleView mEndLevelCircle;
    private TextView mScoreText;
    private LevelTextView mLevelNumberText;

    private int mSoundDoorOpen = -1;
    private int mSoundDoorClose = -1;
    private int mSoundMatchWrong = -1;
    private int mSoundMatchRight = -1;
    private int mSoundBeep = -1;
    private int mSoundGameOver = -1;
    private MediaPlayer mBackgroundMusic;
    private SoundPool mSoundPool;
    private Handler mDoorCloseHandler;
    private Runnable mDoorCloseRunnable;
    private boolean mDoorCloseTimerTicking = false;
    private long mDoorCloseTimerStart = 0;
    private long mDoorCloseTimeRemaining = DOOR_CLOSE_DELAY_MILLIS;

    private TextView mTimerTextView;
    private View mViewPlayAgainBackground;
    private View mViewPlayAgainMain;
    private TextView mTextPlayAgainScore;
    private TextView mTextPlayAgainLevel;
    private ImageView mButtonPlay;
    private ImageView mButtonPause;
    private ImageButton mButtonBigPlay;
    private Button mPlayAgainBtn;
    private ImageButton mButtonCancelBar;
    private ImageButton mButtonMenu;
    private ImageView mInviteButton;
    private ImageView mViewInstructions;
    private View mViewPauseOverlay;
    private View mViewBonusSnowman;
    private AnimationDrawable mInstructionDrawable;
    private ImageView mViewGPlusSignIn;
    private View mLayoutGPlus;

    /**
     * Handler that dismisses the game instructions when the game is started for the first time.
     */
    private Handler mDelayHandler = new Handler();

    /**
     * Handler and Runnable to fix the occasional "nothing is clickable" bug, The timeout
     * is set as slightly longer than our max animation duration.
     */
    private static final long ANIMATION_TIMEOUT = 1005;
    private Handler mClickabilityHandler = new Handler();
    private Runnable mMakeClickableRunnable = new Runnable() {
        @Override
        public void run() {
            isClickable = true;
        }
    };

    /**
     * Preferences that store whether the game instructions have been viewed.
     */
    private SharedPreferences mPreferences;

    /**
     * Indicates whether the screen is clickable.
     * It is disabled when a full screen animation is in progress at the end of the level or at the
     * end of the game.
     */
    private boolean isClickable = true;

    private AppInvitesFragment mInvitesFragment;

    /**
     * Create a new instance of this fragment.
     */
    public static MemoryMatchFragment newInstance() {
        return new MemoryMatchFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_memory_match, container, false);
        rootView.setKeepScreenOn(true);

        // Below ICS, display a special, simplified background for the entire fragment
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            rootView.findViewById(R.id.match_score_layout).setBackgroundResource(
                    R.drawable.score_background_gingerbread);
        }

        // Initialise the sound pool and all sound effects
        mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        mSoundDoorOpen = mSoundPool.load(getActivity(), R.raw.mmg_open_door_3, 1);
        mSoundDoorClose = mSoundPool.load(getActivity(), R.raw.mmg_close_door, 1);
        mSoundMatchWrong = mSoundPool.load(getActivity(), R.raw.mmg_wrong, 1);
        mSoundMatchRight = mSoundPool.load(getActivity(), R.raw.mmg_right, 1);
        mSoundGameOver = mSoundPool.load(getActivity(), R.raw.gameover, 1);
        mSoundBeep = mSoundPool.load(getActivity(), R.raw.mmg_open_door_2, 1);

        mDoorCloseHandler = new Handler();
        mDoorCloseRunnable = new Runnable() {
            @Override
            public void run() {
                hideBothPreviousCards();
                mDoorCloseTimerTicking = false;
                mDoorCloseTimerStart = 0;
                mDoorCloseTimeRemaining = DOOR_CLOSE_DELAY_MILLIS;
            }
        };

        // Set up all animations.
        loadAnimations();

        // G+ sign-in views
        mViewGPlusSignIn = (ImageView) rootView.findViewById(R.id.gplus_button);
        mViewGPlusSignIn.setOnClickListener(this);
        mLayoutGPlus = rootView.findViewById(R.id.play_again_gplus);
        mLayoutGPlus.setVisibility(View.GONE);

        // 'Play again' screen views
        mTextPlayAgainScore = (TextView) rootView.findViewById(R.id.play_again_score);
        mTextPlayAgainScore.setText(String.valueOf(mMatchScore));
        mTextPlayAgainLevel = (TextView) rootView.findViewById(R.id.play_again_level);
        mTextPlayAgainLevel.setText(String.valueOf(mLevelNumber));
        mViewPlayAgainBackground = rootView.findViewById(R.id.play_again_bkgrd);
        mViewPlayAgainMain = rootView.findViewById(R.id.play_again_main);
        mPlayAgainBtn = (Button) rootView.findViewById(R.id.play_again_btn);
        mPlayAgainBtn.setOnClickListener(this);

        // Level, countdown and score views at the bottom of the screen
        mTimerTextView = (TextView) rootView.findViewById(R.id.match_timer);
        mLevelNumberText = (LevelTextView) rootView.findViewById(R.id.card_end_level_number);
        mLevelNumberText.setVisibility(View.GONE);
        mScoreText = (TextView) rootView.findViewById(R.id.match_score);
        mScoreText.setText(String.valueOf(mMatchScore));

        // End of level animated circle
        mEndLevelCircle = (CircleView) rootView.findViewById(R.id.card_end_level_circle);
        mEndLevelCircle.setVisibility(View.GONE);

        // The snowman that is animated as a bonus when the player is particularly awesome
        mViewBonusSnowman = rootView.findViewById(R.id.match_snowman);

        // 'Pause' screen views
        mButtonMenu = (ImageButton) rootView.findViewById(R.id.main_menu_button);
        mButtonMenu.setOnClickListener(this);
        mButtonMenu.setVisibility(View.GONE);
        mInviteButton = (ImageView) rootView.findViewById(R.id.invite_button);
        mInviteButton.setOnClickListener(this);
        mInviteButton.setVisibility(View.GONE);
        mButtonPlay = (ImageView) rootView.findViewById(R.id.match_play_button);
        mButtonPlay.setOnClickListener(this);
        mButtonPlay.setVisibility(View.GONE);
        mButtonPause = (ImageView) rootView.findViewById(R.id.match_pause_button);
        mButtonPause.setOnClickListener(this);
        mButtonPause.setVisibility(View.VISIBLE);
        mViewPauseOverlay = rootView.findViewById(R.id.match_pause_overlay);
        mViewPauseOverlay.setVisibility(View.GONE);
        mButtonBigPlay = (ImageButton) rootView.findViewById(R.id.match_big_play_button);
        mButtonBigPlay.setOnClickListener(this);
        mButtonCancelBar = (ImageButton) rootView.findViewById(R.id.match_cancel_bar);
        mButtonCancelBar.setOnClickListener(this);
        mButtonCancelBar.setVisibility(View.GONE);

        // Playing cards (doors)
        mViewCard[0] = rootView.findViewById(R.id.card_position_1);
        mViewCard[1] = rootView.findViewById(R.id.card_position_2);
        mViewCard[2] = rootView.findViewById(R.id.card_position_3);
        mViewCard[3] = rootView.findViewById(R.id.card_position_4);
        mViewCard[4] = rootView.findViewById(R.id.card_position_5);
        mViewCard[5] = rootView.findViewById(R.id.card_position_6);
        mViewCard[6] = rootView.findViewById(R.id.card_position_7);
        mViewCard[7] = rootView.findViewById(R.id.card_position_8);
        mViewCard[8] = rootView.findViewById(R.id.card_position_9);
        mViewCard[9] = rootView.findViewById(R.id.card_position_10);
        mViewCard[10] = rootView.findViewById(R.id.card_position_11);
        mViewCard[11] = rootView.findViewById(R.id.card_position_12);

        // Display the instructions if they haven't been seen by the player yet.
        mPreferences = getActivity()
                .getSharedPreferences(MatchingGameConstants.PREFERENCES_FILENAME,
                        Context.MODE_PRIVATE);
        if (!mPreferences.getBoolean(MatchingGameConstants.MATCH_INSTRUCTIONS_VIEWED, false)) {
            // Instructions haven't been viewed yet. Construct an AnimationDrawable with instructions.
            mInstructionDrawable = new AnimationDrawable();
            mInstructionDrawable
                    .addFrame(VectorDrawableCompat.create(getResources(),
                            R.drawable.instructions_touch_1, null), 300);
            mInstructionDrawable
                    .addFrame(VectorDrawableCompat.create(getResources(),
                            R.drawable.instructions_touch_2, null), 300);
            mInstructionDrawable.setOneShot(false);
            mViewInstructions = (ImageView) rootView.findViewById(R.id.instructions);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mViewInstructions.setImageResource(R.drawable.instructions_touch_1);
            } else {
                mViewInstructions.setImageDrawable(mInstructionDrawable);
                mInstructionDrawable.start();
            }
            // Set a timer to hide the instructions after 2 seconds
            mViewInstructions.postDelayed(new StartGameDelay(), 2000);
        } else {
            //Instructions have already been viewed. Start the first level.
            setUpLevel();
        }

        return rootView;
    }

    /**
     * Load and initialise all animations required for the game.
     */
    private void loadAnimations(){
        mAnimationTimerAlpha = new AlphaAnimation(0.0f, 1.0f);
        mAnimationTimerAlpha.setDuration(1000);
        mAnimationTimerAlpha.setRepeatMode(Animation.REVERSE);
        mAnimationTimerAlpha.setRepeatCount(Animation.INFINITE);

        mAnimationPlayAgainBackground = AnimationUtils
                .loadAnimation(getActivity(), R.anim.play_again_bkgrd_anim);
        mAnimationPlayAgainBackground.setFillAfter(true);
        mAnimationPlayAgainBackground.setAnimationListener(this);
        mAnimationCardCover = AnimationUtils.loadAnimation(getActivity(), R.anim.card_answer_flash);
        mAnimationCardCover.setFillAfter(true);
        mAnimationPlayAgainMain = AnimationUtils
                .loadAnimation(getActivity(), R.anim.play_again_main_anim);
        mAnimationPlayAgainMain.setFillAfter(true);
        mAnimationPlayAgainMain.setAnimationListener(this);
        // Special bonus animation to play if the player is particularly awesome.
        mAnimationSetSnowman = new AnimationSet(true);
        mAnimationSnowman = new TranslateAnimation(150, 0, 150, 0);
        mAnimationSnowman.setDuration(1000);
        mAnimationSetSnowman.addAnimation(mAnimationSnowman);
        mAnimationSnowmanBack = new TranslateAnimation(0, 150, 0, 150);
        mAnimationSnowmanBack.setDuration(1000);
        mAnimationSnowmanBack.setStartOffset(1500);
        mAnimationSnowmanBack.setAnimationListener(this);
        mAnimationSetSnowman.addAnimation(mAnimationSnowmanBack);
        mAnimationSetSnowman.setAnimationListener(this);

        mAnimationRightPaneSlideOut = AnimationUtils
                .loadAnimation(getActivity(), android.R.anim.slide_out_right);
        mAnimationRightPaneSlideOut.setFillAfter(true);
        mAnimationLeftPaneSlideOut = AnimationUtils
                .loadAnimation(getActivity(), R.anim.left_pane_slide_out);
        mAnimationLeftPaneSlideOut.setFillAfter(true);
        mAnimationLeftPaneSlideIn = AnimationUtils
                .loadAnimation(getActivity(), android.R.anim.slide_in_left);
        mAnimationLeftPaneSlideIn.setFillAfter(true);
        mAnimationRightPaneSlideIn = AnimationUtils
                .loadAnimation(getActivity(), R.anim.right_pane_slide_in);
        mAnimationRightPaneSlideIn.setFillAfter(true);
        mAnimationScaleLevelDown = AnimationUtils
                .loadAnimation(getActivity(), R.anim.scale_level_anim_down);
        mAnimationScaleLevelDown.setAnimationListener(this);
        mAnimationLevelFadeOut = AnimationUtils
                .loadAnimation(getActivity(), R.anim.level_fade_out_anim);
        mAnimationLevelFadeOut.setAnimationListener(this);
        mAnimationLevelScaleUp = AnimationUtils
                .loadAnimation(getActivity(), R.anim.scale_up_level_anim);
        mAnimationLevelScaleUp.setAnimationListener(this);
        mAnimationRightPaneSlideOut.setAnimationListener(this);
        mAnimationLeftPaneSlideOut.setAnimationListener(this);
    }

    /**
     * Runnable that stars the game after the instructions have been viewed.
     * It hides the instructions, marks them as viewed and starts the game.
     */
    private class StartGameDelay implements Runnable {

        @Override
        public void run() {
            // Start the first level.
            setUpLevel();

            // Hide the instructions.
            mInstructionDrawable.stop();
            mViewInstructions.setVisibility(View.GONE);
            // Mark the instructions as 'viewed'.
            Editor edit = mPreferences.edit();
            edit.putBoolean(MatchingGameConstants.MATCH_INSTRUCTIONS_VIEWED, true);
            edit.commit();
        }

    }

    public void onSignInSucceeded() {
        setSignInButtonVisibility(false);
    }

    public void onSignInFailed() {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mInvitesFragment = AppInvitesFragment.getInstance(getActivity());
    }


    @Override
    public void onResume() {
        super.onResume();
        isClickable = true;
        if (wasPaused && mViewPauseOverlay.getVisibility() != View.VISIBLE) {
            mCountDownTimer = new GameCountdown(mTimeLeftInMillis, mCountDownInterval);
            mCountDownTimer.start();
            wasPaused = false;
        }
        loadBackgroundMusic();
        updateSignInButtonVisibility();
    }

    /**
     * Toggles visibility of the G+ sign in layout if the user is not already signed in.
     */
    private void setSignInButtonVisibility(boolean show) {
        mLayoutGPlus.setVisibility(show && !Utils.isSignedIn(this) ? View.VISIBLE : View.GONE);
    }

    private void updateSignInButtonVisibility() {
        if (mLayoutGPlus.getVisibility() == View.VISIBLE && Utils.isSignedIn(this)) {
            setSignInButtonVisibility(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseGame();
        stopBackgroundMusic();
        if (mCountDownTimer != null && mViewPauseOverlay.getVisibility() != View.VISIBLE) {
            mCountDownTimer.cancel();
            wasPaused = true;
        }
    }

    private void stopBackgroundMusic() {
        if (mBackgroundMusic != null) {
            mBackgroundMusic.stop();
            mBackgroundMusic.release();
            mBackgroundMusic = null;
        }
    }


    private void loadBackgroundMusic() {
        mBackgroundMusic = MediaPlayer.create(getActivity(), R.raw.santatracker_musicloop);
        mBackgroundMusic.setLooping(true);
        mBackgroundMusic.setVolume(.1f, .1f);
        mBackgroundMusic.start();
    }

    /**
     * Starts the next level.
     * Shuffles the cards, sets up the views and starts the countdown for the next level.
     */
    private void setUpLevel() {
        mCurrentCorrectMoves = 0;
        mWrongAnswers = 0;
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }

        // Display the level number
        mTextPlayAgainLevel.setText(String.valueOf(mLevelNumber));

        // Lock all doors, unlock them individually per level later
        for (View card : mViewCard) {
            setUpLockedCard(card);
        }

        if(mLevelNumber > 1){
            // Add the 'next level' bonus time
            mTimeLeftInMillis += MatchingGameConstants.MATCH_ADD_TIME_NEXT_LEVEL;
        }

        int pairsRequired = Math.min(mLevelNumber, 5) + 1;
        mCorrectMovesRequired = pairsRequired;
        ArrayList<MemoryCard> memoryCards =
                MemoryCard.getGameCards(pairsRequired * 2, mCardFaceIds, mCardCloakIds);
        int[] cardSlots;

        if (mLevelNumber == 1) {
            cardSlots = new int[] {2, 3, 8, 9};
        } else if (mLevelNumber == 2) {
            cardSlots = new int[] {0, 1, 2, 3, 4, 5};
        } else if (mLevelNumber == 3) {
            cardSlots = new int[] {1, 2, 3, 4, 7, 8, 9, 10};
        } else if (mLevelNumber == 4) {
            cardSlots = new int[] {0, 1, 2, 3, 4, 5, 7, 8, 9, 10};
        } else {
            cardSlots = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        }
        for (int i = 0; i < cardSlots.length; i++) {
            setUpMemoryCard(mViewCard[cardSlots[i]], memoryCards.get(i));
        }

        // Start the countdown for the new level
        mCountDownTimer = new GameCountdown(mTimeLeftInMillis, mCountDownInterval);
        mCountDownTimer.start();
    }

    /**
     * Sets the card displayed by this view to the 'locked' state.
     */
    private void setUpLockedCard(View view) {
        view.setOnClickListener(null);
        view.findViewById(R.id.card_locked).setVisibility(View.VISIBLE);
        view.findViewById(R.id.card_cloak).setVisibility(View.GONE);
        view.findViewById(R.id.card_frame).setVisibility(View.GONE);
        view.findViewById(R.id.card_image).setVisibility(View.GONE);
        view.findViewById(R.id.card_pane_right).clearAnimation();
        view.findViewById(R.id.card_pane_left).clearAnimation();
        view.findViewById(R.id.card_pane_left).setVisibility(View.GONE);
        view.findViewById(R.id.card_pane_right).setVisibility(View.GONE);
        view.findViewById(R.id.card_cover).setVisibility(View.GONE);
    }

    /**
     * Sets the viewCard that displays a card to show the face and cloaking indicated by the
     * {@link com.google.android.apps.santatracker.games.matching.MemoryCard}.
     */
    private void setUpMemoryCard(View viewCard, MemoryCard card) {
        viewCard.setOnClickListener(this);
        card.mView = viewCard;
        viewCard.setTag(card);
        viewCard.findViewById(R.id.card_locked).setVisibility(View.GONE);
        viewCard.findViewById(R.id.card_frame).setVisibility(View.VISIBLE);
        ((ImageView) viewCard.findViewById(R.id.card_cloak)).setImageResource(card.mCardCloakId);
        viewCard.findViewById(R.id.card_cloak).setVisibility(View.VISIBLE);
        ((ImageView) viewCard.findViewById(R.id.card_image)).setImageResource(card.mCardImageId);
        viewCard.findViewById(R.id.card_image).setVisibility(View.VISIBLE);
        viewCard.findViewById(R.id.card_pane_right).clearAnimation();
        viewCard.findViewById(R.id.card_pane_left).clearAnimation();
        viewCard.findViewById(R.id.card_pane_left).setVisibility(View.VISIBLE);
        viewCard.findViewById(R.id.card_pane_right).setVisibility(View.VISIBLE);
        viewCard.findViewById(R.id.card_cover).setVisibility(View.INVISIBLE);
    }

    /**
     * Plays a sound unveils the given card.
     */
    private void showCard(View view) {
        mSoundPool.play(mSoundDoorOpen, 1, 1, 0, 0, 1.0f);
        view.findViewById(R.id.card_pane_right).startAnimation(mAnimationRightPaneSlideOut);

        view.findViewById(R.id.card_pane_left).startAnimation(mAnimationLeftPaneSlideOut);
    }

    /**
     * Plays a sound and hides the given card.
     */
    private void hideCard(View view) {
        mSoundPool.play(mSoundDoorClose, 1, 1, 0, 0, 1.0f);
        view.findViewById(R.id.card_pane_left).startAnimation(mAnimationLeftPaneSlideIn);
        view.findViewById(R.id.card_pane_right).startAnimation(mAnimationRightPaneSlideIn);
    }

    @Override
    public void onClick(View view) {
        // Check if the cards are not currently clickable and skip if necessary.
        if (view.getTag() != null && isClickable) {
            // A card has been clicked.
            onCardClick(view);
        } else if (view.equals(mPlayAgainBtn)) {
            // The 'play again' button has been clicked. Stop the music and restart.
            stopBackgroundMusic();
            loadBackgroundMusic();

            // Reset the game state.
            resetGameState();

            // Reset the UI and clear all animations.
            mScoreText.setText(String.valueOf(mMatchScore));
            mTextPlayAgainScore.setText(String.valueOf(mMatchScore));
            mViewPlayAgainBackground.clearAnimation();
            mViewPlayAgainMain.clearAnimation();
            mViewPlayAgainBackground.setVisibility(View.GONE);
            mViewPlayAgainMain.setVisibility(View.GONE);
            mButtonMenu.setVisibility(View.GONE);
            mInviteButton.setVisibility(View.GONE);
            setSignInButtonVisibility(false);
        } else if (view.equals(mButtonPause)) {
            // Pause button.
            pauseGame();
        } else if (view.equals(mButtonPlay) || view.equals(mButtonBigPlay)) {
            // Play button, unmute the game.
            resumeGame();
        } else if (view.equals(mButtonCancelBar) || view.equals(mButtonMenu)) {
            // Exit the game.
            exit();
        } else if (view.equals(mViewGPlusSignIn)) {
            // Start sign-in flow.
            PlayGamesActivity activity = Utils.getPlayGamesActivity(this);
            if (activity != null) {
                activity.startSignIn();
            }
        } else if (view.equals(mInviteButton)) {
            // Send app invite
            mInvitesFragment.sendGameInvite(
                    getString(R.string.memory),
                    getString(R.string.memory_game_id),
                    mMatchScore);
        }
    }

    private void resetGameState() {
        mLevelNumber = 1;
        mTimeLeftInMillis = MatchingGameConstants.MATCH_INIT_TIME;
        setUpLevel();
        mMatchScore = 0;
    }

    /**
     * Hides the cards of the previous turn.
     * Should be called when the previous turn was an incorrect match.
     */
    private void hideBothPreviousCards() {
        if (mVisibleCard1 != null && mVisibleCard2 != null) {
            hideCard(mVisibleCard1);
            hideCard(mVisibleCard2);
            mVisibleCard2.setOnClickListener(this);
            mVisibleCard1.setOnClickListener(this);
            mVisibleCard1 = null;
            mVisibleCard2 = null;
        }
    }

    /**
     * Handles onClick events for views that represent cards.
     * Unveils the card and checks for a match if another card has already been unveiled.
     */
    private void onCardClick(View view) {
        MemoryCard card1 = (MemoryCard) view.getTag();
        if (mVisibleCard1 != null && mVisibleCard2 != null) {
            // Two cards are already unveiled, hide them both
            // This is also triggered by a timer but this occurs if the user
            // plays the next turn before the timer is up.
            hideBothPreviousCards();
            mVisibleCard1 = view;
            mVisibleCard1.setOnClickListener(null);
            showCard(mVisibleCard1);
        } else if (mVisibleCard1 != null && mVisibleCard2 == null) {
            // One card is already unveiled and a second one has been selected
            MemoryCard card2 = (MemoryCard) mVisibleCard1.getTag();

            if (card1.mCardImageId == card2.mCardImageId) {
                // The second card matches the face of the first one - we have a winner!
                mVisibleCard2 = view;
                mVisibleCard2.setOnClickListener(null);
                mVisibleCard1.setOnClickListener(null);
                showCard(view);
                if (mVisibleCard1.findViewById(R.id.card_cover).getVisibility() != View.GONE) {
                    // Play an animation to flash the background of both cards in green
                    mVisibleCard1.findViewById(R.id.card_cover).setBackgroundColor(Color.GREEN);
                    mVisibleCard2.findViewById(R.id.card_cover).setBackgroundColor(Color.GREEN);
                    mVisibleCard1.findViewById(R.id.card_cover).clearAnimation();
                    mVisibleCard2.findViewById(R.id.card_cover).clearAnimation();
                    mVisibleCard1.findViewById(R.id.card_cover).setVisibility(View.VISIBLE);
                    mVisibleCard2.findViewById(R.id.card_cover).setVisibility(View.VISIBLE);
                    mAnimationCardCover.setAnimationListener(this);
                    mHiddenCard1 = mVisibleCard1;
                    mHiddenCard2 = mVisibleCard2;
                    mVisibleCard1.findViewById(R.id.card_cover).startAnimation(
                            mAnimationCardCover);
                    mVisibleCard2.findViewById(R.id.card_cover).startAnimation(
                            mAnimationCardCover);

                    mVisibleCard2 = null;
                    mVisibleCard1 = null;

                    // Add the cards to the tally of correct cards
                    mCurrentCorrectMoves++;
                    increaseScoreMatch();

                    // Check if this level is finished
                    checkNextLevel();
                }
            } else {
                // The second card does not match the first one - this is not a match.
                mSoundPool.play(mSoundMatchWrong, 1, 1, 0, 0, 1.0f);
                mWrongAnswers++;
                mVisibleCard2 = view;
                mVisibleCard2.setOnClickListener(null);
                showCard(mVisibleCard2);
                // Play an animation to flash the background of both cards in red
                mVisibleCard1.findViewById(R.id.card_cover).setBackgroundColor(Color.RED);
                mVisibleCard2.findViewById(R.id.card_cover).setBackgroundColor(Color.RED);
                mVisibleCard1.findViewById(R.id.card_cover).clearAnimation();
                mVisibleCard2.findViewById(R.id.card_cover).clearAnimation();
                mVisibleCard1.findViewById(R.id.card_cover).setVisibility(View.VISIBLE);
                mVisibleCard2.findViewById(R.id.card_cover).setVisibility(View.VISIBLE);
                mAnimationCardCover.setAnimationListener(null);
                mVisibleCard1.findViewById(R.id.card_cover).startAnimation(mAnimationCardCover);
                mVisibleCard2.findViewById(R.id.card_cover).startAnimation(mAnimationCardCover);

                // 1.5 seconds after this turn was deemed incorrect, hide both cards.
                mDoorCloseHandler.postDelayed(mDoorCloseRunnable, DOOR_CLOSE_DELAY_MILLIS);
                mDoorCloseTimerStart = System.currentTimeMillis();
                mDoorCloseTimerTicking = true;
            }
        } else {
            // This is the first card that has been unveiled.
            mVisibleCard1 = view;
            mVisibleCard1.setOnClickListener(null);
            showCard(mVisibleCard1);
        }
    }

    @Override
    public void onStop() {
        if(mDoorCloseHandler != null && mDoorCloseRunnable != null) {
            mDoorCloseHandler.removeCallbacks(mDoorCloseRunnable);
        }
        super.onStop();
    }

    /**
     * Advances the game to the next level if all matching pairs have been unveiled.
     */
    private void checkNextLevel() {
        if (mCurrentCorrectMoves >= mCorrectMovesRequired) {
            // Increment the level count
            increaseScoreLevel();
            mLevelNumber++;
            mLevelNumberText.setLevelNumber(mLevelNumber);
            // Start the 'next level' animation after a short delay.
            mDelayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLevelNumberText.startAnimation(mAnimationLevelScaleUp);
                    mEndLevelCircle.startAnimation(mAnimationScaleLevelDown);
                    setUpLevel();
                }
            }, 750);
        }
    }

    private void exit() {
        getActivity().finish();
    }

    private void resumeGame() {
        mButtonPause.setVisibility(View.VISIBLE);
        mButtonPlay.setVisibility(View.GONE);
        mCountDownTimer = new GameCountdown(mTimeLeftInMillis, mCountDownInterval);

        // If the user had gotten the previous turn incorrect and the CloseDoor timer was
        // ticking before the mute, continue the timer.
        if(mDoorCloseHandler != null && mDoorCloseRunnable != null && mDoorCloseTimerTicking) {
            mDoorCloseHandler.postDelayed(mDoorCloseRunnable, mDoorCloseTimeRemaining);
            mDoorCloseTimerStart = System.currentTimeMillis();
        }
        mCountDownTimer.start();
        mViewPauseOverlay.setVisibility(View.GONE);
        mButtonCancelBar.setVisibility(View.GONE);
        if (Utils.hasKitKat()) {
            ImmersiveModeHelper.setImmersiveSticky(getActivity().getWindow());
        }
    }

    /**
     * CountDownTimer that handles the game timing logic of the memory match game.
     */
    public class GameCountdown extends CountDownTimer {

        private Boolean animationStarted = false;

        public GameCountdown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onFinish() {
            // When the countdown is over, end the game.
            mTimerTextView.clearAnimation();
            animationStarted = false;
            mTimerTextView.setTextColor(Color.WHITE);
            mTimerTextView.setTypeface(Typeface.DEFAULT);
            if (mViewPlayAgainBackground.getVisibility() != View.VISIBLE && !wasPaused) {
                submitScore(MatchingGameConstants.LEADERBOARDS_MATCH, mMatchScore);
                stopBackgroundMusic();

                // Show the 'play again' screen.
                mTextPlayAgainScore.setText(String.valueOf(mMatchScore));
                mViewPlayAgainBackground.startAnimation(mAnimationPlayAgainBackground);
                mViewPlayAgainMain.startAnimation(mAnimationPlayAgainMain);
                mViewPlayAgainBackground.setVisibility(View.VISIBLE);
                mViewPlayAgainMain.setVisibility(View.VISIBLE);
                mButtonMenu.setVisibility(View.VISIBLE);
                mInviteButton.setVisibility(View.VISIBLE);
                setSignInButtonVisibility(true);

                mSoundPool.play(mSoundGameOver, .2f, .2f, 0, 0, 1.0f);
            }
        }

        @Override
        public void onTick(long millisUntilFinished) {

            long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished);
            if (seconds >= 6) {
                animationStarted = false;
                mTimerTextView.clearAnimation();
                mTimerTextView.setTypeface(Typeface.DEFAULT);
                mTimerTextView.setTextColor(Color.WHITE);
            } else if (!animationStarted) {
                // Start flashing the countdown time
                animationStarted = true;
                mTimerTextView.setTypeface(Typeface.DEFAULT_BOLD);
                mTimerTextView.setTextColor(Color.RED);
                mTimerTextView.clearAnimation();
                mTimerTextView.startAnimation(mAnimationTimerAlpha);
            }

            // Update the displayed countdown time
            mTimeLeftInMillis = millisUntilFinished;
            mTimerTextView.setText(
                    String.format("%d:%02d", TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished),
                            seconds - TimeUnit.MINUTES.toSeconds(
                                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));

        }

    }

    /**
     * Increases the current score when a match is found.
     * The score is based on the current level.
     */
    private void increaseScoreMatch() {
        mSoundPool.play(mSoundMatchRight, 1, 1, 0, 0, 1.0f);
        mMatchScore += (50 * (Math.pow(1.1, mLevelNumber - 1)));
        mScoreText.setText(String.valueOf(mMatchScore));
        mTextPlayAgainScore.setText(String.valueOf(mMatchScore));
    }

    /**
     * Increases the current score when advancing to the next level.
     */
    private void increaseScoreLevel() {
        mMatchScore += (500 * (Math.pow(1.1, mLevelNumber - 1)));
        if (mLevelNumber > 2 && mWrongAnswers == 0) {
            // Show an amazing bonus animation if this the player is particularly skillful ;)
            mViewBonusSnowman.startAnimation(mAnimationSnowman);
        }
        mScoreText.setText(String.valueOf(mMatchScore));
        mTextPlayAgainScore.setText(String.valueOf(mMatchScore));
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        // The game is clickable again now that the animation has ended
        isClickable = true;
        mClickabilityHandler.removeCallbacksAndMessages(null);

        if (animation == mAnimationScaleLevelDown) {
            // After the scale level down animation, fade out the end level circle
            mLevelNumberText.startAnimation(mAnimationLevelFadeOut);
            mEndLevelCircle.startAnimation(mAnimationLevelFadeOut);
        } else if (animation == mAnimationLevelFadeOut) {
            // Hide the end level circle after the animation has finished
            mEndLevelCircle.clearAnimation();
            mLevelNumberText.clearAnimation();
            mLevelNumberText.setVisibility(View.GONE);
            mEndLevelCircle.setVisibility(View.GONE);
        } else if (animation == mAnimationSetSnowman) {
            mViewBonusSnowman.clearAnimation();
            mViewBonusSnowman.setVisibility(View.GONE);
        } else if (animation == mAnimationCardCover) {
            // Reset the state and animations of both cards after they have been hidden again
            mHiddenCard1.clearAnimation();
            mHiddenCard2.clearAnimation();

            mHiddenCard1.findViewById(R.id.card_pane_right).clearAnimation();
            mHiddenCard1.findViewById(R.id.card_pane_left).clearAnimation();
            mHiddenCard2.findViewById(R.id.card_pane_right).clearAnimation();
            mHiddenCard2.findViewById(R.id.card_pane_left).clearAnimation();
            mHiddenCard1.findViewById(R.id.card_pane_right).setVisibility(View.GONE);
            mHiddenCard1.findViewById(R.id.card_pane_left).setVisibility(View.GONE);
            mHiddenCard2.findViewById(R.id.card_pane_right).setVisibility(View.GONE);
            mHiddenCard2.findViewById(R.id.card_pane_left).setVisibility(View.GONE);
            mHiddenCard1.findViewById(R.id.card_cover).setBackgroundColor(Color.TRANSPARENT);
            mHiddenCard2.findViewById(R.id.card_cover).setBackgroundColor(Color.TRANSPARENT);
            mHiddenCard1.findViewById(R.id.card_cover).setVisibility(View.GONE);
            mHiddenCard2.findViewById(R.id.card_cover).setVisibility(View.GONE);
        }
    }


    @Override
    public void onAnimationRepeat(Animation animation) {
    }

    @Override
    public void onAnimationStart(Animation animation) {
        // Mark the game as not clickable when an animation is in progress
        isClickable = false;

        // Mark the correct views as visible before the start of an animation.
        if (animation == mAnimationScaleLevelDown) {
            mEndLevelCircle.setVisibility(View.VISIBLE);
            mLevelNumberText.setVisibility(View.VISIBLE);
        } else if (animation == mAnimationPlayAgainBackground) {
            mViewPlayAgainBackground.setVisibility(View.VISIBLE);
        } else if (animation == mAnimationPlayAgainMain) {
            mViewPlayAgainMain.setVisibility(View.VISIBLE);
            setSignInButtonVisibility(true);
        } else if (animation == mAnimationSetSnowman) {
            mViewBonusSnowman.setVisibility(View.VISIBLE);
            mViewBonusSnowman.postDelayed(new Runnable() {

                @Override
                public void run() {
                    mSoundPool.play(mSoundBeep, .5f, .5f, 0, 0, 1.0f);
                }
            }, 800);
        }

        // Set a clickability timeout
        mClickabilityHandler.postDelayed(mMakeClickableRunnable, ANIMATION_TIMEOUT);
    }


    @Override
    public void invalidateDrawable(Drawable who) {
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
    }

    /**
     * Pauses the game when the back key is pressed.
     */
    public void onBackKeyPressed() {
        if (mViewPlayAgainMain.getVisibility() == View.VISIBLE) {
            exit();
        } else {
            if (mButtonPause.getVisibility() != View.GONE) {// check if already handled
                pauseGame();
            } else {
                // Exit the game.
                exit();
            }
        }
    }

    private void pauseGame() {
        mButtonPause.setVisibility(View.GONE);
        mButtonPlay.setVisibility(View.VISIBLE);
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }

        // If the user got the previous turn incorrect and the CloseDoor timer is ticking,
        // stop the CloseDoor timer but remember how much time is left on it.
        if (mDoorCloseHandler != null && mDoorCloseRunnable != null && mDoorCloseTimerTicking) {
            mDoorCloseHandler.removeCallbacks(mDoorCloseRunnable);
            mDoorCloseTimeRemaining =
                    DOOR_CLOSE_DELAY_MILLIS - (System.currentTimeMillis() - mDoorCloseTimerStart);
        }
        mViewPauseOverlay.setVisibility(View.VISIBLE);
        mButtonCancelBar.setVisibility(View.VISIBLE);
        if (Utils.hasKitKat()) {
            ImmersiveModeHelper.setImmersiveStickyWithActionBar(getActivity().getWindow());
        }
    }

    /**
     * Submit score to Play Games services and the leader board.
     */
    private void submitScore(int resId, int score) {
        PlayGamesActivity act = Utils.getPlayGamesActivity(this);
        if (act != null) {
            act.postSubmitScore(resId, score);
        }
    }
}
