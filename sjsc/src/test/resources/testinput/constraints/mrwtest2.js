function B() {
	this.b = 1;
} 
B.prototype = { a : 1, f: function (x) { this.a = x; }}; // MRW includes a 
var b = new B(); // BAD: B is abstract, since it does not have an RW a property
b.f(3); 
