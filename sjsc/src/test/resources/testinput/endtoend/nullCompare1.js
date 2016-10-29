function f(p) {
    var t = p || 'hello';
    if (t == 'white')
        t = 'black';
    return t;
}
f(null);
