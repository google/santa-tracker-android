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
package com.google.android.apps.santatracker.doodles.shared;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.google.android.apps.santatracker.doodles.R;
import com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.Builder;
import com.google.android.apps.santatracker.doodles.shared.sound.SoundManager;

import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.DEFAULT_DOODLE_NAME;
import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.HOME_CLICKED;
import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.MUTE_CLICKED;
import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.PAUSE_CLICKED;
import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.REPLAY_CLICKED;
import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.UNMUTE_CLICKED;
import static com.google.android.apps.santatracker.doodles.shared.PineappleLogEvent.UNPAUSE_CLICKED;

/**
 * The overlay which is shown when a game is paused.
 */
public class PauseView extends FrameLayout {

  private static final int BIG_PAUSE_FADE_IN_MS = 200;  // Fading in paused screen elements.
  private static final int FADE_IN_MS = 500;  // Fading in paused screen elements.
  private static final int FADE_OUT_MS = 200;  // Fading out paused screen elements.
  private static final int BUMP_MS = 200;  // The paused text over-zooms a bit on pause.
  private static final int RELAX_MS = 200;  // The paused text shrinks a bit after zooming up.
  private static final float ZOOM_UP_SCALE_OVERSHOOT = 1.2f;

  public static final int FADE_DURATION_MS = 400;  // The pause button fading in and out.

  /**
   * A listener for interacting with the PauseView.
   */
  public interface GamePausedListener {
    void onPause();
    void onResume();
    void onReplay();

    String gameType();
    float score();
  }
  private GamePausedListener listener;
  private DoodleConfig doodleConfig;
  private PineappleLogger logger;

  private ImageButton muteButton;
  private GameOverlayButton pauseButton;
  private View resumeButton;
  private View buttonContainer;
  private View background;

  public boolean isPauseButtonEnabled = true;
  private float backgroundAlpha;
  private float pauseButtonAlpha;

  public PauseView(Context context) {
    this(context, null);
  }

