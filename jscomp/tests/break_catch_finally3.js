
while (true) {
    L1: try {
        throw "Hello";
    } catch (e) {
        console.log("caught exception");
        break L1;
        console.log("after catching exception");
    } finally {
        console.log("Inside finally");
    }
    console.log("after try");
}