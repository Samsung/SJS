// The Great Computer Language Shootout
// http://shootout.alioth.debian.org/
//
// contributed by Isaac Gouy

var n = 10;
var str = new String("");
while(n--){ str += "hello\n"; }
console.log("" + str.length); // FT: coercions not yet supported 
