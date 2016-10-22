function foo(x) {
	x.a = 3;
}

var a = { a : 1, f: function (x) { foo(this); }}; // MRW includes a

