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

package com.google.android.apps.santatracker.doodles.shared.animation;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Pair;
import com.google.android.apps.santatracker.doodles.shared.BitmapCache;
import com.google.android.apps.santatracker.doodles.shared.CallbackProcess;
import com.google.android.apps.santatracker.doodles.shared.ProcessChain;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.WaitProcess;
import com.google.android.apps.santatracker.doodles.shared.physics.Util;
import com.google.android.apps.santatracker.util.SantaLog;
import java.util.ArrayList;
import java.util.List;

/**
 * An animated image. This also handles static, non-animated images: those are just animations with
 * only 1 frame.
 */
public class AnimatedSprite {
    private static final String TAG = AnimatedSprite.class.getSimpleName();
    private static final int DEFAULT_FPS = 24;
    private static final int NUM_TRIES_TO_LOAD_FROM_MEMORY = 3;

    // When loading any sprite, this was the last successful sampleSize.  We start loading the next
    // Sprite with this sampleSize.
    public static int lastUsedSampleSize = 1;

    private static BitmapCache bitmapCache;
    public int frameWidth;
    public int frameHeight;
    public Vector2D anchor = Vector2D.get();
    private Bitmap[] frames;
    private int fps = DEFAULT_FPS;
    private int numFrames;
    private boolean loop = true;
    private boolean paused = false;
    private boolean flippedX = false;
    private List<AnimatedSpriteListener> listeners;
    private Vector2D position = Vector2D.get();
    private boolean hidden;
    private float scaleX = 1;
    private float scaleY = 1;
    private float rotation;
    private Paint paint;
    private int sampleSize = 1;

    private float frameIndex;

    // These are fields in order to avoid allocating memory in draw(). Not threadsafe, but why would
    // draw get called from multiple threads?
    private Rect srcRect = new Rect();
    private RectF dstRect = new RectF();

    /** Use fromFrames() to construct an AnimatedSprite. */
    private AnimatedSprite(Bitmap[] frames, int sampleSize) {
        this.frames = frames;
        this.sampleSize = sampleSize;
        if (lastUsedSampleSize < sampleSize) {
            lastUsedSampleSize = sampleSize;
        }
        numFrames = this.frames.length;
        if (numFrames == 0) {
            throw new IllegalArgumentException("Can't have AnimatedSprite with zero frames.");
        }
        frameWidth = frames[0].getWidth() * sampleSize;
        frameHeight = frames[0].getHeight() * sampleSize;
        listeners = new ArrayList<>();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
    }

