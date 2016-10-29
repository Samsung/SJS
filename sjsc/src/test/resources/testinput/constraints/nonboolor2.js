var x = { a : 3, b : 4 } === { b : 4, s : 5 };
//console.log(x.q + "");

var y = { a : 3, b : 4 } || { b : 4, s : 5 };

var o = void 0 || { f : 3 };
console.assert(o.f === 3);
printInt(o.f);

var p = { g : 4 } || void 0;
console.assert(p.g === 4);
printInt(p.g);
