function AnimalProto() {
  this.move = function(meters) {
    console.log(this.name + " moved " + meters + "m.");
  }
}
function Animal(theName) {
  this.name = theName;
}
Animal.prototype = new AnimalProto();
function HorseProto() {
  this.eatApples = function(numApples) {
    console.log(this.name + " ate " + numApples + " apples.");
  }
}
HorseProto.prototype = Animal.prototype;
function Horse(name) {
  this.name = name;
}
Horse.prototype = new HorseProto();
var x = new Horse("Ed");
x.eatApples(3);
x.move(4);