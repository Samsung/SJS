/* The Great Computer Language Shootout
   http://shootout.alioth.debian.org/
   contributed by Isaac Gouy */ 

var n =50, partialSum = 0.0;
for (var d = 1; d <= n; d++) partialSum += 1.0/d;
printString(partialSum.toFixed(9));

