function C() {}

C.prototype.get_str = function () {
  return "hello";
};

var myC = new C();

console.log(myC.get_str());
