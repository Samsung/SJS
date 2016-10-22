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
var args = [];
var nonExistentValue = new Object();
var target = -1; // default target is -1

function OBJECT_FUNCTION() {
    this.properties = new Object(null);
}

function BOX() {
    this.value = undefined;
}

function ToObject(arg) {
    return arg; // @todo consider the case where arg is a primitive value, undefined, or null
}

function ToString(arg) {
    return arg + ""; // @todo consider the case where arg is an instance of OBJECT_FUNCTION
}

function OP_CLEARARGS() {
    args = [];
}

function OP_POPARG() {
    return args.pop();
}

function OP_PUSHARG(arg1) {
    args.push(arg1);
}

function OP_TYPEOF(arg1) {
    var type = typeof arg1;
    if (type === "object") {
        if (arg1 !== null) {
            if (arg1["[[Class]]"] === "Function") {
                type = "function";
            }
        }
    }
    return type;
}

function OP_POS(arg1) {
    return +arg1;
}

function OP_NEG(arg1) {
    return -arg1;
}

function OP_INC(arg1) {
    return ++arg1;
}

function OP_DEC(arg1) {
    return --arg1;
}

function OP_BITNOT(arg1) {
    return ~arg1;
}

function OP_LOGNOT(arg1) {
    return !arg1;
}

function OP_ITERATOR(arg1) {
    var ret = [], proto = null;
    while (arg1 !== null) {
        arg1 = ToObject(arg1);
        for (var key in arg1.properties) {
            if (key === "*__proto__") {
                proto = OP_GETOWNPROP(arg1, key);
            } else if (arg1.properties[key] !== undefined && arg1.properties[key] !== nonExistentValue && arg1.properties[key]["[[Enumerable]]"]) {
                ret.push(key.substring(1));
            }
        }
        arg1 = proto;
    }
    return {keys: ret, index: 0};
}

function OP_NEXTKEY(iter) {
    return iter.keys[iter.index++];
}

function OP_BITOR(arg1, arg2) {
    return arg1 | arg2;
}

function OP_BITXOR(arg1, arg2) {
    return arg1 ^ arg2;
}

function OP_BITAND(arg1, arg2) {
    arg1 & arg2;
}

function OP_EQ(arg1, arg2) {
    return arg1 == arg2;
}

function OP_NE(arg1, arg2) {
    return arg1 != arg2;
}

function OP_STRICTEQ(arg1, arg2) {
    return arg1 === arg2;
}

function OP_STRICTNE(arg1, arg2) {
    return arg1 !== arg2;
}

function OP_LT(arg1, arg2) {
    return arg1 < arg2;
}

function OP_GT(arg1, arg2) {
    return arg1 > arg2;
}

function OP_LE(arg1, arg2) {
    return arg1 <= arg2;
}

function OP_GE(arg1, arg2) {
    return arg1 >= arg2;
}

function OP_INSTANCEOF(arg1, arg2) {
    var arg2prototype = OP_GETOWNPROP(arg2, "prototype");
    do {
        arg1 = OP_GETOWNPROP(arg1, "__proto__");
        if (arg1 === arg2prototype) return true;
    } while (arg1 !== null);
    return false;
}

function OP_IN(arg1, arg2) {
    arg2 = ToString(arg2);
    do {
        arg1 = ToObject(arg1);
        var ret = arg1.properties["*" + arg2];
        if (ret === undefined || ret === nonExistentValue) {
            arg1 = OP_GETOWNPROP(arg1, "__proto__");
        } else {
            return true;
        }
    } while (arg1 !== null);
    return false;
}

function OP_SHL(arg1, arg2) {
    return arg1 << arg2;
}

function OP_SHR(arg1, arg2) {
    return arg1 >> arg2;
}

function OP_USHR(arg1, arg2) {
    return arg1 >>> arg2;
}

function OP_ADD(arg1, arg2) {
    return arg1 + arg2;
}

function OP_SUB(arg1, arg2) {
    return arg1 - arg2;
}

function OP_MUL(arg1, arg2) {
    return arg1 * arg2;
}

function OP_DIV(arg1, arg2) {
    return arg1 / arg2;
}

function OP_MOD(arg1, arg2) {
    return arg1 % arg2;
}

function OP_NAN() {
    return NaN;
}

function OP_UNDEF() {
    return undefined;
}

function OP_NONEXISTENT() {
    return nonExistentValue;
}

function OP_INFINITY() {
    return Infinity;
}

function OP_NULL() {
    return null;
}

function OP_NUMBER(arg1) {
    return arg1;
}

function OP_STRING(arg1) {
    return arg1;
}

function OP_FUNCTION(fun, env) {
    var ret = new OBJECT_FUNCTION();
    ret["[[Class]]"] = "Function";
    ret["[[Call]]"] = fun;
    ret["[[Scope]]"] = env;
    return ret;
}

function OP_NEWOBJECT() {
    var ret = new OBJECT_FUNCTION();
    ret["[[Class]]"] = "Object";
    return ret;
}

function OP_NEWARGUMENTS() {
    var ret = new OBJECT_FUNCTION();
    ret["[[Class]]"] = "Arguments";
    OP_SETPROP(ret, "length", args.length);
    var len = args.length;
    for (var i=0; i<len; i++) {
        OP_SETPROP(ret, i, args[len-i-1]);
    }
    return ret;
}

function OP_NEWARRAY() {
    var ret = new OBJECT_FUNCTION();
    ret["[[Class]]"] = "Array";
    OP_SETPROP(ret, "length", 0);
    return ret;
}

