// On line 5, there is no "y" field on x.

var x = { x : null, a : 1 };
x.x = x;
var y = x.x.x.x.y;
printInt(x.x.x.x.a);
