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
cd `dirname $0`
BASE=`pwd`
if [ -e "$BASE/gc/native/lib/libgc.a" ]; then
    exit 0;
fi
pushd gc

git clone https://github.com/ivmai/bdwgc boehm_native
pushd boehm_native
git clone https://github.com/ivmai/libatomic_ops

# Still in gc/boehm_native

pushd libatomic_ops
autoreconf -vif
popd
autoreconf -vif
automake --add-missing
./configure --prefix=$BASE/gc/native --enable-single-obj-compilation
make
make install

popd
