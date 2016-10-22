function C() {
   this.a = 3;
   var x = null;
   if (true) {
	   x = function() { return this.a; };
   } else {
	   x = function () { return 4; };
   }
   this.m = x;
}

var y = new C();
var z = y.m();

