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

curl https://s3.amazonaws.com/mozilla-games/emscripten/releases/emsdk-portable.tar.gz > emsdk.tgz
tar -zxvf emsdk.tgz
mv emsdk_portable emsdk


# Set up to build Boehm GC for native and asm.js (but don't build yet)
mkdir gc
pushd gc
GCBASE=`pwd`
echo $GCBASE
mkdir native
mkdir asmjs

git clone https://github.com/ivmai/bdwgc boehm_native
pushd boehm_native
git clone https://github.com/ivmai/libatomic_ops
popd

git clone https://github.com/ivmai/bdwgc boehm_asmjs
pushd boehm_asmjs
git clone https://github.com/ivmai/libatomic_ops
popd
