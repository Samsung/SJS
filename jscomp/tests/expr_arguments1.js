
function sum() {
    var len = arguments.length;
    var ret = 0;
    for(var i=0 ;i<len; i++) {
        console.log(arguments[i]++);
        ret += arguments[i];
    }
    return ret;
}

console.log(sum(3,4,5));
