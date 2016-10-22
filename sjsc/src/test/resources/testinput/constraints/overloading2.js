var n1 = Number();
console.log(n1);      // if called with zero arguments, returns 0

var n2 = Number(true);
console.log(n2);      // if called with boolean, returns 1 or 0 

var n3 = Number("33.44");
console.log(n3);          // if called with string, returns a number (float or int) if the string represents a valid number
var n4 = Number("3e4");
console.log(n4);          // prints 30000
var n5 = Number("33apples");
console.log(n5);      // … or NaN if a string argument is given that does not represent a valid number   
