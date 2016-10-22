// This illustrates 4 "overloaded" variants of the Date constructor:
//   var d = new Date();
//   var d = new Date(milliseconds);
//   var d = new Date(dateString);
//   var d = new Date(year, month, day, hours, minutes, seconds, milliseconds);  
// (see http://www.w3schools.com/jsref/jsref_obj_date.asp for details)


var date1 = new Date(); // current date & time
console.log(date1.toLocaleDateString()); // e.g., Tuesday, February 03, 2015

var date2 = new Date(10000000000000);
console.log(date2.toLocaleDateString()); // Saturday, November 20, 2286

var date3 = new Date("January 1, 1999");
console.log(date3.toLocaleDateString()); // Friday, January 01, 1999

var date4 = new Date(2000, 1, 1, 0, 30, 30, 0); 
console.log(date4.toLocaleDateString()); // Tuesday, February 01, 2000