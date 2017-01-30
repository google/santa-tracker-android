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

package com.google.android.apps.santatracker.games.simpleengine.game;

import com.google.android.apps.santatracker.games.simpleengine.Renderer;

import java.util.ArrayList;
import java.util.Arrays;

// this class is final for performance reasons
public final class GameObject {

    private World mWorld;

    // position, velocity, acceleration
    public float x = 0.0f;
    public float y = 0.0f;
    public float velX = 0.0f;
    public float velY = 0.0f;
    public float accX = 0.0f;
    public float accY = 0.0f;

    // collides?
    public boolean collides = false;

    // if it collides, width and height of collider box (centered on x,y)
    public float collBoxWidth = 0.0f;
    public float collBoxHeight = 0.0f;

    // type -- the meaning of this is up to the game developer
    public int type = 0;

    // for developer use
    public int ivar[] = new int[16];
    public float fvar[] = new float[16];
    public boolean bvar[] = new boolean[16];

    // flag that indicates that this GameObject should be deleted asap
    public boolean dead = false;

    // countdown to this object's death
    public float timeToLive = Float.POSITIVE_INFINITY;

    // sprites
    public ArrayList<Renderer.Sprite> mSprites = new ArrayList<Renderer.Sprite>();

    // texture that shows collider size (for debug purposes)
    private int mColliderSpriteIdx = -1;

    public final void update(float deltaT) {
        if (dead) {
            return;
        }
        if ((timeToLive -= deltaT) <= 0.0f) {
            dead = true;
            return;
        }

        velX += accX * deltaT;
        velY += accY * deltaT;
        displaceBy(velX * deltaT, velY * deltaT);

        if (mColliderSpriteIdx >= 0) {
            mSprites.get(mColliderSpriteIdx).enabled = (System.currentTimeMillis() % 200) < 100;
        }
    }

    // only created by the World (if you want a GameObject, ask the World for one)
    GameObject(World world) {
        mWorld = world;
        clear();
    }

    // only called by the World
    void clear() {
        deleteSprites();
        dead = false;
        timeToLive = Float.POSITIVE_INFINITY;
        x = y = velX = velY = accX = accY = 0.0f;
        collides = false;
        collBoxHeight = collBoxWidth = 0.0f;
        type = 0;
        Arrays.fill(ivar, 0);
        Arrays.fill(fvar, 0.0f);
        Arrays.fill(bvar, false);
    }

    public void displaceBy(float dx, float dy) {
        x += dx;
        y += dy;

        // displace sprites
        int i;
        for (i = 0; i < mSprites.size(); i++) {
            mSprites.get(i).x += dx;
            mSprites.get(i).y += dy;
        }
    }

    public void displaceTo(float toX, float toY) {
        displaceBy(toX - x, toY - y);
    }

    public void displaceTowards(float targetX, float targetY, float maxDisplacement) {
        if (distanceTo(targetX, targetY) <= maxDisplacement) {
            displaceTo(targetX, targetY);
        } else {
            float velX = targetX - x;
            float velY = targetY - y;
            float modulus = (float) Math.sqrt(velX * velX + velY * velY);
            displaceBy(velX * maxDisplacement / modulus, velY * maxDisplacement / modulus);
        }
    }

    public float distanceTo(float x, float y) {
        return (float) Math.sqrt((this.x - x) * (this.x - x) + (this.y - y) * (this.y - y));
    }

    public int addSprite() {
        // create renderer sprite
        Renderer.Sprite s = mWorld.getRenderer().createSprite();
        s.x = x;
        s.y = y;
        mSprites.add(s);
        return mSprites.size() - 1;
    }

    public Renderer.Sprite getSprite(int i) {
        return i >= 0 && i < mSprites.size() ? mSprites.get(i) : null;
    }

    public int getSpriteCount() {
        return mSprites.size();
    }

    public void deleteSprites() {
        int i;
        for (i = 0; i < mSprites.size(); i++) {
            mWorld.getRenderer().deleteSprite(mSprites.get(i));
        }
        mSprites.clear();
    }

    public void setBoxCollider(float width, float height) {
        collBoxHeight = height;
        collBoxWidth = width;
        collides = true;
    }

    public void debugShowCollider() {
        Renderer.Sprite s = getSprite(mColliderSpriteIdx = addSprite());
        s.x = x;
        s.y = y;
        s.width = collBoxWidth;
        s.height = collBoxHeight;
        s.color = 0xffff0000;
        s.tintFactor = 1.0f;
        s.texIndex = -1;
    }

    public void hide() {
        show(false);
    }

    public void show() {
        show(true);
    }

    public void show(boolean show) {
        int i;
        for (i = 0; i < mSprites.size(); i++) {
            Renderer.Sprite s = mSprites.get(i);
            s.enabled = show;
        }
    }

    public void bringToFront() {
        int i;
        for (i = 0; i < mSprites.size(); i++) {
            mWorld.getRenderer().bringToFront(mSprites.get(i));
        }
    }

    public void sendToBack() {
        int i;
        for (i = 0; i < mSprites.size(); i++) {
            mWorld.getRenderer().sendToBack(mSprites.get(i));
        }
    }

}
