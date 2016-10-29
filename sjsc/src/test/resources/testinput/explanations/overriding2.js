function C() {
    this.m = function(q) { printString(q); };
}
C.prototype = { m : function(p) { console.log(string_of_int(p)); } };;
