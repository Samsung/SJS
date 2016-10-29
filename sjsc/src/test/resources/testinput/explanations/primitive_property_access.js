// On line 5: assignment to a property (which actually exists) of a primitive
// type.

var x = 1;
x.toString = function() { return "nope"; }
x += 10;
x += 11;
