
var x;

x = 2;

console.log(x);
//
//
function foo() {
    var a = 9, b = 3;

    a = 5;

    function g() {
        b = 3;
        return b;
    }

    return a + g();
}

console.log(foo());
