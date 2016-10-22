function getFoo(p) {
  if ("foo" in p) {
    return p.foo;
  }
  return "default";
}
