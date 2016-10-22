
x = 1;
var x = 2;

function f1() {
    var x = 3;

    function f2() {
        var x = 4;

        function f3() {
            var x = 5;

            console.log(x);
            delete x;
            console.log(x);
            delete x;
            console.log(x);
            delete x;
            console.log(x);
        }
        return f3;
    }
    return f2();
}

f1()();
