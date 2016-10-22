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

# Run RSS memory profiling.
# Author: Cole Schlesinger

# SJSC_HOME should be path/to/sjs_compiler/sjsc.
if [ "$SJSC_HOME" = "" ]; then
    echo "Please set the environment variable SJSC_HOME=path/to/sjs_compiler/sjsc."
    exit 1
fi

# Run from SJSC_HOME, then switch back.
pushd `pwd` 1>/dev/null 2>/dev/null
cd $SJSC_HOME

# Commands.
SJSC=./sjsc
OTHER=node
RSS=./profiling/monitorRSS

# Configuration.
outdir=profiling_data
outdata=$outdir/profiles.csv
modes="normal all"
test_root="src/test/resources/testinput"

tests="\
    `ls $test_root/endtoend/*.js` \
    $test_root/octane-hacks/navier-stokes.js \
    $test_root/octane-hacks/richards.js \
    $test_root/octane-hacks/splay.js"

skipped_tests="tsp_ga"


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

# Check whether the RSS monitoring tool exists.
check_monitorRSS() {
    if [ ! -e $RSS ]; then
        gcc -o $RSS $RSS.c
    fi
}

# Check whether $outdir exists, and create it if not.
check_outdir() {
    if [ ! -d $outdir ]; then
        mkdir $outdir
        if [ $? -ne 0 ]; then
            echo "cannot create directory $outdir"
            return 1
        fi
    fi
}

# Remove old profiling results.
clean_outdir() {
    rm -f $outdir/*.out
    rm -f $outdir/*.csv
}

# args: [1] path to JS file to profile
# output: print peak RSS use
profile_sjs() {
    target=$1
    target_base=${target##*/}
    target_basename=${target_base%%.*}
    exe=$outdir/$target_basename

    # Compile with SJS.
    if [ ! -x $exe ]; then
        $SJSC $target 2>/dev/null 1>/dev/null
        if [ $? -ne 0 ]; then
            echo "$SJSC $target"
            echo "failed"
            return 1
        fi
        if [ ! -x a.out ]; then
            echo "no a.out"
            echo "`pwd`"
            echo $SJSC $target
            return 1
        fi
        mv a.out $exe
    fi

    # Run with monitorRSS.
    $RSS $exe | grep "=== max RSS" | awk '{ print $4; }'
    if [ $? -ne 0 ]; then
        echo "$RSS $exe"
        echo "failed"
        return 1
    fi
}

# args: [1] path to JS file to profile
# output: print peak RSS use
profile_other() {
    target=$1
    target_base=${target##*/}
    target_basename=${target_base%%.*}

    $RSS node $target | grep "=== max RSS" | awk '{ print $4; }'
    if [ $? -ne 0 ]; then
        echo "$RSS node $target"
        echo "failed"
        return 1
    fi
}

# Check for the presence of a word in a list of words 
# args: [1] word to find
#       [2] list of words
# return: 0 if present, 1 otherwise.
is_in() {
    search_word=$1
    words=$2
    found=1

    for word in $words; do
        if [ $word = $search_word ]; then
            found=0
            break
        fi
    done

    return $found
}

# Run the tests.
check_outdir
clean_outdir
check_monitorRSS
touch $outdata
echo "test name	sjs peak memory	`echo $OTHER` peak memory" >> $outdata

for test in $tests; do
    echo -n "$test"
    bn=`base $test`

    # Skip some tests.
    is_in $bn $skipped_tests
    if [ $? -eq 0 ]; then
        echo " skipped"
        continue
    fi
        
    # Does the test exist?
    if [ ! -e $test ]; then
        echo " does not exist"
        continue
    fi

    # If so, does it run as normal JS?
    $OTHER $test 1>/dev/null 2>/dev/null
    if [ $? -ne 0 ]; then
        echo " does not run as js"
        continue
    fi

    # Profile.
    echo -n "$bn	" >> $outdata
    echo -n " [sjs"
    sjs_rss=`profile_sjs $mode $test`
    if [ $? -ne 0 ]; then
        echo $sjs_rss
    else
        echo -n "$sjs_rss	" >> $outdata
    fi
    echo -n ", $OTHER"
    other_rss=`profile_other $mode $test`
    if [ $? -ne 0 ]; then
        echo $other_rss
    else
        echo -n "$other_rss	" >> $outdata
    fi
    echo "" >> $outdata
    echo "]"
done

# Return to original directory.
popd
