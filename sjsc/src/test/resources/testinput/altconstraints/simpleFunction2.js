function f(x) { return x.foo; }

var y = { foo: 3, bar: false };
var z = f(y);

var v = { foo: 7, baz: 33 };
var w = f(v);

printInt(3);