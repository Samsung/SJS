function C() {
	this.a = 3;
	this.b = false;
	this.p = function m() {
		return this.a; // the type of this should not refer to b
	}
}