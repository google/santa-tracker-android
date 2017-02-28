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

package com.google.android.apps.santatracker.games.simpleengine;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public final class Renderer {

    public static final int DIM_HEIGHT = BitmapTextureMaker.DIM_HEIGHT;
    public static final int DIM_WIDTH = BitmapTextureMaker.DIM_WIDTH;

    // for getRelativePos()
    public static final int REL_CENTER = 0;
    public static final int REL_LEFT = 1;
    public static final int REL_TOP = 2;
    public static final int REL_RIGHT = 3;
    public static final int REL_BOTTOM = 4;

    private boolean mInitDone = false;

    private ArrayList<Sprite> mSprites = new ArrayList<Sprite>(64);
    private ArrayList<Sprite> mSpriteRecycleBin = new ArrayList<Sprite>(64);

    // Non null only when we are currently loading bitmaps. So this will be null
    // if and only if we have all the requested bitmaps ready as textures.
    private BitmapTextureMaker mBitmapTextureMaker = null;

    // Non null only when we are currently making text textures.
    private TextTextureMaker mTextTextureMaker = null;

    // current surface dimensions
    int mSurfWidth = 0;
    int mSurfHeight = 0;

    // coordinate system bounds
    RectF mBounds = new RectF();

    // information about each texture requested
    private ArrayList<TexInfo> mTexInfo = new ArrayList<TexInfo>();

    private class TexInfo {

        int glTex = 0;  // OpenGL texture handle, if loaded; 0 if not yet loaded
        float aspect;  // aspect ratio (width/height), computed when texture is loaded
        int width, height; // computed when texture is loaded

        // texture request parameters
        static final int TYPE_IMAGE = 0;
        static final int TYPE_TEXT = 1;

        int type = TYPE_IMAGE;

        int resId; // (for TYPE_IMAGE, it's a drawable; for TYPE_TEXT, it's a string res id)
        String name;

        // for TYPE_IMAGE only:
        int dimType;
        float maxDim;

        // for TYPE_TEXT only:
        float fontSize;
        int textAnchor = TEXT_ANCHOR_CENTER | TEXT_ANCHOR_MIDDLE;
        int textColor = 0xffffffff;
    }

    public static final int TEXT_ANCHOR_CENTER = 0x00;
    public static final int TEXT_ANCHOR_LEFT = 0x01;
    public static final int TEXT_ANCHOR_RIGHT = 0x02;
    private static final int TEXT_ANCHOR_HORIZ_MASK = 0x0f;
    public static final int TEXT_ANCHOR_TOP = 0x10;
    public static final int TEXT_ANCHOR_BOTTOM = 0x20;
    public static final int TEXT_ANCHOR_MIDDLE = 0x00;
    private static final int TEXT_ANCHOR_VERT_MASK = 0xf0;

    // locations of attributes and uniforms in our shader
    private int mLocMatrix = -1;
    private int mLocColor = -1;
    private int mLocTintFactor = -1;
    private int mLocSampler = -1;
    private int mLocPosition = -1;
    private int mLocTexCoord = -1;

    // quad data
    private static float[] QUAD_GEOM = {  // screenX, screenY, z, u, v
            -0.5f, -0.5f, 0.0f, 0.0f, 1.0f,
            0.5f, -0.5f, 0.0f, 1.0f, 1.0f,
            -0.5f, 0.5f, 0.0f, 0.0f, 0.0f,
            0.5f, 0.5f, 0.0f, 1.0f, 0.0f,

    };
    private final int SIZEOF_FLOAT = 4;
    private final int QUAD_GEOM_VERTEX_COUNT = 4;
    private final int QUAD_GEOM_STRIDE = 5 * SIZEOF_FLOAT;
    private final int QUAD_GEOM_POS_OFFSET = 0;
    private final int QUAD_GEOM_TEXCOORD_OFFSET = 3;
    FloatBuffer mQuadGeomBuf = null;

    // projection matrix
    float[] mProjMat = null;

    // temp working matrices
    float[] mTmpMatA = new float[16];
    float[] mTmpMatB = new float[16];

    // color used to clear the screen
    private static final int DEFAULT_CLEAR_COLOR = 0xffff0000;

    Renderer() {
    }

    public void onGLSurfaceCreated(Context ctx) {
        initGL();
        refreshTextures(ctx);
        mInitDone = true;
    }

    private int compileShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private int linkProgram(int vertShader, int fragShader) {
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertShader);
        GLES20.glAttachShader(program, fragShader);
        GLES20.glLinkProgram(program);
        return program;
    }

    private void initGL() {
        Logger.d("Initializing OpenGL");
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        Logger.d("Compiling vertex shader.");
        int vertShader = compileShader(GLES20.GL_VERTEX_SHADER, ShaderSource.VERTEX_SHADER);
        Logger.d("Vertex shader is " + vertShader);
        Logger.d("Vertex shader compilation log: " + GLES20.glGetShaderInfoLog(vertShader));
        int fragShader = compileShader(GLES20.GL_FRAGMENT_SHADER, ShaderSource.FRAG_SHADER);
        Logger.d("Fragment shader is " + fragShader);
        Logger.d("Fragment shader compilation log: " + GLES20.glGetShaderInfoLog(fragShader));
        int program = linkProgram(vertShader, fragShader);
        Logger.d("Program is " + program);
        Logger.d("Program linking log: " + GLES20.glGetProgramInfoLog(program));

        Logger.d("Activating shader.");
        GLES20.glUseProgram(program);

        // get locations
        mLocMatrix = GLES20.glGetUniformLocation(program, "u_Matrix");
        mLocColor = GLES20.glGetUniformLocation(program, "u_Color");
        mLocTintFactor = GLES20.glGetUniformLocation(program, "u_TintFactor");
        mLocSampler = GLES20.glGetUniformLocation(program, "u_Sampler");
        mLocPosition = GLES20.glGetAttribLocation(program, "a_Position");
        mLocTexCoord = GLES20.glGetAttribLocation(program, "a_TexCoord");
        Logger.d("Locations: " +
                "mLocMatrix=" + mLocMatrix + "; " +
                "mLocColor=" + mLocColor + "; " +
                "mLocTintFactor=" + mLocTintFactor + "; " +
                "mLocSampler=" + mLocSampler + "; " +
                "mLocPosition=" + mLocPosition + "; " +
                "mLocTexCoord=" + mLocTexCoord);

        ByteBuffer bb = ByteBuffer.allocateDirect(SIZEOF_FLOAT * QUAD_GEOM.length);
        bb.order(ByteOrder.nativeOrder());
        mQuadGeomBuf = bb.asFloatBuffer();
        mQuadGeomBuf.put(QUAD_GEOM);
        mQuadGeomBuf.position(0);

        // set up opengl blending
        GLES20.glEnable(GLES20.GL_BLEND);

        // we use GL_ONE instead of GL_SRC_ALPHA because Android premultiplies
        // the alpha channel in the PNG by r,g,b, so if we use GL_SRC_ALPHA, we get
        // a gray halo around things.
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void pushTex(int tex) {
        GLES20.glUniform1i(mLocSampler, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
        GLES20.glBindTexture(GLES20.GL_TEXTURE0, tex);
    }

    private void pushColor(float r, float g, float b, float a, float factor) {
        GLES20.glUniform4f(mLocColor, r, g, b, a);
        GLES20.glUniform1f(mLocTintFactor, factor);
    }

    private void drawQuad(float centerX, float centerY, float width, float height,
            float rotation) {
        // compute final matrix
        float[] modelViewM = mTmpMatA;
        float[] finalM = mTmpMatB;
        Matrix.setIdentityM(modelViewM, 0);
        Matrix.translateM(modelViewM, 0, centerX, centerY, 0.0f);
        Matrix.rotateM(modelViewM, 0, rotation, 0.0f, 0.0f, 1.0f);
        Matrix.scaleM(modelViewM, 0, width, height, 1.0f);
        Matrix.multiplyMM(finalM, 0, mProjMat, 0, modelViewM, 0);

        // push matrix
        GLES20.glUniformMatrix4fv(mLocMatrix, 1, false, finalM, 0);

        // push positions
        GLES20.glEnableVertexAttribArray(mLocPosition);
        mQuadGeomBuf.position(QUAD_GEOM_POS_OFFSET);
        GLES20.glVertexAttribPointer(mLocPosition, 3, GLES20.GL_FLOAT, false,
                QUAD_GEOM_STRIDE, mQuadGeomBuf);

        // push texture coordinates
        GLES20.glEnableVertexAttribArray(mLocTexCoord);
        mQuadGeomBuf.position(QUAD_GEOM_TEXCOORD_OFFSET);
        GLES20.glVertexAttribPointer(mLocTexCoord, 2, GLES20.GL_FLOAT, false,
                QUAD_GEOM_STRIDE, mQuadGeomBuf);

        // draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, QUAD_GEOM_VERTEX_COUNT);
    }

    void calcCoordSystemBounds(int surfWidth, int surfHeight, RectF outBounds) {
        if (surfWidth > surfHeight) {
            // landscape orientation -- height is set to 1.0, width is proportional
            outBounds.right = (surfWidth / (float) surfHeight) * 0.5f;
            outBounds.left = -outBounds.right;
            outBounds.top = 0.5f;
            outBounds.bottom = -0.5f;
        } else {
            // portrait orientation -- width is set to 1.0, height is proportional
            outBounds.top = (surfWidth / (float) surfHeight) * 0.5f;
            outBounds.bottom = -outBounds.right;
            outBounds.right = 0.5f;
            outBounds.left = -0.5f;
        }
    }

    public final float convertScreenX(float screenX) {
        return mBounds.left + (screenX / mSurfWidth) * (mBounds.right - mBounds.left);
    }

    public final float convertScreenY(float screenY) {
        float factor = 1 - (screenY / mSurfHeight);
        return mBounds.bottom + factor * (mBounds.top - mBounds.bottom);
    }

    public final float convertScreenDeltaX(float deltaX) {
        return convertScreenX(deltaX) - convertScreenX(0.0f);
    }

    public final float convertScreenDeltaY(float deltaY) {
        return convertScreenY(deltaY) - convertScreenY(0.0f);
    }

    void onGLSurfaceChanged(int width, int height) {
        Logger.d("Renderer.onGLSurfaceChanged " + width + "x" + height);
        GLES20.glViewport(0, 0, width, height);
        mSurfHeight = height;
        mSurfWidth = width;

        // calculate bounds for our coordinate system
        calcCoordSystemBounds(mSurfWidth, mSurfHeight, mBounds);

        // set up projection matrix
        mProjMat = new float[16];
        Matrix.orthoM(mProjMat, 0, mBounds.left, mBounds.right, mBounds.bottom,
                mBounds.top, -1.0f, 1.0f);
    }

    void generateImageTextures() {
        int count = mBitmapTextureMaker.getBitmapCount();
        int i;
        for (i = 0; i < count; i++) {
            int texIndex = mBitmapTextureMaker.getTag(i);
            generateTexture(texIndex, mBitmapTextureMaker.getBitmap(i));
        }
        mBitmapTextureMaker.dispose();
        mBitmapTextureMaker = null;
    }

    void generateTextTextures() {
        int count = mTextTextureMaker.getCount();
        int i;
        for (i = 0; i < count; i++) {
            int texIndex = mTextTextureMaker.getTag(i);
            generateTexture(texIndex, mTextTextureMaker.getBitmap(i));
        }
        mTextTextureMaker.dispose();
        mTextTextureMaker = null;
    }

    private void generateTexture(int texIndex, Bitmap bmp) {
        int[] texH = new int[1];
        GLES20.glGenTextures(1, texH, 0);
        TexInfo ti = mTexInfo.get(texIndex);
        bitmapToGLTexture(texH[0], bmp);

        ti.glTex = texH[0];
        ti.width = bmp.getWidth();
        ti.height = bmp.getHeight();
        ti.aspect = bmp.getWidth() / (float) bmp.getHeight();
    }

    void bitmapToGLTexture(int texH, Bitmap bmp) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texH);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
    }

    // returns true if all resources are ready, false if still loading
    boolean prepareFrame() {
        if (mBitmapTextureMaker != null && mBitmapTextureMaker.isFinishedLoading()) {
            // bitmap bank finished loading bitmaps -- time to convert them to textures!
            generateImageTextures();
        } else if (mTextTextureMaker != null && mTextTextureMaker.isFinishedLoading()) {
            // text texture generator finished -- time to convert into textures!
            generateTextTextures();
        }

        // we are ready if and only if there are no pending images/text to load
        return mBitmapTextureMaker == null && mTextTextureMaker == null;
    }

    private void parseColor(int color, float[] out) {
        long c = color;
        out[0] = ((c & 0x00ff0000L) >>> 16) / 255.0f;
        out[1] = ((c & 0x0000ff00L) >>> 8) / 255.0f;
        out[2] = (c & 0x000000ffL) / 255.0f;
        out[3] = ((c & 0xff000000L) >>> 24) / 255.0f;

        // our setup requires that we premultiply the alpha. This is because we're using
        // blending mode glBlend(GL_ONE, GL_ONE_MINUS_SRC_ALPHA), to be compatible with
        // how GLUtils loads PNGs.
        out[0] *= out[3];
        out[1] *= out[3];
        out[2] *= out[3];
    }


    private float[] mTmpColor = new float[4];

    public void doFrame() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mBitmapTextureMaker != null || mTextTextureMaker != null) {
            // we are still loading textures, so don't render any sprites yet
            return;
        }
        int i, size = mSprites.size();
        for (i = 0; i < size; i++) {
            Sprite s = mSprites.get(i);
            drawSprite(s);
        }
    }

    private boolean drawSprite(Sprite s) {
        if (!s.enabled) {
            return false;
        }

        float tintFactor = s.tintFactor;
        TexInfo ti = null;
        if (s.texIndex >= 0 && s.texIndex < mTexInfo.size()) {
            ti = mTexInfo.get(s.texIndex);
            pushTex(ti.glTex);

        } else {
            pushTex(0);
            tintFactor = 1.0f;
        }
        parseColor(s.color, mTmpColor);
        pushColor(mTmpColor[0], mTmpColor[1], mTmpColor[2], mTmpColor[3], tintFactor);

        float width = s.width, height = s.height, x = s.x, y = s.y;

        if (ti != null && ti.type == TexInfo.TYPE_IMAGE) {
            width = s.width;
            height = s.height;
            if (Float.isNaN(width) && ti != null) {
                // auto calculate width based on texture aspect ratio
                width = s.width = s.height * ti.aspect;
            }
            if (Float.isNaN(height) && ti != null) {
                // auto calculate height based on texture aspect ratio
                height = s.height = s.width / ti.aspect;
            }
        } else if (ti != null && ti.type == TexInfo.TYPE_TEXT) {
            // text images don't respect width/height -- they render at whatever
            // size they were created, in order to respect the originally requested font size
            width = pixelsToLogical(ti.width);
            height = pixelsToLogical(ti.height);

            // adjust x,y according to text anchor parameter
            int horizAnchor = ti.textAnchor & TEXT_ANCHOR_HORIZ_MASK;
            int vertAnchor = ti.textAnchor & TEXT_ANCHOR_VERT_MASK;
            if (horizAnchor == TEXT_ANCHOR_LEFT) {
                x += width * 0.5f;
            } else if (horizAnchor == TEXT_ANCHOR_RIGHT) {
                x -= width * 0.5f;
            }
            if (vertAnchor == TEXT_ANCHOR_TOP) {
                y += height * 0.5f;
            } else if (vertAnchor == TEXT_ANCHOR_BOTTOM) {
                y -= height * 0.5f;
            }
        }
        drawQuad(x, y, width, height, s.rotation);
        return true;
    }

    private float pixelsToLogical(int pixels) {
        float logicalWidth = mBounds.width();
        float pixelsWidth = mSurfWidth;
        return (pixels / (float) pixelsWidth) * logicalWidth;
    }

    public int requestImageTex(int resId, String name, int dimType, float maxDim) {
        TexInfo ti = new TexInfo();
        ti.type = TexInfo.TYPE_IMAGE;
        ti.resId = resId;
        ti.name = name;
        ti.dimType = dimType;
        ti.maxDim = maxDim;
        ti.glTex = 0; // loading
        ti.aspect = Float.NaN; // unknown for now
        mTexInfo.add(ti);
        return mTexInfo.size() - 1;
    }

    public int requestTextTex(int resId, String name, float fontSize, int textAnchor, int color) {
        TexInfo ti = new TexInfo();
        ti.type = TexInfo.TYPE_TEXT;
        ti.resId = resId;
        ti.fontSize = fontSize;
        ti.glTex = 0; // loading
        ti.aspect = Float.NaN; // unknown for now
        ti.textAnchor = textAnchor;
        ti.textColor = color;
        mTexInfo.add(ti);
        return mTexInfo.size() - 1;
    }

    public int requestTextTex(int resId, String name, float fontSize) {
        return requestTextTex(resId, name, fontSize,
                TEXT_ANCHOR_CENTER | TEXT_ANCHOR_MIDDLE, 0xffffffff);
    }

    public float getLeft() {
        return mBounds.left;
    }

    public float getRight() {
        return mBounds.right;
    }

    public float getTop() {
        return mBounds.top;
    }

    public float getBottom() {
        return mBounds.bottom;
    }

    public float getWidth() {
        return mBounds.width();
    }

    public float getHeight() {
        // height() doesn't work because in our coord system top > bottom,
        // and RectF doesn't like that.
        return mBounds.top - mBounds.bottom;
    }

    public void deleteTextures() {
        int[] arr = new int[1];
        for (TexInfo ti : mTexInfo) {
            if (ti.glTex > 0) {
                arr[0] = ti.glTex;
                GLES20.glDeleteTextures(1, arr, 0);
                ti.glTex = 0;
            }
        }
        mTexInfo.clear();
    }

    public void reset() {
        deleteTextures();
        deleteSprites();
    }

    private void refreshTextures(Context ctx) {
        if (mTexInfo.size() > 0) {
            for (TexInfo ti : mTexInfo) {
                ti.glTex = 0;
            }
            startLoadingTexs(ctx);
        }
    }

    void startLoadingTexs(Context ctx) {
        mBitmapTextureMaker = new BitmapTextureMaker();
        mTextTextureMaker = new TextTextureMaker();

        int i = 0;
        for (i = 0; i < mTexInfo.size(); i++) {
            TexInfo ti = mTexInfo.get(i);
            if (ti.type == TexInfo.TYPE_IMAGE) {
                mBitmapTextureMaker.request(i, ti.resId, ti.name, ti.dimType, ti.maxDim);
            } else if (ti.type == TexInfo.TYPE_TEXT) {
                mTextTextureMaker.requestTex(i, ctx.getString(ti.resId), ti.fontSize, ti.textColor);
            }
        }

        // start loading the textures in a background thread
        mBitmapTextureMaker.startLoading(ctx);
        mTextTextureMaker.startLoading(ctx);
    }

    void dispose() {
        if (mBitmapTextureMaker != null) {
            mBitmapTextureMaker.dispose();
            mBitmapTextureMaker = null;
        }
        if (mTextTextureMaker != null) {
            mTextTextureMaker.dispose();
            mTextTextureMaker = null;
        }
    }

    public class Sprite {

        // note: NaN on width OR height means "compute automatically based on texture's
        // aspect ratio.
        public boolean enabled;
        public float x, y, width, height;
        public int texIndex;
        public int color;
        public float tintFactor;
        public float rotation;

        public Sprite() {
            clear();
        }

        public Sprite clear() {
            x = y = 0.0f;
            width = height = 1.0f;
            texIndex = -1;
            color = 0xff000080;
            tintFactor = 1.0f;
            enabled = true;
            rotation = 0.0f;
            return this;
        }
    }

    public Sprite createSprite() {
        Sprite s;
        if (mSpriteRecycleBin.size() > 0) {
            s = mSpriteRecycleBin.remove(mSpriteRecycleBin.size() - 1);
        } else {
            s = new Sprite();
        }
        mSprites.add(s);
        return s;
    }

    public void deleteSprite(Sprite s) {
        s.clear();
        mSprites.remove(s);
        mSpriteRecycleBin.add(s);
    }

    public void deleteSprites() {
        int i;
        for (i = 0; i < mSprites.size(); i++) {
            mSpriteRecycleBin.add(mSprites.get(i).clear());
        }
        mSprites.clear();
    }

    public float getRelativePos(int relativeTo, float delta) {
        return delta + (relativeTo == REL_RIGHT ? mBounds.right :
                relativeTo == REL_LEFT ? mBounds.left :
                        relativeTo == REL_TOP ? mBounds.top :
                                relativeTo == REL_BOTTOM ? mBounds.bottom : 0.0f);
    }

    public void bringToFront(Sprite sp) {
        int idx = mSprites.indexOf(sp);
        if (idx >= 0 && idx < mSprites.size()) {
            mSprites.remove(idx);
            mSprites.add(sp);
        }
    }

    public void sendToBack(Sprite sp) {
        int idx = mSprites.indexOf(sp);
        if (idx > 0 && idx < mSprites.size()) {
            mSprites.remove(idx);
            mSprites.add(0, sp);
        }
    }
}
