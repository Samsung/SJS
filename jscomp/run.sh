#!/usr/bin/env bash

BASEDIR=$(dirname $0)
RESULTDIR=$BASEDIR/testresults


mkdir -p `dirname "$RESULTDIR/$2"`
cat $2; node src/js/compiler/compile.js $2 2>&1 | tee $RESULTDIR/$2.new

if [ -f $RESULTDIR/$2.old ] ;
then
    diff $RESULTDIR/$2.old $RESULTDIR/$2.new > /dev/null 2>&1
    if [ $? -eq 0 ] ;
    then
        rm $RESULTDIR/$2.diff
	    rm -f $RESULTDIR/$2.new
        echo "$2 passed"
        echo "$2 passed" >> $RESULTDIR/test.log
    else
        diff $RESULTDIR/$2.old $RESULTDIR/$2.new
        diff $RESULTDIR/$2.old $RESULTDIR/$2.new > $RESULTDIR/$2.diff
        echo "$2 failed"
        echo "$2 failed" >> $RESULTDIR/test.log
        if [ "$1" = "-a" ]
        then
            cp $RESULTDIR/$2.new $RESULTDIR/$2.old
        fi
    fi
else
    cp $RESULTDIR/$2.new $RESULTDIR/$2.old
    rm -f $RESULTDIR/$2.new
    echo "$2 created"
    echo "$2 created" >> $RESULTDIR/test.log
fi
