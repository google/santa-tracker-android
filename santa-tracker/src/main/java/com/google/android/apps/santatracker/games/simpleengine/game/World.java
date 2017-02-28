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

import android.graphics.RectF;

import java.util.ArrayList;

public final class World {

    // renderer
    Renderer mRenderer;

    public ArrayList<GameObject> gameObjects = new ArrayList<GameObject>();

    // recycle bin of objects for reuse
    private ArrayList<GameObject> mRecycleBin = new ArrayList<GameObject>(64);

    public World(Renderer r) {
        mRenderer = r;
    }

    private GameObject newGameObject(int type, float x, float y, boolean createSprite,
            int texIndex, int color, float tintFactor, float spriteWidth, float spriteHeight) {
        GameObject o;
        if (mRecycleBin.isEmpty()) {
            o = new GameObject(this);
        } else {
            o = mRecycleBin.remove(mRecycleBin.size() - 1);
        }
        o.x = x;
        o.y = y;
        o.type = type;

        if (createSprite) {
            Renderer.Sprite s = o.getSprite(o.addSprite());
            s.texIndex = texIndex;
            s.tintFactor = tintFactor;
            s.color = color;
            s.width = spriteWidth;
            s.height = spriteHeight;
        }

        gameObjects.add(o);
        return o;
    }

    public GameObject newGameObject(int type, float x, float y) {
        return newGameObject(type, x, y, false, -1, 0, 0.0f, 0.0f, 0.0f);
    }

    public GameObject newGameObjectWithImage(int type, float x, float y,
            int texIndex, float spriteWidth, float spriteHeight) {
        return newGameObject(type, x, y, true, texIndex, 0, 0.0f, spriteWidth, spriteHeight);
    }

    public GameObject newGameObjectWithColor(int type, float x, float y, int color,
            float spriteWidth, float spriteHeight) {
        return newGameObject(type, x, y, true, -1, color, 1.0f, spriteWidth, spriteHeight);
    }

    public void doFrame(float deltaT) {
        int i;

        // we iterate backwards so that the iteration is not affected by things
        // getting added as part of the update process
        for (i = gameObjects.size() - 1; i >= 0; --i) {
            gameObjects.get(i).update(deltaT);
        }

        // remove dead objects
        for (i = gameObjects.size() - 1; i >= 0; --i) {
            if (gameObjects.get(i).dead) {
                // more efficient than remove() because this doesn't cause
                // the whole array to shift. In other words, remove() is O(n),
                // this method is O(1):
                GameObject deleted = gameObjects.get(i);
                int last = gameObjects.size() - 1;
                gameObjects.set(i, gameObjects.get(last));
                gameObjects.remove(last);

                // recycle it!
                deleted.clear();
                mRecycleBin.add(deleted);
            }
        }
    }

    public boolean detectCollisions(GameObject object,
            ArrayList<GameObject> result, boolean clear) {
        int i;
        boolean found = false;

        if (clear) {
            result.clear();
        }
        if (object.dead || !object.collides) {
            return false;
        }
        for (i = 0; i < gameObjects.size(); i++) {
            GameObject o = gameObjects.get(i);
            if (!o.dead && o != object && o.collides && objectsCollide(object, o)) {
                found = true;
                result.add(o);
            }
        }
        return found;
    }

    RectF mRect1 = new RectF();
    RectF mRect2 = new RectF();

    private boolean objectsCollide(GameObject a, GameObject b) {
        if (a.dead || b.dead || !a.collides || !b.collides) {
            return false;
        }
        getColliderBounds(a, mRect1);
        getColliderBounds(b, mRect2);
        return mRect1.intersect(mRect2);
    }

    private void getColliderBounds(GameObject obj, RectF result) {
        result.left = obj.x - obj.collBoxWidth * 0.5f;
        result.right = obj.x + obj.collBoxWidth * 0.5f;
        result.top = obj.y - obj.collBoxHeight * 0.5f;
        result.bottom = obj.y + obj.collBoxHeight * 0.5f;
    }

    public Renderer getRenderer() {
        return mRenderer;
    }
}
