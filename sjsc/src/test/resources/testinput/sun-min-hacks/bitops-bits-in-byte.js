// Copyright (c) 2004 by Arthur Langereis (arthur_ext at domain xfinitegames, tld com)


var result = 0;

// 1 op = 2 assigns, 16 compare/branches, 8 ANDs, (0-8) ADDs, 8 SHLs
// O(n)
function bitsinbyte(b) {
var m = 1, c = 0;
while(m<0x100) {
if(b & m) c++;
m = m << 1; // SJS
}
return c;
}

function timeFunc(func) {
var x, y; // t not used
var sum = 0;
for(var x=0; x<350; x++)
for(var y=0; y<256; y++) sum += func(y);
return sum;
}

var result = timeFunc(bitsinbyte);

var expected = 358400;
if (result != expected)
     console.error("Error")
//    throw "ERROR: bad result: expected " + expected + " but got " + result;

