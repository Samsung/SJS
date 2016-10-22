
function Node(v, nxt) {
    this.val = v;
    this.next = nxt;
}


var x = null;
var i = 0;
for (i = 0; i < 10; i++) {
    x = new Node(i, x);
}
var cur = x;
for (i = 0; i < 10; i++) {
    console.log(string_of_int(cur.val));
    cur = cur.next; // <-- currently not handled in either frontend
}

for (cur = x; cur != null; cur = cur.next) {
    console.log(string_of_int(cur.val));
}
