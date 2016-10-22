//http://rosettacode.org/wiki/Matrix_multiplication#C


function Matrix(h, w, buffer){
	this.height = h;
	this.width = w;
	this.data = buffer;
}

function rowColumnDot(a, b, aindex, j){
	var r = 0;
	var i ;
	var ax;
	var bx;
	for(i = 0 ; i < a.width ; i++){
		ax = a.data[aindex+i];
		bx = b.data[j];
		r += (ax * bx);
		j += b.width;
	}

	return r;
}

function mul(a, b){
	if(a.width !== b.height) return null;

	var buffer = [];

	var r = new Matrix(a.height, b.width, buffer);

	var index = 0;
	var aindex = 0;
	var i, j;
        var tmp = 0;
	for(i=0; i < a.height ; i++){
		for(j=0; j < b.width ; j++){
			buffer[index] = (rowColumnDot(a , b , aindex , j));
			index++;
		}
		aindex += a.width;
	}

	return r;
}

var data =  [];
var i;
var s = 1000;
for(i=0 ; i<s*s; i++){
	data[i] = (i+1) % 100;
}

var a = new Matrix(s, s, data);
var b = new Matrix(s, s, data);
var c = mul(a,b);

console.log(c.data[0].toString());
console.log(c.data[1].toString());
console.log(c.data[2].toString());
console.log(c.data[3].toString());
