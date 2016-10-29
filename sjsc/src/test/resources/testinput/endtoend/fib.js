// Use Wontae's unification hint system
var intHint = 0;

// Currently must explicitly perform declaration in order to save a closure allocation
var fib = function fib(x) {
    intHint = x; // How do we recognize and optimize these away in the backend?
    if (x <= 2) {
        return 1;
    } else {
        return fib(x-1) + fib(x-2);
    }
}

assert(fib(1) == 1);
assert(fib(2) == 1);
assert(fib(3) == 2);
assert(fib(4) == 3);
assert(fib(5) == 5);
