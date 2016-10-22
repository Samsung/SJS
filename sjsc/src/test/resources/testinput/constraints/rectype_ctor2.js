
function Node(v, nxt) {
    // var inthint = 3; inthint = v;
    // Node.instance = this;
    // Node.instance = nxt;
    this.val = v;
    this.next = nxt;
}


var x = null;
var i = 0;
for (i = 0; i < 10; i++) {
    x = new Node(i, x);
}
for (i = 0; i < 10; i++) {
    console.log(string_of_int(x.val));
    x = x.next; // <-- currently not handled in either frontend
}
