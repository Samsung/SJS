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

var Pair = {
  alloc : js_alloc_pair,
  print : js_print_pair,
//  add : js_add_pair,
  register : js_register_cb
};

var p = Pair.alloc();
Pair.print(p);
p.setX(3);
Pair.print(p);
p.setY(9);
Pair.print(p);


function pair_callback(x) {
    console.log("Running in pair_callback in SJS");
    x.setY(x.getY()+1);
    Pair.print(x);
    console.log("Printing pair captured in SJS closure:");
    Pair.print(p);
}

Pair.register(pair_callback);
//Pair.addPair(p);

//
