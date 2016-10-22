function C() {
  this.f = 0;
}
C.prototype = { m1: function () { return this.f+1; } };

function D() {
  this.f = 0;
}
D.prototype = new C();
// override m1 method
D.prototype.m1 = function() { return this.f-1; };