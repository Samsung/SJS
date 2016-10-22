

function f1(a, b) {
    console.log(a);
    console.log(arguments[1]);

    arguments[1]++;
    return function f2() {
        console.log(b);
    }

}

f1(1,2)();
