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
package com.google.android.apps.santatracker.doodles.penguinswim;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import com.google.android.apps.santatracker.doodles.shared.Debug;
import com.google.android.apps.santatracker.doodles.shared.Touchable;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.actor.Actor;
import com.google.android.apps.santatracker.doodles.shared.actor.SpriteActor;
import com.google.android.apps.santatracker.doodles.shared.animation.AnimatedSprite;
import com.google.android.apps.santatracker.doodles.shared.physics.Polygon;
import com.google.android.apps.santatracker.doodles.shared.physics.Util;
import com.google.android.apps.santatracker.util.SantaLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.json.JSONException;
import org.json.JSONObject;

/** A sprite actor which contains a pre-set convex collision body. */
public class BoundingBoxSpriteActor extends CollisionActor implements Touchable {
    public static final String DUCK = "duck";
    public static final String ICE_CUBE = "cube1";
    public static final String HAND_GRAB = "hand grab";
    public static final Map<String, Data> TYPE_TO_RESOURCE_MAP;
    protected static final float SCALE = 2;
    private static final String TAG = BoundingBoxSpriteActor.class.getSimpleName();
    private static final Random RANDOM = new Random();

    static {
        TYPE_TO_RESOURCE_MAP = new HashMap<>();

        Vector2D[] duckVertexOffsets = {
            Vector2D.get(0, 0), Vector2D.get(87, 0), Vector2D.get(87, 186), Vector2D.get(0, 186),
        };
        TYPE_TO_RESOURCE_MAP.put(
                DUCK,
                new Data(
                        PenguinSwimSprites.penguin_swim_elf,
                        1,
                        Vector2D.get(0, 0).scale(SCALE),
                        duckVertexOffsets));

        Vector2D[] iceCube1VertexOffsets = {
            Vector2D.get(0, 0), Vector2D.get(101.9f, 0),
            Vector2D.get(101.9f, 100.2f), Vector2D.get(0, 100.2f)
        };
        TYPE_TO_RESOURCE_MAP.put(
                ICE_CUBE,
                new Data(
                        PenguinSwimSprites.penguin_swim_ice,
                        1,
                        Vector2D.get(0, 0).scale(SCALE),
                        iceCube1VertexOffsets));

        // This is just a placeholder so that we can create hand grabs programatically. This data
        // shouldn't actually be used.
        TYPE_TO_RESOURCE_MAP.put(
                HAND_GRAB, new Data(null, 0, Vector2D.get(0, 0).scale(SCALE), null));
    }

    public final SpriteActor spriteActor;
    public String type;
    public Vector2D spriteOffset;

    public BoundingBoxSpriteActor(
            Polygon collisionBody, SpriteActor spriteActor, Vector2D spriteOffset, String type) {
        super(collisionBody);

        this.spriteOffset = spriteOffset;
        this.type = type;
        this.spriteActor = spriteActor;
        scale = SCALE;
    }

    public static BoundingBoxSpriteActor create(
            Vector2D position, String type, Resources resources) {
        if (!TYPE_TO_RESOURCE_MAP.containsKey(type)) {
            SantaLog.e(TAG, "Unknown object type: " + type);
            return null;
        }
        Data data = TYPE_TO_RESOURCE_MAP.get(type);

        BoundingBoxSpriteActor actor;
        if (type.equals(HAND_GRAB)) {
            actor = HandGrabActor.create(position, resources);
        } else {
            actor =
                    new BoundingBoxSpriteActor(
                            getBoundingBox(position, data.vertexOffsets, SCALE),
                            new SpriteActor(
                                    AnimatedSprite.fromFrames(resources, data.resIds),
                                    Vector2D.get(position),
                                    Vector2D.get(0, 0)),
                            Vector2D.get(data.spriteOffset),
                            type);
        }

        actor.zIndex = data.zIndex;

        // Start at a random frame index so that all of the sprites aren't synced up.
        actor.spriteActor.sprite.setFrameIndex(
                RANDOM.nextInt(actor.spriteActor.sprite.getNumFrames()));

        return actor;
    }

    public static BoundingBoxSpriteActor fromJSON(JSONObject json, Context context)
            throws JSONException {
        String type = json.getString(Actor.TYPE_KEY);
        Vector2D position =
                Vector2D.get((float) json.getDouble(X_KEY), (float) json.getDouble(Y_KEY));
        return create(position, type, context.getResources());
    }

    protected static Polygon getBoundingBox(
            Vector2D position, Vector2D[] vertexOffsets, float scale) {
        List<Vector2D> vertices = new ArrayList<>();
        for (int i = 0; i < vertexOffsets.length; i++) {
            vertices.add(Vector2D.get(position).add(Vector2D.get(vertexOffsets[i]).scale(scale)));
        }
        return new Polygon(vertices);
    }

    @Override
    public void update(float deltaMs) {
        super.update(deltaMs);
        spriteActor.update(deltaMs);
        spriteActor.position.set(position.x, position.y);
    }

