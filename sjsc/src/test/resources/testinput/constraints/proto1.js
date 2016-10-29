function T1() {
    this.a = 42;
}

var t1 = new T1();

function T2() {
    this.b = false;
}

T2.prototype = t1;

var t2 = new T2();

console.log(t2.a + "");

