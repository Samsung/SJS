// This is a larger example involving a simple linked list class (LL). Here we
// try to use the type polymorphically---putting ints in list1 and strings in
// list2. SJS does not infer polymorphic types.

function Node(val) {
    this.val = val;
    this.next = null;
}

function LL() {
    this.head = null;
}
LL.prototype = {
    push: function(val) {
        var n = new Node(val);
        n.next = this.head;
        this.head = n;
    },
    pop: function() {
        var n = this.head;
        this.head = this.head.next;
        return n.val;
    },
    forEach: function(f) {
        var n = this.head;
        while (n != null) {
            f(n.val);
            n = n.next;
        }
    }
}

var list1 = new LL();
list1.push(1);
list1.push(2);
list1.forEach(function(i) { console.log(i.toString()); });
list1.pop();
list1.forEach(function(i) { console.log(i.toString()); });

var list2 = new LL();
list2.push("hello");
list2.pop();
list2.push("goodbye");
list2.forEach(function(s) { console.log(s); });
