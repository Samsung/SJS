

L1: for(var i=0; i<3; i++) {
    console.log("for 1 b");
    L2: for(var j=0; j<3; j++) {
        console.log("for 2 b");
        try {
            console.log("try b 1");
            break L1;
            console.log("try e 1");
        } finally {
            console.log("finally b 1");
            continue L2;
            console.log("finally e 1");
        }
        console.log("for 2 e");
    }
    console.log("for 1 e");
}