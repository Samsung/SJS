var y = { foo: 3, bar: { fizz: 4} };
var z = { foo: 7, bar: { baz: 33 } };
// tests more complex assignments
z.foo = y.foo;
z.bar.baz = y.foo;
z.bar.baz = y.bar.fizz;
