// creates nested object union types
// statements are in a weird order to force
// badness in the constraint solver

var x = new E(o);
var y = new F(p);
var z = x;
z = y;

function E(w) {
	this.f = w;
}
E.prototype = new C();

function F(w2) {
	this.f = w2;
}
F.prototype = new D();


function C() {}
C.prototype = { g: true };

function D() {}
D.prototype = { g: false };

var a = new C();
var b = new D();
var o = a;
o = b;
var c = new C();
var d = new D();
var p = c;
p = d;


	