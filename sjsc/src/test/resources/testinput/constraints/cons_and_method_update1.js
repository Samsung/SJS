
function O1 () {
    this.a = 2;
    this.b = false;
    this.m1 = function (x) { this.a = x; };
}

var o1 = new O1();

console.log(o1.a + "");

o1.m1(3);

console.log(o1.a + "");

o1.m1 = function (x) { this.a = 2 * x; };

o1.m1(3);

console.log(o1.a + "");