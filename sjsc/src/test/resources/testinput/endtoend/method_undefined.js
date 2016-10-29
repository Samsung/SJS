function C() {
    this.name = "C"
}
C.prototype.who = function () { console.log(this.name)};

function C1Inheritor() {
    /* must list all the methods defined at C1 level */
    this.what = undefined; //function () {};
}
C1Inheritor.prototype = C.prototype;

function C1() {
    this.name = "C1";
    this.job = "salaried"
}
C1.prototype = new C1Inheritor();

C1.prototype.what = function() { console.log(this.job)};

var c1 = new C1();
c1.who();
c1.what();
