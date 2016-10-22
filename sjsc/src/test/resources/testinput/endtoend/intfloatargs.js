var x = 1;
printInt(x);

function f(v) {
    printFloat(v);
}
f(x);
f(9.0);


var o = { f : 0.0, m : function(v) { printFloat(v); printFloat(this.f); } };
o.m(x);
o.m(9.0);


function C(v) {
    printFloat(v);
    this.f = v;
}
new C(x);
new C(9.0);

x = 22;
