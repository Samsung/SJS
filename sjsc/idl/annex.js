/*
 * Copyright (c) 2012, Intel Corporation.
 *
 * This program is licensed under the terms and conditions of the
 * Apache License, version 2.0.  The full text of the Apache License is at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 */

function mString(i) { return i+""; }

// TODO:
// remove this temporary definition
function getMessage(x,y){
    return x;
}

function Heap(){
    this.color = 'board'; // SATISH: using 'board' for undefined
    this.value = -1; // SATISH: -1 stands for undefined. Make corresponding changes later to eliminate undefined
    this.path  = null;
    this.place = null;
    this.bpossible = []; //null;
    this.cpossible = []; //null;
    this.upossible = []; //null;
    this.colorCount = {}; //String -> int
    this.nextLevel = {};  //String -> Heap
}

var World = (function(){
    var w = {
        board: [[],[],[],[],[],[],[],[]], // board entries are 'board', 'black', or 'white'
        bounder: 8,
        boardview: 'board',
        messageview: 'message',
        result: 'result',
        isEnd: false,
        endMessage: '',
        isUserTurn: true,
        isDrawing: false,
        isLock: false,
        isInit: false,
        isConfigure: false,
        isResult: false,
        level: 3,
        hasHelp: false,
        hasInit: false,
        playerNum: 1,
        currentColor: 'black',
        step: 4,
        point: null, //[int]
        directs: [[1,0],[1,1],[1,-1],[0,1],[0,-1],[-1,0],[-1,1],[-1,-1]],
        heap: new Heap(),//{'nextLevel':{}},
        possible: null, //[[int]]

        pendingDrawMessage : 0,
        status : { bc : 0, wc : 0, result : "Unknown"},

        init: function _init(play){

            //this.showWorld();
            this.playerNum = play || 1;

            var i;
            var j;
            for (i = 0; i<this.bounder;i++) { // SATISH: eliminating for-in
                for (j=0; j<this.bounder; j++){
                    this.board[i][j] =  'board';
                }
            }

            this.board[3][3] = 'black';
            this.board[4][4] = 'black';
            this.board[3][4] = 'white';
            this.board[4][3] = 'white';

            this.isUserTurn = true;
            this.isLock = false;
            this.isConfigure = false;
            this.isResult = false;
            this.currentColor = 'white';
            this.step = 4;
            this.level = 3;
            this.point = [];
            this.heap = new Heap();
            this.isInit = true;
            this.endConfigure();
            this.drawMessage();
        },

//        configure: function(){
//            if (!this.isLock || (this.isEnd && this.isLock && !this.isResult)) {
//                if (!this.isConfigure) {
//                    this.isConfigure = true;
//                } else {
//                    this.endConfigure();
//                }
//            }
//        },

        endConfigure: function(){
            this.isConfigure = false;
        },

        startOver: function(){
            this.init(this.playerNum);
            this.endConfigure();
        },

        actionAtPoint: function(place, color){
        console.log("DEBUG: actionAtPoint" + place[0] + place[1] + color);
            var path = []; //TO DO: CHECK

            this.heap = this.heap.nextLevel[mString(place[0]) + mString(place[1])];
            if(!this.heap) this.heap = new Heap();

            if (this.heap.value > -1 && this.heap.color == color){
                path = this.heap.path;
            } else {
                var act = this.getRevertPath(place, color, null);
                path = act.path; // act['path'];
                var heap = new Heap();
                heap.color = color;
                heap.path = path;
                heap.place = place;
                this.heap = heap;
            }

            //this.clearTips();
            //$('#pc'+this.step%2+Math.floor(this.step/2)).attr('src', this.boardTexture['hidePieces']);
            this.step += 1;
            if (this.step >= 50)
                this.level += 1;
            else if (this.step >= 53)
                this.level += 1;
            else if (this.step >= 55)
                this.level += 2;

            this.setPoint(place, color);
            //this.setBorder(place);
            this.drawPath(path, color);
        },

        setPoint: function(place, color){
            this.board[place[0]][place[1]] = color;
            this.drawPoint(place, color);
        },

        drawPoint: function(place, color){
            //$('img#a'+place[0]+place[1]).attr('src', this.boardTexture[color]).removeClass('tip');
            //$('a#l'+place[0]+place[1]).mouseover(null).mouseout(null);
        },

        drawPath: function(path, color){
            this.isDrawing = true;
            var n;
            for (n=0; n < path.length; n++){
                this.board[path[n][0]][path[n][1]] = color;
                //var str = 'World.drawPoint(['+path[n][0]+','+path[n][1]+'],\''+color+'\')';
                //var str = function(){ World.drawPoint([path[n][0],path[n][1]],color)};
                //setTimeout(str, 100*(n+1));
                this.drawPoint([path[n][0],path[n][1]],color);
            }
            //setTimeout('World.drawMessage()', 100*(path.length+1));
            //setTimeout( function(){ World.drawMessage }, 100*(path.length+1));

            // SATISH: fake invocations of drawMessage by keeping a count and doing them later
            this.pendingDrawMessage = this.pendingDrawMessage + 1;
        },

        drawMessage: function(){
        console.log("DEBUG: drawMessage " + this.playerNum + this.currentColor + (this.isUserTurn?"true":"false"));
            var bc = 0;
            var wc = 0;
            if (this.heap.value > -1){
                bc = this.heap.colorCount['black'];
                wc = this.heap.colorCount['white'];
            } else {
                var count = {'black':0, 'white':0};
                var i, j;
                for (i=0; i<this.bounder; i++){
                    for (j=0; j<this.bounder; j++){
                        //count[this.board[i][j]] += parseInt(1);
                        count[this.board[i][j]] += 1;
                    }
                }
                bc = count['black'];
                wc = count['white'];
            }

            var bpossible;
            var wpossible;

            this.status = { bc : bc, wc : wc, result : "Unknown"};
            if (this.heap.value > -1 && this.heap.color == this.currentColor){
                if (this.heap.color == 'white') {
                    bpossible = this.heap.upossible;
                    wpossible = this.heap.cpossible;
                } else {
                    wpossible = this.heap.upossible;
                    bpossible = this.heap.cpossible;
                }
            } else {
                wpossible = this.possiblePlace('white', null);
                bpossible = this.possiblePlace('black', null);
            }

            this.isEnd = ((bpossible.length == 0 && wpossible.length == 0) || (this.step == 64));
            if (!this.isEnd) {
                var tpossible = bpossible;
                if (this.currentColor == 'black') {
                    if (wpossible.length > 0) {
                        this.currentColor = 'white';
                        tpossible = wpossible;
                    }
                } else {
                    if (bpossible.length > 0) {
                        this.currentColor = 'black';
                    } else {
                        tpossible = wpossible;
                    }
                }
                this.possible = tpossible;
                //this.setTips(this.currentColor);
                if (this.playerNum == 1 && this.currentColor == 'white') {
                    this.isUserTurn = false;
                    this.computerTurn();
                } else {
                    this.isUserTurn = true;
                }
            } else {
                //var p = getMessage('player', 'player');
                //var w = getMessage('win', 'Wins');
                if (bc > wc) {
                    //$('.result_win_text').html(p+' 1 '+w+'!');
                    console.log("Player 1 wins");
                    this.status.result = "Player 1 wins";
                } else if (bc == wc) {
                    //$('.result_win_text').html(getMessage('winDraw', 'Draw!'));
                    console.log("Draw");
                    this.status.result = "Draw";
                } else {
//                    if (this.playerNum == 2) {
//                        $('.result_win_text').html(p+' 2 '+w+'!');
//                    } else {
//                        $('.result_win_text').html(getMessage('winComputer', 'Computer Wins!'));
//                    }
                    console.log("Computer wins");
                    this.status.result = "Computer wins";
                }
                //this.playSound('snd_victoryhorns');
                //$('#'+this.result).removeClass('display_none');
                this.isLock = true;
                this.isResult = true;
            }
            console.log("DEBUG: drawMessage exiting " + this.playerNum + this.currentColor + (this.isUserTurn?"true":"false"));
            this.isDrawing = false;
        },
        closeResult: function() {
            //$('#'+this.result).addClass('display_none');
            this.isResult = false;
        },

        displayBoard: function(){
        console.log("DEBUG: displayBoard");

            var i, j;
            for (i=0; i<this.bounder; i++){
                var line = "";
                for (j=0; j<this.bounder; j++){
                    var c = "_";
                    if (this.board[i][j] === 'black')
                        c = "B";
                    else if (this.board[i][j] === 'white')
                        c = "W";
                    line = line + c;
                }
                console.log(line);
            }
            console.log("BC = " + this.status.bc + ", WC = " + this.status.wc + ", Result = " + this.status.result);
            console.log(""); // leave a line
        },

        click: function(i, j){

        console.log("DEBUG: click " + i + j + " " + (this.isUserTurn?"true":"false"));
            if (this.board[i][j] === 'board' && !this.isEnd && this.isUserTurn && !this.isDrawing && !this.isLock && !this.isConfigure) {
                if (this.canRevert([i, j], this.currentColor, null)){
                    this.actionAtPoint([i,j], this.currentColor);
                    if (this.playerNum == 1) this.isUserTurn = false;
                } else {
                    //this.playSound('snd_hint');
                }
            } else if ((this.isConfigure && !this.isEnd && !this.isDrawing && !this.isLock)
                       || (this.isConfigure && this.isEnd && !this.isDrawing && this.isLock && !this.isResult)) {
                this.endConfigure();
            }

            while (this.pendingDrawMessage > 0) {
                this.pendingDrawMessage = this.pendingDrawMessage - 1;
                this.drawMessage();
            }
        },

        computerTurn: function(){
        console.log("DEBUG: computerTurn");
//            if (this.isDrawing) {
//                //setTimeout('World.computerTurn()', 500);
//                setTimeout(function(){ World.computerTurn() }, 500);
//                return;
//            }
            var possible;


            if (this.heap.value > -1){
                if (this.heap.color == 'white') {
                    possible = this.heap.cpossible;
                } else {
                    possible = this.heap.upossible;
                }
            } else {
                possible = this.possiblePlace('white', null);
            }

            var place = this.bestPlace(possible);
            if (possible.length > 0 && place.length > 0) {
                this.actionAtPoint(place, 'white');
            }
        },

        isContain: function(place, _array) {

            var heat = _array || [];
            // var heat = _array ? _array : [];  This worked on 'hacked' version, but does not work here
            var i=0;
            for (i=0; i < heat.length;i++){ // SATISH: eliminating for-in
                if (heat[i][0] == place[0] && heat[i][1] == place[1]) {
                    return true;
                }
            }
            return false;

        },

        possiblePlace: function(color, _board){

            var ret = [];

            var tmp = {};
            var revColor = ((color == 'white')?'black':'white');
            //var board = _board //|| this.board;
            var board = _board ? _board : this.board;
            var i, j, n;
            for (i=0; i<this.bounder; i++){
                for (j=0; j<this.bounder; j++){
                    if (board[i][j] === revColor) {
                        for (n = 0; n < this.directs.length; n++) {  // SATISH for in elimination
                            var ni = i+this.directs[n][0]; // i+parseInt(this.directs[n][0],0);
                            var nj = j+this.directs[n][1]; // j+parseInt(this.directs[n][1],0);
                            if (ni >= 0 && ni < this.bounder && nj >= 0 && nj < this.bounder && board[ni][nj] === 'board'){
                                if (this.canRevert([ni, nj], color, board) && !this.isContain([ni, nj], ret)){
                                    ret.push([ni,nj]); //ret.push([parseInt(ni,0), parseInt(nj,0)]);
                                }
                            }
                        }
                    }
                }
            }

            return ret;
        },

        canRevert: function(place, color, _board){
            //console.log("DEBUG: canRevert" + place[0] + place[1] + color);

            var i = place[0]; // parseInt(place[0],0);
            var j = place[1]; // parseInt(place[1],0);
            var revColor = ((color == 'white')?'black':'white');
            //var board = _board //|| this.board;
            var board = _board ? _board : this.board;
            var n;

            for (n = 0; n < this.directs.length; n++) { // SATISH eliminating for in
                var di = this.directs[n][0]; // parseInt(this.directs[n][0],0);
                var dj = this.directs[n][1]; // parseInt(this.directs[n][1],0);
                var ni = i+di;
                var nj = j+dj;
                while (ni >= 0 && ni < this.bounder && nj >= 0 && nj < this.bounder && board[ni][nj] === revColor){
                    ni += di;
                    nj += dj;
                    if (ni >= 0 && ni < this.bounder && nj >= 0 && nj < this.bounder && board[ni][nj] === color) {
                        return true;
                    }
                }
            }
            return false;
        },

        getClone: function(obj){

            var ret = [[],[],[],[],[],[],[],[]];

            var i, j;
            for(i = 0; i < this.bounder; i++) {
                for(j = 0; j < this.bounder; j++) {
                    ret[i][j] = obj[i][j];
                }
            }
            return ret;
        },


        getRevertPath: function(place, color, _board){

            var i = place[0]; // parseInt(place[0], 0);
            var j = place[1]; // parseInt(place[1], 0);
            var revColor = ((color == 'white')?'black':'white');
            //var board = _board //|| this.board;
            var board = _board ? _board : this.board;

            var path = [];

            var n;
            for (n = 0; n < this.directs.length; n++) { // SATISH eliminating for in
                var ni = i+this.directs[n][0]; // i+parseInt(this.directs[n][0], 0);
                var nj = j+this.directs[n][1]; // j+parseInt(this.directs[n][1], 0);
                var tpath = [];
                while (ni >= 0 && ni < this.bounder && nj >= 0 && nj < this.bounder && board[ni][nj] === revColor){
                    tpath.push([ni, nj]);
                    ni += this.directs[n][0]; // parseInt(this.directs[n][0], 0);
                    nj += this.directs[n][1]; // parseInt(this.directs[n][1], 0);
                    if (ni >= 0 && ni < this.bounder && nj >= 0 && nj < this.bounder && board[ni][nj] === color) {
                        //path = $.merge(path, tpath);
                        //path = path.concat(tpath); // SATISH: - disable concat for now TODO
                        var tmp = [0,0];
                        while (tpath.length > 0) { // SATISH: added the tpath length check
                            tmp = tpath.pop();
                            path.push(tmp);
                        }
                    }
                }
            }
            return {place: place, path: path, color: color};
        },


        doRevert: function(action, _board){

            var color = action.color;
            var board = _board ? _board : this.board;
            var path = action.path;

            var p;
            for (p = 0; p < path.length; p++) {   // SATISH: eliminating for in
                board[path[p][0]][path[p][1]] = color;
            }
            return board;
        },

        getValue: function(place, _board){

            var ret = 0;
            //var board = _board //|| this.board;
            var board = _board ? _board : this.board;
            var i = place[0]; // parseInt(place[0],0);
            var j = place[1]; // parseInt(place[1],0);
            /*
            var mtable = {
            0:{ 0:100, 1:-50, 2:40, 3:30, 4:30, 5:40, 6:-50, 7:100},
            1:{ 0:-50, 1:-30, 2:5,  3:1,  4:1,  5:5,  6:-30, 7:-50},
            2:{ 0:40,  1:5,   2:20, 3:10, 4:10, 5:20, 6:5,   7:40},
            3:{ 0:30,  1:1,   2:10, 3:0,  4:0,  5:10, 6:1,   7:30},
            4:{ 0:30,  1:1,   2:10, 3:0,  4:0,  5:10, 6:1,   7:30},
            5:{ 0:40,  1:5,   2:20, 3:10, 4:10, 5:20, 6:5,   7:40},
            6:{ 0:-50, 1:-30, 2:5,  3:1,  4:1,  5:5,  6:-30, 7:-50},
            7:{ 0:100, 1:-50, 2:40, 3:30, 4:30, 5:40, 6:-50, 7:100}
            };*/
            var mtable = [
                 [  100,  -50,  40,  30,  30,  40,  -50, 100],
                 [  -50,  -30,  5,   1,   1,   5,   -30, -50],
                 [  40,   5,    20,  10,  10,  20,  5,   40],
                 [  30,   1,    10,  0,   0,   10,  1,   30],
                 [  30,   1,    10,  0,   0,   10,  1,   30],
                 [  40,   5,    20,  10,  10,  20,  5,   40],
                 [  -50,  -30,  5,   1,   1,   5,   -30, -50],
                 [  100,  -50,  40,  30,  30,  40,  -50, 100] ]

            return mtable[i][j]; // parseInt(mtable[i][j],0);
        },

        evaluate: function(place, _color, _board, _level, _heap){

            var ret = -100000;
            var level = _level ? _level : this.level;
            var heap = _heap ? _heap : this.heap;

            if (!heap.nextLevel[mString(place[0])+mString(place[1])]){
                heap.nextLevel[mString(place[0])+mString(place[1])] = new Heap();
            }
            heap = heap.nextLevel[mString(place[0])+mString(place[1])];


            //level = parseInt(level,0);
            var toEndLevel = 64 - this.step; // 64-parseInt(this.step,0);
            level = (level>toEndLevel?toEndLevel:level);
            var nextValue = 0.0;

////            var i = parseInt(place[0]);
////            var j = parseInt(place[1]);

            var color = _color || 'white';
            //var board = _board //|| this.board;
            var board = _board ? _board : this.board;
            board = this.getClone(board);
            var revColor = ((color == 'white')?'black':'white');
            var sym = ((color == 'white')?1:-1);

            board[place[0]][place[1]] = color;


            var path;
            var cp;
            var up;
            if (heap.value > -1){
                path = heap.path;
                ret = heap.value;
                cp = heap.cpossible;
                up = heap.upossible;
                board = this.doRevert(heap, board);
            } else {
                var act = this.getRevertPath(place, color, board);
                heap.path = act.path; // act['path'];
                heap.color = color;
                heap.place = place;
                board = this.doRevert(heap, board);
                cp = this.possiblePlace(color, board);
                up = this.possiblePlace(revColor, board);

                var cv = 0;
                var uv = 0;
                var cc = 0;
                var uc = 0;

                var i, j;
                for (i=0; i<this.bounder; i++){
                    for (j=0; j<this.bounder; j++){
                        if (board[i][j] === color) {
                            cv += this.getValue([i, j], board);
                            cc++;
                        } else if (board[i][j] === revColor){
                            uv += this.getValue([i, j], board);
                            uc++;
                        }
                    }
                }

                ret = (cp.length-up.length)*10;
                ret += (cv-uv)*2;
                if (up.length == 0 && cp.length > 0) ret = 100000;

                heap.value = ret;
                heap.nextLevel = {};
                heap.cpossible = cp;
                heap.upossible = up;

                heap.colorCount[color] = cc;
                heap.colorCount[revColor] = uc;
            }

            if (level > 1 && (up.length > 0 || cp.length > 0)){
                if (up.length == 0){
                    up = cp;
                    revColor = color;
                }
                up = this.getBestPlaceSet(up);

                var p;
                for (p = 0; p < up.length; p++) {  // SATISH: eliminating for in
                    nextValue = nextValue +  this.evaluate(up[p], revColor, board, level-1, heap);
                }
                if (up.length > 0){
                    nextValue = nextValue / up.length;
                    ret = Math.round(ret*0.5+nextValue*0.5);
                }
            }

            return ret*sym;
        },

        getBestPlaceSet: function(possible){

            var best = [];
            var middle = [];
            var ret = [];
            var i;

            for (i=0; i<possible.length; i++){  // SATISH: substitution for-in over possible
                var t = this.getValue(possible[i], null);
                if (t == 100) {
                    best.push(possible[i]);
                } else if (t >= 0) {
                    middle.push(possible[i]);
                }
            }

            if (best.length > 0){
                ret = best;
            } else if (middle.length > 0){
                ret = middle;
            } else {
                ret = possible;
            }
            return ret;
        },

        bestPlace: function(possible){

            if (possible.length == 0)
                console.log('Error: No possible places?!!!');
            possible = this.getBestPlaceSet(possible);

            var ret = [];
            if (possible.length > 0) {
                ret = possible[0];
                var p;

                var value = this.evaluate(ret, null, null, 0, null);
                for (p=1; p<possible.length; p++) {
                    var v = this.evaluate(possible[p], null, null, 0, null);
                    if (v > value){
                        value = v;
                        ret = possible[p];
                    }
                }
            } else {
                console.log('Error: No Setting place for Computer');
            }
            return ret;
        },
    };
    return w;
})();

