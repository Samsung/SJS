
console.log("hello, world");

var o = { a : 3, b : true };
if (o.b) {
    console.log("b true");
} else {
    console.log("b false");
}

console.assert(o.a === 3);

function f() {
    //console.log("running in f");
    return "hello from f";
}

console.log(f());
