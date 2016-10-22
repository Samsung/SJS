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
#echo BEFORE: $@
#export EXT="external"
#pushd $EXT/emsdk
##source external/emscripten_env.sh
#./emsdk activate sdk-1.25.0-64bit
#source ./emsdk_env.sh
#popd
#echo AFTER: $@

# For now, we'll build using LLVM's hand-rolled GC, which has been widely used but is not exactly
# high performance

#export PATH="$EXT/emsdk:$EXT/emsdk/clang/e1.25.0_64bit:$EXT/emsdk/node/0.10.18_64bit/bin:$EXT/emsdk/emscripten/1.25.0:$PATH"
#
#export EMSCRIPTEN="$EXT/emsdk/emscripten/1.25.0"


# In order to compile our runtime's use of variadic functions, we need to use a different LLVM
# backend:
#   http://kripken.github.io/emscripten-site/docs/getting_started/FAQ.html#why-do-i-get-error-cannot-compile-this-aggregate-va-arg-expression-yet-and-it-says-compiler-frontend-failed-to-generate-llvm-bitcode-halting-afterwards
#   https://github.com/kripken/emscripten/issues/2238
#export EMCC_LLVM_TARGET=i386-pc-linux-gnu
### TODO: Except with this change, we hang in some random python threadpool... 
##emcc -g -I src/main/resources/backend/ \
##    -I $EXT/emsdk/emscripten/1.22.0/system/include/ \
##    --pre-js src/main/resources/backend/browser/marshalling.js \
##    --js-library src/main/resources/backend/browser/library.js \
##    --js-library $EXT/emsdk/emscripten/1.22.0/src/library_gc.js \
##    src/main/resources/backend/globals.c \
##    src/main/resources/backend/runtime.c \
##    $@
emcc -g -I src/main/resources/backend/ \
    -I external/gc/asmjs/include/ \
    -Werror=implicit-int -Werror=implicit-function-declaration \
    -Wno-attributes -Wno-unused-value -Wno-parentheses-equality \
    --pre-js src/main/resources/backend/browser/marshalling.js \
    --js-library src/main/resources/backend/browser/library.js \
    -D__SJS__ \
    src/main/resources/backend/globals.c \
    src/main/resources/backend/runtime.c \
    src/main/resources/backend/jsmath.c \
    src/main/resources/backend/array.c \
    src/main/resources/backend/map.c \
    src/main/resources/backend/browser/ffi.c \
    src/main/resources/backend/xxhash.c \
    $EXT/gc/asmjs/lib/libgc.dylib \
    $@
#    -s NO_EXIT_RUNTIME=1 \
#    -Lexternal/gc/asmjs/lib/ \
#    -lgc \

# TODO: consider flag for -s INVOKE_RUN=0, which in the browser doesn't immediately call main on
# page load
