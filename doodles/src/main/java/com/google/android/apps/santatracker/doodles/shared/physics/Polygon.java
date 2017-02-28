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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.android.apps.santatracker.doodles.shared.Vector2D;
import com.google.android.apps.santatracker.doodles.tilt.Constants;
import com.google.android.apps.santatracker.doodles.tilt.SwimmingFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A general polygon class (either concave or convex) which can tell whether or not a point
 * is inside of it.
 *
 * <p>NOTE: vertex winding order affects the normals of the line segments, and can affect things
 * like collisions. A non-inverted (normals pointed out) polygon should have its vertices wound
 * clockwise.</p>
 *
 */
public class Polygon {
  private static final String TAG = Polygon.class.getSimpleName();
  private static final float EPSILON = 0.0001f;
  private static final float VERTEX_RADIUS = 10;

  private Paint vertexPaint;
  private Paint midpointPaint;
  private Paint linePaint;

  public List<Vector2D> vertices;
  public List<Vector2D> normals;
  public Vector2D min;
  public Vector2D max;

  private boolean isInverted;

  public Polygon(List<Vector2D> vertices) {
    this.vertices = vertices;
    min = Vector2D.get(0, 0);
    max = Vector2D.get(0, 0);

    vertexPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    vertexPaint.setColor(Color.RED);

    midpointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    midpointPaint.setColor(Color.GREEN);
    midpointPaint.setAlpha(100);

    linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    linePaint.setColor(Color.WHITE);
    linePaint.setStrokeWidth(5);

    updateExtents();
    updateInversionStatus();
    calculateNormals();
  }

  public void updateExtents() {
    min.set(this.vertices.get(0));
    max.set(this.vertices.get(0));
    for (int i = 0; i < vertices.size(); i++) {
      Vector2D point = vertices.get(i);
      min.x = Math.min(min.x, point.x);
      min.y = Math.min(min.y, point.y);
      max.x = Math.max(max.x, point.x);
      max.y = Math.max(max.y, point.y);
    }
  }

  public void calculateNormals() {
    normals = new ArrayList<>();
    for (int i = 0; i < vertices.size(); i++) {
      Vector2D start = vertices.get(i);
      Vector2D end = vertices.get((i + 1) % vertices.size());
      normals.add(Vector2D.get(end).subtract(start).toNormal());
    }
  }

  public float getWidth() {
    return max.x - min.x;
  }

  public float getHeight() {
    return max.y - min.y;
  }

  public void moveTo(float x, float y) {
    float deltaX = x - min.x;
    float deltaY = y - min.y;
    move(deltaX, deltaY);
  }

  public void move(float x, float y) {
    for (int i = 0; i < vertices.size(); i++) {
      Vector2D vertex = vertices.get(i);
      vertex.x += x;
      vertex.y += y;
    }
    // Rather than update the extents by checking all of the vertices here, we can just update them
    // manually (they will move by the same amount as the rest of the vertices).
    min.x += x;
    min.y += y;
    max.x += x;
    max.y += y;
  }

  public void moveVertex(int index, Vector2D delta) {
    Vector2D vertex = vertices.get(index);
    vertex.x += delta.x;
    vertex.y += delta.y;
    updateExtents();
    updateInversionStatus();
  }

  public void addVertexAfter(int index) {
    int nextIndex = index < vertices.size() - 1 ? index + 1 : 0;
    Vector2D newVertex = Util.getMidpoint(vertices.get(index), vertices.get(nextIndex));
    vertices.add(nextIndex, newVertex);
    updateExtents();
    calculateNormals();
  }

  public void removeVertexAt(int index) {
    vertices.remove(index);
    updateExtents();
  }

  /**
   * Return the index of the vertex selected by the given point.
   *
   * @param point the point at which to check for a selected vertex.
   * @param scale the scale of the world, for slackening the selection radius if needed.
   * @return the index of the selected vertex, or -1 if no vertex was selected.
   */
  public int getSelectedIndex(Vector2D point, float scale) {
    for (int i = 0; i < vertices.size(); i++) {
      Vector2D vertex = vertices.get(i);
      if (point.distanceTo(vertex)
          < Math.max(Constants.SELECTION_RADIUS, Constants.SELECTION_RADIUS / scale)) {
        return i;
      }
    }
    return -1;
  }

