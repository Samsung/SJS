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
DIRNAME=`dirname $1`
CFILE=$DIRNAME/$BASENAME.c
EXEC=$DIRNAME/$BASENAME
SJSFLAGS="-Xfields true"

# Build for clang, gcc, generate node.js/iojs and spidermonkey-appropriate forms
echo Compiling with sjsc to $BASENAME...
rm -f $CFILE $EXEC $EXEC-gcc $EXEC-clang
./sjsc $SJSFLAGS $1
echo Clang...
./clang.sh -O2 $CFILE -DUSE_GC -o $EXEC-clang
echo ... returned $?
echo GCC...
./gcc.sh -O2 $CFILE -DUSE_GC -o $EXEC-gcc
echo ... returned $?

echo "function assert(cond) { if (!cond) { throw Error(); } }" > $EXEC-node.js
echo "function print(s) { console.log(s); }" >> $EXEC-node.js
echo "function printInt(s) { console.log(s); }" >> $EXEC-node.js
echo "function printString(s) { console.log(s); }" >> $EXEC-node.js
echo "function printFloat(s) { console.log(s); }" >> $EXEC-node.js
echo "function printFloat10(s) { console.log(s.toFixed(10)); }" >> $EXEC-node.js
echo "function itofp(s) { return s; }" >> $EXEC-node.js
echo "function string_of_int(x) { return x.toString(); }" >> $EXEC-node.js
echo "var TyHint = {};" >> $EXEC-node.js

cat $1 >> $EXEC-node.js

rm $EXEC-spidermonkey.js
echo "function assert(cond) { if (!cond) { throw Error(); } }" > $EXEC-spidermonkey.js
echo "function printInt(s) { print(s); }" >> $EXEC-spidermonkey.js
echo "function printString(s) { print(s); }" >> $EXEC-spidermonkey.js
echo "function printFloat(s) { print(s); }" >> $EXEC-spidermonkey.js
echo "function printFloat10(s) { print(s.toFixed(10)); }" >> $EXEC-spidermonkey.js
echo "function itofp(s) { return s; }" >> $EXEC-spidermonkey.js
echo "function string_of_int(x) { return x.toString(); }" >> $EXEC-spidermonkey.js
echo "var console = { log: print }" >> $EXEC-spidermonkey.js
echo "var TyHint = {};" >> $EXEC-spidermonkey.js

cat $1 >> $EXEC-spidermonkey.js

# Run everything, collecting data
echo Running $1 through node...
/usr/bin/time node $EXEC-node.js 2> $EXEC.node.time > $EXEC.node.out
echo Running $1 through recent v8...
export PATH=/Users/colin.gordon/research/v8/out/native:$PATH
/usr/bin/time d8 $EXEC-spidermonkey.js 2> $EXEC.v8.time > $EXEC.v8.out
export PATH=`pwd`/external/spidermonkey/:$PATH
echo Running $1 through spidermonkey...
/usr/bin/time js $EXEC-spidermonkey.js 2> $EXEC.spider.time > $EXEC.spider.out
#echo Running $1 through spidermonkey --no-ion --no-baseline ...
#time js --no-ion --no-baseline tmp-spidermonkey.js > __spidermonkey_tmp

echo Executing native binary $EXEC-gcc...
/usr/bin/time $EXEC-gcc 2> $EXEC.gcc.time > $EXEC.gcc.out
echo Executing native binary $EXEC-clang...
/usr/bin/time $EXEC-clang 2> $EXEC.clang.time > $EXEC.clang.out

#diff __node_tmp __sjs_clang_tmp

