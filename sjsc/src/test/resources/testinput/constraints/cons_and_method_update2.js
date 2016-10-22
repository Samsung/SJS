
function O1 () {
    this.a = 2;
    this.b = 3;
    this.f = function (x) { this.a = x; };
}

var o1 = new O1();

var g1 = function (x) { this.a = x; };

var g2 = function (y) { this.b = y; };

var g = g1;
g = g2;

o1.f = g;


