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
package com.google.android.apps.santatracker.doodles.tilt;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.google.android.apps.santatracker.doodles.Config;
import com.google.android.apps.santatracker.doodles.R;
import com.google.android.apps.santatracker.doodles.shared.Actor;
import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.shared.physics.Polygon;
import com.google.android.apps.santatracker.doodles.shared.physics.Util;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * One chunk of a level in the swimming game.
 */
public class SwimmingLevelChunk extends Actor {
  private static final String TAG = SwimmingLevelChunk.class.getSimpleName();

  public static final int LEVEL_LENGTH_IN_METERS = 500;
  public static Queue<SwimmingLevelChunk> swimmingLevelChunks;

  public static List<SolutionPath> pathList;
  public static final int CHUNK_HEIGHT = 5000;
  public static final int NUM_ROWS = 100;
  public static final int NUM_COLS = 50;
  public static final float COL_WIDTH = SwimmingModel.LEVEL_WIDTH / (float) NUM_COLS;
  public static final float ROW_HEIGHT = CHUNK_HEIGHT / (float) NUM_ROWS;
  private static final int SOLUTION_PATH_NUM_COLS = 50;
  private static final Random RANDOM = new Random();

  private static final List<String> TYPES;
  static {
    TYPES = new ArrayList<>();
    TYPES.add(BoundingBoxSpriteActor.ICE_CUBE);
    TYPES.add(BoundingBoxSpriteActor.DUCK);
    TYPES.add(BoundingBoxSpriteActor.HAND_GRAB);
  }

  private final float DEFAULT_OBSTACLE_DENSITY;

  public final float startY;
  public final float endY;
  public List<BoundingBoxSpriteActor> obstacles;
  private SolutionPath solutionPath;
  private boolean mirrored;

  public static SwimmingLevelChunk create(float startY, Context context) {
    if (pathList == null || pathList.size() == 0) {
      loadChunkTemplates(context.getResources());
    }
    // Increase the probability that the random chunk will be a "middle open" chunk.
    int pathIndex = Math.min(pathList.size() - 1, RANDOM.nextInt(pathList.size() + 1));
    SolutionPath solutionPath = pathList.get(pathIndex);
    return new SwimmingLevelChunk(startY, solutionPath, RANDOM.nextBoolean(), context);
  }

  public static void generateAllLevelChunks(float startY, Context context) {
    long startTime = System.currentTimeMillis();
    swimmingLevelChunks = new LinkedList<>();
    SwimmingLevelChunk chunk = create(startY, context);
    while (SwimmingModel.getMetersFromWorldY(chunk.endY) < LEVEL_LENGTH_IN_METERS) {
      swimmingLevelChunks.add(chunk);
      chunk = create(chunk.endY, context);
    }
    Log.d(TAG, "generateAllLevelChunks: finished in "
        + ((System.currentTimeMillis() - startTime) / 1000.0f)
        + " seconds.");
  }

  public static SwimmingLevelChunk getNextChunk() {
    if (!swimmingLevelChunks.isEmpty()) {
      return swimmingLevelChunks.remove();
    }
    return null;
  }

  private SwimmingLevelChunk(float startY, SolutionPath solutionPath,
      boolean mirrored, Context context) {

    // Get swimming obstacle density from config
    Config config = new Config();
    DEFAULT_OBSTACLE_DENSITY = (float) config.SWIMMING_OBSTACLE_DENSITY;

    this.solutionPath = solutionPath;
    this.mirrored = mirrored;
    this.startY = startY;
    generateObstacles(context);
    removeObstaclesFromSolutionPath(startY);
    this.endY = startY - solutionPath.getChunkHeight();
  }

  @Override
  public void update(float deltaMs) {
    for (int i = 0; i < obstacles.size(); i++) {
      obstacles.get(i).update(deltaMs);
    }
  }

  @Override
  public void draw(Canvas canvas) {
    //solutionPath.draw(canvas, startY, mirrored);
    for (int i = 0; i < obstacles.size(); i++) {
      obstacles.get(i).draw(canvas);
    }
  }

