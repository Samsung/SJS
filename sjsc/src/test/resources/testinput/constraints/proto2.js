function T1() {
    this.a = 42;
}

var t1 = new T1();

function T2() {
    this.b = 17;
}

T2.prototype = t1;

var t2 = new T2();

function T3() {
  this.c = "foo";
}

T3.prototype = t2;

var t3 = new T3();

console.log(t3.a + t3.b + "");

