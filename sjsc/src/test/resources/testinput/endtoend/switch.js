function say (k) {
    switch (k) {
    case 1:
        console.log("Single");
        break;
    case 2:
        console.log("Double");
        break;
    case 3:
        console.log("Triple");
        break;
    default:
        console.log("Too many");
    }
    return;
}

say(1);
say(2);
say(3);
say(4);

