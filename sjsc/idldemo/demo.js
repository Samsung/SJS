
var Pair = {
  alloc : js_alloc_pair,
  print : js_print_pair,
//  add : js_add_pair,
  register : js_register_cb
};

var p = Pair.alloc();
Pair.print(p);
p.setX(3);
Pair.print(p);
p.setY(9);
Pair.print(p);


function pair_callback(x) {
    console.log("Running in pair_callback in SJS");
    x.setY(x.getY()+1);
    Pair.print(x);
    console.log("Printing pair captured in SJS closure:");
    Pair.print(p);
}

Pair.register(pair_callback);
//Pair.addPair(p);

//
