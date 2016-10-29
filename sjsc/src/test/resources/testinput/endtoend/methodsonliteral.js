
var o = { x: 3, setX : function(y) { this.x = y; },
                getX : function() { return this.x; } };

o.setX(19);
printInt(o.getX());
