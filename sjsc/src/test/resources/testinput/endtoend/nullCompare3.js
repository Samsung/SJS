function C() {
    this.root = { a : 3};
}

C.prototype.foo = function (p) {
    var t = p || this.root;
    console.log ( t.a + "" );
}
var c = new C();
c.foo(null);
c.foo({ a : 4});

