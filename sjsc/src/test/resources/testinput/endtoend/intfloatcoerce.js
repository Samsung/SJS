
var x = new Array(1);
x[0] = 1.0;
x[0] = 0;

console.assert(x[0] == 0.0);


var o = { f : 1.0 };
o.f = 0;

console.assert(o.f == 0.0);
