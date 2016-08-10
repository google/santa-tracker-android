// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

attribute vec4 aPosition;
attribute vec2 aTexCoord;
attribute vec3 aNormal;
attribute vec4 aTangent;
varying vec2 vTexCoord;
varying vec3 vNormal;
varying vec2 vNormalmapCoord;
varying vec3 vTangent;
varying vec3 vObjectSpacePosition;
varying mat3 vBasis;
varying vec3 vTangentSpaceLightVector;
varying vec3 vTangentSpaceCameraVector;
uniform mat4 model_view;
uniform mat4 model_view_projection;
uniform vec3 light_pos;    //in object space
uniform vec3 camera_pos;   //in object space
uniform float normalmap_scale;

void main()
{
    gl_Position = model_view_projection * aPosition;
    vTexCoord = aTexCoord;

    // Warning, Fragile: This ONLY works because our model data is passed in
    // aligned with the XY plane.
    vNormalmapCoord = aPosition.xy * normalmap_scale;

    vNormal = aNormal;
    vTangent = vec3(aTangent.xyz);
    vObjectSpacePosition = aPosition.xyz;

    vec3 n = normalize(vNormal);
    vec3 t = normalize(vTangent);
    vec3 b = normalize(cross(n, t)) * aTangent.w;;

    mat3 world_to_tangent_matrix = mat3(t, b, n);

    vec3 camera_vector = camera_pos - vObjectSpacePosition;
    vec3 light_vector = light_pos - vObjectSpacePosition;

    vTangentSpaceLightVector = world_to_tangent_matrix * light_vector;
    vTangentSpaceCameraVector = world_to_tangent_matrix * camera_vector;
}