World.init(1);
World.displayBoard();
World.click(2,4);
World.displayBoard();
World.click(5,3);
World.displayBoard();
World.click(4,2);
World.displayBoard();

World.init(1);
//World.displayBoard();
//World.click(2,4);
//World.displayBoard();
//World.click(5,3);
//World.displayBoard();
//World.click(4,2);
//World.displayBoard();


var IDC_BUTTON_00 = "IDC_BUTTON_00";
var IDC_BUTTON_01 = "IDC_BUTTON_01";
var IDC_BUTTON_02 = "IDC_BUTTON_02";
var IDC_BUTTON_03 = "IDC_BUTTON_03";
var IDC_BUTTON_04 = "IDC_BUTTON_04";
var IDC_BUTTON_05 = "IDC_BUTTON_05";
var IDC_BUTTON_06 = "IDC_BUTTON_06";
var IDC_BUTTON_07 = "IDC_BUTTON_07";
var IDC_BUTTON_10 = "IDC_BUTTON_10";
var IDC_BUTTON_11 = "IDC_BUTTON_11";
var IDC_BUTTON_12 = "IDC_BUTTON_12";
var IDC_BUTTON_13 = "IDC_BUTTON_13";
var IDC_BUTTON_14 = "IDC_BUTTON_14";
var IDC_BUTTON_15 = "IDC_BUTTON_15";
var IDC_BUTTON_16 = "IDC_BUTTON_16";
var IDC_BUTTON_17 = "IDC_BUTTON_17";
var IDC_BUTTON_20 = "IDC_BUTTON_20";
var IDC_BUTTON_21 = "IDC_BUTTON_21";
var IDC_BUTTON_22 = "IDC_BUTTON_22";
var IDC_BUTTON_23 = "IDC_BUTTON_23";
var IDC_BUTTON_24 = "IDC_BUTTON_24";
var IDC_BUTTON_25 = "IDC_BUTTON_25";
var IDC_BUTTON_26 = "IDC_BUTTON_26";
var IDC_BUTTON_27 = "IDC_BUTTON_27";
var IDC_BUTTON_30 = "IDC_BUTTON_30";
var IDC_BUTTON_31 = "IDC_BUTTON_31";
var IDC_BUTTON_32 = "IDC_BUTTON_32";
var IDC_BUTTON_33 = "IDC_BUTTON_33";
var IDC_BUTTON_34 = "IDC_BUTTON_34";
var IDC_BUTTON_35 = "IDC_BUTTON_35";
var IDC_BUTTON_36 = "IDC_BUTTON_36";
var IDC_BUTTON_37 = "IDC_BUTTON_37";
var IDC_BUTTON_40 = "IDC_BUTTON_40";
var IDC_BUTTON_41 = "IDC_BUTTON_41";
var IDC_BUTTON_42 = "IDC_BUTTON_42";
var IDC_BUTTON_43 = "IDC_BUTTON_43";
var IDC_BUTTON_44 = "IDC_BUTTON_44";
var IDC_BUTTON_45 = "IDC_BUTTON_45";
var IDC_BUTTON_46 = "IDC_BUTTON_46";
var IDC_BUTTON_47 = "IDC_BUTTON_47";
var IDC_BUTTON_50 = "IDC_BUTTON_50";
var IDC_BUTTON_51 = "IDC_BUTTON_51";
var IDC_BUTTON_52 = "IDC_BUTTON_52";
var IDC_BUTTON_53 = "IDC_BUTTON_53";
var IDC_BUTTON_54 = "IDC_BUTTON_54";
var IDC_BUTTON_55 = "IDC_BUTTON_55";
var IDC_BUTTON_56 = "IDC_BUTTON_56";
var IDC_BUTTON_57 = "IDC_BUTTON_57";
var IDC_BUTTON_60 = "IDC_BUTTON_60";
var IDC_BUTTON_61 = "IDC_BUTTON_61";
var IDC_BUTTON_62 = "IDC_BUTTON_62";
var IDC_BUTTON_63 = "IDC_BUTTON_63";
var IDC_BUTTON_64 = "IDC_BUTTON_64";
var IDC_BUTTON_65 = "IDC_BUTTON_65";
var IDC_BUTTON_66 = "IDC_BUTTON_66";
var IDC_BUTTON_67 = "IDC_BUTTON_67";
var IDC_BUTTON_70 = "IDC_BUTTON_70";
var IDC_BUTTON_71 = "IDC_BUTTON_71";
var IDC_BUTTON_72 = "IDC_BUTTON_72";
var IDC_BUTTON_73 = "IDC_BUTTON_73";
var IDC_BUTTON_74 = "IDC_BUTTON_74";
var IDC_BUTTON_75 = "IDC_BUTTON_75";
var IDC_BUTTON_76 = "IDC_BUTTON_76";
var IDC_BUTTON_77 = "IDC_BUTTON_77";
var IDC_LABEL1 = "IDC_LABEL1";

