


function f1(a) {
    try {
        throw "x";
    } catch (arguments) {
        console.log(arguments);
    }
}

f1(3);
