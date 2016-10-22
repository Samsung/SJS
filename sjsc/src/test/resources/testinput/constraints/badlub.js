

var a = { f: 3, val : "hello" };
var b = { f: 6, val : 9.2 };

// Test that if two objects have a common property name, then we forget about the property during
// subtyping, rather than equating them and throwing an error
var o = a;
printInt(o.f);
o = b;
printInt(o.f);
