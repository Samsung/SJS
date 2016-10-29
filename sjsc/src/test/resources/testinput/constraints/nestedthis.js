var count = 3;
var o = {
    v1: { a1 : 2 },
    run: function () {
        this.v1.a1 = count;
    }
};
o.run();