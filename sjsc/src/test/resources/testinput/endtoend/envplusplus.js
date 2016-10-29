
var i = 0;
var arr = [0, 0];

function f() {
    arr[i++] = 9;
}

f();
console.assert(arr[0] == 9);
console.assert(arr[1] == 0);
