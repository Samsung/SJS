function C() {
	this.a = 3;
}
// prototype is abstract
C.prototype = {
		get_a: function m() {
			return this.a;
		}
};

var c = new C();
c.get_a();