  public int getMidpointIndex(Vector2D point, float scale) {
    for (int i = 0; i < vertices.size(); i++) {
      Vector2D start = vertices.get(i);
      Vector2D end = vertices.get(i < vertices.size() - 1 ? i + 1 : 0);
      if (point.distanceTo(Util.getMidpoint(start, end))
          < Math.max(Constants.SELECTION_RADIUS, Constants.SELECTION_RADIUS / scale)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Return whether or not this polygon is inverted (i.e., whether or not the polygon has a normal
   * which points inwards.
   *
   * @return true if the polygon is inverted, false otherwise.
   */
  public boolean isInverted() {
    return isInverted;
  }

  /**
   * Calculate whether or not this polygon is inverted. This checks to see if the point which is one
   * unit in the normal direction on the polygon's first segment is within the bounds of the
   * polygon. If this is the case, then the normal points inwards and the polygon is inverted.
   * Otherwise, the polygon is not inverted.
   *
   * <p>Note: This doesn't deal with polygons which are partially inverted. These sorts of polygons
   * should be avoided, as they will break this function.</p>
   */
  private void updateInversionStatus() {
    Vector2D start = vertices.get(0);
    Vector2D end = vertices.get(1);
    Vector2D midpoint = Util.getMidpoint(start, end);
    Vector2D normal = Vector2D.get(end).subtract(start).toNormal().scale(0.1f);

    if (contains(midpoint.add(normal))) {
      isInverted = true;
    } else {
      isInverted = false;
    }
    normal.release();
    midpoint.release();
  }

  /**
   * Return whether or not this polygon's collision boundaries contain a given point. A polygon
   * contains a point iff the point is contained within the polygon's collision boundaries,
   * regardless of the direction of the polygon's normals.
   *
   * @param point the point to check
   * @return true if this polygon contains the point, false otherwise.
   */
  public boolean contains(Vector2D point) {
    // If the bounding box doesn't contain the point, we don't need to do any more calculations.
    if (!Util.pointIsWithinBounds(min, max, point)) {
      return false;
    }

    // Cast vertical ray from point to outside polygon and counting crossings. Point is in polygon
    // iff number of edges crossed is odd.

    // Find a Y value that's definitely outside the polygon.
    float maxY = max.y + 1;
    Vector2D outsidePoint = Vector2D.get(point.x, maxY);

    // Check how many edges lie between (p.x, p.y) and (p.x, maxY).
    boolean inside = false;
    for (int i = 0; i < vertices.size(); i++) {
      Vector2D p1 = vertices.get(i);
      Vector2D p2;
      if (i < vertices.size() - 1) {
        p2 = vertices.get(i + 1);
      } else {
        p2 = vertices.get(0);
      }

      // First check endpoints. Hitting left-most point counts, hitting right-most
      // doesn't (to weed out case where ray hits 2 lines at their joining vertex)    }
      if (p1.y >= point.y && Math.abs(p1.x - point.x) <= EPSILON) {
        if (p2.x >= point.x) {
          inside = !inside;
        }
        continue;
      } else if (p2.y >= point.y && Math.abs(p2.x - point.x) <= EPSILON) {
        if (p1.x >= point.x) {
          inside = !inside;
        }
        continue;
      }

      // Now check for intersection.
      if (Util.lineSegmentIntersectsLineSegment(p1, p2, point, outsidePoint)) {
        inside = !inside;
      }
    }
    outsidePoint.release();
    return inside;
  }

  public LineSegment getIntersectingLineSegment(Vector2D p, Vector2D q) {
    for (int i = 0; i < vertices.size(); i++) {
      Vector2D p1 = vertices.get(i);
      Vector2D p2;
      if (i < vertices.size() - 1) {
        p2 = vertices.get(i + 1);
      } else {
        p2 = vertices.get(0);
      }

      if (Util.lineSegmentIntersectsLineSegment(p1, p2, p, q)) {
        return new LineSegment(p1, p2);
      }
    }
    return null;
  }

  public void draw(Canvas canvas) {
    if (!(SwimmingFragment.editorMode)) {
      return;
    }
    for (int i = 0; i < vertices.size(); i++) {
      Vector2D start = vertices.get(i);
      Vector2D end;
      if (i < vertices.size() - 1) {
        end = vertices.get(i + 1);
      } else {
        end = vertices.get(0);
      }
      Vector2D midpoint = Util.getMidpoint(start, end);
      Vector2D normal = Vector2D.get(end).subtract(start).toNormal();

      canvas.drawCircle(start.x, start.y, VERTEX_RADIUS, vertexPaint);
      canvas.drawLine(start.x, start.y, end.x, end.y, linePaint);
      canvas.drawCircle(midpoint.x, midpoint.y, VERTEX_RADIUS / 2, midpointPaint);
      canvas.drawLine(midpoint.x, midpoint.y,
          midpoint.x + normal.x * 20, midpoint.y + normal.y * 20, linePaint);

      midpoint.release();
      normal.release();
    }
  }

  public void setPaintColors(int vertexColor, int lineColor, int midpointColor) {
    vertexPaint.setColor(vertexColor);
    linePaint.setColor(lineColor);
    midpointPaint.setColor(midpointColor);
  }

  public JSONArray toJSON() throws JSONException {
    JSONArray json = new JSONArray();
    for (int i = 0; i < vertices.size(); i++) {
      JSONObject vertexJson = new JSONObject();
      Vector2D vertex = vertices.get(i);
      vertexJson.put("x", (double) vertex.x);
      vertexJson.put("y", (double) vertex.y);
      json.put(vertexJson);
    }
    return json;
  }

  public static Polygon fromJSON(JSONArray json) throws JSONException {
    List<Vector2D> vertices = new ArrayList<>();
    for (int i = 0; i < json.length(); i++) {
      JSONObject vertexJson = json.getJSONObject(i);
      Vector2D vertex =
          Vector2D.get((float) vertexJson.getDouble("x"), (float) vertexJson.getDouble("y"));
      vertices.add(vertex);
    }
    return new Polygon(vertices);
  }

  /**
   * A class to specify the starting and ending point of a line segment. Currently only used in
   * determining which line segment is being collided with, so we can determine the normal vector.
   */
  public static class LineSegment {
    public Vector2D start;
    public Vector2D end;

    public LineSegment(Vector2D start, Vector2D end) {
      this.start = start;
      this.end = end;
    }

    public Vector2D getDirection() {
      return Vector2D.get(end).subtract(start);
    }
  }
}
