function B() {
	this.g = 4;
}

B.prototype = { f: 3 };

var b = new B(); // RO f, RW g
var c = { g: 5, f: 7, h: 8 }; // RW f,g,h
var d = false ? c : b;
// d's type should be // RO: f, RW: g