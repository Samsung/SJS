// For some reason, if I define these functions in library.js,
// emscripten strips them out during compilation, regardless of
// any references from JS or extern statements in C
var __marshalling_meta = { nextObj: 0, objTbl: [] };
//document.marshallobj = function(elt) {
function _marshallobj(elt) {
  if (elt.hasOwnProperty('__indirection')) {
    return elt.__indirection;
  } else {
    elt.__indirection = __marshalling_meta.nextObj;
    __marshalling_meta.objTbl[__marshalling_meta.nextObj] = elt;
    return __marshalling_meta.nextObj++;
  }
}

function _unmarshallobj(id) {
  return __marshalling_meta.objTbl[id];
}
