var o = { f : 3, g : { a : true } };
o.g = null; /* this is allowed */
o.f = undefined; /* this is allowed */
o.f = null; /* SJS does not allow null to be assigned to primitive types */