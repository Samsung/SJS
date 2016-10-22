function ack(m, n) {
  return (m == 0
    ? n + 1
    : (n == 0
      ? ack(m - 1, 1) 
      : ack(m - 1, ack(m, n - 1))));
}

var x = ack(3, 2);