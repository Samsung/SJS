


function f1(a, b) {
    console.log(a);

    return function(d) {
        console.log(arguments);
        console.log(b);
        console.log(d);
    }

}

f1();

