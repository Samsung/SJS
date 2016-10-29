function C() {
  this.f = 0;
}
C.prototype.m1 = function () { return this.f+1; };
C.prototype.m2 = function () { return this.f-1; };

var x = new C();
printInt(x.m3()); // ERROR
