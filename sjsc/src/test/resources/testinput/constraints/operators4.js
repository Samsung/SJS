function h(b) {
	// we should infer that b is a float (it could be either float or int, float is a sound upper bound)
	var t = b / 7;
	printFloat(t);
}