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
package com.google.android.apps.santatracker.doodles.presenttoss;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Vibrator;
import com.google.android.apps.santatracker.doodles.presenttoss.ElfActor.WaterPoloActorPart;
import com.google.android.apps.santatracker.doodles.shared.ColoredRectangleActor;
import com.google.android.apps.santatracker.doodles.shared.EventBus;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.actor.Actor;
import com.google.android.apps.santatracker.doodles.shared.actor.ActorHelper;
import com.google.android.apps.santatracker.doodles.shared.actor.CameraShake;
import com.google.android.apps.santatracker.doodles.shared.actor.RectangularInstructionActor;
import com.google.android.apps.santatracker.doodles.shared.actor.SpriteActor;
import com.google.android.apps.santatracker.doodles.shared.actor.TextActor;
import com.google.android.apps.santatracker.doodles.shared.animation.ActorTween;
import com.google.android.apps.santatracker.doodles.shared.animation.ActorTween.Callback;
import com.google.android.apps.santatracker.doodles.shared.animation.AnimatedSprite;
import com.google.android.apps.santatracker.doodles.shared.animation.AnimatedSprite.AnimatedSpriteListener;
import com.google.android.apps.santatracker.doodles.shared.animation.EmptyTween;
import com.google.android.apps.santatracker.doodles.shared.animation.Interpolator;
import com.google.android.apps.santatracker.doodles.shared.animation.Tween;
import com.google.android.apps.santatracker.doodles.shared.animation.TweenManager;
import com.google.android.apps.santatracker.doodles.shared.views.GameFragment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Model for the Water Polo game. */
public class PresentTossModel {

    public static final int WATER_POLO_HEIGHT = 960;
    public static final int WATER_POLO_WIDTH = 540;
    public static final int ONE_STAR_THRESHOLD = 1;
    public static final int TWO_STAR_THRESHOLD = 10;
    public static final int THREE_STAR_THRESHOLD = 20;
    private static final String TAG = PresentTossModel.class.getSimpleName();
    private static final float SHAKE_FALLOFF = 0.9f;
    private static final int VIBRATION_SMALL = 40;
    private static final float TIME_LEFT_TEXT_X = WATER_POLO_WIDTH * 0.5f;
    private static final float TIME_LEFT_TEXT_Y = 12;
    private static final float TIME_LEFT_TEXT_SCALE = 3.2f;
    private static final String TIME_LEFT_UNDER_TEXT = "88:88";
    private static final int TIME_LEFT_TEXT_RGB = 0xFFFFBD2E;
    private static final int TIME_LEFT_TEXT_GLOW_RGB = 0x88FF9A2E;
    private static final int TIME_LEFT_UNDER_RGB = 0xFF3B200D;
    private static final float POINT_TEXT_ANIMATION_TIME = 1.6f;
    private static final int POINT_MINUS_TEXT_RGB = 0xffd61e1e;
    private static final int POINT_PLUS_TEXT_RGB = 0xff4dab1f;
    // Currently throw delay is not supported. The constant and related variables are left in to
    // facilitate prototyping in case throw delay would be supported in the future.
    private static final float THROW_DELAY_SECONDS = 0.4f;
    private static final float TOTAL_TIME = 30f;
    private static final int BALL_SPEED = 1300;
    private static final int OPPONENT_ONE_ENTRANCE_THRESHOLD = 1;
    private static final int OPPONENT_TWO_ENTRANCE_THRESHOLD = 4;
    private static final int OPPONENT_THREE_ENTRANCE_THRESHOLD = 10;
    private static final int OPPONENT_ONE_SPEED = 60;
    private static final int OPPONENT_TWO_SPEED = 120;
    private static final int OPPONENT_THREE_SPEED = 170;
    private static final int MS_BETWEEN_TARGET_FLASHES = 1000;
    private static final float GOAL_BOX_X = 108;
    private static final float GOAL_BOX_Y = 75;
    private static final float GOAL_BOX_WIDTH = 333;
    private static final float GOAL_BOX_HEIGHT = 98;
    private final Vibrator vibrator;
    public CameraShake cameraShake;
    public List<Actor> actors;
    public State state;
    public int score;
    public int ballsThrown;
    public long titleDurationMs = GameFragment.TITLE_DURATION_MS;
    RectangularInstructionActor instructions;
    private List<Actor> effects;
    private Resources resources;
    private EventBus eventBus;
    private TweenManager tweenManager;
    private ElfActor player;
    private ElfActor opponentOne;
    private ElfActor opponentTwo;
    private ElfActor opponentThree;
    private ColoredRectangleActor timeLeftFrameBorder;
    private ColoredRectangleActor timeLeftFrame;
    private TextActor timeLeftText;
    private TextActor timeLeftTextGlow;
    private TextActor timeLeftUnder;
    private List<PresentActor> balls;
    private SpriteActor slideBack;
    private SpriteActor targetLeft;
    private SpriteActor targetMiddle;
    private SpriteActor targetRight;
    private SpriteActor goal;
    private float time;
    private float msTillNextTargetPulse;
    private boolean scoredAtLeastOneShot;
    private boolean canThrow;
    private boolean didReset;

