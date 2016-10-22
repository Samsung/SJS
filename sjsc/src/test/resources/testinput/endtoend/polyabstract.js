function O1 () {
  this.m = function (x) { this.a = x; }
}

function O2 () {
  this.a = 2;
}
O2.prototype = new O1();
var o2 = new O2();
 
o2.m(3);

function O3() {
  this.a = "foo";
}
O3.prototype = new O1();

var o3 = new O3();
o3.m("bar");

