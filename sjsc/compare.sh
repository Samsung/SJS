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
BASENAME=`basename $1 .js`
CFILE=$BASENAME.c
EXEC=$BASENAME
SJSFLAGS="--only-c -Xfields true"

echo Compiling with sjsc to $BASENAME...
rm -f $CFILE $EXEC $EXEC-gcc $EXEC-clang
./sjsc $SJSFLAGS $1
echo Clang...
./clang.sh -O2 $CFILE -DUSE_GC -o $EXEC-clang
echo ... returned $?
echo GCC...
./gcc.sh -O2 $CFILE -DUSE_GC -o $EXEC-gcc
echo ... returned $?
#echo emcc...
#./sjsc $SJSFLAGS --target web -o $BASENAME-web.c $1
#./emcc.sh -s TOTAL_MEMORY=1526726656 -O3 -s AGGRESSIVE_VARIABLE_ELIMINATION=1 -s ASSERTIONS=2 -DLEAK_MEMORY $BASENAME-web.c -o $EXEC-asm.js
#echo ... returned $?

echo "function assert(cond) { if (!cond) { throw Error(); } }" > tmp-node.js
echo "function print(s) { console.log(s); }" >> tmp-node.js
echo "function printInt(s) { console.log(s); }" >> tmp-node.js
echo "function printString(s) { console.log(s); }" >> tmp-node.js
echo "function printFloat(s) { console.log(s); }" >> tmp-node.js
echo "function printFloat10(s) { console.log(s.toFixed(10)); }" >> tmp-node.js
echo "function itofp(s) { return s; }" >> tmp-node.js
echo "function string_of_int(x) { return x.toString(); }" >> tmp-node.js
echo "var TyHint = {};" >> tmp-node.js

cat $1 >> tmp-node.js

echo Running $1 through node...
time node tmp-node.js > __node_tmp

rm -f tmp-spidermonkey.js
echo "function assert(cond) { if (!cond) { throw Error(); } }" > tmp-spidermonkey.js
echo "function printInt(s) { print(s); }" >> tmp-spidermonkey.js
echo "function printString(s) { print(s); }" >> tmp-spidermonkey.js
echo "function printFloat(s) { print(s); }" >> tmp-spidermonkey.js
echo "function printFloat10(s) { print(s.toFixed(10)); }" >> tmp-spidermonkey.js
echo "function itofp(s) { return s; }" >> tmp-spidermonkey.js
echo "function string_of_int(x) { return x.toString(); }" >> tmp-spidermonkey.js
echo "var console = { log: print }" >> tmp-spidermonkey.js
echo "var TyHint = {};" >> tmp-spidermonkey.js

#cat tmp-spidermonkey.js > tmp-asm.js
#echo "var document = {};" >> tmp-asm.js

cat $1 >> tmp-spidermonkey.js

#cat $EXEC-asm.js >> tmp-asm.js

#echo Running $1 through recent v8...
#export PATH=/Users/colin.gordon/research/v8/out/native:$PATH
#time d8 tmp-spidermonkey.js > __v8_tmp
export PATH=`pwd`/external/spidermonkey/:$PATH
echo Running $1 through spidermonkey...
time js tmp-spidermonkey.js > __spidermonkey_tmp
#echo Running $1 through spidermonkey --no-ion --no-baseline ...
#time js --no-ion --no-baseline tmp-spidermonkey.js > __spidermonkey_tmp

echo Executing native binary $EXEC-gcc...
time ./$EXEC-gcc > __sjs_gcc_tmp
echo Executing native binary $EXEC-clang...
time ./$EXEC-clang > __sjs_clang_tmp

#echo Running $BASENAME-asm.js through spidermonkey...
#time js tmp-asm.js > __asm_tmp

diff __node_tmp __sjs_clang_tmp

