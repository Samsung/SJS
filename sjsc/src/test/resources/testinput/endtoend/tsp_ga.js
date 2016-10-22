var intHint = 0;
var intArrHint = [0];
var intArrArrHint = [[0]];

var floatHint = 0.0;
var floatArrHint = [0.0];

var boolHint = true;

var MAXCUSTOMER = 500;
var MAXPOP = 200;
var generationGoal = 0;
var RAND_MAX = 200000000;

var baseX = 0;
var baseY = 0;
var customerNumber = 0;
var customerX = [];
var customerY = [];

var distance = [];
var baseDistance = [];
intArrArrHint = distance;
intArrHint = baseDistance;

var selectivePressure = 0;
var population  = 0;
var mutationRate = 0;

var chromosomes = [[],[]];
function chromoTypeHint(){
	var hint = [[[0]]];
	hint = chromosomes;
}

var scores = [];
intArrHint = scores;

var currentGeneration = 0;
var childGeneration = 0;

var minScore = 0;
var maxScore = 0;
//var minIndex = 0;
//var maxIndex = 0;

var bestChromosome = [];
var bestChromosomeGlobal = [];
var bestScore = 0;
var bestScoreGlobal = 0;
intArrHint = bestChromosome;
intArrHint = bestChromosomeGlobal;

function sqrt(s){
	var x, z;
	var flag = 1;
	x = s;

	if(x === 0.0) return x;
	while(flag){
		x = (x + (s / x)) / 2.0;  
		x = (x + (s / x)) / 2.0;  
		x = (x + (s / x)) / 2.0;  
		x = (x + (s / x)) / 2.0;  
		z = x*x;
		if(z > 0.999*s && z < 1.001*s) flag = 0;
	}
	return x;

	floatHint = s;
}

function randGen(seed){
	var prev = seed;
	function rand(min, max){
		// Linear Congruential Sequence Generator
		prev = (48271 * prev + 12820163) &  16777215;
		var result = min + ((max-min) *  (prev / 16777216));
		return result | 0;

		intHint = max;
		intHint = min;
	}
	return rand;

	intHint = seed;
}

var rand = randGen(0);

function crossover(chromoC, chromoF, chromoM){
	var t1, t2, t3;
	var usedNumber = [];
	var map = [];

	for(t1 = 0; t1 < customerNumber; t1++){
		usedNumber[t1] = 0;
		chromoC[t1] = -1;
	}

	t1 = rand(0,customerNumber);
	t2 = rand(0,t1);
	for(t3 = t2; t3 < t1; t3++){
		chromoC[t3] = chromoM[t3];
		usedNumber[chromoM[t3]] = 1;
	}

	for(t3 = 0; t3 < customerNumber; t3++){
		map[chromoM[t3]] = chromoF[t3];
	}

	for(t3 = 0; t3 < customerNumber; t3++){
		if(chromoC[t3] === -1){
			t1 = chromoF[t3];
			while(usedNumber[t1]){
				t1 = map[t1];
			}
			chromoC[t3] = t1;
			usedNumber[t1];
		}
	}

	chromoC = intArrHint;
	chromoF = intArrHint;
	chromoM = intArrHint;
}

function WHEELPORTION(i){
	var t1, t2, t3;
	t1 = minScore - maxScore;
	if(0 === t1) return (selectivePressure +1) * 100;
	t2 = minScore - scores[i];
	t3 = ((10 * (t1 + selectivePressure * t2)) / t1) | 0;
	return t3 * 10;

	intHint = i;
}

function selection(){
	var t1, t2;
	t1 = rand(0,population*100*(selectivePressure+1));
	t2 = 0;
	while(1){
		t1 = t1 - WHEELPORTION(t2);
		if(t1 <= 0) return t2;
		t2 = ((t2+1)%population) | 0;
	}
	return t2;
}

