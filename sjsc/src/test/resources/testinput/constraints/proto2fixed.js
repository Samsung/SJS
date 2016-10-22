function T1() {
    this.a = 42;
}

function T2() {
    this.b = 17;
}

T2.prototype = new T1();

function T3() {
  this.c = "foo";
}

T3.prototype = new T2();

var t3 = new T3();

console.log(t3.a + t3.b + "");

