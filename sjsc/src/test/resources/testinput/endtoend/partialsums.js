/* The Computer Language Shootout
   http://shootout.alioth.debian.org/
   contributed by Isaac Gouy */

var n = 10000; /*arguments[0];*/ 
var a1 = 0.0, a2 = 0.0, a3 = 0.0, a4 = 0.0, a5 = 0.0, a6 = 0.0, a7 = 0.0, a8 = 0.0, a9 = 0.0;   
var twothirds = 2.0/3.0;
var alt = -1.0;
var k2 = 0.0, k3 = 0.0, sk = 0.0, ck = 0.0;

for (var k = 1; k <= n; k++){
   k2 = k*k*1.0;
   k3 = k2*k;
   sk = Math.sin(k*1.0);
   ck = Math.cos(k*1.0);
   alt = -alt;

   a1 += Math.pow(twothirds,k-1.0);
   a2 += Math.pow(k*1.0,-0.5);
   a3 += 1.0/(k*(k+1.0));
   a4 += 1.0/(k3 * sk*sk);
   a5 += 1.0/(k3 * ck*ck);
   a6 += 1.0/k;
   a7 += 1.0/k2;
   a8 += alt/k;
   a9 += alt/(2*k -1);
}
printString(a1.toFixed(9) + "\t(2/3)^k");
printString(a2.toFixed(9) + "\tk^-0.5");
printString(a3.toFixed(9) + "\t1/k(k+1)");
printString(a4.toFixed(9) + "\tFlint Hills");
printString(a5.toFixed(9) + "\tCookson Hills");
printString (a6.toFixed(9) + "\tHarmonic");
printString(a7.toFixed(9) + "\tRiemann Zeta");
printString(a8.toFixed(9) + "\tAlternating Harmonic");
printString(a9.toFixed(9) + "\tGregory");

