//Banes and Hut N-body simulation algorithm

var floatHint = 0.0;
var intHint = 0;

//------------------------------
//       math functions
//------------------------------
function sqrt(s){
	var x, z;
	var flag = 1;
	x = s;

	if(x === 0.0) return x;
	while(flag){
		x = (x + (s / x)) / 2.0;  
		x = (x + (s / x)) / 2.0;  
		x = (x + (s / x)) / 2.0;  
		x = (x + (s / x)) / 2.0;  
		z = x*x;
		if(z > 0.999*s && z < 1.001*s) flag = 0;
	}
	return x;

	floatHint = s;
}

function dist(x1, x2, y1, y2){
	var dx = x1 - x2;
	var dy = y1 - y2;
	return sqrt(dx * dx + dy * dy);

	floatHint = x1;
	floatHint = x2;
	floatHint = y1;
	floatHint = y2;
}


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


//----------------------------
//      BH sub procedure
//----------------------------
function Obj(x, y, m, id){
	Obj.instance = this;
	this.x = x;
	this.y = y;
	this.m = m;

	this.fx = 0.0;
	this.fy = 0.0;
	this.vx = 0.0;
	this.vy = 0.0;
	this.id = id;

	floatHint = x;
	floatHint = y;
	floatHint = m;
	intHint = id;
}

function Tree(){
	Tree.instance = this;
	this.x = 0.0;
	this.y = 0.0;
	this.dim = 0.0;

	this.obj = null;;
	this.midX = 0.0;
	this.midY = 0.0;
	this.children = [null, null, null, null];
	this.size = 0;

	Obj.instance = this.obj;
}


function allocTree(x,y,dim){
	var tree = new Tree();

	tree.x = x;
	tree.y = y;
	tree.dim = dim;

	tree.obj = null;;
	tree.midX = x + dim/2;
	tree.midY = y + dim/2;
	tree.children[0] = null;
	tree.children[1] = null;
	tree.children[2] = null;
	tree.children[3] = null;;
	tree.size = 0;

	return tree;
}

function allocObj(x,y,mass){
	var obj = new Obj(0,0,0,-1);
	obj.x = x;
	obj.y = y;
	obj.m= mass;
	
	return obj;
}


function checkChild(tree, obj){
	var EW = (obj.x < tree.midX) ? 0 : 1;
	var NS   = (obj.y < tree.midY) ? 0 : 1;
	var q = NS + EW * 2;

	var child = tree.children[q];
	if(child === null){
		var newSize = tree.dim / 2;
		var newX = tree.x + newSize * EW;
		var newY = tree.y + newSize * NS;

		child = allocTree(newX, newY, newSize);
		tree.children[q] = child;
	}

	return child;

	Obj.instance = obj;
	Tree.instance = tree;
}

function insert(tree, obj){
	if(tree.obj === null){
		tree.obj = obj;
		tree.size++;
		return;
	}
	
	var child;
	if(tree.size === 1){
		var tobj = tree.obj;
		tree.obj = allocObj(tobj.x, tobj.y, tobj.m);
		child = checkChild(tree, tobj);
		insert(child, tobj);
	}

	child = checkChild(tree, obj);
	insert(child, obj);

	var newMass = tree.obj.m + obj.m;
	tree.obj.x = (tree.obj.m * tree.obj.x + obj.x * obj.m) / newMass;
	tree.obj.y = (tree.obj.m * tree.obj.y + obj.y * obj.m) / newMass;
	tree.obj.m = newMass;
	tree.size++;

	Obj.instance = obj;
	Tree.instance = tree;
}

function isInternal(tree, obj){
	if(tree.x >= obj.x) return false;
	if(tree.y >= obj.y) return false;
	if(tree.x + tree.dim < obj.x) return false;
	if(tree.y + tree.dim < obj.y) return false;
	return true;

	Tree.instance = tree;
	Obj.instance = obj;
}

function updateForce(obj, objF){
	var f = obj.m * objF.m * 9.8;
	var r = sqrt(dist(obj.x, objF.x, obj.y, objF.y));
	var r3 = r*r*r;
	if(r3 === 0)
	obj.fx += (objF.x - obj.x) / r3;
	obj.fy += (objF.y - obj.y) / r3;

	Obj.instance = obj;
	Obj.instance = objF;
}

function calculateForce(tree, obj){
	if(tree.size === 1){
		if(tree.obj.id === obj.id) return;
		updateForce(obj, tree.obj);
	}
	else{
		if(isInternal(tree, obj)){
			var distance = dist(tree.obj.x, obj.x, tree.obj.y, obj.y);
			var ratio = tree.dim / distance;
			if(ratio > threshold){
				updateForce(obj, tree.obj);
			}
			else{
				var x;
				for(x = 0;x<4;x++){
					var child = tree.children[x];
					if(child){
						calculateForce(child, obj);
					}
				}
			}
		}
		else{
			updateForce(obj, tree.obj);
		}
	}

	Tree.instance = tree;
	Obj.instance = obj
}

function moveObject(obj, delta){
	var i;
	var miniDelta = delta / 10;

	var ax = obj.fx / obj.m;
	var ay = obj.fy / obj.m;

	for(i =0;i<10;i++){
		obj.x += obj.vx * miniDelta;
		obj.y += obj.vy * miniDelta;
		obj.vx += ax * miniDelta;
		obj.vy += ay * miniDelta;
	}

	if(obj.x > dimMax || obj.y > dimMax || obj.x < 0 || obj.y < 0){
		obj.x = rand(dimMin, dimMax);
		obj.y = rand(dimMin, dimMax);
	}

	obj.fx = 0.0;
	obj.fy = 0.0;

	floatHint = delta;
	Obj.instance = obj;
}


//-----------------------------
// BH algorithm
//-----------------------------
var dimMin = 0.0;
var dimMax = 100000.0;
var massMin = 1000.0;
var massMax = 1000000.0;

var n = getIntArg(1);
var tree;  
var objects = [];

var number = n * 10;
var rand = randGen(0)

var obj;
Obj.instance = obj;

var i = 0;
for(i=0 ; i<n ; i++){
	var objX = rand(dimMin, dimMax);
	var objY = rand(dimMin, dimMax);
	var objM = rand(massMin, massMax);
	obj = new Obj(objX, objY, objM, i);
	objects[i] = obj;
}

var threshold = 0.5;
var timestep = 1000;
var delta = 100000.0;

var step;
var x;
var countGoal = 1;
var count = countGoal;

var repeatCount = 0;

for(step = 0; step < timestep; step++){
	tree = allocTree(0.0,0.0,dimMax);

	repeatCount++;

	/*
	if(count === countGoal){
		var xxx = 0;
		for(xxx=0;xxx<n;xxx++){
			console.log(objects[xxx].x);
			console.log(objects[xxx].y);
		}
		count = 0;
	}
	*/	
	

	//console.log(1);
	for(x=0;x<n;x++){
		insert(tree, objects[x]);
		//console.log(objects[x].x);
	}


	//console.log(2);
	for(x=0;x<n;x++){
		obj = objects[x];
		obj.fx = 0.0;
		obj.fy = 0.0;
		calculateForce(tree, obj);
	    //console.log(objects[x].x);	
	}


	//console.log(3);
	for(x=0;x<n;x++){
		obj = objects[x];
		moveObject(obj, delta);
		//console.log(objects[x].x);
	}
	count++;

}
