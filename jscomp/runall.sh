#!/usr/bin/env bash

# to test run ./runall.sh -t
BASEDIR=$(dirname $0)
RESULTDIR=$BASEDIR/testresults

rm $RESULTDIR/test.log
for i in tests/*.js; do ./run.sh $1 $i; done
cat $RESULTDIR/test.log


if [ -f $RESULTDIR/test.log.old ] ;
then
    diff $RESULTDIR/test.log.old $RESULTDIR/test.log > /dev/null 2>&1
    if [ $? -eq 0 ] ;
    then
	    rm -f $RESULTDIR/test.log
        echo "All tests results match"
    else
        diff $RESULTDIR/test.log.old $RESULTDIR/test.log
        echo "At least one test result is different"
        if [ "$1" = "-a" ]
        then
            echo "Replacing old test results with new ones"
            cp $RESULTDIR/test.log $RESULTDIR/test.log.old
        fi
    fi
else
    cp $RESULTDIR/test.log $RESULTDIR/test.log.old
    rm -f $RESULTDIR/test.log
    echo "Test results logged"
fi
