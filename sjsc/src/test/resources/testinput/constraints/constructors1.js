// Test basic support for object construction via constructor, without methods or inheritance

function C() {
    //C.instance = this;
    this.a = 3;
    this.b = "asdf";
}
var x = new C();

assert (x.a == 3);
x.a = 9;
assert (x.a == 9);
console.log(x.b);
