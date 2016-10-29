function B() {
	this.g = 4;
}
B.prototype = { f: 3 };
var b = new B();
var c = b.f;
b.f = 7; // REJECT since this is a read-only property