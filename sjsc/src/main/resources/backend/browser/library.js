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
mergeInto(LibraryManager.library, {
  __document_getElementById: function(id) {
    id = Pointer_stringify(id);
    console.log('documentGetElementById passed: '+id)
    elt = document.getElementById(id);
    // note post-ffi name
    return _marshallobj(elt);
  },
  __htmlelement_innerHTML___get: function(elt) {
    // TODO
    return 0;
  },
  __htmlelement_innerHTML___set: function(elt, val) {
    elt = _unmarshallobj(elt);
    val = Pointer_stringify(val);
    console.log('setInnerHTML passed: ('+elt+', '+val+')')
    elt.innerHTML = val;
  },
  __console_log: function(str) {
    console.log(Pointer_stringify(str));
  },
  __console_error: function(str) {
    console.error(Pointer_stringify(str));
  },
  __console_assert: function(b) {
    console.assert(b);
  }
});
