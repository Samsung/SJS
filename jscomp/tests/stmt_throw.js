


function f1() {
    throw null;
}

try {
    f1();
} catch(e) {
    console.log(e);
}