    public PresentTossModel(Resources resources, Context context) {
        this.resources = resources;
        actors = Collections.synchronizedList(new ArrayList<Actor>());
        effects = new ArrayList<>();
        balls = new ArrayList<>();
        cameraShake = new CameraShake();
        actors.add(cameraShake);
        tweenManager = new TweenManager();
        eventBus = EventBus.getInstance();
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        createActors();
        reset(true);
    }

    private void createActors() {
        Actor pool = actorWithIds(PresentTossSprites.present_throw_floor);
        pool.zIndex = -3;

        // The pool image is 780x960, but the center 540x960 of that image is what corresponds
        // to the game area, so offset the image to the left. The edges only show on screens with
        // aspect
        // ratios wider than 9:16.
        pool.position.x = -120;
        actors.add(pool);

        goal = actorWithIds(PresentTossSprites.present_throw_santabag);
        goal.position.set(-14, 72);
        goal.zIndex = -4;
        goal.sprite.setLoop(false);
        goal.sprite.setPaused(true);
        goal.sprite.addListener(
                new AnimatedSpriteListener() {
                    @Override
                    public void onFinished() {
                        goal.sprite.setFrameIndex(0);
                        goal.sprite.setPaused(true);
                    }
                });
        actors.add(goal);

        slideBack = actorWithIds(PresentTossSprites.present_throw_elfbag);
        slideBack.zIndex = 4;
        actors.add(slideBack);

        moveSlide(0);

        timeLeftFrame =
                new ColoredRectangleActor(
                        Vector2D.get(WATER_POLO_WIDTH * 0.35f, TIME_LEFT_TEXT_Y - 6),
                        Vector2D.get(WATER_POLO_WIDTH * 0.3f, 50));
        timeLeftFrame.setColor(0xff000000);
        timeLeftFrame.zIndex = 4;
        actors.add(timeLeftFrame);

        timeLeftFrameBorder =
                new ColoredRectangleActor(
                        Vector2D.get(WATER_POLO_WIDTH * 0.35f, TIME_LEFT_TEXT_Y - 6),
                        Vector2D.get(WATER_POLO_WIDTH * 0.3f, 50));
        timeLeftFrameBorder.setStyle(Paint.Style.STROKE);
        timeLeftFrameBorder.setStrokeWidth(5);
        timeLeftFrameBorder.setColor(0xff555555);
        timeLeftFrameBorder.zIndex = 4;
        actors.add(timeLeftFrameBorder);

        timeLeftUnder = new TextActor(TIME_LEFT_UNDER_TEXT);
        timeLeftUnder.position.set(TIME_LEFT_TEXT_X, TIME_LEFT_TEXT_Y);
        timeLeftUnder.scale = TIME_LEFT_TEXT_SCALE;
        timeLeftUnder.setBold(true);
        timeLeftUnder.setFont(resources.getAssets(), "dseg7.ttf");
        timeLeftUnder.setColor(TIME_LEFT_UNDER_RGB);
        timeLeftUnder.alignCenter();
        timeLeftUnder.zIndex = 4;
        actors.add(timeLeftUnder);

        timeLeftTextGlow = new TextActor("00:30");
        timeLeftTextGlow.position.set(TIME_LEFT_TEXT_X, TIME_LEFT_TEXT_Y);
        timeLeftTextGlow.scale = TIME_LEFT_TEXT_SCALE;
        timeLeftTextGlow.setBold(true);
        timeLeftTextGlow.enableBlur(0.6f);
        timeLeftTextGlow.setFont(resources.getAssets(), "dseg7.ttf");
        timeLeftTextGlow.setColor(TIME_LEFT_TEXT_GLOW_RGB);
        timeLeftTextGlow.alignCenter();
        timeLeftTextGlow.zIndex = 5;
        actors.add(timeLeftTextGlow);

        timeLeftText = new TextActor("00:30");
        timeLeftText.position.set(TIME_LEFT_TEXT_X, TIME_LEFT_TEXT_Y);
        timeLeftText.scale = TIME_LEFT_TEXT_SCALE;
        timeLeftText.setBold(true);
        timeLeftText.setFont(resources.getAssets(), "dseg7.ttf");
        timeLeftText.setColor(TIME_LEFT_TEXT_RGB);
        timeLeftText.alignCenter();
        timeLeftText.zIndex = 6;
        actors.add(timeLeftText);

        targetLeft = createTarget(166, 128);
        actors.add(targetLeft);

        targetMiddle = createTarget(273, 128);
        actors.add(targetMiddle);

        targetRight = createTarget(380, 128);
        actors.add(targetRight);

        // TODO: Change block sprites
        opponentOne =
                createOpponent(
                        0.8f,
                        PresentTossSprites.present_throw_def_orange_left,
                        PresentTossSprites.present_throw_def_orange_right,
                        PresentTossSprites.present_throw_def_orange_emerge,
                        PresentTossSprites.present_throw_def_orange_blocking);
        actors.add(opponentOne);

        opponentTwo =
                createOpponent(
                        0.9f,
                        PresentTossSprites.present_throw_def_green_left,
                        PresentTossSprites.present_throw_def_green_right,
                        PresentTossSprites.present_throw_def_green_emerge,
                        PresentTossSprites.present_throw_def_green_blocking);
        actors.add(opponentTwo);

        opponentThree =
                createOpponent(
                        1.0f,
                        PresentTossSprites.present_throw_def_red_left,
                        PresentTossSprites.present_throw_def_red_right,
                        PresentTossSprites.present_throw_def_red_emerge,
                        PresentTossSprites.present_throw_def_red_blocking);
        actors.add(opponentThree);

        player = new ElfActor();
        player.addSprite(
                WaterPoloActorPart.BodyIdle,
                -84,
                180,
                spriteWithIds(PresentTossSprites.present_throw_idle));
        player.addSprite(
                WaterPoloActorPart.BodyThrow,
                -84,
                180,
                spriteWithIds(PresentTossSprites.present_throw_throwing));
        player.addSprite(
                WaterPoloActorPart.BodyPickUpBall,
                -84,
                180,
                spriteWithIds(PresentTossSprites.present_throw_reloading));
        player.addSprite(
                WaterPoloActorPart.BodyIdleNoBall,
                -84,
                180,
                spriteWithIds(PresentTossSprites.present_throw_celebrate, 2));

        actors.add(player);

        AnimatedSprite diagram = spriteWithIds(PresentTossSprites.present_throw_tutorials);
        diagram.setFPS(7);

        instructions = new RectangularInstructionActor(resources, diagram);
        instructions.hidden = true;
        instructions.scale = 0.6f;
        instructions.position.set(
                WATER_POLO_WIDTH * 0.5f - instructions.getScaledWidth() / 2,
                WATER_POLO_HEIGHT * 0.46f - instructions.getScaledHeight() / 2f);
        actors.add(instructions);
    }

