// The literal {fun:f} below is abstract (the f method writes to a "b" field
// which is not present).

var f = function() { this.b = 10; };
({ fun : f }).fun();
