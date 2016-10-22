
var x = 0.0;
var y = 1.0;
console.log(x.toString());
console.log(y.toString());

x += y;
console.log(x.toString());
console.log(y.toString());

x -= y;
console.log(x.toString());
console.log(y.toString());

x = y + y;
console.log(x.toString());
console.log(y.toString());

x = y * y + y;
console.log(x.toString());
console.log(y.toString());

var o = { f : 9.0 };

x = o.f;
console.log(x.toString());
console.log(y.toString());

o.f = y;
console.log(x.toString());
console.log(y.toString());

o.f += 3.0;
console.log(o.f.toString());