    @Override
    public void draw(Canvas canvas) {
        spriteActor.draw(
                canvas,
                spriteOffset.x,
                spriteOffset.y,
                spriteActor.sprite.frameWidth * scale,
                spriteActor.sprite.frameHeight * scale);

        if (Debug.DRAW_COLLISION_BOUNDS) {
            collisionBody.draw(canvas);
        }
    }

    @Override
    public boolean canHandleTouchAt(Vector2D worldCoords, float cameraScale) {
        Vector2D lowerRight =
                Vector2D.get(position)
                        .add(spriteOffset)
                        .add(
                                spriteActor.sprite.frameWidth * scale,
                                spriteActor.sprite.frameHeight * scale);
        boolean retVal =
                super.canHandleTouchAt(worldCoords, cameraScale)
                        || Util.pointIsWithinBounds(
                                Vector2D.get(position).add(spriteOffset), lowerRight, worldCoords);

        lowerRight.release();
        return retVal;
    }

    @Override
    public void startTouchAt(Vector2D worldCoords, float cameraScale) {
        selectedIndex = collisionBody.getSelectedIndex(worldCoords, cameraScale);
    }

    @Override
    public boolean handleMoveEvent(Vector2D delta) {
        collisionBody.move(-delta.x, -delta.y);
        position.set(collisionBody.min);
        // NOTE: Leave this commented-out section here. This is used when adding new
        // BoundingBoxSpriteActors in order to fine-tune the collision boundaries and sprite
        // offsets.
        /*
        boolean moved;
        if (selectedIndex >= 0) {
          collisionBody.moveVertex(selectedIndex, Vector2D.get(delta).scale(-1));
          SantaLog.d(TAG, "min: " + collisionBody.min);
          SantaLog.d(TAG, "max: " + collisionBody.max);
        } else {
          spriteOffset.subtract(delta);
          SantaLog.d(TAG, "Sprite offset: " + spriteOffset);
        }
        */
        return true;
    }

    @Override
    public boolean handleLongPress() {
        // NOTE: Leave this commented-out section here. This is used when adding new
        // BoundingBoxSpriteActors in order to fine-tune the collision boundaries and sprite
        // offsets.
        /*
        if (selectedIndex >= 0) {
          if (canRemoveCollisionVertex()) {
            // If we can, just remove the vertex.
            collisionBody.removeVertexAt(selectedIndex);
            return true;
          }
        } else if (midpointIndex >= 0) {
          // Long press on a midpoint, add a vertex to the selected obstacle's polygon.
          collisionBody.addVertexAfter(midpointIndex);
          return true;
        }
        */
        return false;
    }

    @Override
    public boolean resolveCollision(Actor other, float deltaMs) {
        if (other instanceof SwimmerActor) {
            return resolveCollisionInternal((SwimmerActor) other);
        }
        return false;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(TYPE_KEY, getType());
        json.put(X_KEY, position.x);
        json.put(Y_KEY, position.y);
        return json;
    }

    protected boolean resolveCollisionInternal(SwimmerActor swimmer) {
        if (swimmer.isInvincible || swimmer.isUnderwater) {
            return false;
        }
        if (swimmer.collisionBody.min.y > collisionBody.max.y
                || swimmer.collisionBody.max.y < collisionBody.min.y) {
            // Perform a short-circuiting check which fails if the swimmer is outside of the
            // vertical
            // boundaries of this collision body.
            return false;
        }

        // NOTE: We've since removed the diagonal ice cube, so we don't have any
        // non-axis-aligned rectangles to check collisions with. However, there may still be a few
        // artifacts of complex polygon collisions in the code.

        // CAN and ICE_CUBE objects are just axis-aligned rectangles. Use the faster
        // rectangle-to-rectangle collision code in these cases.
        if (Util.rectIntersectsRect(
                swimmer.collisionBody.min.x,
                swimmer.collisionBody.min.y,
                swimmer.collisionBody.getWidth(),
                swimmer.collisionBody.getHeight(),
                collisionBody.min.x,
                collisionBody.min.y,
                collisionBody.getWidth(),
                collisionBody.getHeight())) {

            // If the swimmer is colliding with the side of an obstacle, make the swimmer slide
            // along it
            // instead of colliding.
            if (swimmer.positionBeforeFrame.y < collisionBody.max.y) {
                swimmer.moveTo(swimmer.positionBeforeFrame.x, swimmer.position.y);
            } else {
                swimmer.collide(type);
            }
        }
        return false;
    }

    /** A utility class which contains data needed to create BoundingBoxSpriteActors. */
    protected static class Data {
        public int[] resIds;
        public int numFrames;
        public int zIndex;
        public Vector2D spriteOffset;
        public Vector2D[] vertexOffsets;

        public Data(int[] resIds, int zIndex, Vector2D spriteOffset, Vector2D[] vertexOffsets) {
            this.resIds = resIds;
            this.numFrames = resIds == null ? 0 : resIds.length;
            this.zIndex = zIndex;
            this.spriteOffset = spriteOffset;
            this.vertexOffsets = vertexOffsets;
        }
    }
}
