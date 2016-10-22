var intHint = 0;

function randGen(seed){
	var prev = seed;
	function rand(min, max){
		/* Linear Congruential Sequence Generator*/
		prev = (48271 * prev + 12820163) &  16777215;
		var result = min + ((max-min) *  (prev / 16777216));
		return result | 0;

		// write to max and min with these hints,
		// to ensure they work with directional constraints
		max = intHint;
		min = intHint;
	}
	return rand;

	intHint = seed;
}

var rand = randGen(0);