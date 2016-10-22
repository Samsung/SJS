function f(x) {
	return x;
}

function B() {
	
}
B.prototype = {g: function() { return this.a; }};
f(new B());