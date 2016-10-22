
// Fails on f8c2af521c4bba597e507b9311c8ed630629e606
// You can easily compare it with what node does:
//   $ ./sjsc test2.js && ./a.out
//   $ node test2.js

var a = {
    af : 0,
    fire : function() { ++this.af; return "hello"; },
};

var b = {
    bf : 0,
    fire : function() { --this.bf; return "goodbye"; },
};

function f(x) { }
f(a);
f(b);

// b.bf === 0

b.fire = a.fire;
console.log(b.bf.toString());
console.log(b.fire());

// EXPECTED:
// b.bf === 0

// ACTUAL:
// b.bf === 1

console.log(b.bf.toString());