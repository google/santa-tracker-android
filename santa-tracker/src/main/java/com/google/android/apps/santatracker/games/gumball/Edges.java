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

import org.jbox2d.collision.shapes.EdgeShape;
import org.jbox2d.common.Vec2;

/**
 * Static methods to get the edge paths of the scene
 *
 */
public class Edges {

    public static EdgeShape[] getCaneEnd() {
        EdgeShape[] edgeShapes = new EdgeShape[3];
        //rounded part
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0.22f, .858f), new Vec2(0.22f, 1.02f));
        //bottom
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0.04f, .84f), new Vec2(.2f, .84f));
        //top
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(0.03f, 1.04f), new Vec2(.2f, 1.04f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneEndFlip() {
        EdgeShape[] edgeShapes = new EdgeShape[3];
        //rounded part
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0.04f, .858f), new Vec2(0.04f, 1.02f));
        //bottom
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0.06f, .845f), new Vec2(.263f, .845f));
        //top
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(0.06f, 1.04f), new Vec2(.263f, 1.04f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneEndReverse() {
        EdgeShape[] edgeShapes = new EdgeShape[3];
        //rounded part
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0.22f, .128f), new Vec2(0.22f, .29f));
        //bottom
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0.04f, .11f), new Vec2(.2f, .11f));
        //top
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(0.03f, .31f), new Vec2(.2f, .31f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneEndReverseFlip() {
        EdgeShape[] edgeShapes = new EdgeShape[3];
        //rounded part
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0.04f, .128f), new Vec2(0.04f, .29f));
        //connector
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0.06f, .31f), new Vec2(0.04f, .29f));
        //top
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(0.06f, .31f), new Vec2(.263f, .31f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainLongShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[2];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0f, .845f), new Vec2(5.53f, .845f));
        // edgeShapes[1] = new EdgeShape();
        // edgeShapes[1].set(new Vec2(5.53f, .005f), new Vec2(5.53f, .205f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0f, 1.045f), new Vec2(5.53f, 1.045f));
        // edgeShapes[3] = new EdgeShape();
        // edgeShapes[3].set(new Vec2(0f, .005f), new Vec2(0f, .205f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainReverseLongShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[2];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0f, .115f), new Vec2(5.53f, .115f));
        // edgeShapes[1] = new EdgeShape();
        // edgeShapes[1].set(new Vec2(5.53f, .735f), new Vec2(5.53f, .935f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0f, .315f), new Vec2(5.53f, .315f));
        // edgeShapes[3] = new EdgeShape();
        // edgeShapes[3].set(new Vec2(0f, .735f), new Vec2(0f, .935f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainMedShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[2];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0f, .845f), new Vec2(4.13f, .845f));
        // edgeShapes[1] = new EdgeShape();
        // edgeShapes[1].set(new Vec2(5.53f, .005f), new Vec2(5.53f, .205f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0f, 1.045f), new Vec2(4.13f, 1.045f));
        // edgeShapes[3] = new EdgeShape();
        // edgeShapes[3].set(new Vec2(0f, .005f), new Vec2(0f, .205f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainReverseMedShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[2];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0f, .115f), new Vec2(4.13f, .115f));
        // edgeShapes[1] = new EdgeShape();
        // edgeShapes[1].set(new Vec2(5.53f, .735f), new Vec2(5.53f, .935f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0f, .315f), new Vec2(4.13f, .315f));
        // edgeShapes[3] = new EdgeShape();
        // edgeShapes[3].set(new Vec2(0f, .735f), new Vec2(0f, .935f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainSmallShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[2];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0f, .845f), new Vec2(2.73f, .845f));
        // edgeShapes[1] = new EdgeShape();
        // edgeShapes[1].set(new Vec2(5.53f, .005f), new Vec2(5.53f, .205f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0f, 1.045f), new Vec2(2.73f, 1.045f));
        // edgeShapes[3] = new EdgeShape();
        // edgeShapes[3].set(new Vec2(0f, .005f), new Vec2(0f, .205f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainReverseSmallShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[2];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0f, .115f), new Vec2(2.73f, .115f));
        // edgeShapes[1] = new EdgeShape();
        // edgeShapes[1].set(new Vec2(5.53f, .735f), new Vec2(5.53f, .935f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0f, .315f), new Vec2(2.73f, .315f));
        // edgeShapes[3] = new EdgeShape();
        // edgeShapes[3].set(new Vec2(0f, .735f), new Vec2(0f, .935f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainTinyShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[2];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0f, .845f), new Vec2(1.38f, .845f));
        // edgeShapes[1] = new EdgeShape();
        // edgeShapes[1].set(new Vec2(5.53f, .005f), new Vec2(5.53f, .205f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0f, 1.045f), new Vec2(1.38f, 1.045f));
        // edgeShapes[3] = new EdgeShape();
        // edgeShapes[3].set(new Vec2(0f, .005f), new Vec2(0f, .205f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainReverseTinyShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[2];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0f, .115f), new Vec2(1.38f, .115f));
        // edgeShapes[1] = new EdgeShape();
        // edgeShapes[1].set(new Vec2(5.53f, .735f), new Vec2(5.53f, .935f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0f, .315f), new Vec2(1.38f, .315f));
        // edgeShapes[3] = new EdgeShape();
        // edgeShapes[3].set(new Vec2(0f, .735f), new Vec2(0f, .935f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneHookShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[8];
        // inner top
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(.1f, .82f), new Vec2(.24f, .97f));
        // inner bottom
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(.53f, 1.04f), new Vec2(.77f, 1.04f));
        // inner edge
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(.24f, .185f), new Vec2(.24f, .97f));
        // end part
        edgeShapes[3] = new EdgeShape();
        edgeShapes[3].set(new Vec2(.6f, .138f), new Vec2(.6f, .285f));

        // back edge
        edgeShapes[4] = new EdgeShape();
        edgeShapes[4].set(new Vec2(.1f, .33f), new Vec2(.1f, .82f));
        edgeShapes[5] = new EdgeShape();
        edgeShapes[5].set(new Vec2(.40f, .83f), new Vec2(.40f, 1.03f));
        edgeShapes[6] = new EdgeShape();
        edgeShapes[6].set(new Vec2(.53f, 1.04f), new Vec2(.40f, 1.03f));
        edgeShapes[7] = new EdgeShape();
        edgeShapes[7].set(new Vec2(.24f, .97f), new Vec2(.40f, 1.03f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneHookFlipShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[8];
        // inner top
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(.30f, 1.04f), new Vec2(.40f, 1.03f));
        // inner bottom
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0f, 1.04f), new Vec2(.30f, 1.04f));
        // inner edge
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(.53f, .185f), new Vec2(.527f, .97f));
        // end part
        edgeShapes[3] = new EdgeShape();
        edgeShapes[3].set(new Vec2(.155f, .138f), new Vec2(.155f, .285f));

        // back edge
        edgeShapes[4] = new EdgeShape();
        edgeShapes[4].set(new Vec2(.68f, .33f), new Vec2(.68f, .82f));
        edgeShapes[5] = new EdgeShape();
        edgeShapes[5].set(new Vec2(.40f, .83f), new Vec2(.40f, 1.03f));

        edgeShapes[6] = new EdgeShape();
        edgeShapes[6].set(new Vec2(.40f, 1.03f), new Vec2(.527f, .97f));

        edgeShapes[7] = new EdgeShape();
        edgeShapes[7].set(new Vec2(.527f, .97f), new Vec2(.68f, .82f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneHookReverseShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[7];
        // inner top
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(.24f, .315f), new Vec2(.77f, .315f));

        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(.24f, .97f), new Vec2(.40f, 1.03f));
        // inner edge
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(.24f, .185f), new Vec2(.24f, .97f));
        // end part
        edgeShapes[3] = new EdgeShape();
        edgeShapes[3].set(new Vec2(.6f, .858f), new Vec2(.6f, 1.005f));
        // top part
        edgeShapes[4] = new EdgeShape();
        edgeShapes[4].set(new Vec2(.6f, 1.005f), new Vec2(.40f, 1.03f));
        // back edge
        edgeShapes[5] = new EdgeShape();
        edgeShapes[5].set(new Vec2(.1f, .33f), new Vec2(.1f, .82f));
        edgeShapes[6] = new EdgeShape();
        edgeShapes[6].set(new Vec2(.1f, .82f), new Vec2(.24f, .97f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneHookReverseFlipShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[6];
        // inner top
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0f, .315f), new Vec2(.53f, .315f));

        // inner edge
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(.53f, .185f), new Vec2(.53f, .97f));
        // end part
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(.155f, .858f), new Vec2(.155f, .99f));
        // top part
        edgeShapes[3] = new EdgeShape();
        edgeShapes[3].set(new Vec2(.155f, .99f), new Vec2(.3155f, 1.045f));
        // back edge
        edgeShapes[4] = new EdgeShape();
        edgeShapes[4].set(new Vec2(.68f, .33f), new Vec2(.68f, .82f));
        edgeShapes[5] = new EdgeShape();
        edgeShapes[5].set(new Vec2(.53f, .97f), new Vec2(.3155f, 1.045f));
        return edgeShapes;
    }

    public static EdgeShape[] getPipeSideEdges() {
        EdgeShape[] edgeShapes = new EdgeShape[2];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(.83f, -1f), new Vec2(.01f, .45f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(2.4f, -1f), new Vec2(3.2f, .45f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainSmallAngleNineShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[4];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0.01f, 0.935f), new Vec2(3.66f, 0.325f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0.25f, 0.675f), new Vec2(3.6f, 0.105f));
        //backstop
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(0.15f, 0.755f), new Vec2(0.28f, 1.55f));
        //end
        edgeShapes[3] = new EdgeShape();
        edgeShapes[3].set(new Vec2(3.66f, 0.128f), new Vec2(3.70f, 0.295f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainSmallAngleTwelveShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[4];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0.01f, 0.73f), new Vec2(2.04f, 0.305f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0.25f, 0.475f), new Vec2(2.0f, 0.1f));
        //backstop
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(0.18f, 0.725f), new Vec2(0.29f, 1.30f));
        //end
        edgeShapes[3] = new EdgeShape();
        edgeShapes[3].set(new Vec2(2.01f, 0.128f), new Vec2(2.05f, 0.293f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainTinyAngleSixShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[7];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0.10f, 0.33f), new Vec2(1.9f, 0.425f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0.15f, 0.105f), new Vec2(1.8f, 0.20f));
        //backstop
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(1.94f, 0.425f), new Vec2(1.91f, 1.07f));
        //end
        edgeShapes[3] = new EdgeShape();
        edgeShapes[3].set(new Vec2(0.07f, 0.128f), new Vec2(0.06f, 0.313f));

        edgeShapes[4] = new EdgeShape();
        edgeShapes[4].set(new Vec2(1.55f, .96f), new Vec2(1.55f, 1.09f));
        edgeShapes[5] = new EdgeShape();
        edgeShapes[5].set(new Vec2(1.55f, 1.09f), new Vec2(1.66f, 1.13f));
        edgeShapes[6] = new EdgeShape();
        edgeShapes[6].set(new Vec2(1.91f, 1.07f), new Vec2(1.66f, 1.13f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainSmallAngleSixShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[3];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0.05f, 0.515f), new Vec2(2.69f, 0.329f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0.30f, 0.285f), new Vec2(2.66f, 0.119f));
        //backstop
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(0.15f, 0.455f), new Vec2(0.27f, 1.15f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainMedAngleSixShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[3];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0.06f, 0.53f), new Vec2(2.97f, 1.05f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0.1f, 0.329f), new Vec2(3.26f, 0.90f));
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(2.97f, 1.05f), new Vec2(3.26f, 0.90f));
        return edgeShapes;
    }

    public static EdgeShape[] getCaneMainLargeAngleSixShapes() {
        EdgeShape[] edgeShapes = new EdgeShape[3];
        edgeShapes[0] = new EdgeShape();
        edgeShapes[0].set(new Vec2(0.06f, 0.24f), new Vec2(4.88f, 1.08f));
        edgeShapes[1] = new EdgeShape();
        edgeShapes[1].set(new Vec2(0.12f, 0.009f), new Vec2(5.19f, 0.95f));
        edgeShapes[2] = new EdgeShape();
        edgeShapes[2].set(new Vec2(4.88f, 1.08f), new Vec2(5.22f, 0.95f));
        return edgeShapes;
    }

    public static EdgeShape[] getEdges(int edgeType) {
        switch (edgeType) {
            case TiltGameView.CANE_MAIN_TINY:
                return getCaneMainTinyShapes();
            case TiltGameView.CANE_MAIN_MEDIUM:
                return getCaneMainMedShapes();
            case TiltGameView.CANE_MAIN_LONG:
                return getCaneMainLongShapes();
            case TiltGameView.CANE_MAIN_SMALL:
                return getCaneMainSmallShapes();
            case TiltGameView.CANE_MAIN_TINY_REVERSE:
                return getCaneMainReverseTinyShapes();
            case TiltGameView.CANE_MAIN_MEDIUM_REVERSE:
                return getCaneMainReverseMedShapes();
            case TiltGameView.CANE_MAIN_LONG_REVERSE:
                return getCaneMainReverseLongShapes();
            case TiltGameView.CANE_MAIN_SMALL_REVERSE:
                return getCaneMainReverseSmallShapes();
            case TiltGameView.CANE_HOOK:
                return getCaneHookShapes();
            case TiltGameView.CANE_HOOK_REVERSE:
                return getCaneHookReverseShapes();
            case TiltGameView.CANE_HOOK_FLIP:
                return getCaneHookFlipShapes();
            case TiltGameView.CANE_HOOK_REVERSE_FLIP:
                return getCaneHookReverseFlipShapes();
            case TiltGameView.CANE_END:
                return getCaneEnd();
            case TiltGameView.CANE_END_REVERSE:
                return getCaneEndReverse();
            case TiltGameView.CANE_END_FLIP:
                return getCaneEndFlip();
            case TiltGameView.CANE_END_REVERSE_FLIP:
                return getCaneEndReverseFlip();

            case TiltGameView.CANE_MAIN_SMALL_ANGLE_NINE:
                return getCaneMainSmallAngleNineShapes();
            case TiltGameView.CANE_MAIN_SMALL_ANGLE_SIX:
                return getCaneMainSmallAngleSixShapes();
            case TiltGameView.CANE_MAIN_SMALL_ANGLE_TWELVE:
                return getCaneMainSmallAngleTwelveShapes();
            case TiltGameView.CANE_MAIN_REVERSE_TINY_ANGLE_SIX:
                return getCaneMainTinyAngleSixShapes();

            case TiltGameView.CANE_MAIN_LARGE_ANGLE_SIX:
                return getCaneMainLargeAngleSixShapes();
            case TiltGameView.CANE_MAIN_MED_ANGLE_SIX:
                return getCaneMainMedAngleSixShapes();
        }
        return null;
    }
}
