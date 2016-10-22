// Uncomment this line to run in Node.js
/* var print = function(s) { process.stdout.write(s+"\n"); } */

/****************************************************************************
 * length can be typed either as: mu a. { tl : a } -> int
 * or as:                         mu a. { hd : int, tl : a } -> int
 * or (also) as:                  { tl : mu a. { hd : int, tl : a } } -> int
 * PROBLEM: the first type is incomparable with the last two.
 *          (the last two are comparable with each other using unrolling and 
 *           width subtyping)
 * This test should typecheck without the call of length on l0 or on l3:
 *   see ../endtoend/recursivelist.js
 ****************************************************************************/
var length = function(l) { 
  if (l == null) {
    return 0; 
  } else {
    return 1+length(l.tl); 
  }
}

/* This block requires that length types as:
 *   mu a. { tl : a } -> int
 */
var l0 = { tl : { tl : null } }
var len0 = length(l0)
print(len0+"")

/* This block requires that length types as:
 *   mu a. { hd : int, tl : a } -> int   OR
 *   { tl : mu a. { hd : int, tl : a } } -> int
 */
var l3 = { hd : 3, tl : { hd : 2, tl : null } }
l3.hd = 4
l3.tl.hd = 5
var len3 = length(l3)
print(len3+"")

