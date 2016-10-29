mergeInto(LibraryManager.library, {
  __document_getElementById: function(id) {
    id = Pointer_stringify(id);
    console.log('documentGetElementById passed: '+id)
    elt = document.getElementById(id);
    // note post-ffi name
    return _marshallobj(elt);
  },
  __htmlelement_innerHTML___get: function(elt) {
    // TODO
    return 0;
  },
  __htmlelement_innerHTML___set: function(elt, val) {
    elt = _unmarshallobj(elt);
    val = Pointer_stringify(val);
    console.log('setInnerHTML passed: ('+elt+', '+val+')')
    elt.innerHTML = val;
  },
  __console_log: function(str) {
    console.log(Pointer_stringify(str));
  },
  __console_error: function(str) {
    console.error(Pointer_stringify(str));
  },
  __console_assert: function(b) {
    console.assert(b);
  }
});
