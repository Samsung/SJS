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
OUT1=~/benchmarks-endtoend
OUT2=~/benchmarks-perf


for b in `ls -1 $TESTS | cut -f 1 -d '.'`;
do
    echo Benchmarking $TESTS/$b.js
    cp $TESTS/$b.js $OUT1/$b.js;
    ./benchmark.sh $OUT1/$b.js;
done
for b in `ls -1 $PERFTESTS | cut -f 1 -d '.'`;
do
    echo Benchmarking $PERFTESTS/$b.js
    cp $PERFTESTS/$b.js $OUT2/$b.js;
    ./benchmark.sh $OUT2/$b.js;
done
