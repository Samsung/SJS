var intHint = 0;
var intArrHint = [0];
var intArrArrHint = [[0]];

var code = [];
intArrArrHint = code;


function randGen(seed){
	var prev = seed;
	function rand(min, max){
		// Linear Congruential Sequence Generator
		prev = (48271 * prev + 12820163) &  16777215;
		var result = min + ((max-min) *  (prev / 16777216));
		return result | 0;

		intHint = max;
		intHint = min;
	}
	return rand;

	intHint = seed;
}




function Node(freq, c, nodeA, nodeB){
	Node.instance = this;

	intHint = freq;
	intHint = c;
	Node.instance = nodeA;
	Node.instance = nodeB;

	if(freq){
		this.c = c;
		this.freq = freq;
		this.left = null;
		this.right = null;
	}
	else{
		this.c = 0;
		this.left = nodeA;
		this.right = nodeB;
		this.freq = nodeA.freq + nodeB.freq;
	}
}

function Queue(){
	Queue.instance = this;
	
	this.qend = 1;
	this.q = [null];

	Node.instance = this.q[0];
}

Queue.prototype.insert = function(node){
	Queue.instance = this;
	Node.instance = node;

	var j = 0;
	var i = this.qend++;

	var continueFlag = true;
	while((j = ((i / 2) | 0)) !== 0 && continueFlag){
		if(this.q[j].freq <= node.freq){
			continueFlag = false;
		}
		else{
			this.q[i] = this.q[j];
			i = j;
		}
	}
	this.q[i] = node;
}

Queue.prototype.remove = function(){
	Queue.instance = this;
	
	var i =0;
	var l = 0;
	var node = this.q[i = 1];

	if(this.qend < 2) return null;
	this.qend--;

	while((l = i * 2) < this.qend){
		if((l + 1 < this.qend) && (this.q[l+1].freq < this.q[l].freq)) l++;
		this.q[i] = this.q[l];
		i = l;
	}
	this.q[i] = this.q[this.qend];
	return node;
}

function Huffman(characterRange){
	Huffman.instance = this;
	intHint = characterRange;
	
	this.CharRange = characterRange;
	this.queue = new Queue();
}

Huffman.prototype.build_code = 
function build_code(node, str, len){
	Huffman.instance = this;
	Node.instance = node;
	intArrHint = str;
	intHint = len;

	var out = [];
	intArrHint = out;

	if(node.c > 0){
		var i = 0;
		for(i=0;i<len;i++){
			out[i] = str[i];
		}
		code[node.c] = out;
	}
	else{
		str[len] = 0; this.build_code(node.left,  str, len + 1);
		str[len] = 1; this.build_code(node.right, str, len + 1);
	}
}


Huffman.prototype.init = 
function init(str){
	Huffman.instance = this;
	intArrHint = str;

	var i =0;
	var freq = [];
	intArrHint = freq;

	var c = [];
	intArrHint = c;

	for(i=0;i<this.CharRange;i++){
		freq[i] = 0;
		code[i] = null;
	}

	for(i=0; i<str.length; i++){
		var index = str[i];
		freq[index]++;
	}

	for(i=0; i<this.CharRange; i++){
		if(freq[i] !== 0){
			this.queue.insert(new Node(freq[i], i, null, null));
		}
	}
	while(this.queue.qend > 2){
		this.queue.insert(new Node(0,0, this.queue.remove(), this.queue.remove()));
	}

	this.build_code(this.queue.q[1], c, 0);
}

Huffman.prototype.encode =
function encode(str, out){
	Huffman.instance = this;
	intArrHint = str;
	intArrHint = out;
	
	var i, j;
	for(i=0;i<str.length;i++){
		var code_i = code[str[i]];
		intArrHint = code_i;

		for(j=0; j<code_i.length;j++)
			out.push(code_i[j]);
	}
}

Huffman.prototype.traverse = 
function(node){
	Huffman.instance = this;
	Node.instance = node;

	if(node.c > 0) console.log(node.c);
	else{
		console.log(1);
		this.traverse(node.right);
	}
}

Huffman.prototype.decode = 
function decode(str, node){
	Huffman.instance = this;
	intArrHint = str;
	Node.instance = node;

	var n = node;
	var i;

	var buf = [];

	for(i=0;i<str.length;i++){
		if(str[i] === 0) n = n.left;
		else n = n.right;

		if(n.c > 0){
			buf.push(n.c);
			//console.log(n.c);
			n = node;
		}
	}

	if(node !== n){
		console.log(2222222);
		return null;
	}
	return buf;
}


function compareString(str1, str2){
	var i;
	intArrHint = str1;
	intArrHint = str2;

	if(str1 === null && str2 === null) return true;
	if(!str1) return false;
	if(!str2) return false;

	if(str1.length !== str2.length) return false;
	for(i=0;i<str1.length;i++){
		if(str1[i] !== str2[i]) return false;
	}
	return true;
}


function huffman(str, characterRange){
	intArrHint = str;
	intHint = characterRange

	var huff = new Huffman(characterRange);
	huff.init(str);

	/*
	for(i=0;i<this.CharRange;i++){
		if(code[i]){
		    var j;	
			for(j=0;j<code[i].length;j++)
				console.log(code[i][j]); 
		}
	}*/

	var buf = [];
	intArrHint = buf;
	huff.encode(str, buf);
	//for(i=0;i<buf.length;i++) console.log(buf[i]);

	//huffman.traverse(huffman.queue.q[1]);
	var decodedStr = huff.decode(buf, huff.queue.q[1]);
	console.log(compareString(str, decodedStr));
}

function test1(){
	var i=0;

	var str = [116, 104, 105, 115, 32, 105, 115, 32, 97, 110, 32, 101, 120, 97, 109, 112, 108, 101, 32, 102, 111, 114, 32, 104, 117, 102, 102, 109, 97, 110, 32, 101, 110, 99, 111, 100, 105, 110, 103];
	// "this is an example for huffman encoding"

	huffman(str, 128);
}

function randomTest(range, length){
	intHint = range;
	intHint = length;

	var str = [];
	intArrHint = str;

	var rand = randGen(0);

	var i;
	for(i=0;i<length;i++)
		str.push(rand(1,range));

	huffman(str, range);
}

function main(){
	//test1();
	randomTest(getIntArg(1), getIntArg(2));
}

main();
