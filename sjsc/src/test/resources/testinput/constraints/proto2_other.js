
function C(v) {
    this.x = v;
    this.n = function(){ return this.x * 20; };
}
C.prototype = { x : 3, 
                m : function(){ return this.x; },
                n : function(){ return this.x * 2; } };

var c = new C(19);
var s = C.prototype.m().toString()
console.log(s);
s = c.m().toString()
console.log(s);

function D() {
    this.x = 1;
}
D.prototype = new C(19);
var d = new D();

console.log("d.x =");
console.log(string_of_int(d.x));

s = d.m().toString();
console.log(s);
s = d.n().toString();
console.log(s);
