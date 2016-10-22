function C() {
    this.name = "C"
}
C.prototype.who = function () { console.log(this.name)};

/* glue code */
function C1Inheritor() {
    /* NOTE: must list all the methods defined at C1 level */
    this.what = null;
}
C1Inheritor.prototype = C.prototype;

/* C1 subclasses C */
function C1() {
    this.name = "C1"; /* need to redeclare constructor stuff */
    this.job = "salaried";
}
C1.prototype = new C1Inheritor();

C1.prototype.what = function() {
  console.log(this.job)
};

var c1 = new C1();
c1.who();
c1.what();
