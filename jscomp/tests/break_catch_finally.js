
while (true) {
    L1: try {
        throw "Hello";
    } catch (e) {
        console.log("caught exception");
        break L1;
        console.log("after catching exception");
    }
    //finally {
    //    try {
    //        console.log("Enter finally");
    //        break;
    //        console.log("Exit finally");
    //    } finally {
    //        console.log("Enter finally 2");
    //        break;
    //        console.log("Exit finally 2");
    //    }
    //    console.log("after try 2");
    //
    //}
    console.log("after try");
}