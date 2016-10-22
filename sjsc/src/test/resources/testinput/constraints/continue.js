var a = [1,2,3];
var x = 0;

for (var i = 0; i < a.length; i++) {
	if (a[i] == 2) continue;
	x += a[i];
}

console.log("" + x);


