/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

package com.google.android.apps.santatracker.launch;

import com.google.android.apps.santatracker.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.util.Random;

/**
 * Pin for the launcher.
 * The outline of a pin is drawn programmatically based on the available size of this view.
 */
public class MarkerView extends ImageView {

    /**
     * Lock icon.
     */
    private static final Path LOCK = new Path();
    /**
     * Marker pin outline.
     */
    private static final Path MARKER = new Path();
    /**
     * Fraction of marker width to use as lock diameter
     */
    private static final float LOCK_FRACTION = 0.4f;
    /**
     * Colors available for markers.
     */
    private static final int[] PALETTE = new int[]{R.color.SantaBlue, R.color.SantaBlueGreen,
            R.color.SantaGreen, R.color.SantaOrange, R.color.SantaPink, R.color.SantaPurple,
            R.color.SantaRed, R.color.SantaYellow};

    /**
     * Computed width/height ratio of a mrker, based on the available height and width.
     */
    private static final float MARKER_RATIO;
    /**
     * Saturation level for the disabled state.
     *
     * @see #setDisabled(boolean)
     * @see android.graphics.ColorMatrix#setSaturation(float)
     */
    private static final float DISABLED_SATURATION = 0.1f;

    private static Paint sTouchedPaint = new Paint();
    private static Paint sLockPaint = new Paint();

    private static RectF sBounds = new RectF();
    private static RectF sLockBounds = new RectF();

    static {
        MARKER.moveTo(46.700001f, 164.699997f);
        MARKER.cubicTo(44.700001f, 164.699997f, 43.000000f, 163.099991f, 42.799999f, 161.199997f);
        MARKER.cubicTo(36.900002f, 97.599998f, 0.000000f, 81.000000f, 0.000000f, 46.700001f);
        MARKER.cubicTo(0.000000f, 20.900000f, 20.900000f, 0.000000f, 46.700001f, 0.000000f);
        MARKER.cubicTo(72.500000f, 0.000000f, 93.400002f, 20.900000f, 93.400002f, 46.700001f);
        MARKER.cubicTo(93.400002f, 81.000000f, 56.500000f, 97.699997f, 50.600002f, 161.199997f);
        MARKER.cubicTo(50.400002f, 163.100006f, 48.700001f, 164.699997f, 46.700001f, 164.699997f);
        MARKER.close();
        MARKER.computeBounds(sBounds, true);

        LOCK.moveTo(24.000000f, 13.200000f);
        LOCK.rLineTo(0.000000f, -1.400000f);
        LOCK.cubicTo(24.000000f, 7.900000f, 20.799999f, 4.700000f, 16.900000f, 4.700000f);
        LOCK.cubicTo(13.000000f, 4.700000f, 9.799999f, 7.900001f, 9.799999f, 11.800000f);
        LOCK.rLineTo(0.000000f, 1.400000f);
        LOCK.lineTo(8.400000f, 13.200000f);
        LOCK.rLineTo(0.000000f, 13.300000f);
        LOCK.rLineTo(17.200001f, 0.000000f);
        LOCK.lineTo(25.600000f, 13.200000f);
        LOCK.lineTo(24.000000f, 13.200000f);
        LOCK.close();
        LOCK.moveTo(24.000000f, 13.200000f);
        LOCK.moveTo(12.100000f, 11.800000f);
        LOCK.cubicTo(12.100000f, 9.200001f, 14.200001f, 7.000000f, 16.900002f, 7.000000f);
        LOCK.cubicTo(19.500002f, 7.000000f, 21.700001f, 9.100000f, 21.700001f, 11.800000f);
        LOCK.rLineTo(0.000000f, 1.400000f);
        LOCK.rLineTo(-9.500000f, 0.000000f);
        LOCK.lineTo(12.200001f, 11.800000f);
        LOCK.close();
        LOCK.moveTo(12.100000f, 11.800000f);
        LOCK.moveTo(18.500000f, 23.200001f);
        LOCK.rLineTo(-3.000000f, 0.000000f);
        LOCK.rLineTo(0.900000f, -3.700000f);
        LOCK.cubicTo(15.700000f, 19.200001f, 15.099999f, 18.500000f, 15.099999f, 17.700001f);
        LOCK.cubicTo(15.099999f, 16.700001f, 15.900000f, 15.800001f, 17.000000f, 15.800001f);
        LOCK.cubicTo(18.000000f, 15.800001f, 18.900000f, 16.600000f, 18.900000f, 17.700001f);
        LOCK.cubicTo(18.900000f, 18.500000f, 18.400000f, 19.200001f, 17.699999f, 19.400002f);
        LOCK.lineTo(18.500000f, 23.200001f);
        LOCK.close();
        LOCK.computeBounds(sLockBounds, true);

        MARKER_RATIO = sBounds.height() / sBounds.width();

        sLockPaint.setColor(Color.WHITE);
        sTouchedPaint.setColorFilter(new LightingColorFilter(Color.LTGRAY, 1));
    }

