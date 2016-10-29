function C() {
  this.f = 0;
  this.m1 = function () { return this.f+1; } ;
}
// C.prototype = { m1: function () { return this.f+1; } };

function D() {
  this.f = 0;
}
D.prototype = new C();
// override m1 method
D.prototype.m1 = function() { return this.f-1; };
var x = new D();
printInt(x.m1());

function E() {
	  this.f = 0;
}
E.prototype = { m1: function () { return this.f+1; } };
function E2() {
	this.m1 = function() { return this.f-1; };
}
E2.prototype = new E();
function F() {
	this.f = 0;
}
F.prototype = new E2();

var y = new F();
printInt(y.m1());
