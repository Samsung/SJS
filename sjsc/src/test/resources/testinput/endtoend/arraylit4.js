// Technically, this tests the array implementation more than it tests the array literal
// implementation, assuming the earlier array literal tests pass
// At the moment, we can really only test push and pop; the old JS frontend crashes on
// uses of shift and unshift, even with its type annotation file extended appropriately; it does
// some hard-coding of array methods


var a = [1, 2, 3, 4];

console.assert(a.length == 4);

var x = a.pop();
console.assert(a.length == 3);
console.assert(x == 4);

var y = a.pop();
console.assert(y == 3);
console.assert(a.length == 2);

a.push(9);
a.push(10);
a.push(11);
// a is now [1, 2, 9, 10, 11], and the implementation should have grown (but we're not testing
// wrap-around here since we can only change one end with the current frontend)
console.assert(a.length == 5);
console.assert(a[0] == 1);
console.assert(a[1] == 2);
console.assert(a[2] == 9);
console.assert(a[3] == 10);
console.assert(a[4] == 11);
