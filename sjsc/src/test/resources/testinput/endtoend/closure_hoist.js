

var x = f();

// Because this is a function declaration, this closure allocation and binding should be hoisted
function f() {
    return "hello";
}

console.log(x);
