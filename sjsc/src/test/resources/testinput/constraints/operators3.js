var z = [3];
var rowSize;
function f(x, y) {
	return z[(x + 1) + (y + 1) * rowSize];
}


function g(a) {
	// we should infer that a is an int
	var t = a - 7;
	printInt(t);
}
