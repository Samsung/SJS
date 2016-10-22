function C() {
    this.m = function(q) { console.log("hello"); };
}
C.prototype = { m : function(p) { console.log(string_of_int(p)); } };

new C().m(3);
