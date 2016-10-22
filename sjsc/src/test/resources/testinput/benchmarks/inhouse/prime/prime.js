var a;
var x = 0;

for (a = 2; a < 200000; a++)
{
  if(isPrime(a)) x++;
  //if (isPrime(a)) console.log(false);
  //process.stdout.write('X');
  //else console.log(true);
  //process.stdout.write('O');
}

function isPrime(number)
{
  var a;
  intHint = number;
  for (a = 2; a < number; a++)
  {
    if ((number % a | 0 ) == 0)
    {
      return false;
    }
  }
  return true;
}

console.log(x);
var intHint = 0;
