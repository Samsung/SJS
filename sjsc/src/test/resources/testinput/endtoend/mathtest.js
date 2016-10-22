
// This is just a minimal sanity check on various math functions.  We *should* be able to adapt the
// math tests from V8 or SpiderMonkey, and they should all pass for SJS now

console.assert(Math.abs(1.0) === Math.abs(-1.0));

console.assert(Math.floor(1.4) == 1.0);
console.assert(Math.ceil(1.4) == 2.0);
console.assert(Math.round(1.3) == 1.0);
console.assert(Math.round(1.6) == 2.0);
console.assert(Math.round(2.0) == 2.0);

console.assert(Math.max(3.0, 4.0) == 4.0);
console.assert(Math.min(3.0, 4.0) == 3.0);
