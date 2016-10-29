

var arr = [];
function f() {
    arr[0] = ["hello"];
    var x = arr[0][0];
    return x;
}
console.log(f());