var BUTTON_COUNT = 64;

var HEADER_STYLE_TITLE  = 0;
var E_SUCCESS = 0; // NOT 1 as originally written...
var ALIGNMENT_RIGHT = 2;

function AnnexActionEventListener (f) {

    this.annexForm = f;

    this.OnActionPerformed = function (source, actionId) {

       var i = Math.floor(actionId / 8);

       var j = actionId - (i*8);

       World.click(i,j);

       this.annexForm.showBoard();

       var displayText = "B: " + World.status.bc + " W: " + World.status.wc + " Result: " + World.status.result;

       var s = new TizenLib.String(displayText);

       this.annexForm.labelPrint.SetText(s);

       this.annexForm.Draw();
    }
}

AnnexActionEventListener.prototype = new TizenLib.IActionEventListener();

function AnnexForm() {

   this.labelPrint = null; // this is initialized in AddCalculatorPanel

   this.ael = new AnnexActionEventListener(this);

   // this.calcModel = new CalculatorModel();

   // Formerly Construct, but this allows us to inherit Form::Construct
   this.init = function() {
        var path = new TizenLib.String("IDL_FORM");
        var r = this.Construct(path); // comes from prototype
//        var hdr = this.GetHeader(); // comes from prototype
//        r = hdr.SetStyle(HEADER_STYLE_TITLE);
//        var title = new TizenLib.String("Annex");
//        r = hdr.SetTitleText(title);
        return r;
   }

   this.OnInitializing = function() {
        var r = E_SUCCESS;

        r = this.AddAnnexPanel();

        return r;
   }

   this.OnTerminating = function () {
        var x = this; // INFERENCE HACK
        return E_SUCCESS;
   }

   this.OnDraw = function() {
        var x = this; // INFERENCE HACK
        return E_SUCCESS;
   }

    this.button_names = [
            IDC_BUTTON_00, IDC_BUTTON_01, IDC_BUTTON_02, IDC_BUTTON_03, IDC_BUTTON_04, IDC_BUTTON_05, IDC_BUTTON_06, IDC_BUTTON_07,
            IDC_BUTTON_10, IDC_BUTTON_11, IDC_BUTTON_12, IDC_BUTTON_13, IDC_BUTTON_14, IDC_BUTTON_15, IDC_BUTTON_16, IDC_BUTTON_17,
            IDC_BUTTON_20, IDC_BUTTON_21, IDC_BUTTON_22, IDC_BUTTON_23, IDC_BUTTON_24, IDC_BUTTON_25, IDC_BUTTON_26, IDC_BUTTON_27,
            IDC_BUTTON_30, IDC_BUTTON_31, IDC_BUTTON_32, IDC_BUTTON_33, IDC_BUTTON_34, IDC_BUTTON_35, IDC_BUTTON_36, IDC_BUTTON_37,
            IDC_BUTTON_40, IDC_BUTTON_41, IDC_BUTTON_42, IDC_BUTTON_43, IDC_BUTTON_44, IDC_BUTTON_45, IDC_BUTTON_46, IDC_BUTTON_47,
            IDC_BUTTON_50, IDC_BUTTON_51, IDC_BUTTON_52, IDC_BUTTON_53, IDC_BUTTON_54, IDC_BUTTON_55, IDC_BUTTON_56, IDC_BUTTON_57,
            IDC_BUTTON_60, IDC_BUTTON_61, IDC_BUTTON_62, IDC_BUTTON_63, IDC_BUTTON_64, IDC_BUTTON_65, IDC_BUTTON_66, IDC_BUTTON_67,
            IDC_BUTTON_70, IDC_BUTTON_71, IDC_BUTTON_72, IDC_BUTTON_73, IDC_BUTTON_74, IDC_BUTTON_75, IDC_BUTTON_76, IDC_BUTTON_77
    ];

   this.AddAnnexPanel = function () {

        var i = 0;

        while (i < BUTTON_COUNT) {
            var button_name = new TizenLib.String(this.button_names[i]);
            var button = TizenLib.casts.Button_of_Control(this.GetControl(button_name)); //Button.downcast(GetControl(button_names[i]));

            button.SetActionId(i);
            button.AddActionEventListener(this.ael);

            i++;
        }

        this.showBoard();

        var id = new TizenLib.String(IDC_LABEL1);
        this.labelPrint = TizenLib.casts.Label_of_Control(this.GetControl(id)); //Label.downcast(this.GetControl(IDC_LBL_DISPLAY));

        this.labelPrint.SetTextHorizontalAlignment(ALIGNMENT_RIGHT);

        return E_SUCCESS;
   }

   this.showBoard = function () {
        var i, j;
        for (i=0; i<World.bounder; i++){
            for (j=0; j<World.bounder; j++){
                var c = "-";
                if (World.board[i][j] === 'black')
                    c = "B";
                else if (World.board[i][j] === 'white')
                    c = "W";
                var name = this.button_names[i*8 + j];
               var button_name = new TizenLib.String(name);
               var button = TizenLib.casts.Button_of_Control(this.GetControl(button_name));
               var x = new TizenLib.String(c);
               button.SetText(x);
            }
        }
   }
}

AnnexForm.prototype = new TizenLib.Form();

function Annex() {
    this.OnAppInitializing = function (appRegistry) {
        var frame = new TizenLib.Frame();
        frame.Construct();
        this.AddFrame(frame);
        var form = new AnnexForm();
        form.init(); // formerly Construct()
        frame.AddControl(form);
        frame.SetCurrentForm(form);
        return true;
    }
    this.OnAppTerminating = function (appRegistry, urgent) {
        var x = this; // INFERENCE HACK
        return true;
    }
}

Annex.prototype = new TizenLib.Application();

console.log("hello");
var annex = new Annex();
__platform_return(annex);