    SpriteActor createTarget(float x, float y) {
        SpriteActor target = actorWithIds(PresentTossSprites.present_toss_target);
        target.sprite.setAnchor(target.sprite.frameWidth / 2, target.sprite.frameHeight / 2);
        target.position.set(x, y);
        target.hidden = true;
        return target;
    }

    ElfActor createOpponent(
            float scale,
            int[] leftSprite,
            int[] rightSprite,
            int[] emergeSprite,
            int[] blockSprite) {
        ElfActor opponent = new ElfActor();

        opponent.addSprite(
                WaterPoloActorPart.BodyEntrance, -45, 142, spriteWithIds(emergeSprite, 12));
        opponent.addSprite(WaterPoloActorPart.BodyLeft, -45, 142, spriteWithIds(leftSprite, 6));
        opponent.addSprite(WaterPoloActorPart.BodyRight, -45, 142, spriteWithIds(rightSprite, 6));
        opponent.addSprite(WaterPoloActorPart.BodyBlock, -45, 142, spriteWithIds(blockSprite));
        opponent.scale = scale;
        opponent.setCollisionBox(100, 90);
        return opponent;
    }

    /**
     * Moves the slide left / right. Used to attach the slide to the side of the screen on different
     * aspect ratio screens.
     */
    public void moveSlide(float slideOffsetX) {
        slideBack.position.set(300 + slideOffsetX, 760);
    }

