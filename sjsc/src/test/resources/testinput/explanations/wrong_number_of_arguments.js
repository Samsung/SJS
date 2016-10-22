// On line 6, we call x.f with the wrong number of arguments.

var x = {
    f : function(x) { printInt(x); }
}
x.f(1, 2);
