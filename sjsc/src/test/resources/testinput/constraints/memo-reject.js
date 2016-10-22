
var memoizer = function (memo, fundamental) {
    var shell = function (n) {
        var result = memo[n+""];
        if (result == undefined) {
            result = fundamental(shell, n);
            memo[n+""] = result;
        }
        return result;
    }
    return shell;
};

var m = { "0" : 0, "1" : 1};

var fib = memoizer(m, function (shell, n) {
    return shell(n-1) + shell(n-2);
});

console.log("" + fib(10));


