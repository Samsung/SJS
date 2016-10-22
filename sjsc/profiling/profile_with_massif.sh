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

# Run all memory profiling.
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
OTHER=iojs

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

# args: [1] mode: 'normal': profile stack/heap only
#                 'all':    profile all virtual pages
#       [2] path to JS file to profile
# output: $outdir/massif.basename($1).$mode.sjs.out
profile_sjs() {
    target=$2
    target_base=${target##*/}
    target_basename=${target_base%%.*}
    mode=$1
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

    # Run with Massif.
    case $mode in
    'normal')
        massif_mode='--stacks=yes'
        ;;
    'all')
        massif_mode='--pages-as-heap=yes'
        ;;
    *)
        echo "bad massif mode ($mode) for $target_basename.js"
        return 1
        ;;
    esac
    MASSIF="valgrind --tool=massif \
            $massif_mode \
            --time-unit=B \
            --massif-out-file=$outdir/massif.$target_basename.$mode.sjs.out \
            ./$exe"
    $MASSIF 2>/dev/null 1>/dev/null
    if [ $? -ne 0 ]; then
        echo "$MASSIF"
        echo "failed"
        return 1
    fi
}

# args: [1] mode: 'normal': profile stack/heap only
#                 'all':    profile all virtual pages
#       [2] path to JS file to profile
# output: $outdir/massif.basename($1).$mode.other.out
profile_other() {
    target=$2
    target_base=${target##*/}
    target_basename=${target_base%%.*}
    mode=$1

    # Run with Massif.
    case $mode in
    'normal')
        massif_mode='--stacks=yes'
        ;;
    'all')
        massif_mode='--pages-as-heap=yes'
        ;;
    *)
        echo "bad massif mode ($mode) for $target_basename.js"
        return 1
        ;;
    esac
    MASSIF="valgrind --tool=massif \
            $massif_mode \
            --time-unit=B \
            --massif-out-file=$outdir/massif.$target_basename.$mode.other.out \
            $OTHER $target"
    $MASSIF 2>/dev/null 1>/dev/null
    if [ $? -ne 0 ]; then
        echo "$MASSIF"
        echo "failed"
        return 1
    fi
}

# Extract peak memory use from a Massif file.
# args: [1] path to massif file
# return: peak memory use
get_peak() {
    massif_file=$1

    ms_print $massif_file \
    | grep -v '\-' \
    | grep -v '([^0-9]|[^,])' \
    | grep '[0-9]' \
    | awk '{ print $3 }' \
    | sed -e 's/,//g' \
    | sort -nr \
    | head -n1
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
touch $outdata
echo "test name	mode	sjs peak memory	`echo $OTHER` peak memory" >> $outdata

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

    # Run twice, once to profile stack+heap and once for all virtual pages.
    for mode in $modes; do
        echo -n " [$mode: sjs"
        # Run test.
        profile_sjs $mode $test
        echo -n ", $OTHER"
        profile_other $mode $test
        echo -n "]"

        # Collate results.
        echo -n $bn >> $outdata
        echo -n '	' >> $outdata
        echo -n $mode >> $outdata
        echo -n '	' >> $outdata
        echo -n `get_peak "$outdir/massif.$bn.$mode.sjs.out"` >> $outdata 
        echo -n '	' >> $outdata
        echo -n `get_peak "$outdir/massif.$bn.$mode.other.out"` >> $outdata 
        echo "" >> $outdata
    done
    echo ""
done

# Return to original directory.
popd
