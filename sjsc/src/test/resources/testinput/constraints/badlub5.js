var a = { f: null, g: true };
var b = { f: null, g: false };
var o = a;
o = b;
o.f = o;