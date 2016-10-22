
var o = { a : 1 }; // 0x1 is the box tag bit for fields with top bits all zeroes (pointers)

console.log(o.a.toString()); // This will crash if we incorrectly treat the field value as a box
