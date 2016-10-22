// The Great Computer Language Shootout
// http://shootout.alioth.debian.org/
//
// contributed by Ian Osgood

function lA(i,j) {
  return 1/((i+j)*(i+j+1)/2+i+1);
}

function lAu(u,v) {
  for (var i=0; i<u.length; ++i) {
    var t = 0;
    for (var j=0; j<u.length; ++j)
      t += lA(i,j) * u[j];
    v[i] = t;
  }
}

function lAtu(u,v) {
  for (var i=0; i<u.length; ++i) {
    var t = 0;
    for (var j=0; j<u.length; ++j)
      t += lA(j,i) * u[j];
    v[i] = t;
  }
}

function lAtAu(u,v,w) {
  lAu(u,w);
  lAtu(w,v);
}

function spectralnorm(n) {
  var i, u=[], v=[], w=[], vv=0, vBv=0;
  for (i=0; i<n; ++i) {
    u[i] = 1; v[i] = w[i] = 0;
  }
  for (i=0; i<10; ++i) {
    lAtAu(u,v,w);
    lAtAu(v,u,w);
  }
  for (i=0; i<n; ++i) {
    vBv += u[i]*v[i];
    vv  += v[i]*v[i];
  }
  return Math.sqrt(vBv/vv);
}

var total = 0;

for (var i = 6; i <= 48; i *= 2) {
    total += spectralnorm(i);
}

var expected = 5.086694231303284;

if (total != expected)
    console.log("Error");
//    throw "ERROR: bad result: expected " + expected + " but got " + total;

