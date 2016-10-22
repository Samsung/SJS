var intArrHint = [0];
var floatArrHint = [0.0];
var floatArrArrHint = [[0.0]];
var intHint = 0;
var floatHint = 0;

var EPSILON=1.0E-10; 

function Simplex(A, b, c){
	Simplex.instance = this;

	this.a = null;
	this.M = 0;
	this.N = 0;
	this.basis = null;

	this.M = b.length;
	this.N = c.length;
	this.degenerate = false;

	this.a = [];
	
	var i = 0;
	var j = 0;

	for(i=0;i<=this.M;i++){
		this.a[i] = [];
		for(j=0;j<this.M+this.N+1;j++){
			this.a[i][j] = 0.0;
		}
	}
	for(i=0;i<this.M;i++){
		for(j=0;j<this.N;j++){
			this.a[i][j] = A[i][j];
		}
	}


	for(i=0;i<this.M;i++) this.a[i][this.N+i] = 1.0;
	for(i=0;i<this.N;i++) this.a[this.M][i] = c[i];
	for(i=0;i<this.M;i++) this.a[i][this.M + this.N] = b[i];

	this.basis = [];
	for(i=0;i<this.M;i++) this.basis[i] = this.N + i;

	/*
	for(i=0;i<=this.M;i++){
		for(j=0;j<this.N + this.M + 1;j++){
			console.log(this.a[i][j]);
		}
	}
	*/

	this.solve();
	if(!this.degenerate)
		this.check(A,b,c);

	intArrHint = this.basis;
	floatArrArrHint = this.a;
	floatArrArrHint = A;
	floatArrHint = b;
	floatArrHint = c;
}

Simplex.prototype.solve = function(){
	Simplex.instance = this;

	var whileflag = true;
	while(whileflag){
		var q = this.bland();
		if(q === -1) whileflag=false;
		else{
			var p = this.minRatioRule(q);
			if(p === -1){
				whileflag = false;
				this.degenerate = true;
			}
			else{
				this.pivot(p,q);
				this.basis[p] = q;
			}
		}

	}		
}

Simplex.prototype.bland = function(){
	Simplex.instance = this;
	var i;
	for(i=0;i<this.M+this.N;i++){
		if(this.a[this.M][i] > 0) return i;	
	}
	return -1;
}

Simplex.prototype.minRatioRule = function(q){
	Simplex.instance = this;
	
	var p = -1;
	var i;

	for(i=0;i<this.M;i++){
		if(this.a[i][q] > 0){
			if(p==-1) p = i;
			else{
				var x1 = this.a[i][this.M + this.N];
				var x2 = this.a[i][q];
				var y1 = this.a[p][this.M + this.N];
				var y2 = this.a[p][q];
				if(x1/x2 < y1/y2) p = i;
			}
		}
	}
	return p;

	intHint = q;
}

Simplex.prototype.pivot = function(p, q){
	Simplex.instance = this;

	var i;
	var j;

	for(i=0;i<=this.M;i++)
		for(j=0; j<=this.M+this.N; j++)
			if(i !== p && j !== q) {
				var x1 = this.a[p][j];
				var x2 = this.a[i][q];
				var x3 = this.a[p][q];
				var x4 = this.a[i][j];
				this.a[i][j] = x4 - x1 * x2 / x3;
			}

	for(i=0;i<=this.M;i++)
		if(i != p){
		 	this.a[i][q] = 0.0;
		}

	for(j=0;j<=this.M+this.N;j++)
		if(j!=q){
			var y1 = this.a[p][j];
			var y2 = this.a[p][q];
			this.a[p][j] = y1 / y2;
		}

	this.a[p][q] = 1.0;

	intHint = p;
	intHint = q;
}

Simplex.prototype.isPrimalFeasible = function(A,b){
	Simplex.instance = this;
	floatArrArrHint = A;
	floatArrHint = b;

	var x = this.primal();

	var j;
	var i;

	for(j=0;j<x.length;j++){
		if(x[j]<0.0){
			return false;
		}
	}

	for(i=0;i<this.M;i++){
		var sum = 0.0;
		for(j=0;j<this.N;j++){
			sum += A[i][j] * x[j];
		}
		if(sum > b[i] + EPSILON){
			return false;
		}
	}

	return true;
}

Simplex.prototype.isDualFeasible = function(A,c){
	Simplex.instance = this;
	floatArrArrHint = A;
	floatArrHint = c;

	var y = this.dual();
	var i;
	var j;

	for(i=0;i<y.length;i++){
		if(y[i]<0.0){
			return false;
		}
	}

	for(j=0;j<this.N;j++){
		var sum = 0.0;
		for(i=0;i<this.M;i++){
			sum += A[i][j] * y[i];
		}
		if(sum > c[j] + EPSILON){
			return false;
		}
	}

	return true;
}

