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

OUT1=~/phone_bench_results/benchmarks-endtoend
OUT2=~/phone_bench_results/benchmarks-perf

echo Benchmark, node.js, sjs-gcc-arm > $OUT1/results.csv
echo Benchmark, node.js, sjs-gcc-arm > $OUT2/results.csv
for b in `ls -1 $OUT1 | grep time | cut -f 1 -d '.' | sort | uniq`;
do
    #GCCTIME=`grep elapsed $OUT1/$b.local.time | sed -e 's/real.*//' | sed -e 's/        //'`
    #NODETIME=`grep elapsed $OUT1/$b.node.time | sed -e 's/real.*//' | sed -e 's/        //'`
    GCCTIME=`grep elapsed $OUT1/$b.local.time | cut -f 3 -d ' ' | sed -e 's/elapsed//'`
    NODETIME=`grep elapsed $OUT1/$b.node.time | cut -f 3 -d ' ' | sed -e 's/elapsed//'`
    echo $b, $NODETIME, $GCCTIME >> $OUT1/results.csv;
done
for b in `ls -1 $OUT2 | grep time | cut -f 1 -d '.' | sort | uniq`;
do
    #GCCTIME=`cat $OUT2/$b.local.time | sed -e 's/real.*//' | sed -e 's/        //'`
    #NODETIME=`cat $OUT2/$b.node.time | sed -e 's/real.*//' | sed -e 's/        //'`
    GCCTIME=`grep elapsed $OUT2/$b.local.time | cut -f 3 -d ' ' | sed -e 's/elapsed//'`
    NODETIME=`grep elapsed $OUT2/$b.node.time | cut -f 3 -d ' ' | sed -e 's/elapsed//'`
    echo $b, $NODETIME, $GCCTIME >> $OUT2/results.csv;
done