function OP_NEWENV(size) {
    return new Array(size);
}

function OP_NEWREGEXP(pattern, flags) {
    var ret = new OBJECT_FUNCTION();
    ret["[[Class]]"] = "Function";
    ret["[[Pattern]]"] = pattern;
    ret["[[Flags]]"] = flags;
    return ret;
}

function OP_BOOLEAN(arg1) {
    return arg1;
}

function OP_NEWBOX() {
    var ret = new BOX();
    ret.value = undefined;
    return ret;
}

function OP_GETOWNPROP(arg1, arg2) {
    arg2 = ToString(arg2);
    if (!(arg1 instanceof OBJECT_FUNCTION)) {
        return arg1[arg2];
    }
    var ret = arg1.properties["*" + arg2];
    if (ret === undefined || ret === nonExistentValue) {
        return undefined;
    } else {
        return ret["[[Value]]"];
    }
}

function OP_GETPROP(arg1, arg2) {
    arg2 = ToString(arg2);
    do {
        var ret = OP_GETOWNPROP(arg1, arg2);
        if (ret === undefined || ret === nonExistentValue) {
            arg1 = OP_GETOWNPROP(arg1, "__proto__");
        } else {
            if (ret instanceof BOX) {
                return ret.value; // only used for retrieving properties of argument objects.
            }
            return ret;
        }
    } while (arg1 !== null);
    return undefined;
}

function OP_SETPROP(arg1, arg2, arg3) {
    var oldarg2 = arg2;
    arg2 = ToString(arg2);
    var ret = arg1.properties["*" + arg2];
    if (ret === undefined || ret === nonExistentValue) {
        ret = {"[[Writable]]": true, "[[Enumerable]]": true, "[[Configurable]]": true, isData: true};
        arg1.properties["*" + arg2] = ret;
    }
    if (ret["[[Value]]"] instanceof BOX) {
        ret["[[Value]]"].value = arg3;  // only used to set properties of argument objects.
    } else {
        ret["[[Value]]"] = arg3;
    }
    // need to update array length if arg1 is an array and arg2 is an integer
    if (arg1["[[Class]]"] === "Array" && OP_TYPEOF(oldarg2) === "number") {
        var length = OP_GETOWNPROP(arg1, "length");
        if (length < oldarg2 + 1) {
            OP_SETPROP(arg1, "length", oldarg2 + 1);
        }
    }
    return arg3;
}

function OP_DELPROP(arg1, arg2, arg3) {
    arg2 = ToString(arg2);
    var ret = arg1.properties["*" + arg2];
    if (ret === undefined || ret === nonExistentValue) {
        return false;
    } else if (ret["[[Configurable]]"]) {
        arg1.properties["*" + arg2] = nonExistentValue;
        return true;
    } else {
        return false;
    }
}

function OP_SETGETTER(arg1, arg2, arg3) {
    arg2 = ToString(arg2);
    var ret = arg1.properties["*" + arg2];
    if (ret === undefined || ret === nonExistentValue) {
        ret = {"[[Enumerable]]": true, "[[Configurable]]": true, isData: false};
        arg1.properties["*" + arg2] = ret;
    }
    ret["[[Get]]"] = arg3;
    return arg3;
}

function OP_SETSETTER(arg1, arg2, arg3) {
    arg2 = ToString(arg2);
    var ret = arg1.properties["*" + arg2];
    if (ret === undefined || ret === nonExistentValue) {
        ret = {"[[Enumerable]]": true, "[[Configurable]]": true, isData: false};
        arg1.properties["*" + arg2] = ret;
    }
    ret["[[Set]]"] = arg3;
    return arg3;
}

//function OP_DELPROP(arg1, arg2) {
//    arg2 = ToString(arg2);
//    var ret = arg1.properties["*" + arg2];
//    if (ret !== undefined || ret === nonExistentValue) {
//        if (ret["[[Configurable]]"]) {
//            arg1.properties["*" + arg2] = nonExistentValue;
//            return true;
//        } else {
//            return false;
//        }
//    }
//    return true;
//}

function OP_GETINDEX(env, index) {
    return env[index];
}

function OP_GETINDEXSTAR(env, index) {
    var ret = env[index];
    if (ret !== undefined) {
        return ret.value;
    } else {
        return ret;
    }
}

function OP_SETINDEX(env, index, val) {
    env[index] = val;
}

function OP_SETINDEXSTAR(env, index, val) {
    env[index].value = val;
}

function OP_GETVAR(val) {
    return val;
}

function OP_GETVARSTAR(box) {
    return box.value;
}

function OP_SETVARSTAR(box, val) {
    return box.value = val;
}

function OP_TARGET() {
    return target;
}

function OP_IFTRUE(val, label) {
    if (val) {
        target = label;
        return true;
    }
    return false;
}

function OP_IFFALSE(val, label) {
    if (!val) {
        target = label;
        return true;
    }
    return false;
}

function OP_JUMP(label) {
    target = label;
    return true;
}

function OP_JUMPNE(val1, val2, label) {
    if (val1 !== val2) {
        target = label;
        return true;
    }
    return false;
}

//
function OP_CALL(fun) {
    var f = fun["[[Call]]"];
    OP_PUSHARG(fun);
    OP_PUSHARG(fun["[[Scope]]"]);
    target = -1;
    f();
}

function OP_NEW(fun) {
    var base = OP_POPARG();
    OP_PUSHARG(base);
    OP_CALL(fun);
    var type = OP_TYPEOF(JS_Return);
    if (JS_Return === null || (type !== "object" && type !== "function")) {
        JS_Return = base;
    }
}

