

function f1(a, b) {
    console.log(a);
    console.log(arguments[0]);
    console.log(arguments[1]);
    console.log(arguments[2]);
    console.log(arguments[3]);
    console.log(++arguments[0]);
}

f1(1,2,3);
