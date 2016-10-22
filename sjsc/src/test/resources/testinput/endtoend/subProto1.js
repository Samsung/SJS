var x = { a: 3, b: 4};


function Foo() {
	
}

Foo.prototype = { a : 5 };

var z = new Foo();

function fizz(p) {
	return p.a;
}

fizz(z);
fizz(x);
