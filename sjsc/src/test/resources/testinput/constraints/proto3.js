var p = { x : 3, m : function(){ return this.x; } };
function C(v) {
    this.x = v;
}
C.prototype = p;

var c = new C(19);
var s = p.m().toString()
console.log(s);
s = c.m().toString()
console.log(s);