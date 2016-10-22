

// TODO:
// - the original C++ code relies on in-place mutation of op1 and op2 strings, whereas
//   we have reassigned different string instances to op1 and op2.
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

function State() {

    // constants to be embedded in the state object

    this.errString = "ERROR";

    this.calcMode = MODE_OPERAND1;

    this.operator = BTN_ID_COUNT;

    this.op1 = "0";

    this.op2 = "";

    this.GetResult =  function() {
            switch (this.calcMode) {
            case MODE_OPERATOR:
            case MODE_RESULT:
            case MODE_OPERAND1:
                return this.op1;

            case MODE_OPERAND2:
                if (this.op2 == "") {
                    return this.op1;
                }
                return this.op2;

            case MODE_INVALID:
                return this.errString;

            }
            return this.errString;
        };

    this.UpdateOperand = function (k) {
            var op = "";
            var charCount = MAX_PRINTABLE_CHARS;
            var index = -1;

            if (this.calcMode === MODE_OPERAND1) {
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

                if (k == BTN_ID_DOT) {
                    if (index == -1) { // DOT was not already in the string
                        if (op == "" || op == "-") {
                            op = op.concat("0");
                        }
                        op = op.concat(".");
                    }
                } else if ((BTN_ID_0 <= k) && (k <= BTN_ID_9)) {
                    if (op == "0") {
                        op = "";
                    }
                    op = op.concat(k.toString());
                } else if (k == BTN_ID_MINUS) {
                    if (op == "") {
                        op = op.concat("-");
                    }
                }
                // write back op into the correct of op1 or op2
                if (this.calcMode === MODE_OPERAND1) {
                    this.op1 = op;
                } else { // MODE_OPERAND2 is assured
                    this.op2 = op;
                }
                return true;
            }
            return false;
        };

    this.HandleNumKeys = function (k) {
            switch (this.calcMode) {
            case MODE_INVALID:
                return true;
            case MODE_RESULT:
                this.HandleClearKey();
                this.calcMode = MODE_OPERAND1;
                break;
            case MODE_OPERATOR:
                this.op2 = "";
                this.calcMode = MODE_OPERAND2;
                break;
            case MODE_OPERAND1:
            case MODE_OPERAND2:
                break;
            }
            return this.UpdateOperand(k);
        };

    this.HandleOperatorKeys = function (k) {
            switch (this.calcMode) {
            case MODE_INVALID:
                return;

            case MODE_OPERATOR:
                if ((k === BTN_ID_PLUS) || (k === BTN_ID_MINUS)) {
                    this.op2 = "";
                    this.UpdateOperand(k);
                    this.calcMode = MODE_OPERAND2;
                    break;
                }
                this.operator = k;
                break;

            case MODE_OPERAND1:
                if ((this.op1 == "") || (this.op1 == "0")) {
                    if (k == BTN_ID_PLUS || k == BTN_ID_MINUS) {
                        if (this.op1 == "0") {
                            this.UpdateOperand(k);
                        } else {
                            this.op1 = "";
                            this.UpdateOperand(k);
                            break;
                        }
                    }
                    this.op1 = "0";
                }
                this.calcMode = MODE_OPERATOR;
                this.operator = k;
                break;

            case MODE_OPERAND2:
                this.HandleEqualKey();
                // fall through

            case MODE_RESULT:
                this.calcMode = MODE_OPERAND1;
                this.op2 = "";
                this.HandleOperatorKeys(k);
                break;
            }
        };

    this.Calculate = function () {
            if ((this.errString == "INF") || (this.errString == "-INF"))
                return;

            var op1 = parseFloat(this.op1);
            var op2 = parseFloat(this.op2);

            switch (this.operator) {
            case BTN_ID_PLUS:
                op1 += op2;
                break;

            case BTN_ID_MINUS:
                op1 -= op2;
                break;

            case BTN_ID_MULTIPLY:
                op1 *= op2;
                break;

            case BTN_ID_DIVIDE:
                if (op2 != 0) {
                    op1 /= op2;
                } else {
                    this.calcMode = MODE_INVALID;
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

            if (op1 > 0 && op1 < MIN_PRINTABLE_VALUE) {
                this.op1 = "0";
            } // TODO the other side of this

            if (this.op1 == "inf") { // why would this happen?
                this.errString = "INF";
                this.calcMode = MODE_INVALID;
            } else if (this.op1 == "-inf") {
                this.errString = "-INF";
                this.calcMode = MODE_INVALID;
            } else {
                this.calcMode = MODE_RESULT;
            }
        };

    this.HandleEqualKey = function () {
            switch (this.calcMode) {
            case MODE_INVALID:
                return;

            case MODE_OPERATOR:
                this.op2 = this.op1;
                break;

            case MODE_OPERAND1:
                this.op2 = "0";
                this.operator = BTN_ID_PLUS;
                break;

            case MODE_OPERAND2:
                if (this.op2 == "" || this.op2 == "-") {
                    if (this.operator == BTN_ID_MULTIPLY || this.operator == BTN_ID_DIVIDE) {
                        this.op2 = "1";
                    } else {
                        this.op2 = "0";
                    }
                }
                break;

            case MODE_RESULT:
                break;
            }
            this.Calculate();
        };

    this.HandleClearKey = function () {
            this.calcMode = MODE_OPERAND1;
            this.operator = BTN_ID_COUNT;
            this.op1 = "";
            this.op2 = "";
            this.errString = "";
        };

    this.HandleBackKey = function () {
            switch (this.calcMode) {
            case MODE_RESULT:
            case MODE_INVALID:
                return;
            case MODE_OPERATOR:
                this.operator = BTN_ID_COUNT;
                this.calcMode = MODE_OPERAND1;
            case MODE_OPERAND1:
                if (this.op1 != "") {
                   this.op1 = this.op1.substring(0,this.op1.length-1);
                }
                break;
            case MODE_OPERAND2:
                if (this.op2 != "") {
                    this.op2 = this.op2.substring(0,this.op2.length-1);
                } else {
                    this.calcMode = MODE_OPERATOR;
                    this.HandleBackKey();
                }
                break;
            }

        }
}

function Calculator(manager) {
    this.state = new State();
    this.manager = manager;
    this.RespondToKey = function (k) {
        var status;

        switch (k) {
        case BTN_ID_0:
        case BTN_ID_1:
        case BTN_ID_2:
        case BTN_ID_3:
        case BTN_ID_4:
        case BTN_ID_5:
        case BTN_ID_6:
        case BTN_ID_7:
        case BTN_ID_8:
        case BTN_ID_9:
        case BTN_ID_DOT:
            status = this.state.HandleNumKeys(k);
            if (status === false) {
                // console.log("Bad!");
                manager.popup("Bad!");
                return;
            }
            break;

        case BTN_ID_PLUS:
        case BTN_ID_MINUS:
        case BTN_ID_MULTIPLY:
        case BTN_ID_DIVIDE:
            this.state.HandleOperatorKeys(k);
            break;

        case BTN_ID_BACK:
            this.state.HandleBackKey();
            break;

        case BTN_ID_CLEAR:
            this.state.HandleClearKey();
            break;

        case BTN_ID_EQUAL:
            this.state.HandleEqualKey();
            break;

        // skipped the popup button thing
        }

        if ((this.state.errString == "INF") || (this.state.errString == "-INF")) {
            console.log(this.state.errString);
            return;
        }
        var displayText = this.state.GetResult();

        if (displayText == "") {
            displayText = "0";
        }

        var indexOfDec = displayText.indexOf(".");

        var indexOfE = displayText.indexOf("e");

        var l = displayText.length;

        if (l > MAX_DIGITS_DISPLAY) {
            if ((l - indexOfDec) > DIGITS_AFTER_DECIMAL_POINT) {
                if (indexOfE > 0) {
                    displayText = displayText.substring(0, indexOfDec + MAX_DIGITS_AFTER_DECIMAL_POINT - 1);
                } else {
                    displayText = displayText.substring(0, MAX_DIGITS_DISPLAY - 1);
                }
            }
        }

        //console.log(displayText);
        manager.changeResultText(displayText);
    }
}

function ConsoleUIManager() {
    this.calculator = new Calculator(this);
    this.changeResultText = function (text) {
       console.log ("DISPLAY>>" + text);
    }
    this.popup = function (text) {
        console.log ("POPUP>>" + text);
    }
    this.onKeypress = function (k) {
        this.calculator.RespondToKey(k);
    }
}

var c = new ConsoleUIManager();

c.onKeypress(BTN_ID_MINUS);
c.onKeypress(BTN_ID_3);
c.onKeypress(BTN_ID_3);
c.onKeypress(BTN_ID_DOT);
c.onKeypress(BTN_ID_2);
c.onKeypress(BTN_ID_DIVIDE);
c.onKeypress(BTN_ID_MINUS);
c.onKeypress(BTN_ID_3);
//c.onKeypress(BTN_ID_DOT);
//c.onKeypress(BTN_ID_3);
c.onKeypress(BTN_ID_EQUAL);
c.onKeypress(BTN_ID_CLEAR);
