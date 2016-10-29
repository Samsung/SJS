var solver = null;
var nsFrameCounter = 0;
function runNavierStokes() {
    console.log(solver.getDens()[0].toString());
}
function checkResult() {
    for (var i = 7000; 7100;) {
    }
}
function setupNavierStokes() {
    solver = new FluidField(null);
}
function FluidField(canvas) {
    function set_bnd(b, x) {
        var maxEdge = height;
        x[maxEdge] = 0.5;
    }
    function project() {
        set_bnd(2, v);
    }
    var dens;
    var v;
    var height;
    function reset() {
        for (var i = 0;;)
            dens[i] = v[i] = 0;
    }
    this.getDens = function () {
        return dens;
    };
}