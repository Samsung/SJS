// test codegen for non-immediate (impure) map keys

function getKey() {
    return "asdf";
}

var m = {};
m["asdf"] = "hello";
console.log(m[getKey()]);
