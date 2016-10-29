var arr = [1, 2, 3, 4, 5];

function sum(a){
  var result = 0;
  for (var i=0; i < a.length; i++){
    result = result + a[i];
  }
  return result;  
}

var r = sum(arr);