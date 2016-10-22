var table = { "x": "foo", "y": "bar", "z": "baz" };
var last = "foo";
for (var c in table) {
  table[c] += table[last];
  last = c;
}