function mutate(chromo){
	var t1, t2, t3, t4, i, base, bound;
	var temp = [];

	if(rand(0, RAND_MAX) < RAND_MAX / 1000 * mutationRate){

		t4 = rand(0,customerNumber);
		t3 = rand(0, t4);
		t2 = rand(0, t3);
		t1 = rand(0, t2);
	
		for(i=0;i<t1;i++) temp[i] = chromo[i];

		base = t1;
		bound = t4 - t3;
		for(i=0;i<bound;i++) temp[i+base] = chromo[i+t3];

		base = t4 - t3 + t1;
		bound = t3 - t2;
		for(i=0;i<bound;i++) temp[i+base] = chromo[i+t2];

		base = t1 + t4 - t2;
		bound = t2 - t1;
		for(i=0;i<bound;i++) temp[i+base] = chromo[i+t1];

		for(i=t4;i<customerNumber;i++) temp[i] = chromo[i];
		for(i=0;i<customerNumber;i++) chromo[i] = temp[i];
	}
	intArrHint = chromo;	
}


function switchGeneration(){
	currentGeneration = (currentGeneration + 1) % 2  | 0;
	childGeneration = (childGeneration + 1) % 2 | 0;
}


function initSelect(seed, selected){
	var index = 0;
	while(seed >= 0){
		if(0 === selected[index]) seed--; 
		if(seed >= 0){
			index++;
			if(index == customerNumber)
				index = 0;
		}
		//console.log(1000000);
		//console.log(index);
		//console.log(selected[index]);
		//console.log(seed);
		//console.log(customerNumber);
		}
	selected[index] = 1;
	return index;

	intHint = seed;
	intArrHint = selected;
}

function initializeChromosome(chromo){
	var selected = [];
	var i;

	for(i=0;i<customerNumber;i++) selected[i] = 0;

	//console.log(11111111);
	for(i=0;i<customerNumber;i++){
		chromo[i] = initSelect(rand(0, customerNumber), selected);
		//console.log(20000000);
		//console.log(chromo[i]);
	}
	intArrHint = chromo;
}
function initialize(){
	var i=0;
	currentGeneration = 0;

	for(i=0;i<population;i++)
		initializeChromosome(chromosomes[currentGeneration][i]);
}

function evolve(){
	var i, father, mother;
	var chromoC, chromoF, chromoM;
	for(i=0 ; i<population ; i++){
		var flag = true;
		father = selection();
		while(flag){
			mother = selection();
			if(father !== mother) flag = false;
		}

		chromoC = chromosomes[childGeneration][i];
		chromoF = chromosomes[currentGeneration][father];
		chromoM = chromosomes[currentGeneration][mother];
		crossover(chromoC, chromoF, chromoM);
		mutate(chromoC);
	}

	return;
}

function calcFitness(chromo){
	var t1, t2, i, length;
	intArrHint = chromo;

	length = 0;
	for(i=1;i<customerNumber-1;i++){
		t1 = chromo[i-1];
		t2 = chromo[i];

		length += distance[t1][t2];
	}

	length += baseDistance[chromo[0]];
	length += baseDistance[chromo[customerNumber-1]];
	return length;
}

function evaluate(){
	var i, j;
	var chromo;
	intArrHint = chromo;

	var temp = [];
	var average;

	maxScore = 99999999;
	minScore = 0;
	average = 0;

	for(i=0;i<population;i++){
		chromo = chromosomes[currentGeneration][i];
		scores[i] = calcFitness(chromo);
		
		average += ((scores[i] + (population/2)) /population) | 0;
		
		if(maxScore > scores[i]) maxScore = scores[i];
		if(minScore < scores[i]) minScore = scores[i];

		if(bestScore > scores[i]){
			bestScore = scores[i];
			for(j=0; j < customerNumber;j++)
				bestChromosome[j] = chromo[j];
		}

		if(bestScoreGlobal > scores[i]){
			bestScoreGlobal = scores[i];
			for(j=0 ; j< customerNumber;j++)
				bestChromosome[j] = chromo[j];
		}

		for(j=0; j<customerNumber;j++) temp[j] = false;
		for(j=0; j<customerNumber;j++){
			//console.log(chromosomes[currentGeneration][i][j]);
			if(temp[chromosomes[currentGeneration][i][j]]){
				//console.log(111111111);
			}
			else{
				//console.log(222222222);
			}
			temp[chromosomes[currentGeneration][i][j]] = true;
		}
	}

	if(average === maxScore) return true;
	if(average / (average - maxScore) > 200) return false;
	return true;
}

