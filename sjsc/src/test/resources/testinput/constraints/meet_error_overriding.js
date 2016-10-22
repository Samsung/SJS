// like overriding2.js but with more line breaks to make error message clearer
function C() {
    this.m = function(q) { 
    	printString(q); 
    };
}

C.prototype = { m : function(p) { 
	console.log(string_of_int(p)); 
} };
