var a = "hello";
var b = 1;
switch(a) {
    case "hello": {
        console.log("hello");
        switch (b) {
            case 1: { console.log("hello 2"); break; }
            default: { console.log("default 2"); break; }
        }
        break; }
    case "world": { console.log("world"); break; }
    default:      { console.log("default"); break; }
}
