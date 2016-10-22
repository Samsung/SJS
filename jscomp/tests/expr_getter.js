
var obj = {
    baz: "bar",
    set foo (v) { console.log("setter"); this.baz = v; return "world";}
};

var x = obj.foo = "hello";

console.log(x);


