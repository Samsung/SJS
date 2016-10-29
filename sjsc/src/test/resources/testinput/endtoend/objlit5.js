// Test that we can have field b at multiple offsets and still dispatch
var obj1 = { b: "hello first b" };
console.log(obj1.b);
var obj2 = { a: 3, z: "qxy", b : "hello second b" }
console.log(obj2.b);