Simplex.prototype.isOptimal = function(b,c){
	Simplex.instance = this;
	floatArrHint = b;
	floatArrHint = c;

	var x = this.primal();
	var y = this.dual();
	var value = this.value();

	var i;
	var value1 = 0.0;
	var value2 = 0.0;
	for(i=0;i < x.length ;i++) value1 += c[i] * x[i];
	for(i=0;i < y.length ;i++) value2 += y[i] * b[i];

	if(Math.abs(value - value1) > EPSILON || Math.abs(value - value2) > EPSILON){
		return false;
	}
	return true;
}

Simplex.prototype.primal = function(){
	Simplex.instance = this;

	var x = [];
	var i;

	for(i=0;i<this.M;i++)
		if(this.basis[i] < this.N) x[this.basis[i]] = this.a[i][this.M + this.N];
	for(i=this.M;i<this.N;i++) x[i] = 0.0;
	return x;
}

Simplex.prototype.dual = function(){
	Simplex.instance = this;

	var y = [];
	var i;

	for(i=0;i<this.M;i++)
		y[i] = -1 * this.a[this.M][this.N + i];
	return y;
}

Simplex.prototype.check =function(A,b,c){
	Simplex.instance = this;
	floatArrArrHint = A;
	floatArrHint = b;
	floatArrHint = c;

	return this.isPrimalFeasible(A,b) && this.isDualFeasible(A,c) && this.isOptimal(b,c);
}

Simplex.prototype.value = function(){
	Simplex.instance = this;
	return -1.0 * this.a[this.M][this.M + this.N];
}

Simplex.prototype.print = function(){
	Simplex.instance = this;
	console.log(111111111);
	console.log(this.value());
	console.log(222222222);
	var x = this.primal();
	var y = this.dual();
	var i =0;
	for(i=0;i<x.length;i++) console.log(x[i]);
	console.log(333333333);	
	for(i=0;i<y.length;i++) console.log(y[i]);
}

function test(A,b,c){
	var lp = new Simplex(A,b,c);
	if(!lp.degenerate) lp.print();

	floatArrArrHint = A;
	floatArrHint = b;
	floatArrHint = c;
}

function test1(){
	var A = [
			[ -1.0,  1.0,  0.0],
			[  1.0,  4.0,  0.0],
			[  2.0,  1.0,  0.0],
			[  3.0, -4.0,  0.0],
			[  0.0,  0.0,  1.0]];
	var c = [1.0, 1.0, 1.0];
	var b = [5.0, 45.0, 27.0, 24.0, 4.0];
	test(A,b,c);
}

function test2(){
	var A = [
			[5.0, 15.0],
			[4.0, 4.0],
			[35.0, 20.0]];
	var c = [13.0, 23.0];
	var b = [480.0, 160.0, 1190.0];
	test(A,b,c);
}

function test3(){
	var A = [
			[-2.0, -9.0, 1.0, 9.0],
			[1.0, 1.0, -1.0, -2.0]];
	var c = [2.0, 3.0, -1.0, -12.0];
	var b  =[3.0, 2.0];
	test(A,b,c);
}

function test4(){
	var A = [
			[0.5, -5.5, -2.5, 9.0],
			[0.5, -1.5, -0.5, 1.0],
			[1.0, 0.0, 0.0, 0.0]];
	var c = [10.0, -57.0, -9.0, -24.0];
	var b = [0.0, 0.0, 1.0];
	test(A,b,c);
}

//test1();
//test2();
//test3();
//test4();


function randGen(seed){
	var prev = seed;
	function rand(min, max){
		// Linear Congruential Sequence Generator
		prev = (48271 * prev + 12820163) &  16777215;
		return min + ((max-min) *  (prev / 16777216));

		floatHint = max;
		floatHint = min;
	}
	return rand;

	intHint = seed;
}

function simplex(){
var rand = randGen(getIntArg(1));
var M = getIntArg(2);
var N = getIntArg(3);
var c = [];
var b = [];
var A = [];
var i;
var j;
for(j=0;j<N;j++) c[j] = rand(0, 1000);
for(i=0;i<M;i++) b[i] = rand(0, 1000);
for(i=0;i<M;i++){
	A[i] = [];
	for(j=0;j<N;j++){
		A[i][j] = rand(0, 100);
	}
}
var lp = new Simplex(A,b,c);
console.log(lp.value());
}

simplex();