function smallGA(isFirstCall){
	boolHint = isFirstCall;

	bestScore = 999999999;
	currentGeneration = 0;
	childGeneration = 1;

	if(isFirstCall) initialize();
	while(evaluate()){
		evolve();
		switchGeneration();
	}
}

function bigGA(){
	var chrom=[];
	intArrArrHint = chrom;

	var i,j, average, worstScore, victim1;


	bestScoreGlobal = 999999999;
	//population = 50;
	mutationRate = 10;
	selectivePressure = 4;


	for(i=0;i<2;i++){
		chromosomes[i] = [];
		for(j=0;j<population;j++){
			chromosomes[i][j] = [];
		}
	}

	console.log(1 + ""); // FT: coercions not supported yet

	worstScore = 0;
	for(i=0;i<population;i++){
		smallGA(true);
		chrom[i] = [];
		for(j=0;j<customerNumber;j++)
			chrom[i][j] = bestChromosome[j];
	}

	console.log(2 + ""); // FT: coercions not supported yet

	var gen =0;
	for(;gen < generationGoal; gen++){
		currentGeneration = 0;
		worstScore = 0;
		mutationRate = ((average / (average - bestScore))) * 100 | 0;
		average = 0;

		for(i=0;i<population;i++){
			for(j=0;j<customerNumber;j++)
				chromosomes[currentGeneration][i][j] = chrom[i][j];
		}

		evaluate();
		victim1 = selection();

		for(i=0;i<population;i++){
			average += (calcFitness(chromosomes[currentGeneration][i]) / population) | 0;
			mutate(chromosomes[currentGeneration][i]);
		}
		mutationRate = 10;
		smallGA(false);

		for(i=0;i<customerNumber;i++){
			chrom[victim1][i] = bestChromosome[i];
		}
	}
}

function run(){
	var i, j, t1, t2;

	customerNumber = 1; // getIntArg(1);
	population = 3; // getIntArg(2);
	generationGoal = 1; // getIntArg(3);
	//baseX = getIntArg(2);
	//baseY = getIntArg(3);
	baseX = rand(0,1000);
	baseY = rand(0,1000);

	for(i=0;i<customerNumber;i++){
		customerX[i] = rand(0,1000);
		customerY[i] = rand(0,1000);
	 	//customerX[i] = getIntArg(i*2 + 4);
		//customerY[i] = getIntArg(i*2 + 5);
	}

	console.log(1 + "");

	for(i=0;i<customerNumber;i++) distance[i] = [];

	console.log(1 + "");
	for(i=0;i<customerNumber;i++){
		for(j=i+1;j<customerNumber;j++){
			t1 = customerX[i] - customerX[j];
			t2 = customerY[i] - customerY[j];
			distance[i][j] = (sqrt((t1 * t1 + t2 * t2)*1.0) * 1000) | 0;
			distance[j][i] = distance[i][j];
		}
		t1 = customerX[i] - baseX;
		t2 = customerY[i] - baseY;
		baseDistance[i] = (sqrt((t1 * t1 + t2 * t2)*1.0) * 1000) | 0
	}

	bigGA();

	console.log(baseX + "");
	console.log(baseY + "");
	for(i=0;i<customerNumber;i++){
		console.log(customerX[i] + "");
		console.log(customerY[i] + "");
	}
	console.log(bestScoreGlobal + "");
}

run();
