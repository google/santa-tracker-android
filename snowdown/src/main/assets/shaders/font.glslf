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

varying mediump vec2 vTexCoord;
uniform sampler2D texture_unit_0;
uniform lowp vec4 color;
void main()
{
  lowp vec4 texture_color = texture2D(texture_unit_0, vTexCoord);

  // Font texture is a 1 channel luminance texture.
  // Copying luminance value to alphachannel for blending.
  texture_color.a = texture_color.r;

  // We only render pixels if they are at least somewhat opaque.
  // This will still lead to aliased edges if we render
  // in the wrong order, but leaves us the option to render correctly
  // if we sort our polygons first.
  if (texture_color.a < 0.01)
    discard;
  gl_FragColor = color * texture_color;
}