  public void resolveCollisions(SwimmerActor swimmer, float deltaMs) {
    for (int i = 0; i < obstacles.size(); i++) {
      obstacles.get(i).resolveCollision(swimmer, deltaMs);
    }
  }

  public static void loadChunkTemplates(Resources res) {
    pathList = new ArrayList<>();

    Options decodeOptions = new Options();
    decodeOptions.inScaled = false;

    Bitmap b = BitmapFactory.decodeResource(res, R.raw.diamond, decodeOptions);
    pathList.add(new GridSolutionPath(b));

    b = BitmapFactory.decodeResource(res, R.raw.zig, decodeOptions);
    pathList.add(new GridSolutionPath(b));

    b = BitmapFactory.decodeResource(res, R.raw.ziggeroo, decodeOptions);
    pathList.add(new GridSolutionPath(b));

    b = BitmapFactory.decodeResource(res, R.raw.fork_in, decodeOptions);
    pathList.add(new GridSolutionPath(b));

    b = BitmapFactory.decodeResource(res, R.raw.fork_out, decodeOptions);
    pathList.add(new GridSolutionPath(b));

    b = BitmapFactory.decodeResource(res, R.raw.middle_open, decodeOptions);
    pathList.add(new GridSolutionPath(b));
  }

  private void generateObstacles(Context context) {
    obstacles = new ArrayList<>();
    for (int i = 0; i < solutionPath.getNumRows() * DEFAULT_OBSTACLE_DENSITY; i++) {
      float x = RANDOM.nextInt((4 * SwimmingModel.LEVEL_WIDTH) / 5);
      float y = startY - (SwimmingModel.LEVEL_WIDTH / 5)
          - RANDOM.nextInt((int) solutionPath.getChunkHeight() - (SwimmingModel.LEVEL_WIDTH / 5));

      int metersY = SwimmingModel.getMetersFromWorldY(y);
      int type;
      if (metersY < SwimmingModel.SCORE_THRESHOLDS[0]) {
        // Only show ice cubes before the bronze threshold.
        type = 0;
      } else if (metersY < SwimmingModel.SCORE_THRESHOLDS[1]) {
        // Show ice cubes and cans before the silver threshold.
        type = RANDOM.nextInt(TYPES.size() - 1);
      } else {
        // After the silver threshold, use all obstacles.
        boolean isInMiddleThreeLanes = SwimmingModel.LEVEL_WIDTH / 5 <= x
            && x <= 3 * SwimmingModel.LEVEL_WIDTH / 5;
        if (isInMiddleThreeLanes) {
          // Only place octograbs in the middle three lanes. If we are generating an obstacle in the
          // middle 3 lanes, give it a higher chance of being an octograb.
          type = Math.min(TYPES.size() - 1, RANDOM.nextInt(TYPES.size() + 1));
        } else {
          // If we are outside of the middle 3 lanes, give each other option equal weight.
          type = RANDOM.nextInt(TYPES.size() - 1);
        }
      }

      BoundingBoxSpriteActor obstacle = BoundingBoxSpriteActor.create(
          Vector2D.get(x, y), TYPES.get(type), context.getResources());
      Polygon obstacleBody = obstacle.collisionBody;
      boolean shouldAdd = true;
      for (int j = 0; j < obstacles.size(); j++) {
        Polygon otherBody = obstacles.get(j).collisionBody;
        if (Util.rectIntersectsRect(
            otherBody.min.x, otherBody.min.y,
            otherBody.getWidth(), otherBody.getHeight(),
            obstacleBody.min.x, obstacleBody.min.y,
            obstacleBody.getWidth(), obstacleBody.getHeight())) {
          shouldAdd = false;
        }
      }
      if (shouldAdd) {
        obstacles.add(obstacle);
      }
    }
  }

  private void removeObstaclesFromSolutionPath(float startY) {
    for (int i = obstacles.size() - 1; i >= 0; i--) {
      BoundingBoxSpriteActor obstacle = obstacles.get(i);
      Vector2D min = obstacle.collisionBody.min;
      Vector2D max = obstacle.collisionBody.max;
      if (solutionPath.intersects(startY, min.x, min.y, max.x, max.y, mirrored)) {
        obstacles.remove(i);
      }
    }
  }

