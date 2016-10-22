
var x = 0;

function f1(a, b) {
    var c = 3, d = 4;

    function f2(e, f) {
        var g = 7;

        console.log(a);
        console.log(b);
        console.log(c);
        console.log(d);
        console.log(e);
        console.log(f);
        console.log(g);
        console.log(x);
    }

    f2(5, 6);
}

f1(1,2);
