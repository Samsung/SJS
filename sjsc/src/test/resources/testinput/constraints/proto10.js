var x = { m: function M() { this.p = 7; }};

function N() {}
N.prototype = new x.m();

var y = new N();
console.log(y.p+"");