    /** Return AnimatedSprite built from separate images (one image per frame). */
    public static AnimatedSprite fromFrames(Resources resources, int[] ids) {
        int sampleSize = lastUsedSampleSize;
        Bitmap frames[] = new Bitmap[ids.length];
        for (int i = 0; i < ids.length; i++) {
            Pair<Bitmap, Integer> pair = getBitmapFromCache(ids[i], 0);
            if (pair != null) {
                frames[i] = pair.first;
                sampleSize = pair.second;
            }
            if (frames[i] == null) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                for (int tries = 0; tries < NUM_TRIES_TO_LOAD_FROM_MEMORY; tries++) {
                    try {
                        // Decode bitmap with inSampleSize set
                        options.inSampleSize = sampleSize;
                        frames[i] = BitmapFactory.decodeResource(resources, ids[i], options);
                    } catch (OutOfMemoryError oom) {
                        sampleSize *= 2;
                        SantaLog.d(TAG, "loading failed, trying sampleSize: " + sampleSize, oom);
                    }
                }
                putBitmapInCache(frames[i], ids[i], 0, sampleSize);
            }
        }
        return new AnimatedSprite(frames, sampleSize);
    }

    /** Return AnimatedSprite built from the given Bitmap objects. (For testing). */
    public static AnimatedSprite fromBitmapsForTest(Bitmap frames[]) {
        return new AnimatedSprite(frames, 1);
    }

    /**
     * Return AnimatedSprite built from the same frames as another animated sprite. This isn't a
     * deep clone, only the frames & FPS of the original sprite are copied.
     */
    public static AnimatedSprite fromAnimatedSprite(AnimatedSprite other) {
        AnimatedSprite sprite = new AnimatedSprite(other.frames, other.sampleSize);
        sprite.setFPS(other.fps);
        return sprite;
    }

    private static Pair<Bitmap, Integer> getBitmapFromCache(int id, int frame) {
        if (bitmapCache == null) {
            bitmapCache = new BitmapCache();
        }
        return bitmapCache.getBitmapFromCache(id, frame);
    }

    private static void putBitmapInCache(Bitmap bitmap, int id, int frame, int sampleSize) {
        if (bitmapCache == null) {
            bitmapCache = new BitmapCache();
        }
        bitmapCache.putBitmapInCache(bitmap, id, frame, sampleSize);
    }

    public static void clearCache() {
        if (AnimatedSprite.bitmapCache != null) {
            AnimatedSprite.bitmapCache.clear();
        }
    }

    /** Set whether to loop the animation or not. */
    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public boolean isFlippedX() {
        return flippedX;
    }

    public void setFlippedX(boolean value) {
        this.flippedX = value;
    }

    public void setPaused(boolean value) {
        this.paused = value;
    }

    /**
     * Pause this sprite and return a process chain which can be updated to unpause the sprite after
     * the specified length of time.
     *
     * @param durationMs how many milliseconds to pause the sprite for.
     * @return a process chain which will unpause the sprite after the duration has completed.
     */
    public ProcessChain pauseFor(long durationMs) {
        setPaused(true);
        CallbackProcess unpause =
                new CallbackProcess() {
                    @Override
                    public void updateLogic(float deltaMs) {
                        setPaused(false);
                    }
                };
        return new WaitProcess(durationMs).then(unpause);
    }

    /** Change the speed of the animation. */
    public void setFPS(int fps) {
        this.fps = fps;
    }

    public int getFrameIndex() {
        return (int) frameIndex;
    }

    /** Sets the current frame. */
    public void setFrameIndex(int frame) {
        frameIndex = frame;
    }

    public int getNumFrames() {
        return numFrames;
    }

    public float getDurationSeconds() {
        return numFrames / (float) fps;
    }

    /** Update the animation based on deltaMs having passed. */
    public void update(float deltaMs) {
        if (paused) {
            return;
        }

        float deltaFrames = (deltaMs / 1000.0f) * fps;

        // In order to make sure that we don't skip any frames when notifying listeners, this
        // carefully
        // accumulates deltaFrames instead of just immediately adding it into frameIndex. Be careful
        // of floating point precision issues below.
        while (deltaFrames > 0) {
            // First, try accumulating the remaining deltaFrames and see if we make it to the next
            // frame.
            float newFrameIndex = frameIndex + deltaFrames;
            if ((int) newFrameIndex == (int) frameIndex) {
                // Didn't make it to the next frame. Done accumulating.
                frameIndex = newFrameIndex;
                deltaFrames = 0;
            } else {
                // Move forward to next frame, notify listeners, then keep accumlating.
                float oldFrameIndex = frameIndex;
                frameIndex = 1 + (int) frameIndex; // ignores numFrames, will handle it below.
                deltaFrames -= frameIndex - oldFrameIndex;

                if (frameIndex < numFrames) {
                    sendOnFrameNotification((int) frameIndex);
                } else {
                    if (loop) {
                        frameIndex = 0;
                        sendOnLoopNotification();
                        sendOnFrameNotification((int) frameIndex);
                    } else {
                        frameIndex = numFrames - 1;
                        sendOnFinishNotification();
                        // In this branch, there are no further onFrame notifications.
                        deltaFrames = 0; // No more changes to frameIndex, done accumulating.
                    }
                }
            }
        }
    }

    void sendOnLoopNotification() {
        for (int i = 0; i < listeners.size(); i++) { // Avoiding iterators to avoid garbage.
            // Call the on-loop callbacks.
            listeners.get(i).onLoop();
        }
    }

    void sendOnFinishNotification() {
        for (int i = 0; i < listeners.size(); i++) { // Avoiding iterators to avoid garbage
            // Call the on-finished callbacks.
            listeners.get(i).onFinished();
        }
    }

    void sendOnFrameNotification(int frame) {
        for (int i = 0; i < listeners.size(); i++) { // Avoiding iterators to avoid garbage.
            listeners.get(i).onFrame(frame);
        }
    }

    public void draw(Canvas canvas) {
        if (!hidden) {
            // Integer cast should round down, but clamp it just in case the synchronization with
            // the
            // update thread isn't perfect.
            int frameIndexFloor = Util.clamp((int) frameIndex, 0, numFrames - 1);
            float scaleX = flippedX ? -this.scaleX : this.scaleX;

            canvas.save();
            srcRect.set(0, 0, frameWidth, frameHeight);
            dstRect.set(-anchor.x, -anchor.y, -anchor.x + frameWidth, -anchor.y + frameHeight);

            canvas.translate(position.x, position.y);
            canvas.scale(scaleX, scaleY, 0, 0);
            canvas.rotate((float) Math.toDegrees(rotation), 0, 0);

            canvas.drawBitmap(frames[frameIndexFloor], srcRect, dstRect, paint);
            canvas.restore();
        }
    }

    // Unlike Actors, AnimatedSprites use setters instead of public fields for position, scale, etc.
    // This matches how it works on iOS, which uses setters because the actual values must be passed
    // down into SKNodes.
    public void setPosition(float x, float y) {
        position.x = x;
        position.y = y;
    }

    /** @param alpha: 0.0 = transparent, 1.0 = opaque. */
    public void setAlpha(float alpha) {
        paint.setAlpha((int) (alpha * 255));
    }

    // You can use this to more closely match the logic of iOS, where there is no draw method so the
    // only way to hide something is to set this flag. On Android, you can also just not call draw
    // if you want a sprite hidden.
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void setScale(float scaleX, float scaleY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public float getScaledWidth() {
        return scaleX * frameWidth;
    }

    public float getScaledHeight() {
        return scaleY * frameHeight;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    // Sets the anchor point which determines where the sprite is drawn relative to its position.
    // This
    // is also the point around which sprite rotates & scales. (x, y) are in pixels, relative to
    // top-left corner. Initially set to the upper-left corner.
    public void setAnchor(float x, float y) {
        anchor.x = x;
        anchor.y = y;
    }

    public void addListener(AnimatedSpriteListener listener) {
        listeners.add(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    public int getNumListeners() {
        return listeners.size();
    }

    // Reverse the frames of the animation. This doesn't update frameIndex, which may or may not
    // be what you want.
    public void reverseFrames() {
        for (int i = 0; i < frames.length / 2; i++) {
            Bitmap temp = frames[i];
            frames[i] = frames[frames.length - i - 1];
            frames[frames.length - i - 1] = temp;
        }
    }

    /** A class which can be implemented to provide callbacks for AnimatedSprite events. */
    public static class AnimatedSpriteListener {
        public void onFinished() {}

        public void onLoop() {}

        public void onFrame(int index) {}
    }
}
