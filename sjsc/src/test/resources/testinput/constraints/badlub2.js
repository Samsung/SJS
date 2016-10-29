var a = { check : function(x) { return x == 0 || this.val2 != null; }, val2 : "hello" };
var b = { check : function(x) { return this.val2 > x; }, val2 : 9.2 };

var o = a;
console.assert(o.check(0));
o = b;
console.assert(o.check(0));