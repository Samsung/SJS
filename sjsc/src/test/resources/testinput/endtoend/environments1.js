function f() {
    return 3;
}

function g() {
    return f();
}

function h() {
    return g();
}

console.assert(h() == 3);
