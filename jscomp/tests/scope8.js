




function f1(a) {
    var arguments;
    try {
        throw "x";
    } catch (arguments) {
        console.log(arguments);
    }
    console.log(arguments);
}

f1(3);
