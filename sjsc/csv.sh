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

OUT1=~/benchmarks-endtoend
OUT2=~/benchmarks-perf

echo Benchmark, node.js, v8, spidermonkey, sjs-gcc, sjs-clang > $OUT1/results.csv
echo Benchmark, node.js, v8, spidermonkey, sjs-gcc, sjs-clang > $OUT2/results.csv
for b in `ls -1 $OUT1 | grep time | cut -f 1 -d '.' | sort | uniq`;
do
    CLANGTIME=`cat $OUT1/$b.clang.time | sed -e 's/real.*//' | sed -e 's/        //'`
    GCCTIME=`cat $OUT1/$b.gcc.time | sed -e 's/real.*//' | sed -e 's/        //'`
    NODETIME=`cat $OUT1/$b.node.time | sed -e 's/real.*//' | sed -e 's/        //'`
    V8TIME=`cat $OUT1/$b.v8.time | sed -e 's/real.*//' | sed -e 's/        //'`
    SPIDERTIME=`cat $OUT1/$b.spider.time | sed -e 's/real.*//' | sed -e 's/        //'`
    echo $b, $NODETIME, $V8TIME, $SPIDERTIME, $GCCTIME, $CLANGTIME >> $OUT1/results.csv;
done
for b in `ls -1 $OUT2 | grep time | cut -f 1 -d '.' | sort | uniq`;
do
    CLANGTIME=`cat $OUT2/$b.clang.time | sed -e 's/real.*//' | sed -e 's/        //'`
    GCCTIME=`cat $OUT2/$b.gcc.time | sed -e 's/real.*//' | sed -e 's/        //'`
    NODETIME=`cat $OUT2/$b.node.time | sed -e 's/real.*//' | sed -e 's/        //'`
    V8TIME=`cat $OUT2/$b.v8.time | sed -e 's/real.*//' | sed -e 's/        //'`
    SPIDERTIME=`cat $OUT2/$b.spider.time | sed -e 's/real.*//' | sed -e 's/        //'`
    echo $b, $NODETIME, $V8TIME, $SPIDERTIME, $GCCTIME, $CLANGTIME >> $OUT2/results.csv;
done
