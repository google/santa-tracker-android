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
package com.google.android.apps.santatracker.doodles.snowballrun;

import android.content.res.Resources;
import android.graphics.Canvas;
import com.google.android.apps.santatracker.doodles.shared.actor.Actor;
import com.google.android.apps.santatracker.doodles.shared.animation.AnimatedSprite;
import com.google.android.apps.santatracker.doodles.shared.animation.AnimatedSprite.AnimatedSpriteListener;

/** A fruit that runs down the screen. */
public class RunnerActor extends Actor {
    private static final int RUNNING_Z_INDEX = 10;
    private static final int DEAD_Z_INDEX = 3;
    protected int lane;
    protected RunnerType type;
    protected RunnerState state;
    protected float radius;
    protected AnimatedSprite currentSprite;
    protected AnimatedSprite runningSprite;
    protected AnimatedSprite crouchSprite;
    protected AnimatedSprite enteringSprite;
    protected AnimatedSprite runningLeftSprite;
    protected AnimatedSprite runningRightSprite;
    protected AnimatedSprite standingSprite;
    protected AnimatedSprite deadSprite;
    protected AnimatedSprite dyingSprite;

    RunnerActor(Resources resources, RunnerType type, int lane) {
        this.lane = lane;
        this.type = type;

        runningSprite = AnimatedSprite.fromFrames(resources, type.runRes);
        crouchSprite = AnimatedSprite.fromFrames(resources, type.crouchRes);
        enteringSprite = AnimatedSprite.fromFrames(resources, type.enteringRes);
        runningLeftSprite = AnimatedSprite.fromFrames(resources, type.runLeftRes);
        runningRightSprite = AnimatedSprite.fromFrames(resources, type.runLeftRes);
        standingSprite = AnimatedSprite.fromFrames(resources, type.standRes);
        deadSprite = AnimatedSprite.fromFrames(resources, type.deadRes);
        dyingSprite = AnimatedSprite.fromFrames(resources, type.dyingRes);

        enteringSprite.setLoop(false);
        enteringSprite.setFPS(
                (int) ((enteringSprite.getNumFrames() + 1) / PursuitModel.RUNNER_ENTER_DURATION));

        setSpriteAnchorUpright(runningSprite);
        setSpriteAnchorWithYOffset(crouchSprite, 0);
        setSpriteAnchorUpright(enteringSprite);
        setSpriteAnchorUpright(runningLeftSprite);
        setSpriteAnchorUpright(runningRightSprite);
        setSpriteAnchorUpright(standingSprite);
        setSpriteAnchorCenter(deadSprite);
        switch (type) {
            case PLAYER:
                setSpriteAnchorWithYOffset(dyingSprite, 0);
                break;
            case REINDEER:
                setSpriteAnchorWithYOffset(dyingSprite, 0);
                break;
            case ELF:
                setSpriteAnchorWithYOffset(dyingSprite, 0);
                break;
            case SNOWMAN:
                setSpriteAnchorWithYOffset(dyingSprite, 0);
                break;
        }

        currentSprite = runningSprite;
        state = RunnerState.RUNNING;
        zIndex = RUNNING_Z_INDEX;
    }

    protected static void setSpriteAnchorCenter(AnimatedSprite sprite) {
        setSpriteAnchorWithYOffset(sprite, sprite.frameHeight * 0.5f);
    }

    protected static void setSpriteAnchorUpright(AnimatedSprite sprite) {
        setSpriteAnchorWithYOffset(sprite, 0);
    }

    protected static void setSpriteAnchorWithYOffset(AnimatedSprite sprite, float yOffset) {
        sprite.setAnchor(sprite.frameWidth * 0.5f, sprite.frameHeight - yOffset);
    }

    @Override
    public void update(float deltaMs) {
        super.update(deltaMs);

        if (state == RunnerState.RUNNING) {
            currentSprite.setFPS(3 + (int) (5 * velocity.y / PursuitModel.BASE_SPEED));
        }

        currentSprite.update(deltaMs);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (currentSprite == null) {
            return;
        }

        float runnerScale = scale * 1.50f;
        currentSprite.setScale(runnerScale, runnerScale);
        currentSprite.setPosition(position.x, position.y);

        if (currentSprite == runningRightSprite) {
            currentSprite.setScale(-runnerScale, runnerScale);
        }

        currentSprite.draw(canvas);
    }

    public int getLane() {
        return lane;
    }

