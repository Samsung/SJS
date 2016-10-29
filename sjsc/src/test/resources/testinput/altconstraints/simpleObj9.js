var x = { a: { b: 1, c: 1 } }; // object o1
var y = { a: { b: 1 } }; // object o2
var z = false ? x : y;
printInt(z.a.b);