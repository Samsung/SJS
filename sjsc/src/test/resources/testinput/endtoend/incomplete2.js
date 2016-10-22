function f(g) {
   return g(3);
}

f(function (x) { return x + 1;});
/**
 * Handle a function whose argument itself is a function, but the latter's definition is unknown
 */

