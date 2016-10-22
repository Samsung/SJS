var x = 1, y = 1, k, c = "f";
var u = {f:1}, v = {f:1}, w = {f:1}, z = {f:1};

k = x++;
console.log(x);
console.log(k);
k = ++y;
console.log(y);
console.log(k);
k = u.f++;
console.log(u.f);
console.log(k);
k = ++v.f;
console.log(v.f);
console.log(k);
k = w[c]++
console.log(w[c]);
console.log(k);
k = ++z[c];
console.log(z[c]);
console.log(k);

