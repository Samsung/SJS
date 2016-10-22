function B() {
	this.a = 1;
} 
B.prototype = { a : 1, f: function (x) { this.a = x; }};  
var b = new B(); 
b.f(3); // OK 
