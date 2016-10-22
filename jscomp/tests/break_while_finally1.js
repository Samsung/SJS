
var x = 1;

for (var i=0; i<3; i++) {
    try {
        break;
    } finally {
        x = 2;
    }
    x = 3;
}