  public PauseView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public PauseView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    loadLayout(context);
    hidePauseScreen();
  }

  public void setDoodleConfig(DoodleConfig doodleConfig) {
    this.doodleConfig = doodleConfig;
  }

  public void setLogger(PineappleLogger logger) {
    this.logger = logger;
  }

  public GamePausedListener getListener() {
    return this.listener;
  }

  public void setListener(GamePausedListener listener) {
    this.listener = listener;
  }

  public void hidePauseButton() {
    isPauseButtonEnabled = false;
    UIUtil.fadeOutAndHide(pauseButton, FADE_DURATION_MS, pauseButtonAlpha);
  }

  public void showPauseButton() {
    isPauseButtonEnabled = true;
    UIUtil.showAndFadeIn(pauseButton, FADE_DURATION_MS, pauseButtonAlpha);
  }

  public void onFinishedLoading() {
    setVisibility(View.VISIBLE);
  }

  protected void loadLayout(final Context context) {
    setVisibility(View.INVISIBLE);
    LayoutInflater inflater = LayoutInflater.from(context);

    View view = inflater.inflate(R.layout.pause_view, this);

    buttonContainer = view.findViewById(R.id.button_container);

    muteButton = (ImageButton) view.findViewById(R.id.mute_button);
    if (SoundManager.soundsAreMuted) {
      muteButton.setImageResource(R.drawable.common_btn_speaker_off);
    }
    muteButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        boolean shouldMute = !SoundManager.soundsAreMuted;

        String logEventName = shouldMute ? MUTE_CLICKED : UNMUTE_CLICKED;
        logger.logEvent(new Builder(DEFAULT_DOODLE_NAME, logEventName)
            .withEventSubType(listener.gameType()).build());

        muteButton.setImageResource(
            shouldMute ? R.drawable.common_btn_speaker_off : R.drawable.common_btn_speaker_on);
        muteButton.setContentDescription(context.getResources().getString(
            shouldMute ? R.string.unmute : R.string.mute));
        EventBus.getInstance().sendEvent(EventBus.MUTE_SOUNDS, shouldMute);
      }
    });

    pauseButton = (GameOverlayButton) view.findViewById(R.id.pause_button);
    pauseButtonAlpha = pauseButton.getAlpha();
    pauseButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        pause();
      }
    });

    resumeButton = view.findViewById(R.id.resume_button);
    resumeButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        logger.logEvent(new Builder(DEFAULT_DOODLE_NAME, UNPAUSE_CLICKED)
            .withEventSubType(listener.gameType()).build());
        unpause();
      }
    });

    View replayButton = view.findViewById(R.id.replay_button);
    replayButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        logger.logEvent(new Builder(DEFAULT_DOODLE_NAME, REPLAY_CLICKED)
            .withEventSubType(listener.gameType())
            .withLatencyMs(PineappleLogTimer.getInstance().timeElapsedMs())
            .withEventValue1(listener.score())
            .build());
        replay();
      }
    });

    View menuButton = view.findViewById(R.id.menu_button);
    menuButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.menu_item_click);

        logger.logEvent(new Builder(DEFAULT_DOODLE_NAME, HOME_CLICKED)
            .withEventSubType(listener.gameType())
            .withLatencyMs(PineappleLogTimer.getInstance().timeElapsedMs())
            .withEventValue1(listener.score())
            .build());

        LaunchDecisionMaker.finishActivity(context);
      }
    });

    background = view.findViewById(R.id.pause_view_background);
    backgroundAlpha = background.getAlpha();
  }

  private void replay() {
    PineappleLogTimer.getInstance().unpause();
    hidePauseScreen();
    if (listener != null) {
      EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.menu_item_click);
      listener.onReplay();
    }
  }

  /**
   * Pauses the current game.
   */
  public void pause() {
    if (!isPauseButtonEnabled) {
      return;
    }
    logger.logEvent(new Builder(DEFAULT_DOODLE_NAME, PAUSE_CLICKED)
            .withEventSubType(listener.gameType())
            .withLatencyMs(PineappleLogTimer.getInstance().timeElapsedMs())
            .build());
    PineappleLogTimer.getInstance().pause();
    if (listener != null) {
      EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.menu_item_click);
      listener.onPause();
    }
    showPauseScreen();
    AndroidUtils.allowScreenToTurnOff(getContext());
  }

  private void unpause() {
    PineappleLogTimer.getInstance().unpause();
    hidePauseScreen();
    if (listener != null) {
      EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.menu_item_click);
      listener.onResume();
    }
    AndroidUtils.forceScreenToStayOn(getContext());
  }

  private void showPauseScreen() {
    isPauseButtonEnabled = false;
    SoundManager soundManager = SoundManager.getInstance();
    soundManager.mute(R.raw.fruit_doodle_music);
    soundManager.pauseShortSounds();

    if (SoundManager.soundsAreMuted) {
      muteButton.setImageResource(R.drawable.common_btn_speaker_off);
    } else {
      muteButton.setImageResource(R.drawable.common_btn_speaker_on);
    }

    muteButton.setAlpha(0.0f);
    resumeButton.setAlpha(0.0f);
    buttonContainer.setAlpha(0);
    background.setAlpha(0);

    muteButton.setVisibility(VISIBLE);
    resumeButton.setVisibility(VISIBLE);
    buttonContainer.setVisibility(VISIBLE);
    background.setVisibility(VISIBLE);

    ValueAnimator fadeBigPauseIn = UIUtil.animator(BIG_PAUSE_FADE_IN_MS,
        new AccelerateDecelerateInterpolator(),
        new AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            resumeButton.setAlpha((float) valueAnimator.getAnimatedValue("textAlpha"));
            background.setAlpha((float) valueAnimator.getAnimatedValue("bgAlpha"));
          }
        },
        UIUtil.floatValue("textAlpha", 0, 1),
        UIUtil.floatValue("bgAlpha", 0, backgroundAlpha)
    );

    ValueAnimator fadePauseButtonOut = UIUtil.animator(BIG_PAUSE_FADE_IN_MS,
        new AccelerateDecelerateInterpolator(),
        new AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            pauseButton.setAlpha((float) valueAnimator.getAnimatedValue("pauseButtonAlpha"));
          }
        },
        UIUtil.floatValue("pauseButtonAlpha", pauseButtonAlpha, 0)
    );
    fadePauseButtonOut.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        pauseButton.setVisibility(INVISIBLE);
      }
    });

    ValueAnimator zoomUp = UIUtil.animator(BUMP_MS, new AccelerateDecelerateInterpolator(),
        new AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            float scale = (float) valueAnimator.getAnimatedValue("scale");
            resumeButton.setScaleX(scale);
            resumeButton.setScaleY(scale);
          }
        },
        UIUtil.floatValue("scale", 0, ZOOM_UP_SCALE_OVERSHOOT)
    );

    ValueAnimator relax = UIUtil.animator(RELAX_MS, new OvershootInterpolator(),
        new AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            float scale = (float) valueAnimator.getAnimatedValue("scale");
            resumeButton.setScaleX(scale);
            resumeButton.setScaleY(scale);
          }
        },
        UIUtil.floatValue("scale", ZOOM_UP_SCALE_OVERSHOOT, 1));

    ValueAnimator fadeIn = UIUtil.animator(FADE_IN_MS, new AccelerateDecelerateInterpolator(),
        new AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            muteButton.setAlpha((float) valueAnimator.getAnimatedValue("alpha"));
            buttonContainer.setAlpha((float) valueAnimator.getAnimatedValue("alpha"));
          }
        },
        UIUtil.floatValue("alpha", 0, 1)
    );
    AnimatorSet animations = new AnimatorSet();
    animations.play(fadeBigPauseIn).with(zoomUp);
    animations.play(fadePauseButtonOut).with(zoomUp);
    animations.play(relax).after(zoomUp);
    animations.play(fadeIn).after(zoomUp);
    animations.start();
  }

  private void hidePauseScreen() {
    SoundManager soundManager = SoundManager.getInstance();
    soundManager.unmute(R.raw.fruit_doodle_music);
    soundManager.resumeShortSounds();

    ValueAnimator fadeOut = UIUtil.animator(FADE_OUT_MS, new AccelerateDecelerateInterpolator(),
        new AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            muteButton.setAlpha((float) valueAnimator.getAnimatedValue("overlayAlpha"));
            background.setAlpha((float) valueAnimator.getAnimatedValue("bgAlpha"));
            buttonContainer.setAlpha((float) valueAnimator.getAnimatedValue("overlayAlpha"));

            resumeButton.setAlpha((float) valueAnimator.getAnimatedValue("overlayAlpha"));
            resumeButton.setScaleX((float) valueAnimator.getAnimatedValue("iconScale"));
            resumeButton.setScaleY((float) valueAnimator.getAnimatedValue("iconScale"));

          }
        },
        UIUtil.floatValue("overlayAlpha", 1, 0),
        UIUtil.floatValue("bgAlpha", backgroundAlpha, 0),
        UIUtil.floatValue("iconScale", 1, 2)
    );
    fadeOut.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        muteButton.setVisibility(INVISIBLE);
        resumeButton.setVisibility(INVISIBLE);
        buttonContainer.setVisibility(INVISIBLE);
        background.setVisibility(INVISIBLE);

        isPauseButtonEnabled = true;
      }
    });

    pauseButton.setAlpha(0.0f);
    pauseButton.setVisibility(VISIBLE);
    ValueAnimator fadePauseButtonIn = UIUtil.animator(FADE_OUT_MS,
        new AccelerateDecelerateInterpolator(),
        new AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            pauseButton.setAlpha((float) valueAnimator.getAnimatedValue("alpha"));
          }
        },
        UIUtil.floatValue("alpha", 0, pauseButtonAlpha)
    );

    AnimatorSet animations = new AnimatorSet();
    animations.play(fadeOut).with(fadePauseButtonIn);
    animations.start();
  }
}
