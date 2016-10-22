

while(true) {
    try {
        console.log("Enter while");
        break;
        console.log("after break");
    } finally {
        try {
            console.log("Enter finally");
            break;
            console.log("Exit finally");
        } finally {
            console.log("Enter finally 2");
            break;
            console.log("Exit finally 2");
        }
        console.log("after try 2");
    }
    console.log("after try");
}
console.log("After while");
