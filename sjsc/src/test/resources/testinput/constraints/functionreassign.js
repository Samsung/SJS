function foo(x) {
  if (x == 0) return 0;
  return foo(x-1) + 2;
}

var g = foo;
foo = function (p) { return p; }
console.log(g(2)+"");