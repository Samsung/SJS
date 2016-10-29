// supertype { foo: integer, bar: integer } -> { foo: integer }
function p(x) { return { foo: x.foo + x.bar }; }

// subtype { foo: integer } -> { foo: integer, bar: integer }
// NOTE: with our current lack of function subtyping, we
// infer the same type as p for this function
function q(x) { return { foo: x.foo, bar: x.foo }; }

function g(f,x) { return f(x); }

printInt(g(p, {foo: 3, bar: 4}).foo);
printInt(g(q, {foo: 5, bar: 7}).foo);

