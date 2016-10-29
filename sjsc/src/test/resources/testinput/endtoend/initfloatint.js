var x = 0;

// thwart inlining
if (false) {
    x = 9.3;
}

printFloat(x);
