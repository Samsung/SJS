var x = { p : 4 };
var a = { foo: x , bar : 5 };
var t = a.foo;
var s = t.p;

// exercises "opening" up of object type term constructor, but the left to right flow needs to also do
// another opening up