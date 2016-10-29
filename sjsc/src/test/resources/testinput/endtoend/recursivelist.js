var length = function(l) {
  if (l == null) {
    return 0;
  } else {
    return 1+length(l.tl);
  }
}

var l1 = { tl : null }
var len1 = length(l1)
console.log(len1+"")  // 1

var l2 = { hd : 2, tl : null }
l2.hd = 1       // otherwise field hd of l2 can just be ignored
var len2 = length(l2)
console.log(len2+"")  // should return 1 but type error

var l3 = { hd : 3, tl : { hd : 2, tl : null } }
l3.hd = 4
l3.tl.hd = 5
var len3 = length(l3)
console.log(len3+"")
