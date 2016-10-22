function id1(x) { return x; }
function id2(x) { return x; }
var y = id2(3);
function id3(x) { return x; }
var z = id3("hello");

var p = id1(id2);
var q = id1(id3);