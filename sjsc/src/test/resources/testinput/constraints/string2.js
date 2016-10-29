
var x = String();
console.log(x + "!"); // !

var y = String("hello");
console.log(y); // hello

var z = String.fromCharCode(65);
console.log(z); // A

var s = String;

var p = s("foo");
console.log(p); // foo

var q = s.fromCharCode(66);
console.log(q); // B
