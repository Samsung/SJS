
// TODO:
// Note that this version is OK for type checking but not OK for end-to-end compilation
// because case labels are not constants (they are field reads).

// TODO:
// - the original C++ code relies on in-place mutation of op1 and op2 strings, whereas
//   we have reassigned different string instances to op1 and op2.


function mkState() {

    // constants to be embedded in the state object
    var BTN_ID_0 = 0;
    var BTN_ID_1 = 1;
    var BTN_ID_2 = 2;
    var BTN_ID_3 = 3;
    var BTN_ID_4 = 4;
    var BTN_ID_5 = 5;
    var BTN_ID_6 = 6;
    var BTN_ID_7 = 7;
    var BTN_ID_8 = 8;
    var BTN_ID_9 = 9;
    var BTN_ID_DOT = 10;
    var BTN_ID_PLUS = 11;
    var BTN_ID_MINUS = 12;
    var BTN_ID_MULTIPLY = 13;
    var BTN_ID_DIVIDE = 14;
    var BTN_ID_BACK = 15;
    var BTN_ID_CLEAR = 16;
    var BTN_ID_EQUAL = 17;
    var BTN_ID_COUNT = 18;

    var MODE_OPERAND1 = 100;
    var MODE_OPERATOR = 101;
    var MODE_OPERAND2 = 102;
    var MODE_RESULT = 103;
    var MODE_INVALID = 104;

    var MAX_PRINTABLE_CHARS = 20;
    var MAX_DIGITS_DISPLAY = 15;
    var DIGITS_AFTER_DECIMAL_POINT = 7;
    var MAX_DIGITS_AFTER_DECIMAL_POINT = 9;
    var MIN_PRINTABLE_VALUE = 0.000000001;

    return {
        BTN_ID_0 : BTN_ID_0,
        BTN_ID_1 : BTN_ID_1,
        BTN_ID_2 : BTN_ID_2,
        BTN_ID_3 : BTN_ID_3,
        BTN_ID_4 : BTN_ID_4,
        BTN_ID_5 : BTN_ID_5,
        BTN_ID_6 : BTN_ID_6,
        BTN_ID_7 : BTN_ID_7,
        BTN_ID_8 : BTN_ID_8,
        BTN_ID_9 : BTN_ID_9,
        BTN_ID_DOT : BTN_ID_DOT,
        BTN_ID_PLUS : BTN_ID_PLUS,
        BTN_ID_MINUS : BTN_ID_MINUS,
        BTN_ID_MULTIPLY : BTN_ID_MULTIPLY,
        BTN_ID_DIVIDE : BTN_ID_DIVIDE,
        BTN_ID_BACK : BTN_ID_BACK,
        BTN_ID_CLEAR : BTN_ID_CLEAR,
        BTN_ID_EQUAL : BTN_ID_EQUAL,
        BTN_ID_COUNT : BTN_ID_COUNT,

        MODE_OPERAND1 : MODE_OPERAND1,
        MODE_OPERATOR : MODE_OPERATOR,
        MODE_OPERAND2 : MODE_OPERAND2,
        MODE_RESULT : MODE_RESULT,
        MODE_INVALID : MODE_INVALID,

        MAX_PRINTABLE_CHARS : MAX_PRINTABLE_CHARS,
        MAX_DIGITS_DISPLAY : MAX_DIGITS_DISPLAY,
        DIGITS_AFTER_DECIMAL_POINT : DIGITS_AFTER_DECIMAL_POINT,
        MAX_DIGITS_AFTER_DECIMAL_POINT : MAX_DIGITS_AFTER_DECIMAL_POINT,
        MIN_PRINTABLE_VALUE : MIN_PRINTABLE_VALUE,

        errString : "ERROR",
        calcMode : MODE_OPERAND1,
        operator : BTN_ID_COUNT,
        op1 : "0",
        op2 : "",

        getResult :  function() {
            switch (this.calcMode) {
            case this.MODE_OPERATOR:
            case this.MODE_RESULT:
            case this.MODE_OPERAND1:
                return this.op1;

            case this.MODE_OPERAND2:
                if (this.op2 == "") {
                    return this.op1;
                }
                return this.op2;

            case this.MODE_INVALID:
                return this.errString;

            }
            return this.errString;
        },

        updateOperand : function (k) {
            var op = "";
            var charCount = this.MAX_PRINTABLE_CHARS;
            var index = -1;

            if (this.calcMode === this.MODE_OPERAND1) {
                op = this.op1;
            } else { // MODE_OPERAND2 is assured
                op = this.op2;
            }

            var sign = op.charAt(0);

            if (sign == "-" || sign == "+") {
                charCount++;
            }

            index = op.indexOf(".");

            if (index != -1) {
                charCount++;
            }

            if (op.length < charCount) {

                if (k == this.BTN_ID_DOT) {
                    if (index == -1) { // DOT was not already in the string
                        if (op == "" || op == "-") {
                            op = op.concat("0");
                        }
                        op = op.concat(".");
                    }
                } else if ((this.BTN_ID_0 <= k) && (k <= this.BTN_ID_9)) {
                    if (op == "0") {
                        op = "";
                    }
                    op = op.concat(k.toString());
                } else if (k == this.BTN_ID_MINUS) {
                    if (op == "") {
                        op = op.concat("-");
                    }
                }
                // write back op into the correct of op1 or op2
                if (this.calcMode === this.MODE_OPERAND1) {
                    this.op1 = op;
                } else { // MODE_OPERAND2 is assured
                    this.op2 = op;
                }
                return true;
            }
            return false;
        },

        handleNumKeys : function (k) {
            switch (this.calcMode) {
            case this.MODE_INVALID:
                return true;
            case this.MODE_RESULT:
                this.handleClearKey();
                this.calcMode = this.MODE_OPERAND1;
                break;
            case this.MODE_OPERATOR:
                this.op2 = "";
                this.calcMode = this.MODE_OPERAND2;
                break;
            case this.MODE_OPERAND1:
            case this.MODE_OPERAND2:
                break;
            }
            return this.updateOperand(k);
        },

        handleOperatorKeys : function (k) {
            switch (this.calcMode) {
            case this.MODE_INVALID:
                return;

            case this.MODE_OPERATOR:
                if ((k === this.BTN_ID_PLUS) || (k === this.BTN_ID_MINUS)) {
                    this.op2 = "";
                    this.updateOperand(k);
                    this.calcMode = this.MODE_OPERAND2;
                    break;
                }
                this.operator = k;
                break;

            case this.MODE_OPERAND1:
                if ((this.op1 == "") || (this.op1 == "0")) {
                    if (k == this.BTN_ID_PLUS || k == this.BTN_ID_MINUS) {
                        if (this.op1 == "0") {
                            this.updateOperand(k);
                        } else {
                            this.op1 = "";
                            this.updateOperand(k);
                            break;
                        }
                    }
                    this.op1 = "0";
                }
                this.calcMode = this.MODE_OPERATOR;
                this.operator = k;
                break;

            case this.MODE_OPERAND2:
                this.handleEqualKey();
                // fall through

            case this.MODE_RESULT:
                this.calcMode = this.MODE_OPERAND1;
                this.op2 = "";
                this.handleOperatorKeys(k);
                break;
            }
        },

        calculate : function () {
            if ((this.errString == "INF") || (this.errString == "-INF"))
                return;

            var op1 = parseFloat(this.op1);
            var op2 = parseFloat(this.op2);

            switch (this.operator) {
            case this.BTN_ID_PLUS:
                op1 += op2;
                break;

            case this.BTN_ID_MINUS:
                op1 -= op2;
                break;

            case this.BTN_ID_MULTIPLY:
                op1 *= op2;
                break;

            case this.BTN_ID_DIVIDE:
                if (op2 != 0) {
                    op1 /= op2;
                } else {
                    this.calcMode = this.MODE_INVALID;
                    if (op1 == 0) {
                        this.errString = "NAN";
                    } else {
                        if (op1 > 0) {
                            this.errString = "INF";
                        } else {
                            this.errString = "-INF";
                        }
                    }
                    return;
                }
                break;
            }

            var indexOfDec = 0;

            indexOfDec = op1.toString().indexOf(".");

            if (indexOfDec > 7 || (indexOfDec == -1 && op1.toString().length > 8)) {
                this.op1 = op1.toExponential(7);
            } else {
                this.op1 = op1.toString();
            }
            // TODO handle the case of +/- 0.000000002

            if (op1 > 0 && op1 < this.MIN_PRINTABLE_VALUE) {
                this.op1 = "0";
            } // TODO the other side of this

            if (this.op1 == "inf") { // why would this happen?
                this.errString = "INF";
                this.calcMode = this.MODE_INVALID;
            } else if (this.op1 == "-inf") {
                this.errString = "-INF";
                this.calcMode = this.MODE_INVALID;
            } else {
                this.calcMode = this.MODE_RESULT;
            }
        },

        handleEqualKey : function () {
            switch (this.calcMode) {
            case this.MODE_INVALID:
                return;

            case this.MODE_OPERATOR:
                this.op2 = this.op1;
                break;

            case this.MODE_OPERAND1:
                this.op2 = "0";
                this.operator = this.BTN_ID_PLUS;
                break;

            case this.MODE_OPERAND2:
                if (this.op2 == "" || this.op2 == "-") {
                    if (this.operator == this.BTN_ID_MULTIPLY || this.operator == this.BTN_ID_DIVIDE) {
                        this.op2 = "1";
                    } else {
                        this.op2 = "0";
                    }
                }
                break;

            case this.MODE_RESULT:
                break;
            }
            this.calculate();
        },

        handleClearKey : function () {
            this.calcMode = this.MODE_OPERAND1;
            this.operator = this.BTN_ID_COUNT;
            this.op1 = "";
            this.op2 = "";
            this.errString = "";
        },

        handleBackKey : function () {
            switch (this.calcMode) {
            case this.MODE_RESULT:
            case this.MODE_INVALID:
                return;
            case this.MODE_OPERATOR:
                this.operator = this.BTN_ID_COUNT;
                this.calcMode = this.MODE_OPERAND1;
            case this.MODE_OPERAND1:
                if (this.op1 != "") {
                   this.op1 = this.op1.substring(0,this.op1.length-1);
                }
                break;
            case this.MODE_OPERAND2:
                if (this.op2 != "") {
                    this.op2 = this.op2.substring(0,this.op2.length-1);
                } else {
                    this.calcMode = this.MODE_OPERATOR;
                    this.handleBackKey();
                }
                break;
            }

        }
    }
}

