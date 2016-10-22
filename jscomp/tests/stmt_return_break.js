

function foo(x) {
    L1: for(var j=0; j<3; j++) {
        for (var i = 0; i < 3; i++) {
            try {
                if (x>0) {
                    return x;
                } else if (x==0) {
                    break L1;
                } else {
                    throw "Hello";
                }
            } catch (e) {
                return x + e;
            } finally {
                x = 3;
            }
        }
    }
}

console.log(foo(-1));
