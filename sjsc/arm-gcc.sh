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

ARMCC=/Users/colin.gordon/tizen-sdk-2.2/tools/arm-linux-gnueabi-gcc-4.5/bin/arm-linux-gnueabi-gcc

TIZEN_ROOT=/Users/colin.gordon/tizen-sdk-2.2/platforms/tizen2.2/rootstraps/tizen-device-2.2.native

# Tizen SDK doesn't support C11... it's GCC 4.5, circa 2010

# adding -v gives good debuggin info about paths, subcommands, etc.

$ARMCC -g -I src/main/resources/backend/ \
    -D__SJS__ \
    -DLEAK_MEMORY \
    -DTIZEN \
    -lc_nonshared \
    -lm -pthread -ldl \
    --sysroot=$TIZEN_ROOT \
    -Werror=implicit-int -Werror=implicit-function-declaration \
    -Wno-attributes -Wno-unused-value -Wno-parentheses-equality \
    /Users/colin.gordon/research/sjs-fresh/sjsc/external/gc/boehm_arm/extra/gc.o \
    $@
#    /Users/colin.gordon/research/sjs-fresh/sjsc/src/main/resources/backend/globals.c \
#    /Users/colin.gordon/research/sjs-fresh/sjsc/src/main/resources/backend/runtime.c \
#    /Users/colin.gordon/research/sjs-fresh/sjsc/src/main/resources/backend/jsmath.c \
#    /Users/colin.gordon/research/sjs-fresh/sjsc/src/main/resources/backend/array.c \
#    /Users/colin.gordon/research/sjs-fresh/sjsc/src/main/resources/backend/map.c \
#    /Users/colin.gordon/research/sjs-fresh/sjsc/src/main/resources/backend/native/ffi.c \
#    /Users/colin.gordon/research/sjs-fresh/sjsc/src/main/resources/backend/xxhash.c \
#    $@
