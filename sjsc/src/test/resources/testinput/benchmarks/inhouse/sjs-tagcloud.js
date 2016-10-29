


//JSonVal ::=
//   int tag;
//   Array<JSonVal>
//   Map<JSonVal>
//   int value;



var Constants = {
   ARRAY : 0,
   MAP : 1,
   INT : 2,
   STR : 3
}

function JSONVal() {
    this.tag = Constants.ARRAY;
    this.a = null;
    this.m = null;
    this.intval = 0;
    this.strval = "";
}

function f(a,b) {
  if (a === "popularity") {
    console.log("Here!");
    return createIntJSONval(b.intval - 1);
  } else { return b; }}

function transform(s, filter) {

  // function walk(k, v) {
  //     var i, n;
  //     if (v && typeof v === 'object') {
  //         for (i in v) {
  //             console.log(i);
  //             if (Object.prototype.hasOwnProperty.apply(v, [i])) {
  //                 n = walk(i, v[i]);
  //                 if (n !== undefined) {
  //                     v[i] = n;
  //                 }
  //             }
  //         }
  //     }
  //     return filter(k, v);
  // }

  function walk(k, v) {
    var i;
    var j;
    var n;
    switch (v.tag) {
      case Constants.INT:
      case Constants.STR:
        break;
      case Constants.MAP:
        for (var i in v.m) {  // 'var' is needed in SJS - BUG?
          console.log(i);
          n = walk(i, v.m[i]);
          if (n !== undefined) {
            v.m[i] = n;
          }
        }
        break;
      case Constants.ARRAY:
        for (j = 0; j < v.a.length; j++) {
          console.log(j+"");
          n = walk(j+"",v.a[j]); // convert j to string
          if (n !== undefined) {
              v.a[j] = n;
          }
        }
        break;
    }
    return filter(k,v);
  }

   if (filter != null) {
     return walk("", s);
   }
   else { return s;}
 };

function createStringJSONval(s) {
    var j = new JSONVal();
    j.strval = s;
    j.tag = Constants.STR;
    return j;
}

function createIntJSONval(i) {
    var j = new JSONVal();
    j.intval = i;
    j.tag = Constants.INT;
    return j;
}

function createMapJSONval(m) {
   var j = new JSONVal();
   j.m = m;
   j.tag = Constants.MAP;
   return j;
}

function createArrayJSONval(a) {
  var j = new JSONVal();
  j.a = a;
  j.tag = Constants.ARRAY;
  return j;
}

var m1 = createMapJSONval({ "tag" : createStringJSONval("hello"),
                            "popularity" : createIntJSONval(400)});

var a1 = createArrayJSONval([m1,m1]);

var r = transform(a1, f);
