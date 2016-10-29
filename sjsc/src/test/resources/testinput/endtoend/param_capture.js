function f(x) {
    return function() { return x; };
}

var z = f({ x : 3})();
