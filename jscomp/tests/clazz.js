
function Point(x, y) {
    this.x = x;
    this.y = y;
}

Point.prototype.z = 9;

Point.prototype.distsquare = function() {
    console.log(this.z);
    console.log(this.u);
    return this.x * this.x + this.y * this.y;
};

var p = new Point(3,4);
console.log(p.distsquare());


