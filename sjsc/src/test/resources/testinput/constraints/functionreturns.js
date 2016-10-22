function foo(v){
	var x = 1;
	var y = x;
	return v;
}

function bar(w){
	var x = false;
	var z = x;
	return w;
}

function zap(){
  function zip(){ return 18; }
}

foo(17);
bar(3.3);