  private interface SolutionPath {
    boolean intersects(float startY, float minX, float minY, float maxX, float maxY,
        boolean mirrored);
    void draw(Canvas canvas, float startY, boolean mirrored);
    int getEndCol(boolean mirrored);
    float getChunkHeight();
    int getNumRows();
  }

  private static class SolutionPathImpl implements SolutionPath {
    private static final int DRIFT_SAME = 0;
    private static final int DRIFT_REVERSE = 1;
    private final int[] driftDistribution = new int[] { 50, 75, 100 };
    private SolutionPathRow[] rows;
    private int endCol;
    private int drift;
    private Paint paint;

    public SolutionPathImpl(int startCol) {
      rows = new SolutionPathRow[NUM_ROWS];
      paint = new Paint();
      paint.setColor(Color.DKGRAY);

      for (int i = 0; i < rows.length; i++) {

        // Decide which way to drift.
        int driftToken = RANDOM.nextInt(100);
        if (driftToken < driftDistribution[DRIFT_SAME]) {
          if (drift == 0) {
            // If the path is going straight, switch it to a random direction.
            drift = RANDOM.nextBoolean() ? 1 : -1;
          }
        } else if (driftToken < driftDistribution[DRIFT_REVERSE]) {
          drift = 0;
        } else {
          drift *= -1;
        }
        if (startCol == 0) {
          drift = 1;
        } else if (startCol == NUM_COLS - SOLUTION_PATH_NUM_COLS - 1) {
          drift = -1;
        }
        startCol = Util.clamp(startCol + drift, 0, NUM_COLS - SOLUTION_PATH_NUM_COLS - 1);

        rows[i] = new SolutionPathRow(startCol, SOLUTION_PATH_NUM_COLS);
      }
      this.endCol = startCol;
    }