    // Put everything back to the beginning state.
    // Used at start of game & also if user clicks replay.
    public void reset(boolean firstPlay) {
        tweenManager.removeAll();
        if (firstPlay) {
            setState(State.TITLE);
        } else {
            setState(State.WAITING);
        }

        // Remove all temporary effects
        for (int i = effects.size() - 1; i >= 0; i--) {
            Actor effect = effects.get(i);
            actors.remove(effect);
            effects.remove(effect);
        }

        // Remove all balls
        for (int i = balls.size() - 1; i >= 0; i--) {
            ThrownActor ball = balls.get(i);
            actors.remove(ball);
            balls.remove(ball);
        }

        score = 0;
        scoredAtLeastOneShot = false;
        ballsThrown = 0;
        eventBus.sendEvent(EventBus.SCORE_CHANGED, score);

        // No opponents at start of game (give them 1 easy point to re-inforce that they are
        // supposed
        // to get goals, not attack the goalie).
        opponentOne.hidden = true;
        opponentTwo.hidden = true;
        opponentThree.hidden = true;

        msTillNextTargetPulse = MS_BETWEEN_TARGET_FLASHES;

        // Y positions picked so opponents look evenly spaced when in perspective.
        // X positions picked to make opponents come up on the left side (because they swim right
        // first, so coming up on the left side gives room to swim) but not all at the same position
        // (staggered looks better).
        opponentOne.position.set(171, 300);
        opponentTwo.position.set(271, 440);
        opponentThree.position.set(171, 600);
        player.position.set(198, 845);
        player.idle();

        canThrow = true;
        maybePickUpAnotherBall();
        if (firstPlay) {
            // TODO: If you swipe before the instructions show, they still show.
            // Fix this by adding a state machine like the android games have.
            tweenManager.add(
                    new EmptyTween(1.3f) {
                        @Override
                        protected void onFinish() {
                            if (ballsThrown == 0) {
                                instructions.show();
                            }
                        }
                    });
        }
        didReset = true;
        time = TOTAL_TIME;
    }

    public void update(long deltaMs) {
        // Track whether reset was called at any point.
        // If so, this short-circuits the rest of the update.
        didReset = false;

        if (state == State.PLAYING) {
            time = time - ((int) deltaMs / 1000f);
            if (time <= 0) {
                time = 0;
                player.idleNoBall();
                setState(State.GAME_OVER);
            }
        }

        updateTimeLeftText();

        tweenManager.update(deltaMs);
        if (didReset) {
            return;
        }

        synchronized (actors) {
            for (int i = actors.size() - 1; i >= 0; i--) {
                Actor actor = actors.get(i);
                actor.update(deltaMs);
                if (didReset) {
                    return;
                }
            }
        }

        for (int i = balls.size() - 1; i >= 0; i--) {
            updateBall(balls.get(i));
        }

        // Show the targets until the player gets at least 1 shot in the goal.
        if (!scoredAtLeastOneShot) {
            float msTillNextTargetPulseBefore = msTillNextTargetPulse;
            msTillNextTargetPulse -= deltaMs;
            if (msTillNextTargetPulseBefore > 0 && msTillNextTargetPulse <= 0) {
                pulseTargets();
            }
        }

        synchronized (actors) {
            Collections.sort(actors);
        }
    }

