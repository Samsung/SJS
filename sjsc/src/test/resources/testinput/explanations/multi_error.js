// Here x is used inconsistently: both as a string and as an int. It is used
// inconsistently repeatedly!

var x = 1;
var a = [1,2,3];

printInt(a[0]);
printInt(a[1]);
printInt(a[2]);
printInt(a[x]);
printInt(a[x]);
printInt(a[x]);
printInt(a[x]);
printInt(a[x]);
printInt(parseInt(x));
printInt(parseInt(x));
printInt(parseInt(x));
printInt(parseInt(x));
