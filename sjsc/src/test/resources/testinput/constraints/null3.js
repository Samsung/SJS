function isNull(q) {
    return q == null;
}

function f(p) {
    var t = isNull(p) ? 'hello' : p;
    if (t == 'white')
        t = 'black';
    return t;
}
print(f(null));

// question: why do we get |q|=|null| constraint?
// question: why don't we get string on the upper bound of |t|?
// TODO: add enough inference to get string for |p| directly from conditional expression