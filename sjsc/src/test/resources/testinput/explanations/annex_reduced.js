function myString(i) {
    return;
}
var World = function () {
    var w = {
        actionAtPoint: function (place) {
            this.heap = this.heap.nextLevel[myString(place[0])];
        },
        evaluate: function (place) {
            var heap = this.heap;
            heap = heap.nextLevel[myString() + myString()];
        }
    };
}();
