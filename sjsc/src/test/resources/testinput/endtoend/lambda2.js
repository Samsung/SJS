var x = 130;
assert(x == 130);
var y = 239;
assert(y == 239);
x = x * 2;
assert(x == 260);
var b = true;
assert(b);
assert((function f(){ return (b ? x : y); })() == 260);
