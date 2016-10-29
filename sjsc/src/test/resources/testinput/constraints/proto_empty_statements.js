function C() {
};
C.prototype = { g: 3, m: function() { return this.g; }};;;
C.prototype.m = function() { return this.g+1; };;