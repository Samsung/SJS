

var x = f();

// Because this isn't a function declaration, this closure isn't hoisted
var f = function () {
    return "hello";
}

console.log(x);

