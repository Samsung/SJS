

L2: for (var j=0; j<3; j++ ) {
    L1: for (var i = 0; i < 10; i++) {
        x = i;
        break L2;
        x++;
    }
    console.log(j);
}

console.log(x);