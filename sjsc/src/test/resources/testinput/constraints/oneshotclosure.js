var functions = (function(){ 
  function foo(o) { return o; }
  return { add : foo }; 
})();

(functions.add)(4);


