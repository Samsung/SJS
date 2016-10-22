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

BASE=$1
APPNAME=$2

if [ -e "$BASE" ]; then
    # TODO: generalize to take tizen target js file
    #   ./parse_sjs_idl.py idl/tizen idl/tizen.idl
    #./sjsc --only-c $2.js --extra-decls idl/tizen.json --native-libs idl/tizen.linkage.json --guest-runtime
    ./sjsc --only-c $2.js
    cp src/main/resources/backend/*.h $1/inc/
    cp external/gc/native/include/gc.h $1/inc/
    cp -r external/gc/native/include/gc $1/inc/
    cp $2.c $1/src/$2.c
    cp $2.h $1/inc/$2.h
    # !!! TODO: this tizen.cpp has a hardcoded path to a calc.h....
    # modify glue generation to refer to "app" .h?
    #cp idl/tizen.cpp $1/src/
    make -C src/main/resources/backend/ native
    cp src/main/resources/backend/sjs_runtime.clang.a $1/lib/
else
    echo "error: cannot find target project path [$BASE]"
    echo "usage: $0 <path/to/Tizen IDE project>"
    exit 1
fi
