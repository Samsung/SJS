
// Strictly speaking, if we run this test around midnight, it might fail
var d = new Date();
var e = new Date();

var b = false;
b = (d.getDate() === e.getDate());
console.assert(b);
b = (d.getDay() === e.getDay());
console.assert(b);
b = (d.getFullYear() === e.getFullYear());
console.assert(b);
b = (d.getHours() === e.getHours());
console.assert(b);
b = (d.getMinutes() === e.getMinutes());
console.assert(b);
b = (d.getMonth() === e.getMonth());
console.assert(b);
b = (d.getSeconds() === e.getSeconds());
console.assert(b);
b = (d.getTimezoneOffset() === e.getTimezoneOffset());
console.assert(b);

// Note: getMilliseconds and getTime convert microseconds to milliseconds, so comparing them is more
// than slightly unstable

var s1 = d.toDateString();
var s2 = e.toDateString();
console.assert(s1 === s2);