    public void setLane(int lane) {
        this.lane = lane;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public RunnerState getRunnerState() {
        return state;
    }

    // yOffset is in percentage not game units, or pixels
    // yOffset of 0.5f centers the sprite vertically
    // yOffset of 0 draws the sprite starting from its y position

    public void setRunnerState(RunnerState state) {
        if (this.state == state) {
            return;
        }

        this.state = state;

        switch (state) {
            case RUNNING:
                currentSprite = runningSprite;
                break;
            case CROUCH:
                currentSprite = crouchSprite;
                break;
            case ENTERING:
                currentSprite = enteringSprite;
                break;
            case RUNNING_LEFT:
                currentSprite = runningLeftSprite;
                break;
            case RUNNING_RIGHT:
                currentSprite = runningRightSprite;
                break;
            case STANDING:
                currentSprite = standingSprite;
                break;
            case DYING:
                currentSprite = dyingSprite;
                dyingSprite.setLoop(false);
                dyingSprite.setFrameIndex(0);
                dyingSprite.clearListeners();

                int frame = 3;
                switch (type) {
                    case SNOWMAN:
                        frame = 2;
                        break;
                    case ELF:
                        frame = 2;
                        break;
                }
                final int finalFrame = frame;

                dyingSprite.addListener(
                        new AnimatedSpriteListener() {
                            @Override
                            public void onFrame(int index) {
                                super.onFrame(index);
                                if (index == finalFrame) {
                                    setRunnerState(RunnerState.DEAD);
                                }
                            }
                        });
                break;
            case DEAD:
                currentSprite = deadSprite;
                break;
        }

        if (state == RunnerState.DEAD) {
            zIndex = DEAD_Z_INDEX;
        } else {
            zIndex = RUNNING_Z_INDEX;
        }
    }

    /** Currently there are four different kinds of fruits. */
    public enum RunnerType {
        PLAYER(
                SnowballRunSprites.snowballrun_running_normal,
                R.drawable.snowballrun_running_starting_runner,
                SnowballRunSprites.snowballrun_running_sidestep,
                SnowballRunSprites.snowballrun_running_appearing,
                R.drawable.snowballrun_standing_elf,
                SnowballRunSprites.snowballrun_elf_squish,
                R.drawable.snowballrun_elf_squished_05),

        SNOWMAN(
                SnowballRunSprites.snowballrun_running_snowman_opponent,
                R.drawable.snowballrun_running_starting_snowman,
                SnowballRunSprites.empty_frame,
                SnowballRunSprites.empty_frame,
                R.drawable.snowballrun_standing_snowman,
                SnowballRunSprites.running_snowman_squish,
                R.drawable.snowballrun_snowman_squished_09),

        ELF(
                SnowballRunSprites.snowballrun_running_elf_opponent,
                R.drawable.snowballrun_running_starting_elfopponent,
                SnowballRunSprites.empty_frame,
                SnowballRunSprites.empty_frame,
                R.drawable.snowballrun_standing_elfopponent,
                SnowballRunSprites.running_elfopponent_squish,
                R.drawable.snowballrun_elfopponent_squished_09),

        REINDEER(
                SnowballRunSprites.snowballrun_running_reindeer_opponent,
                R.drawable.snowballrun_running_starting_reindeer,
                SnowballRunSprites.empty_frame,
                SnowballRunSprites.empty_frame,
                R.drawable.snowballrun_standing_reindeer,
                SnowballRunSprites.snowballrun_reindeer_squish,
                R.drawable.snowballrun_reindeer_squished_05);
        public int[] runRes, crouchRes, runLeftRes, enteringRes, standRes, dyingRes, deadRes;

        RunnerType(
                int[] runRes,
                int crouchRes,
                int[] runLeftRes,
                int[] enteringRes,
                int standRes,
                int[] dyingRes,
                int deadRes) {

            this.runRes = runRes;
            this.crouchRes = new int[] {crouchRes};
            this.runLeftRes = runLeftRes;
            this.enteringRes = enteringRes;
            this.standRes = new int[] {standRes};
            this.dyingRes = dyingRes;
            this.deadRes = new int[] {deadRes};
        }
    }

    // TODO: Because the running left animation is no longer used for the opponents'
    // entrance, consider moving RUNNING_LEFT and RUNNING_RIGHT to PlayerActor.
    enum RunnerState {
        RUNNING,
        CROUCH,
        ENTERING,
        RUNNING_LEFT,
        RUNNING_RIGHT,
        STANDING,
        DYING,
        DEAD,
    }
}
