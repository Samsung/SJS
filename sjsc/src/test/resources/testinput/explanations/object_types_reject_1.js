
function AF() { this.seats = 300; }

AF.prototype = {
	    airline : "Oceanic",
	    number : 815
	};


var another_flight = new AF();

console.log(another_flight.airline);

another_flight.seats = 350;

another_flight.number = 850; // SJS type failure: number should be a RO property

