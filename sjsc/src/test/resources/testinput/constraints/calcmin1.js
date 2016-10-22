function Frame() {
    this.is_frame = true;
    this.Construct = function() { console.log("Constructing... woo!"); };
}
function Application() {
    this.AddFrame = function(fr) { console.assert(fr.is_frame); fr.Construct(); }
}


function Calculator() {
    this.OnAppInitializing = function () {
        var frame = new Frame();
        frame.Construct();
        this.AddFrame(frame);
    };
}
Calculator.prototype = new Application();
