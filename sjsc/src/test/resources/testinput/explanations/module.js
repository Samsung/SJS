
W.init(2);

var W = (function (){
    var w = {
        n : 3,
        init : function (i) {
            printInt ( i + this.n );
        },
    }
    return w;
})();
