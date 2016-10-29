
function ack(m, n) {
  return (m == 0
    ? n + 1
    : (n == 0
      ? ack(m - 1, 1) 
      : ack(m - 1, ack(m, n - 1))));
}

var n = 7;  
//TyHint.int = n; 
printString("ack(3, ");
printInt(n); 
printString("): ");
printInt(ack(3, n));