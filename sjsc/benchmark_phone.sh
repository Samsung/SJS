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
SJSFLAGS="-Xfields true --only-c"

# Build for arm, generate node.js/iojs and spidermonkey-appropriate forms
echo Compiling with sjsc to $BASENAME...
rm -f $CFILE $EXEC-arm
./sjsc-fast $SJSFLAGS $1
#echo Clang...
#sed -i -e 's/#include<gc.h>//' $CFILE;
#sed -i -e 's/GC_INIT();//' $CFILE;
#./arm-gcc.sh -std=c99 -D__USE_BSD -D__USE_XOPEN -O2 $CFILE -DLEAK_MEMORY -o $EXEC-arm
echo ... returned $?

./nodify.sh $EXEC
