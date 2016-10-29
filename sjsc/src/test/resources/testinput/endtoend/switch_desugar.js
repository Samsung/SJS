function foo() {
    console.log("Foo!");
    return 3;
}

switch (1 + 2) {
    case (foo()):
        console.log("Success!");
        break;
    case (7):
        console.log("Failure.  Math is broken.");
        break;
    default:
        console.log("Failure.  No dynamic switching for you.");
}

foo();