    @Override
    public boolean intersects(float startY, float minX, float minY, float maxX, float maxY,
        boolean mirrored) {
      // Subtract y from startY because the level proceeds in the negative y direction.
      int minRowIndex = (int) Math.max(0, (startY - maxY) / ROW_HEIGHT);
      int maxRowIndex = (int) Math.max(0, (startY - minY) / ROW_HEIGHT);
      for (int i = minRowIndex; i <= maxRowIndex; i++) {
        if (rows[i].intersects(Math.min(minX, SwimmingModel.LEVEL_WIDTH), mirrored)
            || rows[i].intersects(Math.min(maxX, SwimmingModel.LEVEL_WIDTH), mirrored)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public void draw(Canvas canvas, float startY, boolean mirrored) {
      float y = startY;
      for (int i = 0; i < rows.length; i++) {
        float startX = rows[i].startX;
        float endX = rows[i].endX;
        if (mirrored) {
          startX = SwimmingModel.LEVEL_WIDTH - rows[i].endX;
          endX = SwimmingModel.LEVEL_WIDTH - rows[i].startX;
        }
        canvas.drawRect(startX, y, endX, y + ROW_HEIGHT, paint);
        y -= ROW_HEIGHT;
      }
    }

    @Override
    public int getEndCol(boolean mirrored) {
      return mirrored ? NUM_COLS - 1 - endCol : endCol;
    }

    @Override
    public float getChunkHeight() {
      return CHUNK_HEIGHT;
    }

    @Override
    public int getNumRows() {
      return rows.length;
    }
  }

  private static class GridSolutionPath implements SolutionPath {

    public final float chunkHeight;
    public final int numRows;
    public final float rowHeight;

    private boolean[][] grid;
    private Paint paint;

    /**
     * Initialize this solution path with a bitmap. The length of the solution path will scale with
     * the height of the image, where 1px in the image = 1 grid unit in the chunk. The width of the
     * solution path is fixed to NUM_COLS and will just sample the bitmap at NUM_COLS points. Any
     * bitmap with a higher horizontal resolution than NUM_COLS will be down-sampled, and any bitmap
     * with a lower resolution will have single pixels being sampled more than once horizontally.
     *
     * In order to maintain visual consistency with the supplied bitmap, it is recommended that
     * the input PNGs are 50px wide.
     */
    public GridSolutionPath(Bitmap bitmap) {
      int bitmapWidth = bitmap.getWidth();
      int bitmapHeight = bitmap.getHeight();
      chunkHeight = bitmapHeight * COL_WIDTH;
      numRows = bitmapHeight;
      rowHeight = chunkHeight / numRows;

      Log.d(TAG, "bitmapHeight: " + bitmapHeight);
      Log.d(TAG, "chunkHeight: " + chunkHeight);
      Log.d(TAG, "numRows: " + numRows);
      Log.d(TAG, "rowHeight: " + rowHeight);

      paint = new Paint();
      paint.setColor(Color.DKGRAY);
      grid = new boolean[numRows][NUM_COLS];

      for (int i = 0; i < grid.length; i++) {
        for (int j = 0; j < grid[0].length; j++) {
          int bitmapX = (int) ((((float) j) / grid[0].length) * bitmapWidth);
          int bitmapY = (int) ((((float) i) / grid.length) * bitmapHeight);
          int pixel = bitmap.getPixel(bitmapX, bitmapY);
          grid[i][j] = (pixel & 0x00ffffff) != 0;
        }
      }
    }

    @Override
    public boolean intersects(float startY, float minX, float minY, float maxX, float maxY,
        boolean mirrored) {
      if (mirrored) {
        float tmpMinX = minX;
        minX = SwimmingModel.LEVEL_WIDTH - maxX;
        maxX = SwimmingModel.LEVEL_WIDTH - tmpMinX;
      }

      // Subtract y from startY because the level proceeds in the negative y direction.
      int minRowIndex = (int) Math.max(0, (startY - maxY) / rowHeight);
      int maxRowIndex = (int) Math.max(0, (startY - minY) / rowHeight);
      int minColIndex = (int) Math.min(NUM_COLS - 1, minX / COL_WIDTH);
      int maxColIndex = (int) Math.min(NUM_COLS - 1, maxX / COL_WIDTH);
      for (int i = minRowIndex; i <= maxRowIndex; i++) {
        for (int j = minColIndex; j <= maxColIndex; j++) {
          if (grid[i][j]) {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public void draw(Canvas canvas, float startY, boolean mirrored) {
      float y = startY - rowHeight;
      for (int i = 0; i < grid.length; i++) {
        float x = 0;
        for (int j = 0; j < grid[0].length; j++) {
          if (!mirrored && grid[i][j]) {
            canvas.drawRect(x, y, x + COL_WIDTH, y + rowHeight, paint);
          } else if (mirrored && grid[i][grid[0].length - 1 - j]) {
            canvas.drawRect(x, y, x + COL_WIDTH, y + rowHeight, paint);
          }
          x += COL_WIDTH;
        }
        y -= rowHeight;
      }
    }

    @Override
    public int getEndCol(boolean mirrored) {
      return mirrored ? NUM_COLS - 1 : 0;
    }

    @Override
    public float getChunkHeight() {
      return chunkHeight;
    }

    @Override
    public int getNumRows() {
      return numRows;
    }
  }

  private static class SolutionPathRow {
    // The first column which is in the solution path.
    public final int startCol;
    // The column after the last column in the solution path.
    public final int endCol;
    public final float startX;
    public final float endX;
    public SolutionPathRow(int startCol, int numCols) {
      this.startCol = startCol;
      this.endCol = startCol + numCols;
      this.startX = startCol * COL_WIDTH;
      this.endX = endCol * COL_WIDTH;
    }

    public boolean intersects(float x, boolean mirrored) {
      float startX = this.startX;
      float endX = this.endX;
      if (mirrored) {
        startX = SwimmingModel.LEVEL_WIDTH - this.endX;
        endX = SwimmingModel.LEVEL_WIDTH - this.startX;
      }
      return startX <= x && x <= endX;
    }
  }
}
