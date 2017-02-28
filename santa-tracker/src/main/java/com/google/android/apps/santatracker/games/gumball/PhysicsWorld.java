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

package com.google.android.apps.santatracker.games.gumball;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.EdgeShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the game world and physics simulation for the gumball game.
 */
public class PhysicsWorld {

    /**
     * All {@link org.jbox2d.dynamics.Body} objects in the world.
     */
    private List<Body> mBodies = new ArrayList<Body>();
    /**
     * The Physics world.
     */
    private World mWorld;
    /**
     * Bodies that are to be removed from the scene.
     */
    public List<Body> mBodiesToBeRemoved = new ArrayList<Body>();
    /**
     * Render refresh rate.
     */
    private static final float FRAME_RATE = 1.0f / 45.0f;
    /**
     * Create the physics world and draws the boundries
     */
    public void create(Vec2 gravity) {

        // Create Physics World with Gravity
        mWorld = new World(gravity);
        mWorld.setAllowSleep(false);
        mWorld.setSleepingAllowed(false);
        mWorld.setAutoClearForces(true);

        BodyDef groundBodyDef = new BodyDef();

        // Create Ground Box
        groundBodyDef.position.set(new Vec2(5.0f, -2.0f));
        Body groundBody = mWorld.createBody(groundBodyDef);
        PolygonShape polygonShape = new PolygonShape();

        // Create top bound
        groundBodyDef.position.set(new Vec2(5.0f, 32.0f));
        groundBody = mWorld.createBody(groundBodyDef);
        groundBody.createFixture(polygonShape, 1.0f);

        polygonShape.setAsBox(2.0f, 18.0f);

        // Create left wall
        groundBodyDef.position.set(new Vec2(-2.0f, 16.0f));
        groundBody = mWorld.createBody(groundBodyDef);
        groundBody.createFixture(polygonShape, 1.0f);

        // Create right wall
        groundBodyDef.position.set(new Vec2(12.0f, 16.0f));
        groundBody = mWorld.createBody(groundBodyDef);
        groundBody.createFixture(polygonShape, 1.0f);

    }

    /**
     * Adds a gumball to the scene.
     */
    public void addGumball(float x, float y, Gumball gumball, float density, float radius,
            float bounce, float friction, BodyType bodyType) {
        // Create Shape with Properties
        CircleShape circleShape = new CircleShape();
        circleShape.m_radius = radius;
        addItem(x, y, circleShape, bounce, gumball, density, friction, bodyType);
    }

    public void addPipeSides(float x, float y, int data, float density, float bounce,
            float friction, BodyType bodyType) {
        EdgeShape[] edgeShapes = new EdgeShape[2];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(.23f, -1f), new Vec2(.01f, .48f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(1.4f, -1f), new Vec2(1.55f, .45f));
        addItem(x, y, edgeShapes, bounce, data, density, friction, bodyType);
    }


    public void addPipeBottom(float x, float y, int data, float density, float bounce,
            float friction, BodyType bodyType) {
        EdgeShape[] edgeShapes = new EdgeShape[1];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(.83f, 0f), new Vec2(2.40f, 0f));
        addItem(x, y, edgeShapes, bounce, data, density, friction, bodyType);
    }

    public void addFloor(float x, float y, int data, float density, float bounce, float friction,
            BodyType bodyType) {
        EdgeShape[] edgeShapes = new EdgeShape[1];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(-9f, -.8f), new Vec2(9f, -.8f));
        addItem(x, y, edgeShapes, bounce, data, density, friction, bodyType);
    }

    public void addItem(float x, float y, Shape[] shapes, float bounce, int data, float density,
            float friction, BodyType bodyType) {

        // Create Dynamic Body
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(x, y);
        bodyDef.userData = data;
        bodyDef.type = bodyType;
        Body body = mWorld.createBody(bodyDef);
        mBodies.add(body);

        for (int i = 0; i < shapes.length; i++) {
            // Assign shape to Body
            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shapes[i];
            fixtureDef.density = density;
            fixtureDef.friction = friction;
            fixtureDef.restitution = bounce;

            body.createFixture(fixtureDef);
        }
    }

    public void addItem(float x, float y, Shape shape, float bounce, int data, float density,
            float friction, BodyType bodyType) {

        // Create Dynamic Body
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(x, y);
        bodyDef.userData = data;
        bodyDef.type = bodyType;
        Body body = mWorld.createBody(bodyDef);
        mBodies.add(body);

        // Assign shape to Body
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = density;
        fixtureDef.friction = friction;
        fixtureDef.restitution = bounce;
        body.createFixture(fixtureDef);
    }

    public void addItem(float x, float y, Shape shape, float bounce, Gumball gumball,
            float density, float friction, BodyType bodyType) {

        // Create Dynamic Body
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(x, y);
        bodyDef.userData = gumball;
        bodyDef.type = bodyType;
        Body body = mWorld.createBody(bodyDef);
        mBodies.add(body);

        // Assign shape to Body
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = density;
        fixtureDef.friction = friction;
        fixtureDef.restitution = bounce;
        body.createFixture(fixtureDef);
    }

    /**
     * Updates the physics world by removing all pending bodies.
     */
    public void update() {
        // Update Physics World
        for (int i = 0; i < mBodiesToBeRemoved.size(); i++) {
            mWorld.destroyBody(mBodiesToBeRemoved.get(i));
        }
        mBodiesToBeRemoved.clear();
        mWorld.step(FRAME_RATE, 10, 10);
        mWorld.clearForces();
    }

    /**
     * Gets a reference to the world.
     */
    public World getWorld() {
        return mWorld;
    }

}
