function B() {
	this.a = 1;
} 
B.prototype = { f: function (x) { this.a = x; }}; 
var b = new B();
b.f(3); // OK
function C() {
	
}
C.prototype = new B(); 
var c = new C(); // BAD missing RW field a
c.f(4); 
