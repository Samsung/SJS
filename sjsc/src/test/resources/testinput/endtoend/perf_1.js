function A(v) {
    this.v = 0;
}

var sum = 0;

for(var i = 0; i < 10000; ++i) {
    for (var j = 0; j < 10000; ++j) {
        var a = new A(i);
        sum += a.v;
    }
}

console.log(sum+"");