function a(i,j) {
  return 1.0/((i+j)*(i+j+1.0)/2.0 +i+1.0); 
}

function au(u,v) {
  for (var i=0; i<u.length; ++i) {
    var t = 0.0;
    for (var j=0; j<u.length; ++j)
      t += a(i,j) * u[j];
    v[i] = t;
  }
}

function atu(u,v) {
  for (var i=0; i<u.length; ++i) {
    var t = 0.0;
    for (var j=0; j<u.length; ++j)
      t += a(j,i) * u[j];
    v[i] = t;
  }
}

function atAu(u,v,w) {
  au(u,w);
  atu(w,v);
}

function spectralnorm(n) {
  var i, u=[], v=[], w=[], vv=0.0, vBv=0.0;
  for (i=0; i<n; ++i) {
    u[i] = 1.0; v[i] = w[i] = 0.0; 
  }
  for (i=0; i<10; ++i) {
    atAu(u,v,w);
    atAu(v,u,w);
  }
  for (i=0; i<n; ++i) {
    vBv += u[i]*v[i];
    vv  += v[i]*v[i];
  }
  return Math.sqrt(vBv/vv);
}

var argument = 10000;
var x = spectralnorm(argument);
var y = x.toFixed(9);
console.log(y);
