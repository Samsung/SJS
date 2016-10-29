function D(){
  this.m = function(y){ console.log(y); }
}

var x = new D();
x.m();