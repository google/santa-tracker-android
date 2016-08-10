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

varying vec2 vTexCoord;
varying vec2 vTexCoordGround;
uniform sampler2D texture_unit_0;  // The billboard we're shadowing.
uniform sampler2D texture_unit_1;  // The shadow texture to apply.

void main()
{
  const float offset = 0.015;  // TODO, this should depend on texture size
  // Sample texture multiple times, to blend their alpha values for simple
  // edge fuzziness.
  vec4 tex1 = texture2D(texture_unit_0,
              clamp(vTexCoord + vec2(offset, offset), 0.0, 1.0));
  vec4 tex2 = texture2D(texture_unit_0,
              clamp(vTexCoord + vec2(-offset, -offset), 0.0, 1.0));
  vec4 tex3 = texture2D(texture_unit_0,
              clamp(vTexCoord + vec2(-offset, offset), 0.0, 1.0));
  vec4 tex4 = texture2D(texture_unit_0,
              clamp(vTexCoord + vec2(offset, -offset), 0.0, 1.0));
  vec4 shadow = texture2D(texture_unit_1, vTexCoordGround);
  gl_FragColor = vec4(shadow.rgb, (tex1.a + tex2.a + tex3.a + tex4.a) * 0.25);
}

