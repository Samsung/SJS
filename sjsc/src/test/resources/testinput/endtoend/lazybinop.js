

// If binary || is miscompiled as overly-eager, this will crash on null
function lazyor(a) {
    if (a == null || a.foo > 0) {
        console.log("all good in lazy ||");
    }
}


var o = { foo : 3 };

lazyor(o);
lazyor(null);




// This will also crash on null if && is miscompiled as fully eager
function stopfastand(a) {
    if (a != null && a.foo > 0) {
        console.log("all good in fail-fast &&");
    }
}


stopfastand(o);
stopfastand(null);
