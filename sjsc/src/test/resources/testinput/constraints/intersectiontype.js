function foo(p) {
  var x = p(3);
  var y = p(false, true);
}

foo(Array);