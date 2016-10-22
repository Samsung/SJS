// A very simple case of incompatible types. While these sorts of examples are
// usually ambiguous, the call to printInt suggests that x is more likely to be
// an int. The error is the assignment "x=hello on line 7.

var x;
x = 1;
x = "hello";
printInt(x);
