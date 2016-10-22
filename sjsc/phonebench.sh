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

TESTS=src/test/resources/testinput/endtoend/
PERFTESTS=src/test/resources/testinput/perf/
#OUT1=~/phone-bench/benchmarks-endtoend
#OUT2=~/phone-bench/benchmarks-perf
rm -rf ./phone-img
mkdir phone-img
BASE=`pwd`/phone-img
OUT2=$BASE/benchmarks-perf
mkdir $OUT2
gradle depJar
make -C src/main/resources/backend tizen
pushd $BASE
git clone https://github.com/ivmai/bdwgc boehm_hf
pushd boehm_hf
git clone https://github.com/ivmai/libatomic_ops
popd
popd
#for b in `ls -1 $TESTS | cut -f 1 -d '.'`;
#do
#    echo Benchmarking $TESTS/$b.js
#    cp $TESTS/$b.js $OUT1/$b.js;
#    ./benchmark_phone.sh $OUT1/$b.js;
#done
for b in `ls -1 $PERFTESTS | cut -f 1 -d '.'`;
do
    echo Benchmarking $PERFTESTS/$b.js
    cp $PERFTESTS/$b.js $OUT2/$b.js;
    ./benchmark_phone.sh $OUT2/$b.js;
done
#cp external/gc/boehm_arm/extra/gc.o $OUT1/
##cp external/gc/boehm_arm/extra/gc.o $OUT2/
#cp armeabi.sh $OUT1/
cp armeabi.sh $OUT2/
#cp -r src/main/resources/backend $OUT1/backend
cp -r src/main/resources/backend $OUT2/backend
#cp phone_armbench.sh $OUT1/
cp phone_armbench.sh $OUT2/
#cp phone_nodebench.sh $OUT1/
cp phone_nodebench.sh $OUT2/
#cp -r external/gc/arm_tizen $OUT1/arm_tizen_gc
#cp -r external/gc/arm_tizen $OUT2/arm_tizen_gc
#cp phonebuild.sh $OUT1/
cp phonebuild.sh $OUT2/

cp phonegc.sh $BASE/
