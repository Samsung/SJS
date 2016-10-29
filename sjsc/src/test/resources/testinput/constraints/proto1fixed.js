function T1() {
    this.a = 42;
}

function T2() {
    this.b = false;
}

T2.prototype = new T1();

var t2 = new T2();

console.log(t2.a + "");

