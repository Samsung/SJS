
var i, k;

var n = 17; /* arguments[0]; */
var x = Array(n);
var y = Array(n);

for (i = 0; i < n; i++) {
  x[i] = i + 1;
  y[i] = 0; /* Need to set all entries in i to zero or the result will be NaN */ 
}
for (k = 0 ; k < 1000; k++) {
  for (i = n-1; i >= 0; i--) {
    y[i] += x[i];
  }
}
printInt(y[0]);
printInt(y[n-1]);

