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

# Notes:
#   For this script to display results properly, sqlite3 must be in the path,
#   and it must be built with extension loading enabled, eg. with the following
#   config:
#
#       cd path/to/sqlite/source
#       ./configure --enable-dynamic-extensions

db=profiling_data/octane_min.db

# Get basename.
# Eg. bn 'foo/bar.baz' returns 'bar'.
# args: [1] path
# return: base
base() {
    path="$1"
    file="${path##*/}"
    base="${file%%.*}"
    echo $base
}

# Check for duk and node.
if [ -z `which duk` ]; then
    echo "please add 'duk' to your path"
    exit 1
fi

if [ -z `which node` ]; then
    echo "please add 'node' to your path"
    exit 1
fi

pushd .. 2>/dev/null 1>/dev/null

# Check that we're in the right directory.
d=`pwd`
if [ ${d##*/} != "sjsc" ]; then
    echo "please run from sjs-compiler/sjsc/profiling"
    popd 2>/dev/null 1>/dev/null
    exit 1
fi

# Set up database.
rm -f $db
sqlite3 $db 'CREATE TABLE results(runtime varchar(10), test varchar(30), peak_rss unsigned big int, user_time double, system_time double)'

# sjs
for word in `ls src/test/resources/testinput/octane-min-hacks/*.js`; do
    ./sjsc_silent $word 
    tst=`base $word`
    echo -n -e "$tst:\t"
    for i in {1..30}; do
        echo -n "$i "
        # sjs
        results=`monitorRSS ./silent ./a.out | awk '{printf("%d, %f, %f", $1, $2, $3)}'`
        sqlite3 $db "insert into results values(\"sjs\", \"$tst\", $results)"
        # node
        results=`monitorRSS ./silent node $word | awk '{printf("%d, %f, %f", $1, $2, $3)}'`
        sqlite3 $db "insert into results values(\"node\", \"$tst\", $results)"
        #duk
        results=`monitorRSS ./silent duk $word | awk '{printf("%d, %f, %f", $1, $2, $3)}'`
        sqlite3 $db "insert into results values(\"duk\", \"$tst\", $results)"
    done
    echo ""
done

# Print results.
gcc -g -fPIC -dynamiclib profiling/sqlite-extension-functions.c -o profiling/libsqlitefunctions.dylib
echo ""
echo -e "runtime,test,memory,error,time,error"
sqlite3 \
    -cmd "SELECT load_extension('$(pwd)/profiling/libsqlitefunctions');" \
    $db \
    "SELECT runtime, test, avg(peak_rss), stdev(peak_rss) / sqrt(30), avg(user_time) + avg(system_time), (stdev(user_time) / sqrt(30)) + (stdev(system_time) / sqrt(30)) FROM results GROUP BY runtime, test;" | \
    sed -e 's/|/,/g'

popd 2>/dev/null 1>/dev/null
