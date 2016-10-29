function D(a) {
  this.a = undefined;
  this.init(a);
}

D.prototype.init = function (a) {
/* actual initialization, which could be complex */
    this.a = a;
}

var d = new D(3);

console.log("" + d.a);
