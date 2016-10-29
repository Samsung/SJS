function f(x) {
	return x;
}

f({g: function() { return this.a; }});