    /**
     * Transformation matrix for drawing this marker.
     */
    private Matrix mMatrix = new Matrix();

    private Path mMarkerPath;
    private Path mLockPath;
    private Bitmap mBadgeBitmap;
    private Bitmap mLockedBitmap;
    private Paint mPaint = new Paint();
    private ColorFilter mPaintColorFilter;
    private Paint mLockCirclePaint = new Paint();
    private Rect mBadgePadding;
    private int mColor;
    private boolean mLocked = false;

    public MarkerView(Context context) {
        super(context);
    }

    public MarkerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MarkerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public MarkerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    public void setDrawable(Drawable drawable) {
        mBadgeBitmap = drawableToBitmap(drawable);
    }

    public void setLockedDrawable(Drawable lockedDrawable) {
        mLockedBitmap = drawableToBitmap(lockedDrawable);
    }

    public void setColor(int color) {
        mColor = color;
        mPaint.setColor(color);
        mLockCirclePaint.setColor(color);
        mLockCirclePaint.setColorFilter(new LightingColorFilter(Color.LTGRAY, 1));
    }

    public void setLocked(boolean locked) {
        mLocked = locked;
    }

    public void setDisabled(boolean disabled) {
        if (disabled) {
            mPaint.setColor(getResources().getColor(R.color.disabledMarker));
            ColorMatrix desatMatrix = new ColorMatrix();
            desatMatrix.setSaturation(DISABLED_SATURATION);
            mPaintColorFilter = new ColorMatrixColorFilter(desatMatrix);
        } else {
            mPaint.setColor(mColor);
            mPaintColorFilter = null;
        }
    }

    /**
     * Sets the amount of padding to apply around the border of the badge. As the marker is an
     * uneven shape, this value applies to the 'circular' (top) part of the marker, where the image
     * is scaled to fit in the marker (sans left and right padding) and drawn offset from the top.
     */
    public void setBadgePadding(int left, int top, int right) {
        mBadgePadding = new Rect(left, top, right, 0);
    }

    private void init(AttributeSet attrs) {
        // Fix for invisible markers on ICS and JB devices
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        // set up the local artwork
        mMarkerPath = new Path(MARKER);
        mLockPath = new Path(LOCK);

        // process the XML attributes
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MarkerView);

        setDrawable(a.getDrawable(R.styleable.MarkerView_badge));
        setLockedDrawable(a.getDrawable(R.styleable.MarkerView_lockedBadge));

        int defaultColor = getContext().getResources().getColor(
                PALETTE[new Random().nextInt(PALETTE.length)]);
        setColor(a.getColor(R.styleable.MarkerView_markerColor, defaultColor));

