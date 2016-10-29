// The error here is on line 4, but the explanation is subtle.
// SJS only allows method attachment in the following scenarios:
//   - property of object literal
//   - property of 'this' inside a constructor
//   - property 'prototype' of a constructor

function f(x) {
    x.init = function() {
        console.log(this.f);
    };
}
