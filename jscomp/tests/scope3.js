



var e = 3;

function f1() {
    var e = 2;
    console.log(e);
    try {
        throw new Error("x");
    } catch (e) {
        console.log(e);
        return function() {
            console.log(e);
        }
    } finally {
        console.log(e);
    }
}
