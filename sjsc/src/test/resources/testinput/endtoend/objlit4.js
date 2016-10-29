var fn = { code: function() { console.log("hello"); } };
(fn.code)();
// Note that at least with Wontae's frontend, this is compiled as invoking a _function_ stored in a field,
// not a method invocation.
