var o1 = {
  a : 1,
  f : function (x) {this.a = x;},
  g : function () {this.b = this.a;}
}
o1.f(3);
// Why reject? o1 is an abstract type, because b is not there.
// Although f can run fine, to make things easy we just prevent
// any method calls on objects that are abstract.
