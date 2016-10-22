var x =  { f: 3, m: function() { return this.f; }};

var y = x;
y.m = function() { return this.g; };

x.m();
