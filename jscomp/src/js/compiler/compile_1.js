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
// Author: Koushik Sen

(function () {
    var global = this;
    var JSON = {parse: global.JSON.parse, stringify: global.JSON.stringify};


    var Syntax = {
        AssignmentExpression: 'AssignmentExpression',
        ArrayExpression: 'ArrayExpression',
        BlockStatement: 'BlockStatement',
        BinaryExpression: 'BinaryExpression',
        BreakStatement: 'BreakStatement',
        CallExpression: 'CallExpression',
        CatchClause: 'CatchClause',
        ConditionalExpression: 'ConditionalExpression',
        ContinueStatement: 'ContinueStatement',
        DoWhileStatement: 'DoWhileStatement',
        DebuggerStatement: 'DebuggerStatement',
        EmptyStatement: 'EmptyStatement',
        ExpressionStatement: 'ExpressionStatement',
        ForStatement: 'ForStatement',
        ForInStatement: 'ForInStatement',
        FunctionDeclaration: 'FunctionDeclaration',
        FunctionExpression: 'FunctionExpression',
        Identifier: 'Identifier',
        IfStatement: 'IfStatement',
        Literal: 'Literal',
        LabeledStatement: 'LabeledStatement',
        LogicalExpression: 'LogicalExpression',
        MemberExpression: 'MemberExpression',
        NewExpression: 'NewExpression',
        ObjectExpression: 'ObjectExpression',
        Program: 'Program',
        Property: 'Property',
        ReturnStatement: 'ReturnStatement',
        SequenceExpression: 'SequenceExpression',
        SwitchStatement: 'SwitchStatement',
        SwitchCase: 'SwitchCase',
        ThisExpression: 'ThisExpression',
        ThrowStatement: 'ThrowStatement',
        TryStatement: 'TryStatement',
        UnaryExpression: 'UnaryExpression',
        UpdateExpression: 'UpdateExpression',
        VariableDeclaration: 'VariableDeclaration',
        VariableDeclarator: 'VariableDeclarator',
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
            (type === 'Property' && key ==='key') ||
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

    function newFunction() {
        currentFun = {instructions: [], name: "jsfun_" + nameCounter, params: [], locals: []};
        nameCounter++;
        funTable.push(currentFun);
        funStack.push(currentFun);
        return currentFun.name;
    }

    newFunction();

    function popFunction() {
        var tmp = funStack.pop();
        currentFun = funStack[funStack.length - 1];
        return tmp.name;
    }

    function dumpFunctions() {
        var len = funTable.length;
        for (var i = 0; i < len; i++) {
            console.log("void " + funTable[i].name + "(){");
            var instructions = funTable[i].instructions;
            var len2 = instructions.length;
            for (var j = 0; j < len2; j++) {
                console.log("    " + instructions[j]);
            }
            console.log("}");
        }
    }

    var instructions = {
        OP_POP: "OP_POP",
        OP_DUP2: "OP_DUP2",

        OP_TYPEOF: "OP_TYPEOF",
        OP_POS: "OP_POS",
        OP_NEG: "OP_NEG",
        OP_BITNOT: "OP_BITNOT",
        OP_LOGNOT: "OP_LOGNOT",

        OP_BITOR: "OP_BITOR",
        OP_BITXOR: "OP_BITXOR",
        OP_BITAND: "OP_BITAND",
        OP_EQ: "OP_EQ",
        OP_NE: "OP_NE",
        OP_STRICTEQ: "OP_STRICTEQ",
        OP_STRICTNE: "OP_STRICTNE",
        OP_LT: "OP_LT",
        OP_GT: "OP_GT",
        OP_LE: "OP_LE",
        OP_GE: "OP_GE",
        OP_INSTANCEOF: "OP_INSTANCEOF",
        OP_IN: "OP_IN",
        OP_SHL: "OP_SHL",
        OP_SHR: "OP_SHR",
        OP_USHR: "OP_USHR",
        OP_ADD: "OP_ADD",
        OP_SUB: "OP_SUB",
        OP_MUL: "OP_MUL",
        OP_DIV: "OP_DIV",
        OP_MOD: "OP_MOD",

        OP_NAN: "OP_NAN",
        OP_UNDEF: "OP_UNDEF",
        OP_INFINITY: "OP_INFINITY",
        OP_NULL: "OP_NULL",
        OP_NUMBER: "OP_NUMBER",
        OP_STRING: "OP_STRING",
        OP_FUNCTION: "OP_FUNCTION",
        OP_NEWOBJECT: "OP_NEWOBJECT",
        OP_NEWARRAY: "OP_NEWARRAY",
        OP_NEWREGEXP: "OP_NEWREGEXP",
        OP_BOOLEAN: "OP_BOOLEAN",

        OP_INITPROP: "OP_INITPROP",
        OP_INITSETTER: "OP_INITSETTER",
        OP_INITGETTER: "OP_INITGETTER",
        OP_GETPROP: "OP_GETPROP",
        OP_SETPROP: "OP_SETPROP",
        OP_DELPROP: "OP_DELPROP",

        OP_GETVAR: "OP_GETVAR",
        OP_SETVAR: "OP_SETVAR",
        OP_DELVAR: "OP_DELVAR",

        OP_CLOSURE: "OP_CLOSURE",
        OP_CURRENT: "OP_CURRENT",
        OP_THIS: "OP_THIS"
    };

    var unaryOpToStr = {
        "typeof": "OP_TYPEOF",
        "+": "OP_POS",
        "-": "OP_NEG",
        "~": "OP_BITNOT",
        "!": "OP_LOGNOT"
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

    function addInstruction(str) {
        if (!instructions.hasOwnProperty(str)) {
            throw new Error("Cannot fund instruction: " + str);
        }
        str = instructions[str];
        if (arguments.length > 1) {
            str = str + "(";
        }
        for (var i = 1; i < arguments.length; i++) {
            str = str + arguments[i];
            if (i < arguments.length - 1) {
                str = str + ",";
            }
        }
        if (arguments.length > 1) {
            str = str + ")";
        }
        currentFun.instructions.push(str);
    }

    var visitorRRPre = {
        "FunctionExpression": function (node) {
            addInstruction("OP_CLOSURE");
            var name = newFunction();
            if (node.id) {
                addInstruction("OP_CURRENT");
                addInstruction("OP_SETVAR", node.id.name);
            }
        },
        "FunctionDeclaration": function (node) {
            addInstruction("OP_CLOSURE");
            var name = newFunction();
        },
        "ObjectExpression": function (node) {
            addInstruction("OP_NEWOBJECT");
        },
        "ArrayExpression": function (node) {
            addInstruction("OP_NEWARRAY");
        }


    };


    var visitorRRPost = {
        "FunctionExpression": function (node) {
            var name = popFunction();
            addInstruction("OP_FUNCTION", name);
        },
        "FunctionDeclaration": function (node) {
            var name = popFunction();
            addInstruction("OP_FUNCTION", name);
            addInstruction("OP_SETVAR", node.id.name);
        },
        'Literal': function (node, context) {
            if (getNumericContext(node, context) === CONTEXT.RHS) {
                switch (typeof node.value) {
                    case 'number':
                        addInstruction("OP_NUMBER", node.value);
                        break;
                    case 'string':
                        addInstruction("OP_STRING", JSON.stringify(node.value));
                        break;
                    case 'object': // for null
                        if (node.value === null) {
                            addInstruction("OP_NULL");
                        } else {
                            addInstruction("OP_NEWREGEXP", JSON.stringify(node.regex.pattern), JSON.stringify(node.regex.flags));
                        }
                        break;
                    case 'boolean':
                        addInstruction("OP_BOOLEAN", node.value);
                        break;
                }
            }
        },
        'BinaryExpression': function (node) {
            addInstruction(binaryOpToInstr[node.operator]);
        },
        'UnaryExpression': function (node) {
            if (node.operator === "void") {
                addInstruction("OP_POP");
                addInstruction("OP_UNDEF");
            } else if (node.operator === "delete") {
                if (node.argument.object) {
                    addInstruction("OP_DELPROP");
                } else {
                    addInstruction("OP_DELVAR", JSON.stringify(node.argument.name));
                }
            } else {
                addInstruction(unaryOpToStr[node.operator]);
            }
        },
        'ThisExpression': function (node) {
            addInstruction("OP_THIS");
        },
        'Identifier': function (node, context) {
            if (getNumericContext(node, context) === CONTEXT.RHS) {
                if (node.name === "undefined") {
                    addInstruction("OP_UNDEF");
                } else if (node.name === "NaN") {
                    addInstruction("OP_NAN");
                } else if (node.name === "Infinity") {
                    addInstruction("OP_INFINITY");
                } else {
                    addInstruction("OP_GETVAR", node.name);
                }
            } else {
                if (context && context.object && context.object.type === 'MemberExpression' && !context.object.computed) {
                    addInstruction("OP_STRING", JSON.stringify(node.name));
                } else if (context && context.object && context.property === 'left' && context.object.type === 'AssignmentExpression' && context.object.operator !== '=') {
                    addInstruction("OP_GETVAR", node.name);
                }
            }
        },
        'MemberExpression': function (node, context) {
            if (getNumericContext(node, context) === CONTEXT.RHS) {
                addInstruction("OP_GETPROP");
            } else if (context && context.object && context.property === 'left' && context.object.operator !== '=') {
                addInstruction("OP_DUP2");
                addInstruction("OP_GETPROP");
            }
        },
        "Property": function (node) {
            if (node.kind === 'init') {
                addInstruction("OP_INITPROP");
            } else if (node.kind === 'set') {
                addInstruction("OP_INITSETTER");
            } else if (node.kind === 'get') {
                addInstruction("OP_INITGETTER");
            }
        },
        "AssignmentExpression": function (node) {
            if (node.operator !== '=') {
                addInstruction(binaryOpToInstr[node.operator.substring(0, node.operator.length - 1)]);
            }
            if (node.left.type === 'Identifier') {
                addInstruction("OP_SETVAR", node.left.name);
            } else {
                addInstruction("OP_SETPROP");
            }
        }

    };

    function rRPre(node, context) {
        if (context && context.parentContext) {
            var parentObject = context.parentContext.object;
            var parentType = parentObject.type;
            var parentKey = context.parentContext.property;
            if (parentType === 'ArrayExpression' && parentKey === 'elements') {
                addInstruction("OP_NUMBER", context.property | 0);
            }
        }
    }

    function rRPost(node, context) {
        if (context && context.parentContext) {
            var parentObject = context.parentContext.object;
            var parentType = parentObject.type;
            var parentKey = context.parentContext.property;
            if (parentType === 'ArrayExpression' && parentKey === 'elements') {
                addInstruction("OP_INITPROP");
            }
        }
    }

    function transformString(code, pre, visitorsPre, visitorsPost, post) {
        var newAst = acorn.parse(code, {locations: true});
        var len = visitorsPost.length;
        for (var i = 0; i < len; i++) {
            newAst = transformAst(newAst, pre, visitorsPre[i], visitorsPost[i], post, null);
        }
        return newAst;
    }


    /**
     * compiles the provided code.
     *
     * @param {string} code
     * @return {string}
     *
     */
    function compileCode(code) {
        transformString(code, rRPre, [visitorRRPre], [visitorRRPost], rRPost);
    }

    function compileFile() {
        var argparse = require('argparse');
        var fs = require('fs');
        acorn = require("acorn");

        var parser = new argparse.ArgumentParser({
            addHelp: true,
            description: "Command-line utility to perform compilation"
        });
        parser.addArgument(['file'], {
            help: "file to instrument",
            nargs: 1
        });
        var args = parser.parseArgs();

        var fileName = args.file[0];
        var origCode = fs.readFileSync(fileName, "utf8");
        compileCode(origCode);
        dumpFunctions();
    }


    if (typeof window === 'undefined' && (typeof require !== "undefined") && require.main === module) {
        compileFile();
    }
}());

