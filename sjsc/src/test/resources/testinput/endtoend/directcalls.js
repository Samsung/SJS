
var x = "hello from f";
var f = function f() {
    console.log(x);
    return function g() {
        f();
    };
}

// Can't currently do f()() because code-gen will duplicate the calls to f() when expanding macro
// for closure invoke
var tmp = f();
tmp();
