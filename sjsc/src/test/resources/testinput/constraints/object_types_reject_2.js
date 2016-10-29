
var flight = {
    airline : "Oceanic",
    number : 815
};


var beget = function (o) { // in SJS, we should not be able to give a type to this function
                           // because it is parametrized over prototypes
    var F = function () {};
    F.prototype = o;
    return new F();
};

var another_flight = beget(flight);

console.log(another_flight.airline);
