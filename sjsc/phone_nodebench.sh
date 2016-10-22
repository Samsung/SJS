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
#OUT1=/js/phone-bench/benchmarks-endtoend
#OUT2=/js/phone-bench/benchmarks-perf
#for b in `ls -1 $OUT1 | grep node | cut -f 1 -d '-'`;
#do
#    echo $b
#    /usr/bin/time nodejs $OUT1/$b-node.js 2> $OUT1/$b.node.time > $OUT1/$b.node.out
#done
for b in `ls -1 *-node.js`;
do
    echo $b
    /usr/bin/time nodejs $b-node.js 2> $b.node.time > $b.node.out
done