function respondToKey(state, k) {
    var status;

    switch (k) {
    case state.BTN_ID_0:
    case state.BTN_ID_1:
    case state.BTN_ID_2:
    case state.BTN_ID_3:
    case state.BTN_ID_4:
    case state.BTN_ID_5:
    case state.BTN_ID_6:
    case state.BTN_ID_7:
    case state.BTN_ID_8:
    case state.BTN_ID_9:
    case state.BTN_ID_DOT:
        status = state.handleNumKeys(k);
        if (status === false) {
            console.log("Bad!");
            return;
        }
        break;

    case state.BTN_ID_PLUS:
    case state.BTN_ID_MINUS:
    case state.BTN_ID_MULTIPLY:
    case state.BTN_ID_DIVIDE:
        state.handleOperatorKeys(k);
        break;

    case state.BTN_ID_BACK:
        state.handleBackKey();
        break;

    case state.BTN_ID_CLEAR:
        state.handleClearKey();
        break;

    case state.BTN_ID_EQUAL:
        state.handleEqualKey();
        break;

    // skipped the popup button thing
    }

    if ((state.errString == "INF") || (state.errString == "-INF")) {
        console.log(state.errString);
        return;
    }
    var displayText = state.getResult();

    if (displayText == "") {
        displayText = "0";
    }

    var indexOfDec = displayText.indexOf(".");

    var indexOfE = displayText.indexOf("e");

    var l = displayText.length;

    if (l > state.MAX_DIGITS_DISPLAY) {
        if ((l - indexOfDec) > state.DIGITS_AFTER_DECIMAL_POINT) {
            if (indexOfE > 0) {
                displayText = displayText.substring(0, indexOfDec + state.MAX_DIGITS_AFTER_DECIMAL_POINT - 1);
            } else {
                displayText = displayText.substring(0, state.MAX_DIGITS_DISPLAY - 1);
            }
        }
    }

    console.log(displayText);
}

