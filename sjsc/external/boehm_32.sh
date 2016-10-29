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

export CFLAGS="-m32"
export CXXFLAGS="-m32"

# This gets the lowercase os kernel name, e.g. 'linux' or 'darwin'
export HOST=`uname | tr '[:upper:]' '[:lower:]'`

cd `dirname $0`
BASE=`pwd`

if [ -e "$BASE/gc/x86/lib/libgc.a" ]; then
    exit 0;
fi

pushd gc

git clone https://github.com/ivmai/bdwgc boehm_32
pushd boehm_32
git clone https://github.com/ivmai/libatomic_ops

# Still in gc/boehm_32

pushd libatomic_ops
autoreconf -vif
popd

# Back in gc/boehm_32
autoreconf -vif
#automake --add-missing
./configure --prefix=$BASE/gc/x86 --host=i386-pc-$HOST --target=i386-pc-$HOST --enable-single-obj-compilation
make
make install

popd

# If you're unsure whether this successfully generated ARM code, run 'file' on
# .../gc/i386_tizen/lib/libgc.so or .../gc/boehm_32/extra/gc.o

