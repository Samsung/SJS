var a = "hello";
switch(a) {
    case "hello": {
        console.log("hello");
        switch (a) {
            case "hello": { console.log("hello 2"); break; }
            default: { console.log("default 2"); break; }
        }
        break; }
    case "world": { console.log("world"); break; }
    default:      { console.log("default"); break; }
}
