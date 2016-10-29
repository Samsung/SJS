var a = { a : 1 }; 
function B() {
	this.b = 1;
} 
B.prototype = a; // BAD: only obj lit and new expressions are prototypal 
