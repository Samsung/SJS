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
    "use js: console.log(str);";
};

function Array() {
    var ret = [], len, i; //@todo: needs to be implemented properly
    if (arguments.length === 1) {
        len = arguments[0];
        for(i = 0; i<len; i++) {
            ret[i] = undefiend;
        }
    } else if (arguments.length > 1) {
        len = arguments.length;
        for(i = 0; i<len; i++) {
            ret[i] = arguments[i];
        }
    }
    return ret;
}

Array.prototype = {}; //@todo fill this up with necessary builtin functions

function RegExp () {

}

RegExp.prototype = {}; //@todo fill this up with necessary builtin functions

function $ERROR(str) {
    "use js: console.log(str);";
}


function String(x) {
    var ret;
    "use js: ret = ''+x;"
    return ret;
}

String.prototype = {};

var Math = {};

Math.max = function(a,b) {
    return a>b? a:b;
};

Math.sqrt = function(v) {
    var ret;
    "use js: ret = Math.sqrt(v);";
    return ret;
};


Math.sin = function (v){
    var ret;
    "use js: ret = Math.sin(v);";
    return ret;
};

Math.cos = function (v){
    var ret;
    "use js: ret = Math.cos(v);";
    return ret;
};

Math.round = function (v){
    var ret;
    "use js: ret = Math.round(v);";
    return ret;
};

Math.abs = function (v){
    var ret;
    "use js: ret = Math.abs(v);";
    return ret;
};

(function(){
    var ret;
    "use js: ret = Math.PI;";
    Math.PI = ret;
}());
