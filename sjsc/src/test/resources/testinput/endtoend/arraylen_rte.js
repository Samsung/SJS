
var w = {
  a : null,

  f : function() {
      var tmp;

      if (false) {
        this.a = [];
      }

      if (true) {
         tmp = this.a;
      } else {
         tmp = [1];
      }

      return tmp.length;
  }
}

console.log(w.f() + "");