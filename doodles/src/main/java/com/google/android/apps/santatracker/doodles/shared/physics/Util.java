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
package com.google.android.apps.santatracker.doodles.shared.physics;

import com.google.android.apps.santatracker.doodles.shared.Vector2D;

import java.util.List;

/**
 * A utility class for physics-related functions.
 */
public final class Util {
  private static final String TAG = Util.class.getSimpleName();
  private static final float EPSILON = 0.0001f;
  private static final int COLLINEAR = 0;
  private static final int CLOCKWISE = 1;
  private static final int COUNTERCLOCKWISE = 2;

  private Util() {
    // Don't allow instantiation of this class.
  }

  /**
   * Return whether the point is within the axis-aligned rectangle defined by p and q.
   *
   * @param p first bounding point.
   * @param q second bounding point.
   * @param point the point to check.
   * @return true if point is within the bounds defined by p and q, false otherwise.
   */
  public static boolean pointIsWithinBounds(Vector2D p, Vector2D q, Vector2D point) {
    return point.x >= Math.min(p.x, q.x) && point.x <= Math.max(p.x, q.x)
        && point.y >= Math.min(p.y, q.y) && point.y <= Math.max(p.y, q.y);
  }

  /**
   * Find the orientation of the ordered triplet of points. They are either clockwise,
   * counterclockwise, or collinear.
   * Implementation based on: http://goo.gl/a44iML
   */
  private static int orientation(Vector2D p, Vector2D q, Vector2D r) {
    float value = (q.y - p.y) * (r.x - q.x) - (r.y - q.y) * (q.x - p.x);

    // Use this instead of Math.abs(value) here because it is faster.
    if (value < EPSILON && value > -EPSILON) {
      return COLLINEAR;
    }
    return value > 0 ? CLOCKWISE : COUNTERCLOCKWISE;
  }

  /**
   * Compute whether or not lines 1 and 2 intersect.
   * Implementation based on:
   * http://www.geeksforgeeks.org/check-if-two-given-line-segments-intersect/
   *
   * @param p1 The starting point of line 1
   * @param q1 The ending point of line 1
   * @param p2 The starting point of line 2
   * @param q2 The ending point of line 2
   * @return true if 1 and 2 intersect, false otherwise.
   */
  public static boolean lineSegmentIntersectsLineSegment(
      Vector2D p1, Vector2D q1, Vector2D p2, Vector2D q2) {
    int o1 = orientation(p1, q1, p2);
    int o2 = orientation(p1, q1, q2);
    int o3 = orientation(p2, q2, p1);
    int o4 = orientation(p2, q2, q1);

    // General case
    if (o1 != o2 && o3 != o4) {
      return true;
    }

    // Special cases
    if (o1 == 0 && pointIsWithinBounds(p1, q1, p2)
        || o2 == 0 && pointIsWithinBounds(p1, q1, q2)
        || o3 == 0 && pointIsWithinBounds(p2, q2, p1)
        || o4 == 0 && pointIsWithinBounds(p2, q2, q1)) {
      return true;
    }
    return false;
  }

  /**
   * Return whether or not two rectangles intersect. This uses a basic form of the separating axis
   * theorem which should be faster than running a full polygon-to-polygon check.
   *
   * @return true if the rectangles intersect, false otherwise.
   */
  public static boolean rectIntersectsRect(float x1, float y1, float w1, float h1,
                                           float x2, float y2, float w2, float h2) {
    float halfWidth1 = w1 / 2;
    float halfWidth2 = w2 / 2;
    float halfHeight1 = h1 / 2;
    float halfHeight2 = h2 / 2;

    float horizontalThreshold = halfWidth1 + halfWidth2;
    float verticalThreshold = halfHeight1 + halfHeight2;

    float horizontalDistance = Math.abs(x1 + halfWidth1 - (x2 + halfWidth2));
    float verticalDistance = Math.abs(y1 + halfHeight1 - (y2 + halfHeight2));

    return horizontalDistance < horizontalThreshold && verticalDistance < verticalThreshold;
  }

  /**
   * Use the separating axis theorem to determine whether or not two convex polygons intersect.
   *
   * @return true if the polygons intersect, false otherwise.
   */
  public static boolean convexPolygonIntersectsConvexPolygon(Polygon p1, Polygon p2) {
    for (int i = 0; i < p1.normals.size(); i++) {
      Vector2D normal = p1.normals.get(i);
      float p1Min = getMinProjectionInDirection(normal, p1.vertices);
      float p1Max = getMaxProjectionInDirection(normal, p1.vertices);
      float p2Min = getMinProjectionInDirection(normal, p2.vertices);
      float p2Max = getMaxProjectionInDirection(normal, p2.vertices);
      if (p1Max < p2Min || p2Max < p1Min) {
        // If there is a separating axis, these polygons do not intersect.
        return false;
      }
    }
    for (int i = 0; i < p2.normals.size(); i++) {
      Vector2D normal = p2.normals.get(i);
      float p1Min = getMinProjectionInDirection(normal, p1.vertices);
      float p1Max = getMaxProjectionInDirection(normal, p1.vertices);
      float p2Min = getMinProjectionInDirection(normal, p2.vertices);
      float p2Max = getMaxProjectionInDirection(normal, p2.vertices);
      if (p1Max < p2Min || p2Max < p1Min) {
        // If there is a separating axis, these polygons do not intersect.
        return false;
      }
    }
    return true;
  }

  private static float getMaxProjectionInDirection(Vector2D direction, List<Vector2D> points) {
    float max = points.get(0).dot(direction);
    for (int i = 1; i < points.size(); i++) {
      max = Math.max(max, points.get(i).dot(direction));
    }
    return max;
  }

  private static float getMinProjectionInDirection(Vector2D direction, List<Vector2D> points) {
    float min = points.get(0).dot(direction);
    for (int i = 1; i < points.size(); i++) {
      min = Math.min(min, points.get(i).dot(direction));
    }
    return min;
  }

  public static Vector2D getMidpoint(Vector2D p, Vector2D q) {
    float deltaX = q.x - p.x;
    float deltaY = q.y - p.y;
    return Vector2D.get(p.x + deltaX / 2, p.y + deltaY / 2);
  }

  public static float clamp(float value, float lowerBound, float upperBound) {
    return Math.max(lowerBound, Math.min(value, upperBound));
  }

  public static int clamp(int value, int lowerBound, int upperBound) {
    return Math.max(lowerBound, Math.min(value, upperBound));
  }
}
