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
# Script for invoking GCC from an ARM system

gcc -g -O2 -I backend/ -I /js/gc/include/ \
    -D__SJS__ \
    -DUSE_GC \
    -DTIZEN \
    -lm -lbsd -pthread -ldl \
    -Werror=implicit-int -Werror=implicit-function-declaration \
    -Wno-attributes -Wno-unused-value -Wno-parentheses-equality \
    backend/globals.c \
    backend/date.c \
    backend/runtime.c \
    backend/jsmath.c \
    backend/array.c \
    backend/map.c \
    backend/native/ffi.c \
    backend/xxhash.c \
    /js/boehm_hf/extra/gc.o \
    $@
    #backend/sjs_runtime.tizen.a \