function harness() {
    var state = mkState();

    respondToKey(state, state.BTN_ID_MINUS);
    respondToKey(state, state.BTN_ID_3);
    respondToKey(state, state.BTN_ID_3);
    respondToKey(state, state.BTN_ID_DOT);
    respondToKey(state, state.BTN_ID_2);
    respondToKey(state, state.BTN_ID_DIVIDE);
    respondToKey(state, state.BTN_ID_MINUS);
    respondToKey(state, state.BTN_ID_3);
    //respondToKey(state, state.BTN_ID_DOT);
    //respondToKey(state, state.BTN_ID_3);
    respondToKey(state, state.BTN_ID_EQUAL);
    respondToKey(state, state.BTN_ID_CLEAR);

/*
    respondToKey(state, state.BTN_ID_9);
    respondToKey(state, state.BTN_ID_9);
    respondToKey(state, state.BTN_ID_9);
    respondToKey(state, state.BTN_ID_9);
    respondToKey(state, state.BTN_ID_9);
    respondToKey(state, state.BTN_ID_9);
    respondToKey(state, state.BTN_ID_9);
    respondToKey(state, state.BTN_ID_9);
    respondToKey(state, state.BTN_ID_MULTIPLY);
    respondToKey(state, state.BTN_ID_9);
    respondToKey(state, state.BTN_ID_9);
        respondToKey(state, state.BTN_ID_9);
        respondToKey(state, state.BTN_ID_9);
        respondToKey(state, state.BTN_ID_9);
        respondToKey(state, state.BTN_ID_9);
        respondToKey(state, state.BTN_ID_9);
        respondToKey(state, state.BTN_ID_9);
    respondToKey(state, state.BTN_ID_EQUAL);
    respondToKey(state, state.BTN_ID_CLEAR);
*/
}

harness();
