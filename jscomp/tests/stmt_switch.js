
var x = 10;

function f() {
    console.log("f");
    return 4;
}

switch(x) {
    case 2:
    case 3:
        console.log(23);
        y = 1;
    case f():
        console.log(4);
        y = 4;
        break;
    default:
        console.log("default");
        y = 7;
    case 7:
        console.log(7);
        y = 9;
}

console.log(y);
