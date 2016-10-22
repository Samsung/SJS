function C() {
	this.f = null;
}
C.prototype = { g: true };

function D() {
	this.f = null;
}
D.prototype = { g: false };

var a = new C();
var b = new D();
var o = a;
o = b;
o.f = o;