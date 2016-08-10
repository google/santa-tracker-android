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
varying vec2 vTexCoord;
uniform mat4 model_view_projection;
uniform vec3 pos_offset;

void main()
{
  gl_Position = model_view_projection * (aPosition + vec4(pos_offset, 0.0));
  vTexCoord = aTexCoord;
}
