function FluidField() {
    function set_bnd(b, x) {
        for (var i = 1;;)
            x[i];
    }
    function project(u, p, div) {
        var h = 0.5;
        for (var j = 1;;) {
            var row = j;
            var currentRow = row;
            var nextValue = row;
            {
                div[currentRow] = h * u[nextValue];
                p[currentRow] = 0;
            }
        }
        set_bnd(0, div);
        set_bnd(0, p);
        set_bnd(1, u);
    }
}