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
CWD=`pwd`
SJS=$CWD

echo Path to SJS compiler: $SJS
echo Path to jscomp compiler: $JSCOMP

echo Compiling SJS code in: $1
echo Compiling jscomp code in $2


if [ -e "$JSCOMP/src/js/compiler/compile.js" ]; then
    pushd $JSCOMP
    node src/js/compiler/compile.js $CWD/$2 --target C --as-module
    popd
    if [ -e "$CWD/$2.c" ]; then
        echo jscomp succeeded: $CWD/$2.c
    else
        echo ERROR: Cannot find result of jscomp: should be in $CWD/$2.c, but is not!
        exit 3;
    fi
else
    echo ERROR: Cannot find $JSCOMP/src/js/compiler/compile.js --- is \$JSCOMP set correctly?
    exit 1;
fi

if [ -e "$SJS/sjsc" ]; then
    pushd $SJS
    ./sjsc $CWD/$1 --extra-objs $2.c -O0
    echo Running compilation result:
    ./a.out
    popd
else
    echo ERROR: Cannot find $SJS/sjsc --- is this being run from the root of sjsc checkout?
    echo sjsc script expects arguments to --extra-objs --- $2.c --- to be relative to sjsc location
    exit 2;
fi