    void updateTimeLeftText() {
        String timeLeftString = "00:" + String.format(Locale.ENGLISH, "%02d", (int) time);
        timeLeftText.setText(timeLeftString);
        timeLeftTextGlow.setText(timeLeftString);
    }

    private void updateBall(final PresentActor ball) {
        // Make the ball shrink as it travels into the distance.
        float ballStartY = 753;
        float ballEndY = 157;
        if (!ball.shotBlocked) {
            ball.scale = 0.5f + (0.5f * (ball.position.y - ballEndY) / (ballStartY - ballEndY));
        }

        // Only allow blocking if ball is traveling towards goal (otherwise ball can get stuck
        // bouncing
        // between opponents).
        if (ball.velocity.y < 0) {
            final ElfActor blockingOpponent = getBlockingOpponentIfAny(ball);
            if (blockingOpponent != null) {
                // Draw a -1 at the ball
                addPointText(ball.position.x, ball.position.y - 150, "-1", POINT_MINUS_TEXT_RGB);

                newScore(score - 1);
                blockShot(ball, blockingOpponent);
            }
        }

        // TODO: at small scales, ball isn't centered over its position.
        // TODO: randomize vertical position of splats
        if (!ball.shotBlocked && goalBoxContains(ball.position.x, ball.position.y)) {
            // Draw a +1 at the ball.

            if (ball.bounces == 2) {
                addPointText(ball.position.x, ball.position.y, "+2", POINT_PLUS_TEXT_RGB);
                newScore(score + 2);
            } else {
                addPointText(ball.position.x, ball.position.y, "+1", POINT_PLUS_TEXT_RGB);
                newScore(score + 1);
            }
            scoreShot(ball);
        }

        // Check if ball has left screen. If so, count it as a miss and get ready for another throw.
        if ((ball.position.y < 0 && !ball.shotBlocked)) {
            missShot(ball);
        }

        // Check if ball touches the left and right wall and has not bounced off the wall twice.
        // If so, bounce it off the wall.
        // If not, remove the ball from play.
        if (ball.position.x <= 0 || ball.position.x >= WATER_POLO_WIDTH) {
            if (ball.bounces > 1 || ball.shotBlocked) {
                missShot(ball);
            } else {
                ball.velocity.x = -ball.velocity.x;
                ball.rotation =
                        (float) (Math.atan(ball.velocity.y / ball.velocity.x) - (Math.PI / 2));
                ball.update(20);
                ball.bounces++;
                EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.present_throw_block);
            }
        }

