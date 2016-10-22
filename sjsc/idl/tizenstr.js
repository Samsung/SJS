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
// Test Tizen String IDL gen

var str = new __String("") ; // allocate an empty Tizen string.
console.log("Constructed str");
var dalm = new __String(" Dalmatians");
console.log("Constructed dalm");
str.Append(dalm);
console.log("called Append");
var str2 = new __String("STRING TEST");
console.log("Constructed str2");
var substr = str2.SubString(7); // now subStr == "TEST"
var internal = substr.GetPointer(); // get wchar_t* rep of substr
console.log(internal);

var cont = str2.Contains(substr);
if (cont) {
    console.log("success!");
} else {
    console.log("FAILURE");
}

function Sjstr() {
    this.m = function() { console.log("I'm an SJS subtype of a C++ class!"); }
    console.log("constructed a sjstr");
}
Sjstr.prototype = new __String("");

var x = new Sjstr();
x.m();
x.Append(dalm);
console.log("called Append again");
var intern = x.GetPointer();
console.log(intern);
