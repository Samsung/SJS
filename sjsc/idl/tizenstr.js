// Test Tizen String IDL gen

var str = new __String("") ; // allocate an empty Tizen string.
console.log("Constructed str");
var dalm = new __String(" Dalmatians");
console.log("Constructed dalm");
str.Append(dalm);
console.log("called Append");
var str2 = new __String("STRING TEST");
console.log("Constructed str2");
var substr = str2.SubString(7); // now subStr == "TEST"
var internal = substr.GetPointer(); // get wchar_t* rep of substr
console.log(internal);

var cont = str2.Contains(substr);
if (cont) {
    console.log("success!");
} else {
    console.log("FAILURE");
}

function Sjstr() {
    this.m = function() { console.log("I'm an SJS subtype of a C++ class!"); }
    console.log("constructed a sjstr");
}
Sjstr.prototype = new __String("");

var x = new Sjstr();
x.m();
x.Append(dalm);
console.log("called Append again");
var intern = x.GetPointer();
console.log(intern);
