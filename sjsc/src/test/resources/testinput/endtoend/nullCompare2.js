function isNull(p) {
    return p == null;
}

function f(p) {
    var t = isNull(p) ? 'hello' : p;
    if (t == 'white')
        t = 'black';
    return t;
}
f(null);
