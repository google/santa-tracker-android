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

package com.google.android.apps.santatracker.games.simpleengine.ui;

import com.google.android.apps.santatracker.games.simpleengine.Renderer;

import java.util.ArrayList;

public class Button extends Widget {

    private boolean mVisible = true;
    private boolean mArmed = false;
    private boolean mEnabled = true;
    private Renderer mRenderer;
    private float mX, mY, mWidth, mHeight;

    // set of sprites to show when in normal (non highlight) mode
    private ArrayList<Renderer.Sprite> mNormalSprites = new ArrayList<Renderer.Sprite>();

    // set of sprites to show when in highlighted mode
    private ArrayList<Renderer.Sprite> mHighlightSprites = new ArrayList<Renderer.Sprite>();

    private WidgetTriggerListener mListener = null;
    private int mTriggerMessage = 0;

    public Button(Renderer renderer, float x, float y, float width, float height) {
        mX = x;
        mY = y;
        mWidth = width;
        mHeight = height;
        mRenderer = renderer;
    }

    private Renderer.Sprite makeFlatSprite(int color) {
        Renderer.Sprite sp = mRenderer.createSprite();
        sp.color = color;
        sp.x = mX;
        sp.y = mY;
        sp.width = mWidth;
        sp.height = mHeight;
        sp.tintFactor = 0.0f;
        return sp;
    }

    public void addFlatBackground(int normalColor, int highlightColor) {
        mNormalSprites.add(makeFlatSprite(normalColor));
        mHighlightSprites.add(makeFlatSprite(highlightColor));
        updateSprites();
    }

    public void addTex(boolean showWhenNormal, boolean showWhenHighlighted,
            int texIndex, float deltaX, float deltaY, float width, float height) {

        if (!showWhenHighlighted && !showWhenNormal) {
            return;
        }

        Renderer.Sprite sp = mRenderer.createSprite();
        sp.x = mX + deltaX;
        sp.y = mY + deltaY;
        sp.width = width;
        sp.height = height;
        sp.texIndex = texIndex;
        sp.tintFactor = 0.0f;

        if (showWhenHighlighted) {
            mHighlightSprites.add(sp);
        }

        if (showWhenNormal) {
            mNormalSprites.add(sp);
        }

        updateSprites();
    }

    public void addNormalTex(int texIndex, float deltaX, float deltaY, float width, float height) {
        addTex(true, false, texIndex, deltaX, deltaY, width, height);
    }

    public void addNormalTex(int texIndex) {
        addNormalTex(texIndex, 0.0f, 0.0f, mWidth, mHeight);
    }

    public void addHighlightTex(int texIndex, float deltaX, float deltaY, float width,
            float height) {
        addTex(false, true, texIndex, deltaX, deltaY, width, height);
    }

    public void addHighlightTex(int texIndex) {
        addHighlightTex(texIndex, 0.0f, 0.0f, mWidth, mHeight);
    }

    public void addTex(int texIndex, float deltaX, float deltaY, float width, float height) {
        addTex(true, true, texIndex, deltaX, deltaY, width, height);
    }

    public void addTex(int texIndex, float width, float height) {
        addTex(true, true, texIndex, 0.0f, 0.0f, width, height);
    }

    public void addTex(int texIndex) {
        addTex(texIndex, mWidth, mHeight);
    }

    private void enableSprites(ArrayList<Renderer.Sprite> sprites, boolean enable) {
        int i;
        for (i = 0; i < sprites.size(); i++) {
            sprites.get(i).enabled = enable;
        }
    }

    private void updateSprites() {
        if (!mVisible) {
            enableSprites(mNormalSprites, false);
            enableSprites(mHighlightSprites, false);
        } else if (mArmed) {
            enableSprites(mNormalSprites, false);
            enableSprites(mHighlightSprites, true);
        } else {
            enableSprites(mHighlightSprites, false);
            enableSprites(mNormalSprites, true);
        }
    }

    public void setClickListener(WidgetTriggerListener listener, int message) {
        mListener = listener;
        mTriggerMessage = message;
    }

    public void hide() {
        show(false);
    }

    public void show() {
        show(true);
    }

    public void show(boolean show) {
        mVisible = show;
        updateSprites();
    }

    // Simulate button click event.
    public void setPressed(boolean pressed) {
        if (mArmed != pressed) {
            mArmed = pressed;
            updateSprites();
        }
    }

    public void bringToFront() {
        int i;
        for (i = 0; i < mNormalSprites.size(); i++) {
            if (mNormalSprites.get(i).enabled) {
                mRenderer.bringToFront(mNormalSprites.get(i));
            }
        }
        for (i = 0; i < mHighlightSprites.size(); i++) {
            if (mHighlightSprites.get(i).enabled) {
                mRenderer.bringToFront(mHighlightSprites.get(i));
            }
        }
    }

    private boolean isInButton(float x, float y) {
        if (!mVisible) {
            return false;
        }
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        return dx < 0.5f * mWidth && dy <= 0.5f * mHeight;
    }

    @Override
    public void onPointerDown(int pointerId, float x, float y) {
        if(mEnabled) {
            super.onPointerDown(pointerId, x, y);
            if (isInButton(x, y)) {
                mArmed = true;
                updateSprites();
            }
        }
    }

    @Override
    public void onPointerMove(int pointerId, float x, float y, float deltaX, float deltaY) {
        if(mEnabled) {
            super.onPointerMove(pointerId, x, y, deltaX, deltaY);
            if (!isInButton(x, y)) {
                mArmed = false;
                updateSprites();
            }
        }
    }

    @Override
    public void onPointerUp(int pointerId, float x, float y) {
        if(mEnabled) {
            super.onPointerUp(pointerId, x, y);
            if (mArmed && isInButton(x, y)) {
                mArmed = false;
                updateSprites();
                if (mListener != null) {
                    mListener.onWidgetTriggered(mTriggerMessage);
                }
            }
        }
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    @Override
    public void dispose() {
        for (Renderer.Sprite sp : mNormalSprites) {
            mRenderer.deleteSprite(sp);

            // remove it from the other collection too, in case it's shared between them:
            mHighlightSprites.remove(sp);
        }
        mNormalSprites.clear();
        for (Renderer.Sprite sp : mHighlightSprites) {
            mRenderer.deleteSprite(sp);
        }
        mRenderer = null;
    }
}
