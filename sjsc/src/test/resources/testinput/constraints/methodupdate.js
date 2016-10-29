function C () {
  this.m1  = function (x){ this.a = 10; }
  this.a = 10;
}
function D() {
	this.a = 0;
}
D.prototype = new C();
D.prototype.m1 = function (x) { this.a = 2 * x; }; 

var o1 = new D();
o1.m1(3);