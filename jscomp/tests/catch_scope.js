

gl = 2;

var sl = 1;
var f2, f3;

function f1() {
    var e = 1;
    var x = 2;
    console.log(z);
    try {
        throw "Exception 1";
    } catch (e) {
        console.log(e);
        var x = 3;
        var z = 4;
        console.log(x);
        try {
            throw "Exception2";
        } catch(e) {
            return function(){
                console.log(e);
                console.log(sl);
                try {
                    throw "Exception 3";
                } catch(e) {
                    f3 = function() {
                        console.log(e);
                        console.log(arguments[0]);
                    }
                }
            }
        }
    }
    console.log(z);
    console.log(x);
    console.log(e);
    f2 = function() {
        gl2 = 1;
        console.log(e);
        console.log(gl);
        try {
            throw "Exception 4";
        } catch(e) {

        }
    }
}
console.log(sl);

f1()();
