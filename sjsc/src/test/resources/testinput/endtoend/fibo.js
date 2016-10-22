function fib(n) {
    if (n < 2) return 1;
    return fib(n-2) + fib(n-1);
}

var n = 17; //arguments[0];
printInt(fib(n));

