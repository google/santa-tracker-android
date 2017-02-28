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

public class ShaderSource {

    private static final String COMMON_DECLS =
            "precision mediump float;       \n" +
                    "uniform mat4 u_Matrix;         \n" +
                    "uniform vec4 u_Color;         \n" +
                    "uniform float u_TintFactor;    \n" +
                    "uniform sampler2D u_Sampler;   \n " +
                    "varying vec4 v_Color;          \n" +
                    "varying vec2 v_TexCoord;      \n";

    public static final String VERTEX_SHADER = COMMON_DECLS +
            "attribute vec4 a_Position;     \n" +
            "attribute vec2 a_TexCoord;     \n" +
            "void main()                    \n" +
            "{                              \n" +
            "   v_Color = u_Color;          \n" +
            "   v_TexCoord = a_TexCoord;    \n" +
            "   gl_Position = u_Matrix * a_Position; \n" +
            "}                              \n";

    public static final String FRAG_SHADER =
            COMMON_DECLS +
                    "void main()                    \n" +
                    "{                              \n" +
                    "   vec4 c = mix(texture2D(u_Sampler, v_TexCoord), u_Color, u_TintFactor);\n" +
                    "   gl_FragColor = c;\n" +
                    "}\n";
}
