/* 
 * Copyright 2014-2016 Samsung Research America, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// For some reason, if I define these functions in library.js,
// emscripten strips them out during compilation, regardless of
// any references from JS or extern statements in C
var __marshalling_meta = { nextObj: 0, objTbl: [] };
//document.marshallobj = function(elt) {
function _marshallobj(elt) {
  if (elt.hasOwnProperty('__indirection')) {
    return elt.__indirection;
  } else {
    elt.__indirection = __marshalling_meta.nextObj;
    __marshalling_meta.objTbl[__marshalling_meta.nextObj] = elt;
    return __marshalling_meta.nextObj++;
  }
}

function _unmarshallobj(id) {
  return __marshalling_meta.objTbl[id];
}
