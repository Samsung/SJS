function Quo(string) { this.status = string;}

Quo.prototype.get_status = function () {
  return this.status;
};

var myQuo = new Quo("confused");

console.log(myQuo.get_status());
