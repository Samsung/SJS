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

# We assume that $TIZEN points to the root of the Tizen 2.2 SDK

export CPP="$TIZEN/tools/arm-linux-gnueabi-gcc-4.5/bin/arm-linux-gnueabi-cpp --sysroot $TIZEN/platforms/tizen2.2/rootstraps/tizen-device-2.2.native"
export CXX="$TIZEN/tools/arm-linux-gnueabi-gcc-4.5/bin/arm-linux-gnueabi-g++ --sysroot $TIZEN/platforms/tizen2.2/rootstraps/tizen-device-2.2.native"
export CC="$TIZEN/tools/arm-linux-gnueabi-gcc-4.5/bin/arm-linux-gnueabi-gcc --sysroot $TIZEN/platforms/tizen2.2/rootstraps/tizen-device-2.2.native"
export LD="$TIZEN/tools/arm-linux-gnueabi-gcc-4.5/bin/arm-linux-gnueabi-ld --sysroot $TIZEN/platforms/tizen2.2/rootstraps/tizen-device-2.2.native"
export AR="$TIZEN/tools/arm-linux-gnueabi-gcc-4.5/bin/arm-linux-gnueabi-ar"
export CFLAGS="-DNO_SIGCONTEXT_H"

pushd gc

git clone https://github.com/ivmai/bdwgc boehm_arm
pushd boehm_arm
git clone https://github.com/ivmai/libatomic_ops

# Still in gc/boehm_arm

pushd libatomic_ops
autoreconf -vif
popd

# Back in gc/boehm_arm
autoreconf -vif
#automake --add-missing
./configure --prefix=$BASE/gc/arm_tizen --with-pic --host=arm-linux --target=arm-linux-gnueabi --enable-single-obj-compilation
make
make install

popd

# If you're unsure whether this successfully generated ARM code, run 'file' on
# .../gc/arm_tizen/lib/libgc.so or .../gc/boehm_arm/extra/gc.o