        // Ditto for the bottom wall.
        if (ball.position.y > WATER_POLO_HEIGHT) {
            if (ball.bounces > 1 || ball.shotBlocked) {
                missShot(ball);
            } else {
                ball.velocity.y = -ball.velocity.y;
                ball.rotation =
                        (float) (Math.atan(ball.velocity.y / ball.velocity.x) - (Math.PI / 2));
                ball.update(20);
                ball.bounces++;
                EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.present_throw_block);
            }
        }
    }

    private void addBall(float radians) {
        AnimatedSprite ballSprite = spriteWithIds(PresentTossSprites.present_throw_thrownpresent);
        PresentActor ball = new PresentActor(ballSprite);
        ball.shouldScaleWithHeight = false;
        ball.zIndex = 20;
        ball.position.set(player.position.x + 78, player.position.y - 89);
        ball.clearStreak();

        ball.rotation = radians + (float) Math.PI / 2;
        ball.velocity.set(
                BALL_SPEED * (float) Math.cos(radians), BALL_SPEED * (float) Math.sin(radians));
        ball.hidden = false;

        actors.add(ball);
        balls.add(ball);
    }

    private void blockBall(final PresentActor ball, ElfActor blockingOpponent) {
        float rotation = ball.velocity.x / 200;
        float directionX = ball.velocity.x * 0.5f;

        if (ball.velocity.x > 0) {
            directionX += 25;
        } else {
            directionX -= 25;
        }

        float initialX = ball.position.x;
        float finalX = initialX + directionX;

        float initialY = ball.position.y;
        float midY = initialY - 140;
        float finalY = initialY + 220;

        ball.velocity.set(0, 0);
        ball.shotBlocked = true;

        final ActorTween secondYTween =
                new ActorTween(ball) {
                    @Override
                    protected void onFinish() {
                        if (actors.contains(ball)) {
                            missShot(ball);
                        }
                    }
                }.fromY(midY).toY(finalY).withInterpolator(Interpolator.EASE_IN).withDuration(0.4f);

        final ActorTween firstYTween =
                new ActorTween(ball) {
                    @Override
                    protected void onFinish() {
                        tweenManager.add(secondYTween);
                    }
                }.fromY(initialY)
                        .toY(midY)
                        .withInterpolator(Interpolator.EASE_OUT)
                        .withDuration(0.4f);

        final ActorTween xTween =
                new ActorTween(ball)
                        .fromX(initialX)
                        .toX(finalX)
                        .withRotation(0, rotation)
                        .withDuration(0.8f);

        tweenManager.add(firstYTween);
        tweenManager.add(xTween);
    }

    private boolean goalBoxContains(float x, float y) {
        if (x < GOAL_BOX_X || x > GOAL_BOX_X + GOAL_BOX_WIDTH) {
            return false;
        }
        if (y < GOAL_BOX_Y || y > GOAL_BOX_Y + GOAL_BOX_HEIGHT) {
            return false;
        }
        return true;
    }

    private void pulseTargets() {
        targetLeft.hidden = false;
        targetMiddle.hidden = false;
        targetRight.hidden = false;

        targetLeft.alpha = 1;
        targetMiddle.alpha = 1;
        targetRight.alpha = 1;

        final float startScale = 1;
        final float endScale = 0.6f;
        final float bounceScale = 0.8f;

        final Tween fadeout =
                new Tween(0.2f) {
                    @Override
                    protected void updateValues(float percentDone) {
                        float alpha = Interpolator.FAST_IN.getValue(percentDone, 1, 0);
                        targetLeft.alpha = alpha;
                        targetMiddle.alpha = alpha;
                        targetRight.alpha = alpha;
                    }

                    @Override
                    protected void onFinish() {
                        targetLeft.hidden = true;
                        targetMiddle.hidden = true;
                        targetRight.hidden = true;
                        msTillNextTargetPulse = MS_BETWEEN_TARGET_FLASHES;
                    }
                };

        final Tween bounceThree =
                new Tween(0.15f) {
                    @Override
                    protected void updateValues(float percentDone) {
                        float scale =
                                Interpolator.FAST_IN.getValue(percentDone, bounceScale, endScale);
                        targetLeft.scale = scale;
                        targetMiddle.scale = scale;
                        targetRight.scale = scale;
                    }

                    @Override
                    protected void onFinish() {
                        tweenManager.add(fadeout);
                    }
                };

        final Tween bounceTwo =
                new Tween(0.15f) {
                    @Override
                    protected void updateValues(float percentDone) {
                        float scale =
                                Interpolator.FAST_IN.getValue(percentDone, bounceScale, endScale);
                        targetLeft.scale = scale;
                        targetMiddle.scale = scale;
                        targetRight.scale = scale;
                    }

                    @Override
                    protected void onFinish() {
                        tweenManager.add(score == 0 ? bounceThree : fadeout);
                    }
                };

        final Tween bounceOne =
                new Tween(0.15f) {
                    @Override
                    protected void updateValues(float percentDone) {
                        float scale =
                                Interpolator.FAST_IN.getValue(percentDone, bounceScale, endScale);
                        targetLeft.scale = scale;
                        targetMiddle.scale = scale;
                        targetRight.scale = scale;
                    }

                    @Override
                    protected void onFinish() {
                        tweenManager.add(score == 0 ? bounceTwo : fadeout);
                    }
                };

        Tween shrinkIn =
                new Tween(0.3f) {
                    @Override
                    protected void updateValues(float percentDone) {
                        float scale =
                                Interpolator.FAST_IN.getValue(percentDone, startScale, endScale);
                        targetLeft.scale = scale;
                        targetMiddle.scale = scale;
                        targetRight.scale = scale;
                    }

                    @Override
                    protected void onFinish() {
                        tweenManager.add(score == 0 ? bounceOne : fadeout);
                    }
                };

        tweenManager.add(shrinkIn);
    }

    ElfActor getBlockingOpponentIfAny(PresentActor ball) {
        if (ball.shotBlocked) {
            return null; // Already blocked once, let it go.
        }

        if (opponentOne.canBlock(ball.position.x, ball.position.y)) {
            return opponentOne;
        } else if (opponentTwo.canBlock(ball.position.x, ball.position.y)) {
            return opponentTwo;
        } else if (opponentThree.canBlock(ball.position.x, ball.position.y)) {
            return opponentThree;
        }
        return null;
    }

    private void blockShot(final PresentActor ball, final ElfActor blockingOpponent) {
        // Blocked!
        ball.shotBlocked = true;
        EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.tennis_eliminate);

        blockingOpponent.blockShot(
                new Callback() {
                    @Override
                    public void call() {
                        blockBall(ball, blockingOpponent);
                        shake(1, VIBRATION_SMALL);
                    }
                });
    }

    private void missShot(PresentActor ball) {
        actors.remove(ball);
        balls.remove(ball);
    }

    private void scoreShot(PresentActor ball) {
        actors.remove(ball);
        balls.remove(ball);

        // Swap ball for splat.
        final SpriteActor splat = actorWithIds(PresentTossSprites.orange_present_falling);
        splat.position.set(
                ball.position.x - splat.sprite.frameWidth / 2,
                ball.position.y - splat.sprite.frameHeight);
        splat.zIndex = -1;
        splat.sprite.setLoop(false);
        splat.sprite.addListener(
                new AnimatedSpriteListener() {
                    @Override
                    public void onFinished() {
                        actors.remove(splat);
                        effects.remove(splat);
                    }
                });
        actors.add(splat);
        effects.add(splat);

        // Shake the goal.
        goal.sprite.setPaused(false);

        // Play goal score sound.
        EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.present_throw_goal);

        // Shake the screen a little bit.
        shake(1, VIBRATION_SMALL);

        // Reset ball.
        ball.velocity.set(0, 0);
        ball.position.set(player.position.x, player.position.y);

        // Time for more opponents?
        if (score >= OPPONENT_ONE_ENTRANCE_THRESHOLD && opponentOne.hidden) {
            bringInOpponent(opponentOne, OPPONENT_ONE_SPEED);
        }
        if (score >= OPPONENT_TWO_ENTRANCE_THRESHOLD && opponentTwo.hidden) {
            bringInOpponent(opponentTwo, OPPONENT_TWO_SPEED);
        }
        if (score >= OPPONENT_THREE_ENTRANCE_THRESHOLD && opponentThree.hidden) {
            bringInOpponent(opponentThree, OPPONENT_THREE_SPEED);
        }

        scoredAtLeastOneShot = true;
    }

    private void newScore(int newScore) {
        score = newScore;
        eventBus.sendEvent(EventBus.SCORE_CHANGED, score);
    }

    private void addPointText(float x, float y, String pointText, int color) {
        final TextActor pointTextActor = new TextActor(pointText);

        x = x - 30;
        y = y - 70;

        pointTextActor.setColor(color);

        pointTextActor.setBold(true);
        pointTextActor.position.set(x, y);
        pointTextActor.scale = 5;
        pointTextActor.zIndex = 1000;
        actors.add(pointTextActor);
        effects.add(pointTextActor);
        tweenManager.add(
                new ActorTween(pointTextActor) {
                    @Override
                    protected void onFinish() {
                        actors.remove(pointTextActor);
                        effects.remove(pointTextActor);
                    }
                }.withDuration(POINT_TEXT_ANIMATION_TIME)
                        .withAlpha(1, 0)
                        .toY(y - 80)
                        .withInterpolator(Interpolator.FAST_IN));
    }

    private void bringInOpponent(final ElfActor opponent, final float speed) {
        EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.present_throw_character_appear);
        opponent.hidden = false;
        opponent.enter(
                new Callback() {
                    @Override
                    public void call() {
                        tweenOpponentRight(opponent, speed);
                    }
                });
    }

    // Move opponent to the right. Chain a tween to the left at the end for endless motion.
    private void tweenOpponentRight(final ElfActor opponent, final float speed) {
        if (state != State.PLAYING) {
            // Stop moving opponents side-to-side at end of game.
            tweenOpponentLeft(opponent, speed);
            return;
        }
        tweenOpponent(
                opponent,
                400,
                speed,
                new Callback() {
                    @Override
                    public void call() {
                        tweenOpponentLeft(opponent, speed);
                    }
                });
        opponent.swimRight();
    }

    // Move opponent to the left. Chain a tween to the right at the end for endless motion.
    private void tweenOpponentLeft(final ElfActor opponent, final float speed) {
        tweenOpponent(
                opponent,
                0,
                speed,
                new Callback() {
                    @Override
                    public void call() {
                        tweenOpponentRight(opponent, speed);
                    }
                });
        opponent.swimLeft();
    }

    // Helper function for moving opponents left & right.
    private void tweenOpponent(ElfActor opponent, float x, float speed, Callback next) {
        ActorTween tween =
                new ActorTween(opponent)
                        .toX(x)
                        .withDuration(
                                ActorHelper.distanceBetween(opponent.position.x, 0, x, 0) / speed)
                        .whenFinished(next);
        tweenManager.add(tween);
    }

    private void maybePickUpAnotherBall() {
        if (state == State.GAME_OVER) {
            // No-op
        } else if (ballsThrown == 0) {
            // No-op
        } else {
            // No-op
        }
    }

    public void onFling(final float radians) {
        if (state == State.GAME_OVER || balls.size() > 0) {
            return;
        }
        if (state == State.WAITING) {
            instructions.hide();
            setState(State.PLAYING);
        }
        canThrow = false; // No more throws until ball either goes in goal or leaves screen.
        tweenManager.add(
                new EmptyTween(THROW_DELAY_SECONDS) {
                    @Override
                    protected void onFinish() {
                        canThrow = true;
                    }
                });
        ballsThrown++;

        EventBus.getInstance().sendEvent(EventBus.PLAY_SOUND, R.raw.present_throw_throw);

        player.throwBall(
                new Callback() {
                    @Override
                    public void call() {
                        addBall(radians);
                    }
                },
                new Callback() {
                    @Override
                    public void call() {
                        maybePickUpAnotherBall();
                    }
                });
    }

    private SpriteActor actorWithIds(int[] ids) {
        return new SpriteActor(spriteWithIds(ids), Vector2D.get(0, 0), Vector2D.get(0, 0));
    }

    private AnimatedSprite spriteWithIds(int[] ids, int fps) {
        AnimatedSprite sprite = spriteWithIds(ids);
        sprite.setFPS(fps);

        return sprite;
    }

    private AnimatedSprite spriteWithIds(int[] ids) {
        return AnimatedSprite.fromFrames(resources, ids);
    }

    private void shake(float screenShakeMagnitude, long vibrationMs) {
        vibrator.vibrate(vibrationMs);
        if (screenShakeMagnitude > 0) {
            cameraShake.shake(33, screenShakeMagnitude, SHAKE_FALLOFF);
        }
    }

    public void setState(State newState) {
        state = newState;
        eventBus.sendEvent(EventBus.GAME_STATE_CHANGED, newState);

        if (newState == State.TITLE) {
            tweenManager.add(
                    new EmptyTween(titleDurationMs / 1000.0f) {
                        @Override
                        protected void onFinish() {
                            setState(State.WAITING);
                        }
                    });
        }
    }

    /** High-level phases of the game are controlled by a state machine which uses these states. */
    enum State {
        TITLE,
        WAITING,
        PLAYING,
        GAME_OVER
    }

    /** A present that the elf throws. */
    class PresentActor extends ThrownActor {
        public boolean shotBlocked;
        public int bounces;

        PresentActor(AnimatedSprite ballSprite) {
            super(null, ballSprite, null, 3);
            bounces = 0;
            shotBlocked = false;
        }
    }
}
