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

exports = { foo: 3,
            bar: true,
            main: function(f) {
                    var o = {foo : 3};
                    f(o);
                    if (o.foo == 12) {
                        console.log("Awesome! Call to typed code from untyped updated o.foo to 12");
                    } else {
                        console.log("uh-oh, something went wrong with the call to the typed closure");
                    }
                  }
          };

