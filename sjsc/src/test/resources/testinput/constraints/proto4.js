var x = { f: function() { return 3; }};

function Foo() {}

Foo.prototype = x;

printInt(Foo.prototype.f());

