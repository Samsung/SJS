
var flags, flagsorig = Array(8193);
var i, k, count;
var n = 17; //getIntArg(0); /*arguments[0];*/

for (i = 2; i <= 8192; i++) {  flagsorig[i] = 1; }
while (n--) {
  count = 0;
  flags = flagsorig.concat();
  for (i = 2; i <= 8192; i++) {
    if (flags[i]) {
      for (k=i+i; k <= 8192; k+=i)
	flags[k] = 0;
      count++;
    }
  }
}

printString("Count:");
printInt(count);
