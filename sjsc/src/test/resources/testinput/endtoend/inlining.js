

// Test inlining optimization

var CONST = 4;

var mut = 9;

function f() {
    mut += CONST;
}

f();
f();
f();
f();

console.log(mut.toString());

// The generated C for this switch (currently) won't compile if inlining fails.
// Eventually it will, since long-term we do need to desugar switch statements with
// non-constant cases into conditionals.
switch (mut) {
    case CONST:
        console.log("Inlining worked if this compiles");
        break;
    default:
        console.log("...");
}
