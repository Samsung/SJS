function Heap(c) {
    this.color = c;
    this.nextlevel = {};

    this.insert = function (s, h) {
        this.nextlevel[s] = h;
    }
}

var h1 = new Heap("red");
var h2 = new Heap("blue");
h2.insert("01", h1);

console.log(h2.color);  // should print blue
console.log(h2.nextlevel["01"].color); // should print red
