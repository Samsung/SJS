# Copyright 2014-2016 Samsung Research America, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#!/bin/bash

echo "function assert(cond) { if (!cond) { throw Error(); } }" > $1-node.js
echo "function print(s) { console.log(s); }" >> $1-node.js
echo "function printInt(s) { console.log(s); }" >> $1-node.js
echo "function printString(s) { console.log(s); }" >> $1-node.js
echo "function printFloat(s) { console.log(s); }" >> $1-node.js
echo "function printFloat10(s) { console.log(s.toFixed(10)); }" >> $1-node.js
echo "function itofp(s) { return s; }" >> $1-node.js
echo "function string_of_int(x) { return x.toString(); }" >> $1-node.js
echo "var TyHint = {};" >> $1-node.js

cat $1.js >> $1-node.js
