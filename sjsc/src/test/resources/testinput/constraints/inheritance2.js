function E() {
	  this.f = 0;
}
E.prototype = { m1: function () { return this.f+1; } };
function E2() {
	this.m1 = function() { return this.f-1; };
	this.m2 = function(p) { this.f = p; };
}
E2.prototype = new E();
function F() {
}
F.prototype = new E2();

var y = new F();
printInt(y.m1());
