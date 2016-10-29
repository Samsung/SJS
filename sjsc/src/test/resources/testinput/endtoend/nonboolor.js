// strings
var x = void 0 || "hello";
//var x = null || "hello";
console.log(x);
var y = "world" || void 4;
console.log(y);

var i = void 0 || 32;
console.log("Checking <undef> || <int>");
console.assert(i === 32);
printInt(i);
var j = 59 || void 0;
console.log("Checking <int> || <undef>");
console.assert(j === 59);
printInt(j);


var o = void 0 || { f : 3 };
console.assert(o.f === 3);
printInt(o.f);
var p = { g : 4 } || void 0;
console.assert(p.g === 4);
printInt(p.g);

var f = void 0 || function() { console.log("tada!"); };
f();
f = function() { console.log("tada!"); } || void 0;;
f();

//var arr = void 0 || [3];
//printInt(arr[0]);
//arr = [5] || void 0;
//printInt(arr[0]);
