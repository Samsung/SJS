var a = { f : 1, p: function m1(x) { this.f = x; }}; // MRW should include f

var b = { f : 1, p: function m2() { return this.f; }}; // MRO should include f

var c = { 
	f: 1, 
	p1: function m3(x) { this.f = x; }, 
	p2: function m4() { return this.f; } 
}; // MRW should contain f, not MRO

var d = { 
	f: 1, 
	g: 2, 
	p1: function m5(x) { this.g = x; }, 
	p2: function m6() { return this.f; } 
}; // MRW contains g, MRO contains f 