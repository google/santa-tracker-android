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

package com.google.android.apps.santatracker.doodles.shared.views;

import static com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogEvent.DEFAULT_DOODLE_NAME;
import static com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogEvent.GAME_OVER;
import static com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogEvent.HOME_CLICKED;
import static com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogEvent.REPLAY_CLICKED;
import static com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogEvent.SHARE_CLICKED;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.google.android.apps.santatracker.doodles.R;
import com.google.android.apps.santatracker.doodles.shared.AndroidUtils;
import com.google.android.apps.santatracker.doodles.shared.EventBus;
import com.google.android.apps.santatracker.doodles.shared.UIUtil;
import com.google.android.apps.santatracker.doodles.shared.animation.ElasticOutInterpolator;
import com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogEvent.Builder;
import com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogTimer;
import com.google.android.apps.santatracker.doodles.shared.logging.DoodleLogger;
import com.google.android.apps.santatracker.util.SantaLog;

/** Displays the score during the game and shows the end screen when the game is over. */
public class ScoreView extends FrameLayout {
    private static final String TAG = ScoreView.class.getSimpleName();
    // Durations of various animations.
    private static final int BUMP_MS = 300; // Bump when a point is scored.
    private static final int ZOOM_UP_MS = 900; // Zooming score to middle of screen.
    private static final int ZOOM_DOWN_MS = 400; // Zooming score to its end position.
    private static final int SHARE_FADE_IN_MS = 500; // Fading in the share image.
    private static final int SHARE_DROP_MS = 400; // Dropping the share image into place.
    private static final int SHARE_Y_OFFSET_PX = -200; // Offset of the share image before it drops.
    private static final int BG_FADE_IN_MS = 400; // Fading in the end screen background.
    private static final int RESET_FADE_IN_MS = 300; // Fading in the score view when game is reset.
    private static final int STAR_BOUNCE_IN_MS = 600; // Bouncing the stars into the end screen.
    private static final int STAR_FADE_IN_MS = 500; // Fading the stars into the end screen.
    private static final float STAR_BIG_SCALE = 2.0f;
    protected DoodleLogger logger;
    protected LevelFinishedListener listener;
    // The score in the upper-left corner during the game. This also gets zoomed up to the
    // middle of the screen at the end of the game.
    private TextView currentScore;
    // An invisible placeholder. Lets us use the android layout engine for figuring out where
    // the score is positioned on the final screen. At the end of the game, currentScore animates
    // from its original position/size to the position/size of finalScorePlaceholder.
    private TextView finalScorePlaceholder;
    // An invisible placeholder which is positioned at the center of the screen and is used as the
    // intermediate position/size before the score drops into its final position.
    private TextView centeredScorePlaceholder;
    // Text that says "Game Over"
    private TextView gameOverText;
    // Widgets on the end screen.
    private TextView bestScore;
    private ImageView shareImage;
    private LinearLayout menuItems;
    private GameOverlayButton shareButton;
    // A semi-opaque background which darkens the game during the end screen.
    private View background;
    // Initial state for the views involved in the end-screen animation, stored so it can be
    // restored if "replay" is tapped.
    private float currentScoreX = Float.NaN;
    private float currentScoreY = Float.NaN;
    private int currentScoreMarginStart;
    private int currentScoreMarginTop;
    private float currentScoreTextSizePx;
    private float currentScoreAlpha;
    private float finalScoreMaxWidth;
    private float finalScoreMaxHeight;
    private float centeredScoreMaxWidth;
    private float centeredScoreMaxHeight;
    private float backgroundAlpha;
    private int mCurrentScoreValue;
    private LinearLayout currentStars;
    private RelativeLayout finalStars;
    private int filledStarCount;
    private boolean canReplay;

    private OnShareClickedListener shareClickedListener;

    public ScoreView(Context context, OnShareClickedListener shareClickedListener) {
        this(context, (AttributeSet) null);
        this.shareClickedListener = shareClickedListener;
    }

