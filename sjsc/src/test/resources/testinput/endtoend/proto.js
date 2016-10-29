
function C(v) {
    this.x = v;
}
C.prototype = { x : 3, m : function(){ return this.x; } };

var c = new C(19);
var s = C.prototype.m().toString()
console.log(s);
s = c.m().toString()
console.log(s);
