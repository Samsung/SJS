var x = { a: 5, b: 3 };

function foo() {
	return { a: 7, c: 25 };
}

var y = foo();
y = x;
var z = y.a;