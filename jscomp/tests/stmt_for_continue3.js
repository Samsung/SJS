
x = 0;

L2: for (var j=0; j<33; j++ ) {
    L1: for (var i = 0; i < 10; i++) {
        x++;
        continue L2;
        x = 0;
        console.log(x);
    }
    console.log(x);
}
console.log(x);
