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
/usr/bin/clang -g -I src/main/resources/backend/ \
    -I external/gc/native/include/ \
    -D__SJS__ \
    -ftrapv \
    -Werror=implicit-int -Werror=implicit-function-declaration \
    -Wno-attributes -Wno-unused-value -Wno-parentheses-equality \
    external/gc/native/lib/libgc.a \
    src/main/resources/backend/globals.c \
    src/main/resources/backend/runtime.c \
    src/main/resources/backend/jsmath.c \
    src/main/resources/backend/array.c \
    src/main/resources/backend/map.c \
    src/main/resources/backend/native/ffi.c \
    src/main/resources/backend/xxhash.c \
    $@