    public ScoreView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScoreView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        loadLayout(context);
        resetToStartState();
    }

    public void setLogger(DoodleLogger logger) {
        this.logger = logger;
    }

    protected void loadLayout(final Context context) {
        setVisibility(View.INVISIBLE);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.score_view, this);

        gameOverText = (TextView) view.findViewById(R.id.text_game_over);
        gameOverText.setVisibility(INVISIBLE);

        currentScore = (TextView) view.findViewById(R.id.current_score);
        // Store these for later so we can put currentScore back where it started after animating
        // it.
        currentScoreTextSizePx = currentScore.getTextSize();
        currentScoreAlpha = currentScore.getAlpha();
        currentScore.post(
                new Runnable() {
                    @Override
                    public void run() {
                        currentScoreX = currentScore.getX();
                        currentScoreY = currentScore.getY();
                    }
                });
        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) currentScore.getLayoutParams();
        if (VERSION.SDK_INT >= 17) {
            currentScoreMarginStart = params.getMarginStart();
        } else {
            currentScoreMarginStart = params.leftMargin;
        }
        currentScoreMarginTop = params.topMargin;

        finalScorePlaceholder = (TextView) view.findViewById(R.id.final_score_placeholder);
        finalScorePlaceholder.setVisibility(INVISIBLE);
        finalScoreMaxWidth = getResources().getDimension(R.dimen.final_score_max_width);
        finalScoreMaxHeight = getResources().getDimension(R.dimen.final_score_max_height);
        finalScorePlaceholder.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void afterTextChanged(Editable editable) {
                        UIUtil.fitToBounds(
                                finalScorePlaceholder, finalScoreMaxWidth, finalScoreMaxHeight);
                    }
                });

        centeredScorePlaceholder = (TextView) view.findViewById(R.id.centered_score_placeholder);
        centeredScorePlaceholder.setVisibility(INVISIBLE);
        centeredScoreMaxWidth = getResources().getDimension(R.dimen.centered_score_max_width);
        centeredScoreMaxHeight = getResources().getDimension(R.dimen.centered_score_max_height);
        centeredScorePlaceholder.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void afterTextChanged(Editable editable) {
                        UIUtil.fitToBounds(
                                centeredScorePlaceholder,
                                centeredScoreMaxWidth,
                                centeredScoreMaxHeight);
                    }
                });

        currentStars = (LinearLayout) view.findViewById(R.id.current_stars);
        currentStars
                .removeAllViews(); // Remove the stickers that are in the XML for testing layout.
        finalStars = (RelativeLayout) view.findViewById(R.id.final_stars);

        bestScore = (TextView) view.findViewById(R.id.best_score);
        shareImage = (ImageView) view.findViewById(R.id.share_image);
        shareImage.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        replay();
                    }
                });

        menuItems = (LinearLayout) view.findViewById(R.id.menu_items);
        View replayButton = view.findViewById(R.id.replay_button);
        replayButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        replay();
                    }
                });

        shareButton = (GameOverlayButton) view.findViewById(R.id.share_button);
        shareButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EventBus.getInstance()
                                .sendEvent(EventBus.PLAY_SOUND, R.raw.menu_item_click);
                        logger.logEvent(
                                new Builder(DEFAULT_DOODLE_NAME, SHARE_CLICKED)
                                        .withEventSubType(listener.gameType())
                                        .withEventValue1(listener.shareImageId())
                                        .build());

                        if (shareClickedListener != null) {
                            shareClickedListener.onShareClicked();
                        }
                    }
                });

        View moreGamesButton = view.findViewById(R.id.menu_button);
        moreGamesButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        goToMoreGames(context);
                    }
                });
        background = view.findViewById(R.id.score_view_background);
        backgroundAlpha = background.getAlpha(); // Store for later use.
    }

    protected void goToMoreGames(Context context) {
        EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.menu_item_click);
        logger.logEvent(
                new Builder(DEFAULT_DOODLE_NAME, HOME_CLICKED)
                        .withEventSubType(listener.gameType())
                        .build());
        AndroidUtils.finishActivity(context);
    }

    private void replay() {
        if (canReplay && listener != null) {
            EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.menu_item_click);
            logger.logEvent(
                    new Builder(DEFAULT_DOODLE_NAME, REPLAY_CLICKED)
                            .withEventSubType(listener.gameType())
                            .build());

            listener.onReplay();
            AndroidUtils.forceScreenToStayOn(getContext());
        }
    }

    public void setListener(LevelFinishedListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the best score field on the end screen to display the given best score.
     *
     * @param newScore The score to be displayed as the best score.
     */
    public void updateBestScore(CharSequence newScore) {
        bestScore.setText(
                AndroidUtils.getText(
                        getResources(),
                        com.google.android.apps.santatracker.common.R.string.end_screen_best_score,
                        newScore));
    }

    /**
     * Sets the end screen header to the given text.
     *
     * <p>
     *
     * <p>This header will be shown in place of the best score.
     *
     * @param text The text to be put into the header.
     */
    public void setHeaderText(CharSequence text) {
        bestScore.setText(text);
    }

    public void updateCurrentScore(CharSequence newScore, boolean shouldBump) {
        currentScore.setText(newScore);
        finalScorePlaceholder.setText(newScore);
        centeredScorePlaceholder.setText(newScore);
        if (shouldBump) {
            animateBump(currentScore);
        }
    }

    public void setShareDrawable(Drawable drawable) {
        shareImage.setImageDrawable(drawable);
    }

    public void clearAllStars() {
        currentStars.removeAllViews();
        filledStarCount = 0;
        for (int i = 0; i < finalStars.getChildCount(); i++) {
            FrameLayout star = (FrameLayout) finalStars.getChildAt(i);
            star.findViewById(R.id.fill).setVisibility(INVISIBLE);
        }
    }

    public void addStar() {
        if (filledStarCount < 3) {
            filledStarCount++;
            int currentStarDimens = (int) AndroidUtils.dipToPixels(40);
            addStarToLayout(
                    currentStars, currentStarDimens, LinearLayout.LayoutParams.MATCH_PARENT);
            EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.ui_positive_sound);
        }
    }

    public int getStarCount() {
        return filledStarCount;
    }

    // Width & height are in pixels.
    private void addStarToLayout(LinearLayout layout, int width, int height) {
        ImageView image = new ImageView(getContext());
        image.setImageResource(R.drawable.pineapple_star_filled);
        animateBump(image);
        layout.addView(image, new LinearLayout.LayoutParams(width, height));
    }

    public void resetToStartState() {
        SantaLog.i(TAG, "Reset to start state");
        currentStars.setVisibility(VISIBLE);

        bestScore.setVisibility(INVISIBLE);
        shareImage.setVisibility(INVISIBLE);
        menuItems.setVisibility(INVISIBLE);
        shareButton.setVisibility(INVISIBLE);
        background.setVisibility(INVISIBLE);
        finalStars.setVisibility(INVISIBLE);
        gameOverText.setVisibility(INVISIBLE);

        if (!Float.isNaN(currentScoreX)) {
            currentScore.setX(currentScoreX);
        }
        if (!Float.isNaN(currentScoreY)) {
            currentScore.setY(currentScoreY);
        }
        currentScore.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentScoreTextSizePx);
        RelativeLayout.LayoutParams params =
                (RelativeLayout.LayoutParams) currentScore.getLayoutParams();

        if (VERSION.SDK_INT >= 17) {
            params.setMarginStart(currentScoreMarginStart);
        } else {
            params.leftMargin = currentScoreMarginStart;
        }
        params.topMargin = currentScoreMarginTop;

        updateCurrentScore(Integer.toString(0), false);
        clearAllStars();

        currentScore.setAlpha(0);
        ValueAnimator fadeInCurrentScore =
                UIUtil.animator(
                        RESET_FADE_IN_MS,
                        new AccelerateDecelerateInterpolator(),
                        new AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                currentScore.setAlpha(
                                        (float) valueAnimator.getAnimatedValue("alpha"));
                            }
                        },
                        UIUtil.floatValue("alpha", 0, currentScoreAlpha));
        fadeInCurrentScore.start();
    }

    public void animateToEndState() {
        AndroidUtils.allowScreenToTurnOff(getContext());
        SantaLog.i(TAG, "Animate to end state");
        canReplay = false;
        setVisibility(View.VISIBLE);
        logger.logEvent(
                new Builder(DEFAULT_DOODLE_NAME, GAME_OVER)
                        .withEventSubType(listener.gameType())
                        .withLatencyMs(DoodleLogTimer.getInstance().timeElapsedMs())
                        .withEventValue1(listener.score())
                        .withEventValue2(getStarCount())
                        .build());

        // TODO: Fade this out instead of making it invisible (will have to remove
        // the layout:alignComponents that attach it current score, else it will move along with the
        // score.)
        currentStars.setVisibility(INVISIBLE);

        // Initial state: controls & background are visible but alpha = 0
        bestScore.setAlpha(0);
        shareImage.setAlpha(0.0f);
        background.setAlpha(0);
        finalStars.setAlpha(0);

        bestScore.setVisibility(VISIBLE);
        shareImage.setVisibility(VISIBLE);
        background.setVisibility(VISIBLE);
        finalStars.setVisibility(VISIBLE);
        gameOverText.setVisibility(VISIBLE);

        // Offset the share image and stars so that they can bounce in.
        final float shareImageY = shareImage.getY();
        final float finalStarsY = finalStars.getY();
        shareImage.setY(shareImageY + SHARE_Y_OFFSET_PX);

        // Zoom the score to center of screen.
        // I tried several other ways of doing this animation, none of which worked:
        // 1. Using TranslateAnimation & ScaleAnimation instead of .animate(): Positions didn't work
        //    right when scaling, maybe because these animate how a view is displayed but not the
        // actual
        //    view properties.
        // 2. Using TranslateAnimation & ObjectAnimator: couldn't add ObjectAnimator to the same
        //    Animation set as TranslateAnimation.
        // 3. Using .animate() to get a PropertyAnimator. Couldn't tween textSize without using
        //    .setUpdateListener, which requires API 19.
        // 4. Small textSize, scaling up from 1: Text is blurry at scales > 1.
        // 5. Large textSize, scaling up to 1: Final position was wrong, I couldn't figure out why.
        // 6. Medium textSize, scaling up to 2.5: Error: font size too large to fit in cache. Tried
        //    turning off HW accel which fixed the cache errors but positioning was still wrong.
        ValueAnimator zoomUp =
                UIUtil.animator(
                        ZOOM_UP_MS,
                        new ElasticOutInterpolator(0.35f),
                        new AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                currentScore.setX((float) valueAnimator.getAnimatedValue("x"));
                                currentScore.setY((float) valueAnimator.getAnimatedValue("y"));
                                currentScore.setTextSize(
                                        TypedValue.COMPLEX_UNIT_PX,
                                        (float) valueAnimator.getAnimatedValue("textSize"));
                                RelativeLayout.LayoutParams params =
                                        (RelativeLayout.LayoutParams)
                                                currentScore.getLayoutParams();
                                if (VERSION.SDK_INT >= 17) {
                                    params.setMarginStart(
                                            (int)
                                                    (float)
                                                            valueAnimator.getAnimatedValue(
                                                                    "marginStart"));
                                } else {
                                    params.leftMargin =
                                            (int)
                                                    (float)
                                                            valueAnimator.getAnimatedValue(
                                                                    "marginStart");
                                }
                                params.topMargin =
                                        (int) (float) valueAnimator.getAnimatedValue("topMargin");
                                currentScore.setAlpha(
                                        (float)
                                                valueAnimator.getAnimatedValue(
                                                        "currentScoreAlpha"));
                            }
                        },
                        UIUtil.floatValue(
                                "x", currentScore.getX(), centeredScorePlaceholder.getX()),
                        UIUtil.floatValue(
                                "y", currentScore.getY(), centeredScorePlaceholder.getY()),
                        UIUtil.floatValue(
                                "textSize",
                                currentScoreTextSizePx,
                                centeredScorePlaceholder.getTextSize()),
                        UIUtil.floatValue("marginStart", currentScoreMarginStart, 0),
                        UIUtil.floatValue("topMargin", currentScoreMarginTop, 0),
                        UIUtil.floatValue("currentScoreAlpha", currentScoreAlpha, 1));

        // Zoom the score up to its final position.
        ValueAnimator zoomBackDown =
                UIUtil.animator(
                        ZOOM_DOWN_MS,
                        new BounceInterpolator(),
                        new AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                currentScore.setX((float) valueAnimator.getAnimatedValue("x"));
                                currentScore.setY((float) valueAnimator.getAnimatedValue("y"));
                                currentScore.setTextSize(
                                        TypedValue.COMPLEX_UNIT_PX,
                                        (float) valueAnimator.getAnimatedValue("textSize"));
                            }
                        },
                        UIUtil.floatValue(
                                "x", centeredScorePlaceholder.getX(), finalScorePlaceholder.getX()),
                        UIUtil.floatValue(
                                "y", centeredScorePlaceholder.getY(), finalScorePlaceholder.getY()),
                        UIUtil.floatValue(
                                "textSize",
                                centeredScorePlaceholder.getTextSize(),
                                finalScorePlaceholder.getTextSize()));

        ValueAnimator fadeInBackground =
                UIUtil.animator(
                        BG_FADE_IN_MS,
                        new AccelerateDecelerateInterpolator(),
                        new AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                background.setAlpha(
                                        (float) valueAnimator.getAnimatedValue("bgAlpha"));
                            }
                        },
                        UIUtil.floatValue("bgAlpha", 0, backgroundAlpha));

        ValueAnimator fadeInBestScore =
                UIUtil.animator(
                        BG_FADE_IN_MS,
                        new AccelerateDecelerateInterpolator(),
                        new AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                bestScore.setAlpha(
                                        (float) valueAnimator.getAnimatedValue("bgAlpha"));
                            }
                        },
                        UIUtil.floatValue("bgAlpha", 0, backgroundAlpha));

        ValueAnimator fadeInMenuItems =
                UIUtil.animator(
                        BG_FADE_IN_MS,
                        new AccelerateDecelerateInterpolator(),
                        new AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                float alpha = (float) valueAnimator.getAnimatedValue("alpha");
                                menuItems.setAlpha(alpha);
                                shareButton.setAlpha(alpha);
                            }
                        },
                        UIUtil.floatValue("alpha", 0, 1));
        fadeInMenuItems.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        // Don't set menu items to be visible until the animation starts so that
                        // they aren't
                        // clickable until they start to appear.
                        menuItems.setVisibility(VISIBLE);
                        menuItems.setAlpha(0);

                        shareButton.setVisibility(VISIBLE);
                        shareButton.setAlpha(0);

                        canReplay = true;
                    }
                });

        ValueAnimator fadeInShareImageAndFinalStars =
                UIUtil.animator(
                        SHARE_FADE_IN_MS,
                        new AccelerateDecelerateInterpolator(),
                        new AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                float alpha = (float) valueAnimator.getAnimatedValue("alpha");
                                shareImage.setAlpha(alpha);
                                finalStars.setAlpha(alpha);
                            }
                        },
                        UIUtil.floatValue("alpha", 0, 1));

        ValueAnimator dropShareImageAndFinalStars =
                UIUtil.animator(
                        SHARE_DROP_MS,
                        new BounceInterpolator(),
                        new AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                float yOffset = (float) valueAnimator.getAnimatedValue("yOffset");
                                shareImage.setY(shareImageY + yOffset);
                            }
                        },
                        UIUtil.floatValue("yOffset", SHARE_Y_OFFSET_PX, 0));

        AnimatorSet animations = new AnimatorSet();

        int numStars = finalStars.getChildCount();
        ValueAnimator bounce = null;
        long starStartDelay = ZOOM_UP_MS + SHARE_FADE_IN_MS + SHARE_DROP_MS / 2;
        for (int i = 0; i < filledStarCount; i++) {
            FrameLayout star = (FrameLayout) finalStars.getChildAt(numStars - i - 1);
            ValueAnimator fade = getStarFadeIn((ImageView) star.findViewById(R.id.fill));
            bounce = getStarBounceIn((ImageView) star.findViewById(R.id.fill));
            animations.play(fade).after(starStartDelay + STAR_FADE_IN_MS * i);
            animations.play(bounce).after(starStartDelay + STAR_FADE_IN_MS * i);
        }
        if (bounce != null) {
            animations.play(fadeInMenuItems).after(bounce);
        } else {
            animations.play(fadeInMenuItems).after(starStartDelay + STAR_FADE_IN_MS);
        }
        animations.play(fadeInBackground).with(zoomUp);
        animations.play(fadeInBestScore).after(fadeInBackground);
        animations.play(zoomBackDown).after(zoomUp);
        animations.play(fadeInShareImageAndFinalStars).after(zoomUp);
        animations.play(dropShareImageAndFinalStars).after(fadeInShareImageAndFinalStars);
        animations.start();
    }

    private ValueAnimator getStarFadeIn(final ImageView star) {
        star.setAlpha(0.0f);
        star.setVisibility(VISIBLE);
        ValueAnimator fadeIn =
                UIUtil.animator(
                        STAR_FADE_IN_MS,
                        new AccelerateDecelerateInterpolator(),
                        new AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                float alpha = (float) valueAnimator.getAnimatedValue("alpha");
                                star.setAlpha(alpha);
                            }
                        },
                        UIUtil.floatValue("alpha", 0, 1));
        fadeIn.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        EventBus.getInstance()
                                .sendEvent(EventBus.PLAY_SOUND, R.raw.ui_positive_sound);
                    }
                });
        return fadeIn;
    }

    private ValueAnimator getStarBounceIn(final ImageView star) {
        return UIUtil.animator(
                STAR_BOUNCE_IN_MS,
                new BounceInterpolator(),
                new AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        float scale = (float) valueAnimator.getAnimatedValue("scale");
                        star.setScaleX(scale);
                        star.setScaleY(scale);
                    }
                },
                UIUtil.floatValue("scale", STAR_BIG_SCALE, 1));
    }

    private void animateBump(final View view) {
        ValueAnimator tween =
                UIUtil.animator(
                        BUMP_MS,
                        new OvershootInterpolator(),
                        new AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                float scale = (float) valueAnimator.getAnimatedValue("scale");
                                view.setScaleX(scale);
                                view.setScaleY(scale);
                            }
                        },
                        UIUtil.floatValue("scale", 1.5f, 1));
        tween.start();
    }

    public interface OnShareClickedListener {
        void onShareClicked();
    }

    /** A listener for events which occur from the level finished screen. */
    public interface LevelFinishedListener {
        void onReplay();

        String gameType();

        float score();

        int shareImageId();
    }
}
