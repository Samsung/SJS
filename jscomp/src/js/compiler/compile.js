/* 
 * Copyright 2015-2016 Samsung Research America, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#!/usr/bin/env node
// Author: Koushik Sen

(function () {
    var global = this;
    var JSON = {parse: global.JSON.parse, stringify: global.JSON.stringify};
    var targetLang = "JS";
    var PREFIX = "JS";
    var AS_MODULE = false;
    var ENV_VAR_NAME = PREFIX + "_Env";
    var BASE_VAR_NAME = PREFIX + "_Base";
    var ARGS_VAR_NAME = PREFIX + "_Args";
    var ARGUMENTS_VAR_NAME = PREFIX + "_" + "argumentsRenamed";
    var FUN_NAME_PREFIX = PREFIX + "_Fun";
    var FUN_VAR_NAME = PREFIX + "_Fun";
    var REGISTER_PREFIX = PREFIX + "_R";
    var LABEL_PREFIX = PREFIX + "_Label";
    var BLOCK_LABEL_PREFIX = PREFIX + "_BLabel";
    var COMPLETION_JUMP_VAR_NAME_PREFIX = PREFIX + "_FinallyJump";
    var COMPLETION_RETURN_VAR_NAME_PREFIX = PREFIX + "_FinallyReturn";
    var COMPLETION_TYPE_VAR_NAME_PREFIX = PREFIX + "_FinallyType";
    var RETURN_VAR_NAME = PREFIX + "_Return";
    var JUMP_TYPE_VAR_NAME = PREFIX + "_JumpType";
    var TMP_VAR_PREFIX = PREFIX+"_TmpLocal";

    var JUMP_TYPE_NORMAL = 0;
    var JUMP_TYPE_RETURN = 1;
    var JUMP_TYPE_EXCEPTION = 2;

    var N_JUMP_TYPES = 5;
    var BEGIN_OFFSET = 0;
    var CONTINUE_OFFSET = 1;
    var END_OFFSET = 2;
    var IF_OFFSET = 3;


    var NATIVE_MARKER;
    var FUN_RETURNTYPE;
    var VALUE_TYPE;
    var HEADER;
    var DRIVER;

    function initBackend(filename) {
        var path = filename.split('/');
        console.log(path);
        console.log(path.length);
        console.log(path.length-1);
        console.log(path[path.length-1]);
        var basename = path[path.length-1];
        // endsWith polyfill from: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/endsWith
        if (!String.prototype.endsWith) {
            String.prototype.endsWith = function(searchString, position) {
            var subjectString = this.toString();
            if (typeof position !== 'number' || !isFinite(position) || Math.floor(position) !== position || position > subjectString.length) {
                position = subjectString.length;
            }
            position -= searchString.length;
            var lastIndex = subjectString.indexOf(searchString, position);
            return lastIndex !== -1 && lastIndex === position;
          };
        }
        var base = basename.endsWith(".js") ? basename.substr(0,basename.length-3) : basename;
        if (targetLang === "C") {
            FUN_RETURNTYPE = "void";
            NATIVE_MARKER = "use C:";
            VALUE_TYPE = "Value";
            HEADER = "#include \"interop_export.h\"\n" +
            "extern " + VALUE_TYPE + " " + RETURN_VAR_NAME + ";\n" +
            "int " + JUMP_TYPE_VAR_NAME + ";\n\n";
            // change driver, and stick basename into FUN_NAME_PREFIX for C-linkage
            // namespacing
            FUN_NAME_PREFIX = PREFIX + "_" + base + "_Fun";
            if (AS_MODULE) {
                DRIVER = "Value __untyped_import_"+base+"(){\n\t" + FUN_NAME_PREFIX + "1();\n\treturn JS_Return;\n}";
            } else {
                DRIVER = "int main(){\n\tGC_INIT();\n\t" + FUN_NAME_PREFIX + "1(); return 0;}";
            }
        } else {
            NATIVE_MARKER = "use js:";
            VALUE_TYPE = "var";
            FUN_RETURNTYPE = "function";
            HEADER = VALUE_TYPE + " " + RETURN_VAR_NAME + ";\n" +
            VALUE_TYPE + " " + JUMP_TYPE_VAR_NAME + ";\n\n";
            DRIVER = FUN_NAME_PREFIX+"1();";
        }
    }



    var fileName;
    var first = true;

    var Syntax = {
        AssignmentExpression: 'AssignmentExpression', // done
        ArrayExpression: 'ArrayExpression', // done
        BlockStatement: 'BlockStatement',
        BinaryExpression: 'BinaryExpression', // done
        BreakStatement: 'BreakStatement',
        CallExpression: 'CallExpression', // done
        CatchClause: 'CatchClause',
        ConditionalExpression: 'ConditionalExpression', // done
        ContinueStatement: 'ContinueStatement',
        DoWhileStatement: 'DoWhileStatement',
        DebuggerStatement: 'DebuggerStatement',
        EmptyStatement: 'EmptyStatement',
        ExpressionStatement: 'ExpressionStatement',
        ForStatement: 'ForStatement',
        ForInStatement: 'ForInStatement',
        FunctionDeclaration: 'FunctionDeclaration', // done
        FunctionExpression: 'FunctionExpression', // done
        Identifier: 'Identifier', // done
        IfStatement: 'IfStatement',
        Literal: 'Literal', // done
        LabeledStatement: 'LabeledStatement',
        LogicalExpression: 'LogicalExpression', // done
        MemberExpression: 'MemberExpression', // done
        NewExpression: 'NewExpression', // done
        ObjectExpression: 'ObjectExpression', // done
        Program: 'Program',
        Property: 'Property', // done
        ReturnStatement: 'ReturnStatement',
        SequenceExpression: 'SequenceExpression', // done
        SwitchStatement: 'SwitchStatement',
        SwitchCase: 'SwitchCase',
        ThisExpression: 'ThisExpression', // done
        ThrowStatement: 'ThrowStatement',
        TryStatement: 'TryStatement',
        UnaryExpression: 'UnaryExpression', // done
        UpdateExpression: 'UpdateExpression', // done
        VariableDeclaration: 'VariableDeclaration', // done
        VariableDeclarator: 'VariableDeclarator', // done
        WhileStatement: 'WhileStatement',
        WithStatement: 'WithStatement'
    };

    var CONTEXT = {
        // TODO what is this?
        RHS: 1,
        // TODO what is this?
        IGNORE: 2,
        // inside the properties of an ObjectExpression
        OEXP: 3,
        // inside the formal parameters of a FunctionDeclaration or FunctionExpression
        PARAMS: 4,
        // TODO what is this?
        OEXP2: 5,
        // inside a getter
        GETTER: 6,
        // inside a setter
        SETTER: 7
    };

    function getNumericContext(node, context) {
        var object = context.object;
        var key = context.property;
        var type = object.type;

        var parentObject = context.parentContext.object;
        var parentType = parentObject.type;
        var parentKey = context.parentContext.property;
        var numContext;

        if ((type === 'AssignmentExpression' && key === 'left') ||
            (type === 'Property' && key === 'key') ||
            (type === 'UpdateExpression' && key === 'argument') ||
            (type === 'UnaryExpression' && key === 'argument' && object.operator === 'delete') ||
            (type === 'ForInStatement' && key === 'left') ||
            ((type === 'FunctionExpression' || type === 'FunctionDeclaration') && key === 'id') ||
            (type === 'LabeledStatement' && key === 'label') ||
            (type === 'BreakStatement' && key === 'label') ||
            (type === 'CatchClause' && key === 'param') ||
            (type === 'ContinueStatement' && key === 'label') ||
            ((type === 'CallExpression' || type === 'NewExpression') &&
            key === 'callee' &&
            (object.callee.type === 'MemberExpression' ||
            (object.callee.type === 'Identifier' && object.callee.name === 'eval'))) ||
            (type === 'VariableDeclarator' && key === 'id') ||
            (type === 'MemberExpression' && !object.computed && key === 'property')) {
            numContext = CONTEXT.IGNORE;
        } else if ((parentType === 'FunctionExpression' || parentType === 'FunctionDeclaration') && parentKey === 'params') {
            numContext = CONTEXT.IGNORE;
        } else if (object.key && key === 'value' && object.kind === 'get') {
            numContext = CONTEXT.GETTER;
        } else if (object.key && key === 'value' && object.kind === 'set') {
            numContext = CONTEXT.SETTER;
        } else if (type === 'CallExpression' && key === 'callee' && node.type === 'Identifier' && node.name === 'eval') {
            numContext = CONTEXT.IGNORE;
        } else {
            numContext = CONTEXT.RHS;
        }
        return numContext;
    }

    function transformAst(object, pre, visitorPre, visitorPost, post, context) {
        var key, child, type, ret, f, tmp2;

        type = object.type;
        if (pre) {
            pre(object, context);
        }
        if (visitorPre && (f = visitorPre[type])) {
            f(object, context);
        }

        for (key in object) {
            child = object[key];
            if (typeof child === 'object' && child !== null && key.indexOf("$") !== 0) {
                var tmp = transformAst(child, pre, visitorPre, visitorPost, post, {
                    object: object,
                    property: key,
                    parentContext: context
                });
                object[key] = tmp;
            }
        }

        if (visitorPost && (f = visitorPost[type])) {
            ret = f(object, context);
            if (ret) {
                object = ret;
            }
        }
        if (post) {
            tmp2 = post(object, context);
            if (tmp2) {
                object = tmp2;
            }
        }
        return object;
    }


    function isArr(val) {
        return Object.prototype.toString.call(val) === '[object Array]';
    }

    function MAP(arr, fun) {
        var len = arr.length;
        if (!isArr(arr)) {
            throw new TypeError();
        }
        if (typeof fun !== "function") {
            throw new TypeError();
        }

        var res = new Array(len);
        for (var i = 0; i < len; i++) {
            if (i in arr) {
                res[i] = fun(arr[i]);
            }
        }
        return res;
    }

    var currentFun;
    var funTable = [];
    var funStack = [];
    var nameCounter = 1;

    function newFunction(jsname) {
        currentFun = {
            instructions: [],
            name: FUN_NAME_PREFIX + nameCounter,
            registerCount: 0,
            iteratorCount: 0,
            blockLabelCount: 0,
            blockLabelStack: [],
            finallyBlockLabels: {},
            pendingJumps: {},
            jumpCounter: 0,
            jsname: jsname
        };
        nameCounter++;
        funTable.push(currentFun);
        funStack.push(currentFun);
        return currentFun.name;
    }

    function popFunction() {
        var tmp = funStack.pop();
        currentFun = funStack[funStack.length - 1];
        return tmp.name;
    }

    function getLabel(label) {
            return label;
    }

    function dumpFunctions() {
        var fs = require('fs'), path = require('path');

        var fd;
        if (targetLang === "C") {
            fd  = fs.openSync(fileName + ".c", "w");
        } else {
            fd  = fs.openSync(fileName + ".js", "w");
        }

        var line;

        if (targetLang !== "C") {
            line = fs.readFileSync(path.resolve(__dirname, '../runtime/instructions.js'), 'UTF-8');
            fs.writeSync(fd, line + "\n");
            console.log(line);
        }


        line = HEADER;
        fs.writeSync(fd, line + "\n");
        console.log(line);

        var len = funTable.length, i;

        if (targetLang === "C") {
            // add signature of functions defined next
            for (i = 0; i < len; i++) {
                line = FUN_RETURNTYPE + " " + funTable[i].name + "();";
                fs.writeSync(fd, line + "\n");
                console.log(line);

            }
        }
        for (i = 0; i < len; i++) {
            line = "// "+funTable[i].jsname;
            fs.writeSync(fd, line + "\n");
            console.log(line);

            line = FUN_RETURNTYPE + " " + funTable[i].name + "(){";
            fs.writeSync(fd, line + "\n");
            console.log(line);

            if (targetLang !== "C") {
                line = " while(true){\n  switch(OP_TARGET()) {\n    case -1:";
                fs.writeSync(fd, line + "\n");
                console.log(line);
            }

            var registerCount = funTable[i].registerCount;
            for (var k = 0; k <= registerCount; k++) {
                line = "        " + VALUE_TYPE + " " + NT(k) + ";";
                fs.writeSync(fd, line + "\n");
                console.log(line);
            }

            var iteratorCount = funTable[i].iteratorCount;
            for (var k = 0; k < iteratorCount; k++) {
                line = "        " + VALUE_TYPE + " " + TMP_VAR_PREFIX + k + ";";
                fs.writeSync(fd, line + "\n");
                console.log(line);
            }

            var finallyLabels = funTable[i].finallyBlockLabels;

            for (var finallyLabel in finallyLabels) {
                if (finallyLabels.hasOwnProperty(finallyLabel)) {
                    line = "        " + VALUE_TYPE + " " + COMPLETION_RETURN_VAR_NAME_PREFIX + finallyLabel + ";";
                    fs.writeSync(fd, line + "\n");
                    console.log(line);

                    if (targetLang === "C") {
                        line = "        int ";
                    } else {
                        line = "        var ";
                    }
                    line = line + COMPLETION_TYPE_VAR_NAME_PREFIX + finallyLabel + ";";
                    fs.writeSync(fd, line + "\n");
                    console.log(line);

                    if (targetLang === "C") {
                        line = "        int ";
                    } else {
                        line = "        var ";
                    }
                    line = line + COMPLETION_JUMP_VAR_NAME_PREFIX + finallyLabel + ";";
                    fs.writeSync(fd, line + "\n");
                    console.log(line);
                }
            }

            var instructions = funTable[i].instructions;
            var len2 = instructions.length;
            for (var j = 0; j < len2; j++) {
                line = instructions[j];
                if (line.indexOf(":") !== line.length - 1) {
                    line = "        "+line + ";";
                } else {
                    line = "    "+getLabel(line);
                }
                fs.writeSync(fd, line + "\n");
                console.log(line);
            }
            line = "        return;";
            fs.writeSync(fd, line + "\n");
            console.log(line);

            if (targetLang !== "C") {
                line = "  }\n }";
                fs.writeSync(fd, line + "\n");
                console.log(line);
            }

            line = "}";
            fs.writeSync(fd, line + "\n");
            console.log(line);
        }
        line = DRIVER;
        fs.writeSync(fd, line + "\n");
        console.log(line);
        fs.closeSync(fd);
    }

    var labelCount = 0;

    function freshLabel() {
        var ret = currentFun.blockLabelCount;
        currentFun.blockLabelCount = currentFun.blockLabelCount + N_JUMP_TYPES;
        return ret + IF_OFFSET;
        //
        //labelCount++;
        //return LABEL_PREFIX + labelCount;
    }

    function getTmpVar() {
        var tmp = currentFun.iteratorCount;
        currentFun.iteratorCount = currentFun.iteratorCount + 1;
        return TMP_VAR_PREFIX + tmp;
    }


    function zeroopInstructionNoAssign(args) {
        if (args.length === 1) {
            return args[0] + "( )";
        } else {
            throw new Error("Bad arguments");
        }
    }

    function unopInstructionNoAssign(args) {
        if (args.length === 2 && args[1] instanceof NT) {
            return args[0] + "( " + args[1] + " )";
        } else {
            throw new Error("Bad arguments");
        }
    }

    function zeroopInstruction(args) {
        if (args.length === 2 && args[0] instanceof NT) {
            return args[0] + " = " + args[1] + "( )";
        } else {
            throw new Error("Bad arguments");
        }
    }

    function jumpSimple(args) {
        if (args.length === 2 && typeof args[1] === "number") {
            if (targetLang === "C") {
                return args[0] + "( " + LABEL_PREFIX + args[1] + " )";
            } else {
                return "if ("+args[0] + "( " + args[1] + " )) break";
            }
        } else {
            throw new Error("Bad arguments");
        }
    }


    function jumpCond(args) {
        if (args.length === 3 && args[1] instanceof NT && typeof args[2] === "number") {
            if (targetLang === "C") {
                return args[0] + "( " + args[1] + " , " + LABEL_PREFIX + args[2] + " )";
            } else {
                return "if ("+args[0] + "( " + args[1] + " , " + args[2] + " )) break";
            }
        } else {
            throw new Error("Bad arguments");
        }
    }

    function unopInstruction(args) {
        if (args.length === 3 && args[0] instanceof NT && args[2] instanceof NT) {
            return args[0] + " = " + args[1] + "( " + args[2] + " )";
        } else {
            throw new Error("Bad arguments");
        }
    }

    function numberInstruction(args) {
        if (args.length === 3 && args[0] instanceof NT && typeof args[2] === "number") {
            return args[0] + " = " + args[1] + "( " + args[2] + " )";
        } else {
            throw new Error("Bad arguments");
        }
    }

    function booleanInstruction(args) {
        if (args.length === 3 && args[0] instanceof NT && typeof args[2] === "boolean") {
            return args[0] + " = " + args[1] + "( " + args[2] + " )";
        } else {
            throw new Error("Bad arguments");
        }
    }

    function stringInstruction(args) {
        console.warn(args[0]);
        if (args.length === 3 && args[0] instanceof NT && typeof args[2] === "string") {
            if (targetLang === "C" && args[1] == "OP_STRING") {
                return args[0] + " = " + args[1] + "( L" + args[2] + " )";
            } else {
                return args[0] + " = " + args[1] + "( " + args[2] + " )";
            }
        } else {
            throw new Error("Bad arguments");
        }
    }


    function binopInstructionNoAssign(args) {
        if (args.length === 3 && typeof args[1] === "string" && args[2] instanceof  NT) {
            return VALUE_TYPE + " " + args[1] + " = " + args[2];
        } else {
            throw new Error("Bad arguments");
        }
    }

    function getIndexInstruction(args) {
        if (args.length === 4 && args[0] instanceof NT && typeof args[2] === "string" && typeof args[3] === "number") {
            return args[0] + " = " + args[1] + "( " + args[2] + " , " + args[3] + " )";
        } else {
            throw new Error("Bad arguments");
        }
    }

    function binopInstruction(args) {
        if (args.length === 4 && args[0] instanceof NT && args[2] instanceof NT && args[3] instanceof NT) {
            return args[0] + " = " + args[1] + "( " + args[2] + " , " + args[3] + " )";
        } else {
            throw new Error("Bad arguments");
        }
    }

    function triopInstructionNoAssign(args) {
        if (args.length === 4 && args[1] instanceof NT && args[2] instanceof NT && args[3] instanceof NT) {
            return args[0] + "( " + args[1] + " , " + args[2] + " , " + args[3] + " )";
        } else {
            throw new Error("Bad arguments");
        }
    }

    function regexpInstruction(args) {
        if (args.length === 4 && args[0] instanceof NT && typeof args[2] === "string" && typeof args[3] === "string") {
            return args[0] + " = " + args[1] + "( " + args[2] + " , " + args[3] + " )";
        } else {
            throw new Error("Bad arguments");
        }
    }


    function jumpDynamic(args) {
        if (args.length === 4 && typeof args[1] === "string" && typeof args[2] === "number" && typeof args[3] === "number") {
            if (targetLang === "C") {
                return args[0] + "( " + args[1] + ", " + args[2] + " , " + LABEL_PREFIX + args[3] + " )";
            } else {
                return "if (" + args[0] + "( " + args[1] + ", " + args[2] + " , " + args[3] + " )) break ";
            }
        } else {
            throw new Error("Bad arguments");
        }
    }


    function functionInstruction(args) {
        if (args.length === 4 && args[0] instanceof NT && typeof args[2] === "string" && args[3] instanceof NT) {
            return args[0] + " = " + args[1] + "( " + args[2] + " , " + args[3] + " )";
        } else {
            throw new Error("Bad arguments");
        }
    }

    function setvarInstruction(args) {
        if (args.length === 4 && args[0] instanceof NT && typeof args[2] === "string" && args[3] instanceof NT) {
            return args[0] + " = " + args[2] + " = " + args[3];
        } else {
            throw new Error("Bad arguments");
        }
    }


    function triopInstruction(args) {
        if (args.length === 5 && args[0] instanceof NT && args[2] instanceof NT && args[3] instanceof NT && args[4] instanceof NT) {
            return args[0] + " = " + args[1] + "( " + args[2] + " , " + args[3] + " , " + args[4] + " )";
        } else {
            throw new Error("Bad arguments");
        }
    }

    function setIndexInstruction(args) {
        if (args.length === 5 && args[0] instanceof NT && typeof args[2] === "string" && typeof args[3] === "number" && args[4] instanceof NT) {
            return args[0] + " = " + args[1] + "( " + args[2] + " , " + args[3] + " , " + args[4] + " )";
        } else if (args.length === 4 && args[1] instanceof NT && typeof args[2] === "number" && args[3] instanceof NT) {
            return args[0] + "( " + args[1] + " , " + args[2] + " , " + args[3] + " )";
        } else if (args.length === 4 && args[1] instanceof NT && typeof args[2] === "number" && typeof args[3] === "string") {
            // permit inlining for global environment setup
            return args[0] + "( " + args[1] + " , " + args[2] + " , " + args[3] + " )";
        } else {
            throw new Error("Bad arguments");
        }
    }

    var instructions = { //@todo OP_CLEARARGS
        OP_CLEARARGS: zeroopInstructionNoAssign,
        OP_POPARG: zeroopInstruction,
        OP_PUSHARG: unopInstructionNoAssign,
        //
        OP_TYPEOF: unopInstruction,
        OP_POS: unopInstruction,
        OP_NEG: unopInstruction,
        OP_INC: unopInstruction,
        OP_DEC: unopInstruction,
        OP_BITNOT: unopInstruction,
        OP_LOGNOT: unopInstruction,
        OP_ITERATOR: unopInstruction,
        OP_NEXTKEY: unopInstruction,

        OP_BITOR: binopInstruction,
        OP_BITXOR: binopInstruction,
        OP_BITAND: binopInstruction,
        OP_EQ: binopInstruction,
        OP_NE: binopInstruction,
        OP_STRICTEQ: binopInstruction,
        OP_STRICTNE: binopInstruction,
        OP_LT: binopInstruction,
        OP_GT: binopInstruction,
        OP_LE: binopInstruction,
        OP_GE: binopInstruction,
        OP_INSTANCEOF: binopInstruction,
        OP_IN: binopInstruction,
        OP_SHL: binopInstruction,
        OP_SHR: binopInstruction,
        OP_USHR: binopInstruction,
        OP_ADD: binopInstruction,
        OP_SUB: binopInstruction,
        OP_MUL: binopInstruction,
        OP_DIV: binopInstruction,
        OP_MOD: binopInstruction,

        OP_NAN: zeroopInstruction,
        OP_UNDEF: zeroopInstruction,
        OP_NONEXISTENT: zeroopInstruction,
        OP_INFINITY: zeroopInstruction,
        OP_NULL: zeroopInstruction,
        OP_NUMBER: numberInstruction,
        OP_STRING: stringInstruction,
        OP_FUNCTION: functionInstruction,
        OP_NEWOBJECT: zeroopInstruction,
        OP_NEWARRAY: zeroopInstruction,
        OP_NEWARGUMENTS: zeroopInstruction,
        OP_NEWENV: numberInstruction,
        OP_NEWREGEXP: regexpInstruction,
        OP_BOOLEAN: booleanInstruction,
        OP_NEWBOX: zeroopInstruction,
        //
        OP_GETPROP: binopInstruction,
        OP_SETPROP: triopInstruction,
        OP_SETGETTER: triopInstructionNoAssign,
        OP_SETSETTER: triopInstructionNoAssign,
        OP_DELPROP: binopInstruction,

        OP_GETINDEX: getIndexInstruction, //@todo return undefined if index out of range
        OP_GETINDEXSTAR: getIndexInstruction, //@todo return new undefined ref if index out of range
        OP_SETINDEX: setIndexInstruction,
        OP_SETINDEXSTAR: setIndexInstruction,

        //@to
        OP_INITVAR: binopInstructionNoAssign,
        OP_GETVAR: stringInstruction,
        OP_GETVARSTAR: stringInstruction,
        OP_SETVAR: setvarInstruction,
        OP_SETVARSTAR: functionInstruction,
        //
        //
        OP_IFTRUE: jumpCond,
        OP_IFFALSE: jumpCond,
        OP_JUMP: jumpSimple,
        OP_JUMPNE: jumpDynamic,
        //
        OP_CALL: unopInstructionNoAssign,
        OP_NEW: unopInstructionNoAssign
    };

    var unaryOpToStr = {
        "typeof": "OP_TYPEOF",
        "+": "OP_POS",
        "-": "OP_NEG",
        "~": "OP_BITNOT",
        "!": "OP_LOGNOT",
        "++": "OP_INC",
        "--": "OP_DEC"
    };

    var binaryOpToInstr = {
        "|": "OP_BITOR",
        "^": "OP_BITXOR",
        "&": "OP_BITAND",
        "==": "OP_EQ",
        "!=": "OP_NE",
        "===": "OP_STRICTEQ",
        "!==": "OP_STRICTNE",
        "<": "OP_LT",
        ">": "OP_GT",
        "<=": "OP_LE",
        ">=": "OP_GE",
        "instanceof": "OP_INSTANCEOF",
        "in": "OP_IN",
        "<<": "OP_SHL",
        ">>": "OP_SHR",
        ">>>": "OP_USHR",
        "+": "OP_ADD",
        "-": "OP_SUB",
        "*": "OP_MUL",
        "/": "OP_DIV",
        "%": "OP_MOD"
    };

    function NT(n) {
        if (!(this instanceof  NT)) {
            return new NT(n);
        }
        if (currentFun.registerCount < n) {
            currentFun.registerCount = n;
        }
        this.n = n;
    }

    NT.prototype.toString = function () {
        return REGISTER_PREFIX + this.n;
    };

    function copyArray(args) {
        var len = args.length;
        var ret = new Array(len);
        for (var i = 0; i < len; i++) {
            ret[i] = args[i];
        }
        return ret;
    }

    function addInstructionAssign(nt, str) {
        if (!instructions.hasOwnProperty(str)) {
            throw new Error("Cannot find instruction: " + str);
        }
        str = instructions[str](copyArray(arguments));
        currentFun.instructions.push(str);
    }

    function addAssign(nt, str) {
        str = nt + " = " + str;
        currentFun.instructions.push(str);
    }

    function addLabel(label) {
        if (targetLang === "C") {
            currentFun.instructions.push(LABEL_PREFIX + label + ":");
        } else {
            currentFun.instructions.push("case " + label + ":");
        }
    }

    function addInstruction(str) {
        if (!instructions.hasOwnProperty(str)) {
            throw new Error("Cannot fund instruction: " + str);
        }
        var tmp = instructions[str](copyArray(arguments));
        currentFun.instructions.push(tmp);
    }

    function addComment(comment) {
        currentFun.instructions.push("// " + comment);
    }

    function addVerbatim(ccode) {
        currentFun.instructions.push(ccode);
    }

    var scope = null;

    function setScope(node) {
        scope = node.$scope;
    }

    function popScope() {
        scope = scope.parent;
    }

    function getEscapedVarName(name) {
        if (name.indexOf(PREFIX) === 0) {
            return PREFIX + name;
        } else if (name.indexOf("#") === 0) {
            return PREFIX + name.substring(1);
        } else if (scope.vars[name] === 'special') {
            return ARGS_VAR_NAME;
        } else if (name === "arguments") {
            return ARGUMENTS_VAR_NAME;
        } else {
            return name;
        }
    }


    function visit(node, nt) {
        if (node === null) return;
        var f = visitors[node.type];
        if (typeof f === 'function') {
            f(node, nt);
        } else {
            for (var key in node) {
                var child = node[key];
                if (typeof child === 'object' && child !== null && key.indexOf("$") !== 0) {
                    clearLabelSet();
                    visit(child, 0);
                }
            }
        }
    }

    function visitProperty(node, nt) {
        if (node.computed) {
            visit(node.property, nt);
        } else {
            addInstructionAssign(NT(nt), "OP_STRING", JSON.stringify(node.property.name));
        }
    }

    function addParam(paramStr, nt, i) {
        var old = paramStr;
        addComment(paramStr);
        paramStr = getEscapedVarName(paramStr);
        if (scope.isBoxed(old)) {
            addInstructionAssign(NT(nt), "OP_NEWBOX");
            addInstruction("OP_INITVAR", paramStr, NT(nt));
            addInstructionAssign(NT(nt), "OP_POPARG");
            addSetVar(NT(nt), old, NT(nt));
        } else {
            addInstructionAssign(NT(nt), "OP_POPARG");
            addInstruction("OP_INITVAR", paramStr, NT(nt));
        }
        if (scope.isArgumentsUsed()) {
            addGetVar(NT(nt+1), "arguments");
            addInstructionAssign(NT(nt + 2), "OP_NUMBER", i);
            addInstructionAssign(NT(nt), "OP_SETPROP", NT(nt + 1), NT(nt + 2), NT(nt));
        }
    }

    function visitParams(params, nt) {
        var len = params.length;
        addInstructionAssign(NT(nt), "OP_POPARG");
        addInstruction("OP_INITVAR", ENV_VAR_NAME, NT(nt));
        addInstructionAssign(NT(nt), "OP_POPARG");
        addInstruction("OP_INITVAR", FUN_VAR_NAME, NT(nt));
        addInstructionAssign(NT(nt), "OP_POPARG");
        addInstruction("OP_INITVAR", BASE_VAR_NAME, NT(nt));
        if (scope.isArgumentsUsed()) {
            addInstructionAssign(NT(nt), "OP_NEWARGUMENTS");
            setPrototype("Object", nt);
            addInstruction("OP_INITVAR", ARGS_VAR_NAME, NT(nt));
        }
        for (var i = 0; i < len; i++) {
            addParam(params[i].name, nt, i);
        }
    }

    function visitVars(nt) {
        var name, varType;
        for (name in scope.vars) {
            if ((varType = scope.hasOwnVar(name)) !== null && varType !== "arg" && varType !== "special") {
                addComment(name);
                var newName = getEscapedVarName(name);
                if (scope.isBoxed(name) && varType !== "catch") {
                    addInstructionAssign(NT(nt), "OP_NEWBOX");
                    addInstruction("OP_INITVAR", newName, NT(nt));
                } else {
                    addInstructionAssign(NT(nt), "OP_UNDEF");
                    addInstruction("OP_INITVAR", newName, NT(nt));
                }
            }
        }
    }

    function visitElements(elements, nt) {
        var len = elements.length;
        for (var i = 0; i < len; i++) {
            addInstructionAssign(NT(nt + 1), "OP_NUMBER", i);
            visit(elements[i], nt + 2);
            addInstructionAssign(NT(nt + 1), "OP_SETPROP", NT(nt), NT(nt + 1), NT(nt + 2));
        }
    }

    function visitExpressions(expressions, nt) {
        var len = expressions.length;
        for (var i = 0; i < len; i++) {
            visit(expressions[i], nt);
        }
    }

    function visitArguments(arguments_, nt) {
        var len = arguments_.length;
        for (var i = len - 1, j = 0; i >= 0; i--, j++) {
            visit(arguments_[i], nt + j);
        }
        for (i = len - 1, j = 0; i >= 0; i--, j++) {
            addInstruction("OP_PUSHARG", NT(nt+j));
        }
    }

    function visitProperties(properties, nt) {
        var len = properties.length;
        for (var i = 0; i < len; i++) {
            if (properties[i].key.type === 'Identifier') {
                addInstructionAssign(NT(nt + 1), "OP_STRING", JSON.stringify(properties[i].key.name));
            } else {
                addInstructionAssign(NT(nt + 1), "OP_STRING", JSON.stringify(properties[i].key.value + ""));
            }
            visit(properties[i].value, nt + 2);
            if (properties[i].kind === 'init') {
                addInstructionAssign(NT(nt + 1), "OP_SETPROP", NT(nt), NT(nt + 1), NT(nt + 2));
            } else if (properties[i].kind === 'set') {
                addInstruction("OP_SETSETTER", NT(nt), NT(nt + 1), NT(nt + 2));
            } else if (properties[i].kind === 'get') {
                addInstruction("OP_SETGETTER", NT(nt), NT(nt + 1), NT(nt + 2));
            }
        }
    }

    function addGetVar(lhs, name) {
        addComment(name);
        var newName = getEscapedVarName(name);
        if (scope.hasOwnVar(name)) {
            if (scope.isBoxed(name)) {
                addInstructionAssign(lhs, "OP_GETVARSTAR", newName);
            } else {
                addInstructionAssign(lhs, "OP_GETVAR", newName);
            }
        } else {
            addInstructionAssign(lhs, "OP_GETINDEXSTAR", ENV_VAR_NAME, scope.getIndexInEnvironment(name));
        }
    }

    function addSetVar(lhs, name, rhs) {
        addComment(name);
        var newName = getEscapedVarName(name);
        if (scope.hasOwnVar(name)) {
            if (scope.isBoxed(name)) {
                addInstructionAssign(lhs, "OP_SETVARSTAR", newName, rhs);
            } else {
                addInstructionAssign(lhs, "OP_SETVAR", newName, rhs);
            }
        } else {
            addInstructionAssign(lhs, "OP_SETINDEXSTAR", ENV_VAR_NAME, scope.getIndexInEnvironment(name), rhs);
        }
    }

    function addGetVarRef(lhs, name) {
        addComment(name);
        var newName = getEscapedVarName(name);
        if (scope.hasOwnVar(name)) {
            addInstructionAssign(lhs, "OP_GETVAR", newName);
        } else {
            addInstructionAssign(lhs, "OP_GETINDEX", ENV_VAR_NAME, scope.getIndexInEnvironment(name));
        }
    }

    function setPrototype(className, nt, base) {
        if (base === "Function") { // special handling for Function function to create a circular prototype chain
            addInstructionAssign(NT(nt + 1), "OP_STRING", JSON.stringify("__proto__"));
            addInstructionAssign(NT(nt + 1), "OP_SETPROP", NT(nt), NT(nt + 1), NT(nt));
        } else {
            addGetVar(NT(nt + 1), className);
            addInstructionAssign(NT(nt + 2), "OP_STRING", JSON.stringify("prototype"));
            addInstructionAssign(NT(nt + 2), "OP_GETPROP", NT(nt + 1), NT(nt + 2));
            addInstructionAssign(NT(nt + 1), "OP_STRING", JSON.stringify("__proto__"));
            addInstructionAssign(NT(nt + 1), "OP_SETPROP", NT(nt), NT(nt + 1), NT(nt + 2));
        }
    }


    function visitGlobals(node, nt) {
        var env = node.$scope.environment;
        var envLen = env.length;
        addComment("Creating env, base");
        addInstructionAssign(NT(nt), "OP_NEWENV", envLen);
        for (var i = 0; i < envLen; i++) {
            addComment(env[i]);
            addInstructionAssign(NT(nt + 1), "OP_NEWBOX");
            addInstruction("OP_SETINDEX", NT(nt), i, NT(nt + 1));
            addInstructionAssign(NT(nt + 1), "OP_NONEXISTENT");
            addInstruction("OP_SETINDEXSTAR", NT(nt), i, NT(nt + 1));
        }
        addInstruction("OP_INITVAR", ENV_VAR_NAME, NT(nt));
        addInstructionAssign(NT(nt), "OP_UNDEF");
        addInstruction("OP_INITVAR", BASE_VAR_NAME, NT(nt));
        addComment("Done creating env, base");
    }


    /**
     *
     * @param type {string} can be "return", "loop", "catch", "finally", "named", "switch"
     * @param name {string array} names of the label if type is "named" or "loop" and undefined otherwise
     * @returns {number}
     */
    function pushNewBlockLabel(type, name) {
        if (name.length > 0 && type !== "named" && type !== "loop") {
            throw new Error("Named block should have a name.");
        }
        var ret = currentFun.blockLabelCount;
        currentFun.blockLabelCount = currentFun.blockLabelCount + N_JUMP_TYPES;
        currentFun.blockLabelStack.push({label: ret, type: type, name: name});
        return ret;
    }

    function popBlockLabel() {
        currentFun.blockLabelStack.pop();
    }

    function iterateBlockLabels(f) {
        var len = currentFun.blockLabelStack.length;
        for (var i = len - 1; i >= 0; i--) {
            if (!f(currentFun.blockLabelStack[i].type, currentFun.blockLabelStack[i].label, currentFun.blockLabelStack[i].name)) {
                break;
            }
        }
    }

    function getStringBlockLabel(n) {
        return BLOCK_LABEL_PREFIX + n;
    }

    function getCompletionVarName(n) {
        var ret = COMPLETION_JUMP_VAR_NAME_PREFIX + n;
        currentFun.finallyBlockLabels[n] = true;
        return ret;
    }

    function addPending(map, type, name, id) {
        var key = type + ":" + name, container;
        if (!map.hasOwnProperty(key)) {
            map[key] = {id: id, counter: 0};
        }
        map[key].counter = map[key].counter + 1;
        return map[key].id;
    }

    function removePending(map, type, name, id) {
        var key = type + ":" + name, container;
        if (map.hasOwnProperty(key)) {
            map[key].counter = map[key].counter - 1;
            if (map[key].counter === 0) {
                delete map[key];
            }
        }
    }

    function getObjectSize(map) {
        var counter = 0;
        for (var key in map) {
            if (map.hasOwnProperty(key)) {
                counter++;
            }
        }
        return counter;
    }

    function iteratePending(map, f) {
        for (var key in map) {
            if (map.hasOwnProperty(key)) {
                var pair = key.split(":");
                var type = pair[0];
                var name = pair[1];
                if (name === "undefined") {
                    name = undefined;
                }
                f(type, name, map[key].id);
            }
        }
    }

    function getNextJumpCounter(type, name) {
        var ret = currentFun.jumpCounter = currentFun.jumpCounter + 1;
        return addPending(currentFun.pendingJumps, type, name, ret);
    }

    function addGetterInvocation(val, base, offset, nt) {
        addInstructionAssign(NT(nt), "OP_ISGETTER", base, offset);
        var label = freshLabel();
        addInstruction("OP_IFFALSE", NT(nt), label);

        addInstruction("OP_CLEARARGS");
        addInstruction("OP_PUSHARG", base); // push base object which will be bound to this
        addInstruction("OP_CALL", val);

        var returnLabel = freshLabel();
        addInstruction("OP_JUMPNE", JUMP_TYPE_VAR_NAME, JUMP_TYPE_EXCEPTION, returnLabel);
        addComment("throw after return");
        addThrowInstructions();
        addLabel(returnLabel);
        addAssign(val, RETURN_VAR_NAME);

        addLabel(label);
    }

    function addSetterInvocation(lhs, val, base, offset, nt) {
        addInstructionAssign(NT(nt), "OP_ISSETTER", base, offset);
        var label = freshLabel(), label2 = freshLabel();
        addInstruction("OP_IFFALSE", NT(nt), label);

        addInstructionAssign(NT(nt), "OP_GETPROP", base, offset);   // get the setter method

        addInstruction("OP_CLEARARGS");
        addInstruction("OP_PUSHARG", val);
        addInstruction("OP_PUSHARG", base); // push base object which will be bound to this
        addInstruction("OP_CALL", NT(nt));

        var returnLabel = freshLabel();
        addInstruction("OP_JUMPNE", JUMP_TYPE_VAR_NAME, JUMP_TYPE_EXCEPTION, returnLabel);
        addComment("throw after return");
        addThrowInstructions();
        addLabel(returnLabel);
        addAssign(lhs, val);
        addInstruction("OP_JUMP", label2);

        addLabel(label);
        addInstructionAssign(lhs, "OP_SETPROP", base, offset, val);
        addLabel(label2);
    }


    function addBreakInstructions(labelName, pendingJumpCounter) {
        var breakLabel, finallyLabel;
        iterateBlockLabels(function (type, label, name) {
            if ((type === "loop" || type === "switch" || type === "named") && breakLabel === undefined && (labelName === undefined || name.hasOwnProperty(labelName))) {
                breakLabel = label + END_OFFSET;
            }
            if (type === "finally" && finallyLabel === undefined && breakLabel === undefined) {
                finallyLabel = label + BEGIN_OFFSET;
            }
            return true;
        });
        if (breakLabel === undefined) {
            throw new Error("No label for break statement found.")
        }
        if (finallyLabel === undefined) {
            addInstruction("OP_JUMP", breakLabel);
            removePending(currentFun.pendingJumps, "break", labelName, pendingJumpCounter);
        } else {
            addAssign(getCompletionVarName(finallyLabel), getNextJumpCounter("break", labelName)); // @todo define JS_Finally(n) vars
            addInstruction("OP_JUMP", finallyLabel);
        }
    }

    function addContinueInstructions(labelName, pendingJumpCounter) {
        var continueLabel, finallyLabel;
        iterateBlockLabels(function (type, label, name) {
            if (type === "loop" && continueLabel === undefined && (labelName === undefined || name.hasOwnProperty(labelName))) {
                continueLabel = label + CONTINUE_OFFSET;
            }
            if (type === "finally" && finallyLabel === undefined && continueLabel === undefined) {
                finallyLabel = label + BEGIN_OFFSET;
            }
            return true;
        });
        if (continueLabel === undefined) {
            throw new Error("No label for continue statement found.")
        }
        if (finallyLabel === undefined) {
            addInstruction("OP_JUMP", continueLabel);
            removePending(currentFun.pendingJumps, "continue", labelName, pendingJumpCounter);
        } else {
            addAssign(getCompletionVarName(finallyLabel), getNextJumpCounter("continue", labelName)); // @todo define JS_Finally(n) vars
            addInstruction("OP_JUMP", finallyLabel);
        }
    }

    function addReturnInstructions(pendingJumpCounter) {
        var returnLabel, finallyLabel;
        iterateBlockLabels(function (type, label, name) {
            if (type === "return") {
                returnLabel = label + END_OFFSET;
            }
            if (type === "finally" && finallyLabel === undefined && returnLabel === undefined) {
                finallyLabel = label + BEGIN_OFFSET;
            }
            return true;
        });
        if (returnLabel === undefined) {
            throw new Error("No label for return statement found.")
        }
        if (finallyLabel === undefined) {
            addInstruction("OP_JUMP", returnLabel);
            removePending(currentFun.pendingJumps, "return", undefined, pendingJumpCounter);
        } else {
            addAssign(getCompletionVarName(finallyLabel), getNextJumpCounter("return")); // @todo define JS_Finally(n) vars
            addInstruction("OP_JUMP", finallyLabel);
        }
    }

    function addThrowInstructions(pendingJumpCounter) {
        var throwLabel, finallyLabel;
        iterateBlockLabels(function (type, label, name) {
            if (type === "catch" && throwLabel === undefined) {
                throwLabel = label + BEGIN_OFFSET;
            }
            if (type === "return" && throwLabel === undefined) {
                throwLabel = label + END_OFFSET;
            }
            if (type === "finally" && finallyLabel === undefined && throwLabel === undefined) {
                finallyLabel = label + BEGIN_OFFSET;
            }
            return true;
        });
        if (throwLabel === undefined) {
            throw new Error("No label for return statement found.")
        }
        if (finallyLabel === undefined) {
            addInstruction("OP_JUMP", throwLabel);
            removePending(currentFun.pendingJumps, "throw", undefined, pendingJumpCounter);
        } else {
            addAssign(getCompletionVarName(finallyLabel), getNextJumpCounter("throw")); // @todo define JS_Finally(n) vars
            addInstruction("OP_JUMP", finallyLabel);
        }
    }

    function addFinallyEndInstructions(label) {
        var pendingJumps = currentFun.pendingJumps;
        var endLabel = freshLabel();
        iteratePending(pendingJumps, function (type, name, jumpCounter) {
            var jLabel = freshLabel();
            addInstruction("OP_JUMPNE", COMPLETION_JUMP_VAR_NAME_PREFIX + label, jumpCounter, jLabel);
            addAssign(JUMP_TYPE_VAR_NAME, COMPLETION_TYPE_VAR_NAME_PREFIX + label);
            addAssign(RETURN_VAR_NAME, COMPLETION_RETURN_VAR_NAME_PREFIX + label);
            switch (type) {
                case "break":
                    addBreakInstructions(name, jumpCounter);
                    break;
                case "continue":
                    addContinueInstructions(name, jumpCounter);
                    break;
                case "return":
                    addReturnInstructions(jumpCounter);
                    break;
                case "throw":
                    addThrowInstructions(jumpCounter);
                    break;
            }
            addInstruction("OP_JUMP", endLabel);
            addLabel(jLabel);
        });
        addLabel(endLabel);
    }


    var labelSet = {};
    var labelSetSize = 0;

    function addToLabelSet(label) {
        if (labelSetSize === 0) {
            labelSet = {}; // things will be added to this set, so create a fresh copy
        }
        if (!labelSet.hasOwnProperty(label)) {
            labelSet[label] = true;
            labelSetSize = labelSetSize + 1;
        }
    }

    function clearLabelSet() {
        if (labelSetSize !== 0) {
            labelSet = {};
            labelSetSize = 0;
        }
    }

    function ifPresentInLabelSet(label) {
        return labelSet.hasOwnProperty(label);
    }

    function getLabelSet() {
        return labelSet;
    }

    function isEmptyLabelSet() {
        return labelSetSize === 0;
    }

    var visitors = {
        "Program": function (node, nt) {
            var iLabel = pushNewBlockLabel("return", getLabelSet());
            clearLabelSet();

            visitGlobals(node, nt);

            setScope(node);
            visitVars(0);
            visit(node.body, 0);
            popScope();

            addLabel(iLabel + END_OFFSET);
        },
        "FunctionExpression": function (node, nt) {
            var env = node.$scope.environment;
            var envLen = env.length;
            addComment("Creating function ");

            addInstructionAssign(NT(nt), "OP_NEWENV", envLen);
            for (var i = 0; i < envLen; i++) {
                addGetVarRef(NT(nt + 1), env[i]);
                addInstruction("OP_SETINDEX", NT(nt), i, NT(nt + 1));
            }

            setScope(node);
            newFunction(node.id?node.id.name:"");

            var iLabel = pushNewBlockLabel("return", getLabelSet());
            clearLabelSet();


            visitParams(node.params, 0);
            visitVars(0);
            if (node.id) {
                addAssign(NT(0), FUN_VAR_NAME);
                addSetVar(NT(0), node.id.name, NT(0));
            }
            visit(node.body, 0);

            addInstructionAssign(NT(0), "OP_UNDEF");
            addAssign(RETURN_VAR_NAME, NT(0));
            addAssign(JUMP_TYPE_VAR_NAME, JUMP_TYPE_RETURN);
            addLabel(iLabel + END_OFFSET);
            popBlockLabel();

            var name = popFunction();
            popScope();

            addInstructionAssign(NT(nt), "OP_FUNCTION", name, NT(nt));
            setPrototype("Function", nt);

//            create empty prototype object of the function
            addComment("Create empty prototype object of the function");
            addInstructionAssign(NT(nt+1), "OP_NEWOBJECT");
            setPrototype("Object", nt+1);
            addInstructionAssign(NT(nt + 2), "OP_STRING", JSON.stringify("constructor"));
            addInstructionAssign(NT(nt + 2), "OP_SETPROP", NT(nt + 1), NT(nt + 2), NT(nt));
            addInstructionAssign(NT(nt + 2), "OP_STRING", JSON.stringify("prototype"));
            addInstructionAssign(NT(nt + 1), "OP_SETPROP", NT(nt), NT(nt + 2), NT(nt + 1));

            addComment("Done creating function");


        },
        "FunctionDeclaration": function (node, nt) {
            var env = node.$scope.environment;
            var envLen = env.length;
            addComment("Creating function "+node.id.name);
            addInstructionAssign(NT(nt), "OP_NEWENV", envLen);
            for (var i = 0; i < envLen; i++) {
                addGetVarRef(NT(nt + 1), env[i]);
                addInstruction("OP_SETINDEX", NT(nt), i, NT(nt + 1));
            }

            setScope(node);
            newFunction(node.id.name);

            var iLabel = pushNewBlockLabel("return", getLabelSet());
            clearLabelSet();

            visitParams(node.params, 0);
            visitVars(0);
            visit(node.body, 0);

            addInstructionAssign(NT(0), "OP_UNDEF");
            addAssign(RETURN_VAR_NAME, NT(0));
            addAssign(JUMP_TYPE_VAR_NAME, JUMP_TYPE_RETURN);
            addLabel(iLabel + END_OFFSET);
            popBlockLabel();

            var name = popFunction();
            popScope();
            addComment("Point 2");
            addInstructionAssign(NT(nt), "OP_FUNCTION", name, NT(nt));
            setPrototype("Function", nt, node.id.name);
            addSetVar(NT(nt), node.id.name, NT(nt));

            // create empty prototype object of the function
            if (!first) { // do not create prototype object for Function at the time of bootstrapping.
                addComment("Create empty prototype object of the function");
                addInstructionAssign(NT(nt + 1), "OP_NEWOBJECT");
                setPrototype("Object", nt + 1);
                addInstructionAssign(NT(nt + 2), "OP_STRING", JSON.stringify("constructor"));
                addInstructionAssign(NT(nt), "OP_SETPROP", NT(nt + 1), NT(nt + 2), NT(nt));
                addInstructionAssign(NT(nt + 2), "OP_STRING", JSON.stringify("prototype"));
                addInstructionAssign(NT(nt + 1), "OP_SETPROP", NT(nt), NT(nt + 2), NT(nt + 1));

            } else {
                first = false;
            }
            addComment("Done creating function");
        },
        'VariableDeclarator': function (node, nt) {
            if (node.init !== null) {
                visit(node.init, 0);
                addSetVar(NT(nt), node.id.name, NT(nt));
            }
        },
        "BinaryExpression": function (node, nt) {
            visit(node.left, nt);
            visit(node.right, nt + 1);
            addInstructionAssign(NT(nt), binaryOpToInstr[node.operator], NT(nt), NT(nt + 1));

        },
        'UnaryExpression': function (node, nt) {
            if (node.operator === "void") {
                visit(node.argument, nt);
                addInstructionAssign(NT(nt), "OP_UNDEF");
            } else if (node.operator === "delete") {
                if (node.argument.type === 'MemberExpression') {
                    visit(node.argument.object, nt);
                    visitProperty(node.argument, nt + 1);
                    addInstructionAssign(NT(nt), "OP_DELPROP", NT(nt), NT(nt + 1));
                } else {
                    throw new Error("To be implemented");
//                    addInstruction("OP_DELVAR", JSON.stringify(node.argument.name));
                }
            } else {
                visit(node.argument, nt);
                addInstructionAssign(NT(nt), unaryOpToStr[node.operator], NT(nt));
            }
        },
        'Literal': function (node, nt) {
            switch (typeof node.value) {
                case 'number':
                    addInstructionAssign(NT(nt), "OP_NUMBER", node.value);
                    break;
                case 'string':
                    if (node.value.indexOf(NATIVE_MARKER) === 0) {
                        addVerbatim(node.value.substring(NATIVE_MARKER.length));
                    } else {
                        addInstructionAssign(NT(nt), "OP_STRING", JSON.stringify(node.value));
                    }
                    break;
                case 'object': // for null
                    if (node.value === null) {
                        addInstructionAssign(NT(nt), "OP_NULL");
                    } else {
                        addInstructionAssign(NT(nt), "OP_NEWREGEXP", JSON.stringify(node.regex.pattern), JSON.stringify(node.regex.flags));
                        setPrototype("RegExp", nt)
                    }
                    break;
                case 'boolean':
                    addInstructionAssign(NT(nt), "OP_BOOLEAN", node.value);
                    break;
            }
        },
        'ThisExpression': function (node, nt) {
            addAssign(NT(nt), BASE_VAR_NAME);
        },
        "ObjectExpression": function (node, nt) {
            addInstructionAssign(NT(nt), "OP_NEWOBJECT");

            // now set the prototype
            setPrototype("Object", nt);

            visitProperties(node.properties, nt);
        },
        "ArrayExpression": function (node, nt) {
            addInstructionAssign(NT(nt), "OP_NEWARRAY");

            // now set the prototype
            setPrototype("Array", nt);

            visitElements(node.elements, nt);
        },
        'Identifier': function (node, nt) {
            if (node.name === "undefined") {
                addInstructionAssign(NT(nt), "OP_UNDEF");
            } else if (node.name === "NaN") {
                addInstructionAssign(NT(nt), "OP_NAN");
            } else if (node.name === "Infinity") {
                addInstructionAssign(NT(nt), "OP_INFINITY");
            } else {
                addGetVar(NT(nt), node.name);
            }
        },
        'MemberExpression': function (node, nt) {
            visit(node.object, nt);
            visitProperty(node, nt + 1);
            addInstructionAssign(NT(nt), "OP_GETPROP", NT(nt), NT(nt + 1));
        },
        "AssignmentExpression": function (node, nt) {
            if (node.operator !== '=') {
                if (node.left.type === 'Identifier') {
                    addGetVar(NT(nt), node.left.name);
                    visit(node.right, nt + 1);
                    addInstructionAssign(NT(nt), binaryOpToInstr[node.operator.substring(0, node.operator.length - 1)], NT(nt), NT(nt + 1));
                } else {
                    visit(node.left.object, nt);
                    visitProperty(node.left, nt + 1);
                    addInstructionAssign(NT(nt + 2), "OP_GETPROP", NT(nt), NT(nt + 1));
                    visit(node.right, nt + 3);
                    addInstructionAssign(NT(nt + 2), binaryOpToInstr[node.operator.substring(0, node.operator.length - 1)], NT(nt + 2), NT(nt + 3));
                }
                if (node.left.type === 'Identifier') {
                    addSetVar(NT(nt), node.left.name, NT(nt));
                } else {
                    addInstructionAssign(NT(nt), "OP_SETPROP", NT(nt), NT(nt + 1), NT(nt + 2));
                }
            } else {
                if (node.left.type === 'Identifier') {
                    visit(node.right, nt);
                    addSetVar(NT(nt), node.left.name, NT(nt));
                } else {
                    visit(node.left.object, nt);
                    visitProperty(node.left, nt + 1);
                    visit(node.right, nt + 2);
                    addInstructionAssign(NT(nt), "OP_SETPROP", NT(nt), NT(nt + 1), NT(nt + 2));
                }
            }
        },
        'UpdateExpression': function (node, nt) {
            if (node.argument.type === 'Identifier') {
                addGetVar(NT(nt), node.argument.name);
                if (node.prefix) {
                    addInstructionAssign(NT(nt), unaryOpToStr[node.operator], NT(nt));
                    addSetVar(NT(nt), node.argument.name, NT(nt));
                } else {
                    addInstructionAssign(NT(nt + 1), unaryOpToStr[node.operator], NT(nt));
                    addSetVar(NT(nt + 1), node.argument.name, NT(nt + 1));
                }
            } else {
                visit(node.argument.object, nt);
                visitProperty(node.argument, nt + 1);
                addInstructionAssign(NT(nt + 2), "OP_GETPROP", NT(nt), NT(nt + 1));
                if (node.prefix) {
                    addInstructionAssign(NT(nt + 2), unaryOpToStr[node.operator], NT(nt + 2));
                    addInstructionAssign(NT(nt), "OP_SETPROP", NT(nt), NT(nt + 1), NT(nt + 2));
                } else {
                    addInstructionAssign(NT(nt + 3), unaryOpToStr[node.operator], NT(nt + 2));
                    addInstructionAssign(NT(nt), "OP_SETPROP", NT(nt), NT(nt + 1), NT(nt + 3));
                    addAssign(NT(nt), NT(nt + 2));
                }
            }
        },
        'SequenceExpression': function (node, nt) {
            visitExpressions(node.expressions, nt);
        },
        'CallExpression': function (node, nt) {
            if (node.callee.type === 'MemberExpression') {
                visit(node.callee.object, nt + 1);
                visitProperty(node.callee, nt + 2);
                addInstructionAssign(NT(nt), "OP_GETPROP", NT(nt + 1), NT(nt + 2));
            } else {
                visit(node.callee, nt);
                addInstructionAssign(NT(nt + 1), "OP_UNDEF"); // using strict semantics
            }
            addInstruction("OP_CLEARARGS");
            visitArguments(node.arguments, nt + 2);
            addInstruction("OP_PUSHARG", NT(nt + 1)); // push base object which will be bound to this
            addInstruction("OP_CALL", NT(nt));

            var returnLabel = freshLabel();
            addInstruction("OP_JUMPNE", JUMP_TYPE_VAR_NAME, JUMP_TYPE_EXCEPTION, returnLabel);
            addComment("throw after return");
            addThrowInstructions();
            addLabel(returnLabel);
            addAssign(NT(nt), RETURN_VAR_NAME);
        },
        'NewExpression': function (node, nt) {
            visit(node.callee, nt);
            visitArguments(node.arguments, nt + 1);

            // create new object
            addInstructionAssign(NT(nt + 1), "OP_NEWOBJECT");
            // set prototpe
            addInstructionAssign(NT(nt + 2), "OP_STRING", JSON.stringify("prototype"));
            addInstructionAssign(NT(nt + 2), "OP_GETPROP", NT(nt), NT(nt + 2));
            addInstructionAssign(NT(nt + 3), "OP_STRING", JSON.stringify("__proto__"));
            addInstructionAssign(NT(nt + 2), "OP_SETPROP", NT(nt + 1), NT(nt + 3), NT(nt + 2));

            addInstruction("OP_PUSHARG", NT(nt + 1)); // push base object which will be bound to this
            addInstruction("OP_NEW", NT(nt));

            var returnLabel = freshLabel();
            addInstruction("OP_JUMPNE", JUMP_TYPE_VAR_NAME, JUMP_TYPE_EXCEPTION, returnLabel);
            addComment("throw after return");
            addThrowInstructions();
            addLabel(returnLabel);
            addAssign(NT(nt), RETURN_VAR_NAME);
        },
        'LogicalExpression': function (node, nt) {
            visit(node.left, nt);
            var label = freshLabel();
            if (node.operator === '&&') {
                addInstruction("OP_IFFALSE", NT(nt), label);
                visit(node.right, nt);
                addLabel(label);
            } else {
                addInstruction("OP_IFTRUE", NT(nt), label);
                visit(node.right, nt);
                addLabel(label);
            }
        },
        'ConditionalExpression': function (node, nt) {
            var label = freshLabel(), label2 = freshLabel();
            visit(node.test, nt);
            addInstruction("OP_IFFALSE", NT(nt), label);
            visit(node.consequent, nt);
            addInstruction("OP_JUMP", label2);
            addLabel(label);
            visit(node.alternate, nt);
            addLabel(label2);
        },
        'LabeledStatement': function (node, nt) {
            nt = 0;
            addToLabelSet(node.label.name);
            visit(node.body, nt);
        },
        'ContinueStatement': function (node, nt) {
            addComment("continue");
            addContinueInstructions(node.label !== null ? node.label.name : undefined);
        },
        'BreakStatement': function (node, nt) {
            addComment("break");
            addBreakInstructions(node.label !== null ? node.label.name : undefined);
        },
        'ReturnStatement': function (node, nt) {
            nt = 0;

            addComment("return");
            if (node.argument) {
                visit(node.argument, nt);
                addAssign(RETURN_VAR_NAME, NT(nt));
                addAssign(JUMP_TYPE_VAR_NAME, JUMP_TYPE_RETURN);

            } else {
                addInstructionAssign(NT(0), "OP_UNDEF");
                addAssign(RETURN_VAR_NAME, NT(nt));
                addAssign(JUMP_TYPE_VAR_NAME, JUMP_TYPE_RETURN);
            }
            addReturnInstructions();
        },
        'ThrowStatement': function (node, nt) {
            nt = 0;

            addComment("throw");
            visit(node.argument, nt);
            addAssign(RETURN_VAR_NAME, NT(nt));
            addAssign(JUMP_TYPE_VAR_NAME, JUMP_TYPE_EXCEPTION);
            addThrowInstructions();
        },
        'IfStatement': function (node, nt) {
            nt = 0;
            addComment("if");
            var label = freshLabel(), label2 = freshLabel();

            var pushed = false, iLabel;
            if (!isEmptyLabelSet()) {
                iLabel = pushNewBlockLabel("named", getLabelSet());
                pushed = true;
            }
            clearLabelSet();

            visit(node.test, nt);
            addInstruction("OP_IFFALSE", NT(nt), label);
            visit(node.consequent, nt);
            addInstruction("OP_JUMP", label2);
            addLabel(label);
            visit(node.alternate, nt);
            addLabel(label2);

            if (pushed) {
                addLabel(iLabel + END_OFFSET);
                popBlockLabel();
            }
        },
        'SwitchStatement': function (node, nt) {
            nt = 0;
            addComment("switch");

            var iLabel = pushNewBlockLabel("switch", getLabelSet());
            clearLabelSet();

            visit(node.discriminant, nt);
            var discriminantVar = getTmpVar();
            addAssign(discriminantVar, NT(nt));

            var i, len = node.cases.length, hasDefault = false;

            var currBeginLabel, currSkipLabel, nextBeginLabel  = freshLabel(), nextSkipLabel = freshLabel(), defaultLabel = freshLabel();
            for (i =0; i<len; i++) {
                addComment("case "+i);
                var currCase = node.cases[i];
                if (currCase.test !== null) {
                    currBeginLabel = nextBeginLabel;
                    nextBeginLabel  = freshLabel();
                    addLabel(currBeginLabel);

                    visit(currCase.test, nt);
                    addAssign(NT(nt+1), discriminantVar);
                    addInstructionAssign(NT(nt), binaryOpToInstr['==='], NT(nt), NT(nt + 1));

                    if (i === len - 1) {
                        addInstruction("OP_IFFALSE", NT(nt), defaultLabel);
                    } else {
                        addInstruction("OP_IFFALSE", NT(nt), nextBeginLabel);

                    }
                } else {
                    addLabel(defaultLabel);
                    hasDefault = true;
                }

                currSkipLabel = nextSkipLabel;
                nextSkipLabel = freshLabel();
                addLabel(currSkipLabel);

                var j, lenj = currCase.consequent.length;
                for (j=0; j<lenj; j++) {
                    visit(currCase.consequent[j], nt);
                }

                if (i !== len - 1) {
                    addInstruction("OP_JUMP", nextSkipLabel);
                }
            }

            if (!hasDefault) {
                addLabel(defaultLabel);
            }
            addLabel(iLabel + END_OFFSET);
            popBlockLabel();
        },
        'BlockStatement': function (node, nt) {
            nt = 0;
            var pushed = false, iLabel;
            if (!isEmptyLabelSet()) {
                addComment("labelled block");
                iLabel = pushNewBlockLabel("named", getLabelSet());
                pushed = true;
            }
            clearLabelSet();

            visit(node.body, nt);
            if (pushed) {
                addLabel(iLabel + END_OFFSET);
                popBlockLabel();
            }
        },
        'WhileStatement': function (node, nt) {
            nt = 0;
            addComment("while");
            var label = freshLabel(), label2 = freshLabel();

            var iLabel = pushNewBlockLabel("loop", getLabelSet());
            clearLabelSet();

            addLabel(iLabel + CONTINUE_OFFSET);
            addLabel(label);
            visit(node.test, nt);
            addInstruction("OP_IFFALSE", NT(nt), label2);
            visit(node.body, nt);
            addInstruction("OP_JUMP", label);
            addLabel(label2);
            addLabel(iLabel + END_OFFSET);

            popBlockLabel();
        },
        'ForStatement': function (node, nt) {
            nt = 0;
            addComment("for");
            var label = freshLabel(), label2 = freshLabel();

            var iLabel = pushNewBlockLabel("loop", getLabelSet());
            clearLabelSet();

            visit(node.init, nt);
            addLabel(label);
            if (node.test === null) {
                addInstructionAssign(NT(nt), "OP_BOOLEAN", true);
            } else {
                visit(node.test, nt);
            }
            addInstruction("OP_IFFALSE", NT(nt), label2);
            visit(node.body, nt);
            addLabel(iLabel + CONTINUE_OFFSET);
            visit(node.update, nt);
            addInstruction("OP_JUMP", label);
            addLabel(label2);
            addLabel(iLabel + END_OFFSET);

            popBlockLabel();
        },
        'ForInStatement': function(node, nt) {
            nt = 0;
            addComment("for in");

            var iLabel = pushNewBlockLabel("loop", getLabelSet());
            clearLabelSet();

            visit(node.right, nt);
            addInstruction("OP_IFFALSE", NT(nt), iLabel + END_OFFSET);
            addInstructionAssign(NT(nt), "OP_ITERATOR", NT(nt));
            var iteratorVar = getTmpVar();
            addAssign(iteratorVar, NT(nt));

            addLabel(iLabel + CONTINUE_OFFSET);
            addAssign(NT(nt), iteratorVar);
            addInstructionAssign(NT(nt), "OP_NEXTKEY", NT(nt));
            addInstruction("OP_IFFALSE", NT(nt), iLabel + END_OFFSET);

            if (node.left.type === 'Identifier') {
                addSetVar(NT(nt), node.left.name, NT(nt));
            } else if (node.left.type === 'MemberExpression') {
                visit(node.left.object, nt+1);
                visitProperty(node.left, nt + 2);
                addInstructionAssign(NT(nt), "OP_SETPROP", NT(nt+1), NT(nt + 2), NT(nt));
            } else if (node.left.type === 'VariableDeclaration') {
                addSetVar(NT(nt), node.left.declarations[0].id.name, NT(nt));
            }

            visit(node.body, nt);
            addInstruction("OP_JUMP", iLabel + CONTINUE_OFFSET);

            addLabel(iLabel + END_OFFSET);

            popBlockLabel();
        },
        'TryStatement': function (node, nt) {
            nt = 0;

            addComment("try-catch-finally");
            var pushed = false, iLabel, flabel, clabel, endLabel;
            endLabel = freshLabel();
            if (!isEmptyLabelSet()) {
                iLabel = pushNewBlockLabel("named", getLabelSet());
                pushed = true;
            }
            clearLabelSet();
            if (node.finalizer) {
                flabel = pushNewBlockLabel("finally", getLabelSet());
            }
            if (node.handler) {
                clabel = pushNewBlockLabel("catch", getLabelSet());
            }
            visit(node.block, nt);
            addInstruction("OP_JUMP", endLabel);

            if (node.handler) {
                addComment("catch");
                addLabel(clabel + BEGIN_OFFSET);
                addAssign(JUMP_TYPE_VAR_NAME, JUMP_TYPE_NORMAL);

                var old = node.handler.param.name;
                var paramStr = getEscapedVarName(old);
                if (scope.isBoxed(old)) {
                    addInstructionAssign(NT(nt), "OP_NEWBOX");
                    addAssign(paramStr, NT(nt));
                    addAssign(NT(nt), RETURN_VAR_NAME);
                    addSetVar(NT(nt), old, NT(nt));
                } else {
                    addAssign(NT(nt), RETURN_VAR_NAME);
                    addSetVar(NT(nt), old, NT(nt));
                }

                visit(node.handler, 0);
            }

            addLabel(endLabel);
            if (node.finalizer) {
                addComment("finally");
                addLabel(flabel + BEGIN_OFFSET);
                addAssign(COMPLETION_TYPE_VAR_NAME_PREFIX + flabel, JUMP_TYPE_VAR_NAME);
                addAssign(COMPLETION_RETURN_VAR_NAME_PREFIX + flabel, RETURN_VAR_NAME);
                visit(node.finalizer, 0);
            }

            if (node.handler) {
                popBlockLabel();
            }
            if (node.finalizer) {
                popBlockLabel();
                addFinallyEndInstructions(flabel);
            }
            if (pushed) {
                addLabel(iLabel + END_OFFSET);
                popBlockLabel();
            }
        }
        //
        //
    };

    function addScopes(ast) {
        var catchCount = 0;
        var globals;
        if (targetLang === "C") {
            globals = ["Object", "Function", "Array", "RegExp", "console", "Math", "Date", "Number"];
            if (AS_MODULE)
                globals.push("exports");
        } else {
            globals = [ "Object", "Function", "Array", "RegExp"];
        }
        function Scope(parent, isCatch) {
            this.vars = {};
            this.captured = [];
            this.environment = [];
            this.usesArguments = false;
            this.parent = parent;
            this.isCatch = isCatch;
            this.id = Scope.allScopes.length;
            Scope.allScopes.push(this);
        }

        Scope.allScopes = [];

        Scope.prototype.print = function () {
            console.log("\nid: " + this.id);
            console.log("vars: " + JSON.stringify(this.vars, null, '\t'));
            console.log("captured: " + JSON.stringify(this.captured, null, '\t'));
            console.log("environment: " + JSON.stringify(this.environment, null, '\t'));
            console.log("usesArguments: " + this.usesArguments);
            console.log("isCatch: " + this.isCatch);
            console.log("parent: " + (this.parent ? this.parent.id : -1));
        };

        Scope.prototype.addVar = function (name, type) {
            var baseScope = this;
            while (baseScope.isCatch) {
                baseScope = baseScope.parent;
            }

            if (type === 'catchtmp') {
                catchCount++;
                this.vars[name] = catchCount;
            } else if (baseScope.vars[name] !== 'arg') {
                if (name !== "arguments" || type === 'arg' || type === 'special')
                    baseScope.vars[name] = type;
            }
        };

        Scope.prototype.setCapturedAndEnvironment = function (name) {
            if (this.hasOwnVar(name) === null) {
                var tmpScope = this;
                while (tmpScope.isCatch) {
                    tmpScope = tmpScope.parent;
                }
                if (tmpScope.environment.indexOf(name) < 0) {
                    tmpScope.environment.push(name);
                }

                tmpScope = tmpScope.parent;
                while (tmpScope.isCatch) {
                    tmpScope = tmpScope.parent;
                }
                if (tmpScope.captured.indexOf(name) < 0) {
                    tmpScope.captured.push(name);
                }
                tmpScope.setCapturedAndEnvironment(name);
            }
        };

        Scope.prototype.isArgumentsUsed = function () {
            return (this.vars["arguments"] === "special" && this.usesArguments)
        };

        Scope.prototype.setUsesArguments = function (val) {
            var tmpScope = this;
            while (tmpScope.isCatch) {
                tmpScope = tmpScope.parent;
            }
            tmpScope.usesArguments = val;
        };

        Scope.prototype.getIndexInEnvironment = function (name) {
            return this.environment.indexOf(name);
        };

        Scope.prototype.isBoxed = function (name) {
            var varType;
            if ((varType = this.hasOwnVar(name)) === null)
                throw new Error("scope.isBoxed(name) cannot be called on a name for which scope.hasOwnVar(name) is false")
            return this.captured.indexOf(name) >= 0 || (scope.isArgumentsUsed() && varType === "arg");
        };

        Scope.prototype.hasOwnVar = function (name) {
            var s = this;

            while (s.isCatch) {
                if (s && s.vars.hasOwnProperty(name))
                    return s.vars[name];
                s = s.parent;
            }
            if (s && s.vars.hasOwnProperty(name))
                return s.vars[name];
            return null;
        };

        Scope.prototype.hasVar = function (name) {
            var s = this;
            while (s !== null) {
                if (s.vars.hasOwnProperty(name))
                    return s.vars[name];
                s = s.parent;
            }
            return null;
        };

        var currentScope = null;
        var globalScope = null;

        function handleFun(node, context, isProgram) {
            var oldScope = currentScope;
            currentScope = new Scope(currentScope, false);
            node.$scope = currentScope;
            if (!isProgram)
                currentScope.addVar("arguments", "special");
            if (node.type === 'FunctionDeclaration') {
                oldScope.addVar(node.id.name, "defun");
                MAP(node.params, function (param) {
                    currentScope.addVar(param.name, "arg");
                });
            } else if (node.type === 'FunctionExpression') {
                if (node.id !== null) {
                    currentScope.addVar(node.id.name, "lambda");
                }
                MAP(node.params, function (param) {
                    currentScope.addVar(param.name, "arg");
                });
            }
        }

        function handleProgram(node, context) {
            globalScope = currentScope = new Scope(null, false);
            handleFun(node, context, true);
        }

        function handleVar(node) {
            currentScope.addVar(node.id.name, "var");
        }

        function handleCatch(node) {
            currentScope = new Scope(currentScope, true);
            node.$scope = currentScope;
            currentScope.addVar(node.param.name, "catchtmp");
        }

        function popScope(node) {
            currentScope = currentScope.parent;
        }

        var visitorPre1 = {
            'Program': handleProgram,
            'FunctionDeclaration': handleFun,
            'FunctionExpression': handleFun,
            'VariableDeclarator': handleVar,
            'CatchClause': handleCatch
        };

        var visitorPost1 = {
            'Program': popScope,
            'FunctionDeclaration': popScope,
            'FunctionExpression': popScope,
            'CatchClause': popScope
        };

        transformAst(ast, undefined, visitorPre1, visitorPost1, undefined, CONTEXT.RHS); // set scopes for alpha renaming of catch params

        function setScope(node) {
            currentScope = node.$scope;
        }

        var visitorPre2 = {
            'Program': setScope,
            'FunctionDeclaration': setScope,
            'FunctionExpression': setScope,
            'CatchClause': setScope
        };

        var visitorPost2 = {
            'Program': popScope,
            'FunctionDeclaration': popScope,
            'FunctionExpression': popScope,
            'CatchClause': function (node) {
                var type = currentScope.hasVar(node.param.name);
                if (typeof type === 'number') {
                    node.param.name = '#' + type + '_' + node.param.name;
                    //currentScope.addVar(node.param.name, "catch");
                }
                popScope(node);
            },
            'Identifier': function (node, context) {
                if (getNumericContext(node, context) === CONTEXT.RHS) {
                    var type = currentScope.hasVar(node.name);
                    if (typeof type === 'number') {
                        node.name = '#' + type + '_' + node.name;
                    }
                }
            },
            "UpdateExpression": function (node) {
                if (node.argument.type === 'Identifier') {
                    var type = currentScope.hasVar(node.argument.name);
                    if (typeof type === 'number') {
                        node.argument.name = '#' + type + '_' + node.argument.name;
                    }
                }
            },
            "AssignmentExpression": function (node) {
                if (node.left.type === 'Identifier') {
                    var type = currentScope.hasVar(node.left.name);
                    if (typeof type === 'number') {
                        node.left.name = '#' + type + '_' + node.left.name;
                    }
                }
            }
        };
        transformAst(ast, undefined, visitorPre2, visitorPost2, undefined, CONTEXT.RHS); // perform alpha renaming of catch params


        visitorPre1.CatchClause = function (node) {
            delete node.$scope;
            currentScope.addVar(node.param.name, "catch");
        };
        visitorPost1.CatchClause = function (node) {
        };

        Scope.allScopes = [];
        transformAst(ast, undefined, visitorPre1, visitorPost1, undefined, CONTEXT.RHS); // set scopes after alpha renaming; no need for special scope for catch blocks

        function handleIdentifierAux(name) {
            if (currentScope.hasVar(name) === null) {
                globalScope.addVar(name, "var")
            }
            if (currentScope.hasOwnVar(name) === null) {
                currentScope.setCapturedAndEnvironment(name)
            }
            if (name === 'arguments') {
                currentScope.setUsesArguments(true);
            }
        }

        function handleIdentifier(node, context) {
            if (getNumericContext(node, context) === CONTEXT.RHS) {
                handleIdentifierAux(node.name);
            }
        }

        function accessGlobals() {
            var len = globals.length;
            for (var i = 0; i < len; i++) {
                handleIdentifierAux(globals[i]);
            }
        }

        function handleScope3(node) {
            setScope(node);
            accessGlobals();
        }

        var visitorPre3 = {
            'Program': handleScope3,
            'FunctionDeclaration': handleScope3,
            'FunctionExpression': handleScope3
        };

        var visitorPost3 = {
            'Program': popScope,
            'FunctionDeclaration': popScope,
            'FunctionExpression': popScope,
            'Identifier': handleIdentifier,
            "UpdateExpression": function (node) {
                if (node.argument.type === 'Identifier') {
                    if (currentScope.hasVar(node.argument.name) === null) {
                        globalScope.addVar(node.argument.name, "var")
                    }
                    if (currentScope.hasOwnVar(node.argument.name) === null) {
                        currentScope.setCapturedAndEnvironment(node.argument.name)
                    }
                    if (node.argument.name === 'arguments') {
                        currentScope.setUsesArguments(true);
                    }
                }
            },
            "AssignmentExpression": function (node) {
                if (node.left.type === 'Identifier') {
                    if (currentScope.hasVar(node.left.name) === null) {
                        globalScope.addVar(node.left.name, "var")
                    }
                    if (currentScope.hasOwnVar(node.left.name) === null) {
                        currentScope.setCapturedAndEnvironment(node.left.name)
                    }
                    if (node.left.name === 'arguments') {
                        currentScope.setUsesArguments(true);
                    }
                }
            }
        };


        transformAst(ast, undefined, visitorPre3, visitorPost3, undefined, CONTEXT.RHS); // populate global scope and captured variables
        // @todo move all function definitions to the beginning of the block
        Scope.allScopes.forEach(function (scope) {
            scope.print();
            console.log("");
        })
    }


    /**
     * compiles the provided code.
     *
     * @param {string} code
     * @return {string}
     *
     */
    function compileCode(code) {
        var newAst = acorn.parse(code, {locations: true});
        addScopes(newAst);
        visit(newAst, 0);
        var newCode = esotope.generate(newAst, {comment: true});
        console.error(newCode);
    }

    function compileFile() {
        var argparse = require('argparse');
        var fs = require('fs');
        var path = require('path');
        acorn = require("acorn");
        esotope = require('esotope');

        var parser = new argparse.ArgumentParser({
            addHelp: true,
            description: "Command-line utility to perform compilation"
        });
        parser.addArgument(['file'], {
            help: "file to instrument",
            nargs: 1
        });
        parser.addArgument(['--target'], {
            help: "target language to emit: JS (default) or C",
            nargs: 1
        });
        parser.addArgument(['--as-module'], {
            help: "emit as a module, not driver (only affects C compilation)",
            nargs: 0
        });
        var args = parser.parseArgs();

        fileName = args.file[0];
        var origCode = fs.readFileSync(fileName, "utf8");

        if (args.target) {
            var candidate = args.target[0];
            if (candidate === "JS" || candidate === "C") {
                targetLang = candidate;
                console.warn("Setting compilation target to: "+targetLang);
            } else {
                throw ("Invalid compilation target: "+candidate);
            }
        }

        if (args.as_module)
            AS_MODULE = true;
        console.dir(args);

        initBackend(fileName);

        // This needs to happen *after* initializing the backend, since that may change function
        // name prefixes to avoid link-time collisions
        newFunction("main");

        if (targetLang !== "C") {
            var libCode = fs.readFileSync(path.resolve(__dirname, '../runtime/library.js'), 'UTF-8');
            origCode = libCode + origCode;
        } else {
            var libCode = fs.readFileSync(path.resolve(__dirname, '../runtime/clibrary.js'), 'UTF-8');
            if (AS_MODULE)
                libCode = libCode + "\nvar exports = {};\n";
            origCode = libCode + origCode;
            if (AS_MODULE)
                origCode = origCode + "\n\"use C: JS_Return = OP_GETVARSTAR( exports )\"\n";
        }

        compileCode(origCode);
        dumpFunctions();
    }


    if (typeof window === 'undefined' && (typeof require !== "undefined") && require.main === module) {
        compileFile();
    }
}());

