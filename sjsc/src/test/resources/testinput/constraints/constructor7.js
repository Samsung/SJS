function C() {
  this.f = function() {
    this.g = "hello";
  }
}

var x = new C();
console.log(x.g + ""); // error: g does not exist on C