        setBadgePadding(a.getDimensionPixelOffset(R.styleable.MarkerView_badgePaddingLeft, 0),
                a.getDimensionPixelOffset(R.styleable.MarkerView_badgePaddingTop, 0),
                a.getDimensionPixelOffset(R.styleable.MarkerView_badgePaddingRight, 0));
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private float ratio;
    private float lockRatio;
    private float badgeRatio;
    private int markerWidth;
    private int markerCanvasDiff;
    private Bitmap badge;
    private Rect badgeRect = new Rect();
    private int badgeW;
    private int badgeH;
    private int radius;
    private int cx;
    private int cy;
    private int lockX;
    private int lockY;

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBadgeBitmap == null) {
            return;
        }

        // scale the marker to fit the view
        mMarkerPath.computeBounds(sBounds, true);
        ratio = getHeight() / sBounds.height();
        mMatrix.setScale(ratio, ratio);
        mMarkerPath.transform(mMatrix);

        // center the marker
        mMarkerPath.computeBounds(sBounds, true);
        markerWidth = (int) sBounds.width();
        markerCanvasDiff = 0;
        if (getWidth() > markerWidth) {
            markerCanvasDiff = getWidth() - markerWidth;
            mMatrix.setTranslate(markerCanvasDiff / 2 - sBounds.left, 0);
            mMarkerPath.transform(mMatrix);
        }

        // draw the marker
        if (this.isPressed() || this.isFocused()) {
            mPaint.setColorFilter(sTouchedPaint.getColorFilter());
        } else {
            mPaint.setColorFilter(mPaintColorFilter);
        }
        canvas.drawPath(mMarkerPath, mPaint);

        // calculate badge position relative to the marker
        badge = (mLocked && mLockedBitmap != null) ? mLockedBitmap : mBadgeBitmap;
        badgeRatio = (float) badge.getHeight() / badge.getWidth();
        badgeW = markerWidth - (mBadgePadding.left + mBadgePadding.right);
        badgeH = (int) (badgeW * badgeRatio);

        badgeRect.left = (markerCanvasDiff / 2) + mBadgePadding.left;
        badgeRect.top = mBadgePadding.top;
        badgeRect.right = badgeRect.left + badgeW;
        badgeRect.bottom = badgeRect.top + badgeH;

        // draw the badge - but only shade the badge if it's pressed and not disabled
        if (mPaintColorFilter == null && (this.isPressed() || this.isFocused())) {
            mPaint.setColorFilter(sTouchedPaint.getColorFilter());
        } else {
            mPaint.setColorFilter(mPaintColorFilter);
        }
        canvas.drawBitmap(badge, null, badgeRect, mPaint);

        // draw the locked icon in the upper-right corner
        if (mLocked) {
            // draw the circle
            radius = (int) (LOCK_FRACTION * markerWidth / 2);
            cx = (markerCanvasDiff / 2) + markerWidth - (2 * radius); // top-left of circle
            cy = 0;
            canvas.drawCircle(cx + radius, cy + radius, radius, mLockCirclePaint);

            // size the lock
            mLockPath.computeBounds(sLockBounds, true);
            lockRatio = radius / sLockBounds.width();
            mMatrix.setScale(lockRatio, lockRatio);
            mLockPath.transform(mMatrix);

            // position the lock
            mLockPath.computeBounds(sLockBounds, true);
            lockX = (int) (cx + radius - (sLockBounds.width() / 2));
            lockY = (int) (cy + radius - (sLockBounds.height() / 2));
            mMatrix.setTranslate(lockX - sLockBounds.left, lockY - sLockBounds.top);
            mLockPath.transform(mMatrix);

            // draw the lock
            canvas.drawPath(mLockPath, sLockPaint);
        }
    }

    // TODO - expand logic to support view too narrow for marker (i.e. top/bottom padded)
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Adjust the width to ensure a correct ratio
        int height = getMeasuredHeight();
        int width = (int) (height / MARKER_RATIO);
        setMeasuredDimension(width + getPaddingLeft() + getPaddingRight(),
                height + getPaddingTop() + getPaddingBottom());

        // Reset artwork
        mMarkerPath = new Path(MARKER);
        mLockPath = new Path(LOCK);
    }
}
