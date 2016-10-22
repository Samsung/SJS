

function C() {
    this.x = 9;
}
C.prototype.showX = function() { console.log(this.x.toString()); }

var a = new C();
a.showX();
