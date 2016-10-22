/* 
 * Copyright 2015-2016 Samsung Research America, Inc.
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

function Function () {

};

Function.prototype = Function; // placeholder
function Object() {
};

Function.__proto__ = Function.prototype = { }; //@todo fill this up with necessary builtin functions
Object.__proto__ = Function.prototype;
Object.prototype = {}; //@todo fill this up with necessary builtin functions
Function.prototype.__proto__ = Object.prototype;
Object.prototype.__proto__ = null;


console = {};

console.log = function(str) {
    // TODO: coercions
    "use C: wprintf(L\"%ls\\n\", val_as_string(str));";
};
console.assert = function(b) {
    "use C: assert(val_is_boolean(b) ? val_as_boolean(b) : !val_is_falsy(b));";
};

function Array() {

}

Array.prototype = {}; //@todo fill this up with necessary builtin functions

function RegExp () {

}

RegExp.prototype = {}; //@todo fill this up with necessary builtin functions

function $ERROR(str) {
    // TODO: coercions
    "use C: fwprintf(stderr, L\"%ls\\n\", val_as_string(str));";
}

var Math = {};

Math.max = function(a,b) {
    return a>b? a:b;
};

Math.sqrt = function(v) {
    var ret;
    "use C: ret = double_as_val(sqrt(val_as_double(v)));";
    return ret;
};
