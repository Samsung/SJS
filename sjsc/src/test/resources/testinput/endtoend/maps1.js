var m = {};
m["colin"] = { first: "colin", last: "gordon" };
m["satish"] = { first: "satish", last: "chandra" };
for (var i in m) {
    console.log ("Name[" + i + "] = " + m[i].last + " ," + m[i].first)
}
