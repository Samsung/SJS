/* 
 * Copyright 2014-2016 Samsung Research America, Inc.
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
/*
 * Inlining Intrinsics
 *
 * Rewrite property accesses on intrinsics, and
 * This is essentially search and replace, modulo scoping issues
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend;

import com.samsung.sjs.types.*;
import com.samsung.sjs.backend.asts.ir.*;
import com.samsung.sjs.CompilerOptions;
import com.samsung.sjs.FFILinkage;

import java.util.List;

public final class IntrinsicsInliningPass extends IRTransformer {

    private final Script script;
    private final CompilerOptions options;

    protected final FFILinkage ffi;

    private final Var Infinity;
    private final IntrinsicName __Infinity;
    private final Var Math;

    public IntrinsicsInliningPass(Script s, CompilerOptions opts, FFILinkage ffi) {
        super(s);
        this.script = s;
        this.options = opts;
        this.ffi = ffi;

        // pre-allocate various vars for comparison
        this.Infinity = mkVar("Infinity");
        this.__Infinity = mkIntrinsicName("INFINITY");
        __Infinity.setType(Types.mkFloat());
        this.Math = mkVar("Math");
    }

    public Script convert() {
        return (Script)script.accept(this);
    }

    @Override public IRNode visitFunction(Function node) {
        Function f = new Function(curScope, node.getName(), node.getEnvironmentName(), node.getReturnType());
        for (int i = 0; i < node.nargs(); i++) {
            f.addParameter(node.argName(i), node.argType(i));
        }
        f.setCaptured(node.getCaptured());
        f.setType(node.getType());
        f.setLayout(node.getEnvLayout());
        if (node.isMethod()) {
            f.markMethod();
        }
        if (node.isConstructor()) {
            f.markConstructor();
        }
        curScope = f.getScope();
        Block oldBlock = currentBlock;
        currentBlock = f.getBody();
        for (Statement s : node.getBody()) {
            f.addBodyStatement((Statement)s.accept(this));
        }
        curScope = curScope.getParentScope();
        currentBlock = oldBlock;
        return f;
    }

    @Override
    public IRNode visitFunctionCall(FunctionCall node) {
        if (node.getTarget().isVar() && node.getTarget().asVar().getIdentifier().equals("itofp")) {
            FunctionCall f = mkFunctionCall(mkIntrinsicName("itofp"));
            for (Expression e : node.getArguments()) {
                f.addArgument(e.accept(this).asExpression());
            }
            f.setType(Types.mkFloat());
            return f;
        } else if (node.getTarget().isVar() && node.getTarget().asVar().getIdentifier().equals("Array")) {
            List<Expression> args = node.getArguments();
            if (args.size() != 1 || args.size() > 0 && (!(args.get(0).asExpression().getType() instanceof IntegerType))) {
                AllocArrayLiteral l = mkAllocArrayLiteral(((ArrayType)node.getType()).elemType());
                l.setType(node.getType());
                for (Expression e : args) {
                    l.addElement(e.accept(this).asExpression());
                }
                return l;
            } else {
                return super.visitFunctionCall(node);
            }
        } else if (node.getTarget().isVar() && node.getTarget().asVar().getIdentifier().equals("String")) {
            List<Expression> args = node.getArguments();
            if (args.size() == 0) {
                return mkStr("");
            } else if (args.size() == 1) {
                return args.get(0);
            } else {
                assert(false); // unsupported String constructor arity
                return null;
            }
        } else {
            return super.visitFunctionCall(node);
        }
    }

    @Override 
    public IRNode visitFieldRead(FieldRead node) {
        if (node.getObject().getType() instanceof StringType) {
            assert (node.getField().equals("length"));
            FunctionCall f = mkFunctionCall(mkIntrinsicName("wcslen"));
            f.addArgument(node.getObject().accept(this).asExpression());
            f.setType(Types.mkInt());
            return f;
        } else if (node.getObject().isVar()) {
            Var v = node.getObject().asVar();
            if (v.getIdentifier().equals("Math") && isGlobalBinding(Math)) {
                // TODO: Math fields, String.length, String.prototype (maybe)
                // TODO: Deal with program names shadowing the C binding of the intrinsic!
                // This is a direct access to a static member of the global Math object
                switch (node.getField()) {
                    case "E":
                    case "LN2":
                    case "LN10":
                    case "LOG2E":
                    case "LOG10E":
                    case "PI":
                    case "SQRT1_2":
                    case "SQRT2": 
                        Expression exp = mkIntrinsicName("M_"+node.getField()); // Take advantage of similar names
                        exp.setType(Types.mkFloat());
                        return exp;
                    default:
                        // Could still see the field read inside a method dispatch
                        System.err.println("Not inlining Math."+node.getField());
                        return super.visitFieldRead(node);
                }
            } else {
                return super.visitFieldRead(node);
            }
        } else {
            return super.visitFieldRead(node);
        }
    }

    @Override
    public IRNode visitMethodCall(MethodCall node) {
        assert (node.getType() != null);
        if (node.getTarget().getType() instanceof FloatType) {
            // rewrite "member access" on a floating point number
            FunctionCall f = mkFunctionCall(mkIntrinsicName("__fp__"+node.getField()));
            f.addArgument(node.getTarget().accept(this).asExpression());
            for (Expression e : node.getArguments()) {
                f.addArgument(e.accept(this).asExpression());
            }
            f.setType(node.getType());
            return f;
        } if (node.getTarget().getType() instanceof IntegerType) {
            // rewrite "member access" on an integer
            FunctionCall f = mkFunctionCall(mkIntrinsicName("__int__"+node.getField()));
            f.addArgument(node.getTarget().accept(this).asExpression());
            for (Expression e : node.getArguments()) {
                f.addArgument(e.accept(this).asExpression());
            }
            f.setType(node.getType());
            return f;
        } if (node.getTarget().getType() instanceof StringType) {
            // rewrite "member access" on a string
            FunctionCall f = mkFunctionCall(mkIntrinsicName("__str__"+node.getField()));
            f.addArgument(node.getTarget().accept(this).asExpression());
            for (Expression e : node.getArguments()) {
                f.addArgument(e.accept(this).asExpression());
            }
            f.setType(node.getType());
            return f;
        } else if (node.getTarget().isVar()) {
            // TODO: Math, primitive types
            Var v = node.getTarget().asVar();
            FunctionCall f = null;
            if (v.getIdentifier().equals("Math") && isGlobalBinding(Math)) {
                switch (node.getField()) {
                    case "abs":
                    case "acos":
                    case "acosh":
                    case "asin":
                    case "asinh":
                    case "atan":
                    case "atanh":
                    case "atan2":
                    case "cbrt":
                    case "ceil":
                    case "cos":
                    case "cosh":
                    case "exp":
                    case "expm1":
                    case "fround":
                    case "hypot":
                    case "imul":
                    case "log":
                    case "log1p":
                    case "log10":
                    case "log2":
                    case "pow":
                    case "random":
                    case "round":
                    case "sign":
                    case "sin":
                    case "sinh":
                    case "sqrt":
                    case "tan":
                    case "tanh":
                    case "trunc":
                        f = mkFunctionCall(mkIntrinsicName(node.getField()));
                        break;
                    case "floor":
                        // XXX awful hack
                        f = mkFunctionCall(mkIntrinsicName("___castFloor"));
                        break;
                    case "max":
                    case "min":
                        // fmin, fmax
                        f = mkFunctionCall(mkIntrinsicName("f"+node.getField()));
                        break;
                    case "toSource":
                        // TODO: string literal "Math"
                    default:
                        return super.visitMethodCall(node);
                }
                for (Expression e : node.getArguments()) {
                    f.addArgument(e.accept(this).asExpression());
                }
                f.setType(node.getType());
                return f;
            } else if (v.getIdentifier().equals("String")) {
                // TODO: build concrete String object
                f = mkFunctionCall(mkIntrinsicName("__str__"+node.getField()));
                for (Expression e : node.getArguments()) {
                    f.addArgument(e.accept(this).asExpression());
                }
                f.setType(node.getType());
                return f;
            } else {
                return super.visitMethodCall(node);
            }
        } else {
            return super.visitMethodCall(node);
        }
    }

    private boolean isGlobalBinding(Var v) {
        // Global environment has null parent scope.
        // Find the binding scope (which must exist) and check if it's global.
        Scope cur = curScope;
        while (cur != null) {
            if (cur.isLocallyBound(v)) {
                break;
            }
            cur = cur.getParentScope();
        }
        assert (cur != null);
        return cur.getParentScope() == null;
    }

    @Override
    public IRNode visitVar(Var node) {
        // TODO: inline Infinity, if global
        if (node.getIdentifier().equals("Infinity") && isGlobalBinding(Infinity)) {
            System.err.println("Inlining Infinity");
            return __Infinity;
        } else {
            // Convert intrinsics
            switch (node.getIdentifier()) {
                case "_____type_violation":
                case "printInt":
                case "printFloat":
                case "assert":
                case "printString":
                case "print":
                case "parseFloat":
                case "parseInt":
                case "readline":
                case "printFloat10":
                case "string_of_int":
                    IntrinsicName in = mkIntrinsicName(node.getIdentifier());
                    in.setType(node.getType());
                    return in;
                default:
                    FFILinkage.LinkEntry link = ffi.get(node.getIdentifier());
                    if (link != null && link.untyped_import) {
                        // Introduce dynamic type check
                        UntypedAccess ua = new UntypedAccess(node);
                        ua.setType(node.getType());
                        return ua;
                    }
                    return super.visitVar(node);
            }
        }
    }

    @Override
    public IRNode visitRequire(Require node) {
        // For now, we conservatively insert a coercion/tag-check for any import
        Expression acc = new UntypedAccess(node);
        acc.setType(node.getType());
        return acc;
    }

    @Override
    public IRNode visitBinaryOp(BinaryOp node) {
        Expression left = node.getLeft().accept(this).asExpression();
        Expression right = node.getRight().accept(this).asExpression();
        String op = node.getOp();
        Expression ret = null;
        if (left.getType() == null) {
            System.err.println("Missing type on: "+left.toSource(0)+" in "+node.toSource(0));
        }
        assert (left.getType() != null);
        if (right.getType() == null) {
            System.err.println("Missing type on: "+right.toSource(0)+" in "+node.toSource(0));
            System.err.println("Assuming type of LHS: "+left.getType().toString());
            right.setType(left.getType());
        }
        //assert (right.getType() != null);
        assert (!"+=".equals(op));
        // TODO: Refactor this into one big type-guarded switch statement
        if ("+".equals(op)) {
            if (left.getType() instanceof StringType) {
                // TODO: We assume we run after a pass inserting appropriate coercions
                assert (right.getType() instanceof StringType ||
                        right.getType() instanceof IntegerType ||
                        right.getType() instanceof FloatType) : left.toSource(0) + " + " + right.toSource(0);
                boolean isInt = right.getType() instanceof IntegerType || right.getType() instanceof FloatType;
                Expression newleft = left.accept(this).asExpression();
                Expression newright = right.accept(this).asExpression();
                if (isInt) {
                    // TODO: symmetric change for <int> + <string> ...
                    // TODO: Write earlier pass to insert implicit coercions
                    FunctionCall f = mkFunctionCall(mkIntrinsicName(right.getType() instanceof IntegerType ? "string_of_int" : "__fp__toString"), newright);
                    f.setType(Types.mkString());
                    ret = mkFunctionCall(mkIntrinsicName("__str__concat"),
                                         newleft,
                                         f);
                } else {
                    // string
                    ret = mkFunctionCall(mkIntrinsicName("__str__concat"),
                                         newleft,
                                         newright);
                }
                ret.setType(Types.mkString());
            } else if (right.getType() instanceof StringType) {
                // Here, we already know left is not a String
                assert (left.getType() instanceof IntegerType ||
                        left.getType() instanceof FloatType);
                Expression newleft = left.accept(this).asExpression();
                Expression newright = right.accept(this).asExpression();
                FunctionCall f = mkFunctionCall(mkIntrinsicName(left.getType() instanceof IntegerType ? "string_of_int" : "__fp__toString"), newleft);
                f.setType(Types.mkString());
                ret = mkFunctionCall(mkIntrinsicName("__str__concat"),
                                     f,
                                     newright);
                ret.setType(Types.mkString());
            } else {
                ret = mkBinaryOp(left, op, right);
            }
        } else if ("!=".equals(op) || "!==".equals(op)) {
            if (left.getType() instanceof StringType) {
                // TODO: We assume we run after a pass inserting appropriate coercions
                assert (right.getType() instanceof StringType || right instanceof NullLiteral);
                // The return of the generated code here is a bit subtle.  strcmp returns 0 if
                // the strings are equal.  We're testing for *in*equality, and will get a non-zero
                // (true in C) value if the strings are non-equal
                ret = mkFunctionCall(mkIntrinsicName("___sjs_strcmp"),
                                     left,
                                     right);
            } else {
                ret = mkBinaryOp(left, op, right);
            }
            
        } else if ("==".equals(op) || "===".equals(op)) {
            if (left.getType() instanceof StringType && right.getType() instanceof StringType) {
                // TODO: We assume we run after a pass inserting appropriate coercions
                // The return of the generated code here is a bit subtle.  strcmp returns 0 if
                // the strings are equal.  We're testing for *in*equality, and will get a non-zero
                // (true in C) value if the strings are non-equal
                FunctionCall f = mkFunctionCall(mkIntrinsicName("___sjs_strcmp"),
                                     left.accept(this).asExpression(),
                                     right.accept(this).asExpression());
                f.setType(Types.mkBool());
                IntLiteral i = mkIntLiteral(0);
                i.setType(Types.mkInt());
                BinaryOp b = mkBinaryOp(f, "==", i);
                b.setType(Types.mkBool());
                ret = b;
            } else {
                ret = mkBinaryOp(left, op, right);
            }
            
        } else if ("%".equals(op)) {
            if (left.getType() instanceof FloatType || right.getType() instanceof FloatType) {
                // rewrite % on floating point numbers to a call to fmod
                FunctionCall f = mkFunctionCall(mkIntrinsicName("fmod"),
                                    left.accept(this).asExpression(),
                                    right.accept(this).asExpression());
                f.setType(Types.mkFloat());
                ret = f;
            } else {
                ret = mkBinaryOp(left, op, right);
            }
        } else {
            ret = mkBinaryOp(left, op, right);
        }
        if (node.getType() == null) {
            System.err.println("Original binop node missing type: "+node.toSource(0));
        }
        assert(node.getType() != null);
        ret.setType(node.getType());
        assert(ret.getType() != null);
        return ret;
    }
    @Override
    public IRNode visitPredictedFieldAssignment(PredictedFieldAssignment node) {
        throw new IllegalArgumentException("Intrinsic inlining should occur before field access optimization");
    }
    @Override
    public IRNode visitFieldAssignment(FieldAssignment node) {
        if (node.getType() instanceof StringType && node.getOperator().equals("+=")) {
            // desugar += for strings, defer to standard binop case
            FieldRead fr = mkFieldRead(node.getObject(), node.getField());
            fr.setType(node.getType());
            BinaryOp binop = mkBinaryOp(fr, "+", node.getValue());
            binop.setType(node.getType());
            FieldAssignment fa = mkFieldAssignment(node.getObject(), node.getField(), "=", binop);
            fa.setType(node.getType());
            return fa.accept(this);
        } else {
            return super.visitFieldAssignment(node);
        }
    }

    @Override 
    public IRNode visitVarAssignment(VarAssignment node) {
        Expression left = node.getAssignedVar().accept(this).asExpression();
        Expression right = node.getAssignedValue().accept(this).asExpression();
        String op = node.getOperator();
        Expression ret = null;
        if (left.getType() == null) {
            System.err.println("Missing type on: "+left.toSource(0)+" in "+node.toSource(0));
        }
        assert (left.getType() != null);
        if (right.getType() == null) {
            System.err.println("Missing type on: "+right.toSource(0)+" in "+node.toSource(0));
            System.err.println("Assuming type of LHS: "+left.getType().toString());
            right.setType(left.getType());
        }
        if (left.getType() instanceof StringType) {
            switch (op) {
                case "+=":
                    BinaryOp binop = mkBinaryOp(left, "+", right);
                    binop.setType(left.getType());
                    Expression replacement = mkVarAssignment(left, "=", binop);
                    replacement.setType(node.getType());
                    return replacement.accept(this);
                default:
                    // continue on...
            }
        }
        //assert (right.getType() != null);
        if (left instanceof ArrayIndex && ("+=".equals(op) || "-=".equals(op))) {
            ArrayIndex ind = (ArrayIndex)left;
            if (ind.getArray().isConst() && ind.getOffset().isConst()) {
                Expression rhs = mkBinaryOp(left, String.valueOf(op.charAt(0)), right);
                rhs.setType(left.getType());
                ret = mkVarAssignment(left, "=", rhs);
            } else {
                // Need to introduce temp variables
                Type arr_type = ind.getArray().getType();
                Var arr = freshVar(curScope, "array", arr_type);
                curScope.declareVariable(arr, arr_type);
                Type off_type = ind.getOffset().getType();
                assert(off_type != null);
                Var off = freshVar(curScope, "i", off_type);
                curScope.declareVariable(off, off_type);

                currentBlock.prefixStatement(mkVarDecl(arr, arr_type, mkIntLiteral(7)));
                currentBlock.prefixStatement(mkVarDecl(off, off_type, mkIntLiteral(9)));

                ArrayIndex ai = mkArrayIndex(arr,off);
                ai.setType(node.getAssignedVar().getType());
                ret = mkBinaryOp(ai, String.valueOf(op.charAt(0)), right);
                ret.setType(node.getType());
                ret = mkVarAssignment(ai, "=", ret);
                ret.setType(node.getType());
                VarAssignment offasgn = mkVarAssignment(off, "=", ind.getOffset());
                offasgn.setType(off_type);
                ret = mkBinaryOp(offasgn,
                              ",",
                              ret);
                ret.setType(node.getType());
                VarAssignment arrasgn = mkVarAssignment(arr, "=", ind.getArray());
                arrasgn.setType(arr_type);
                ret = mkBinaryOp(arrasgn,
                              ",",
                              ret);
                ret.setType(node.getType());
            }
        } else {
            // Var assign
            if ("+=".equals(op)) {
                if (left.getType() instanceof StringType) {
                    // TODO: We assume we run after a pass inserting appropriate coercions
                    if (!(right.getType() instanceof StringType)) {
                        System.err.println("ERROR: Unsupported: += with LHS string and RHS: "+right.getType());
                    }
                    assert (right.getType() instanceof StringType);
                    ret = mkFunctionCall(mkIntrinsicName("__str__concat"),
                                         left,
                                         right);
                    ret.setType(Types.mkString());
                    ret = mkVarAssignment(left, "=", ret);
                } else {
                    ret = mkVarAssignment(left, op, right);
                }
            } else {
                ret = mkVarAssignment(left, op, right);
            }
        }
        assert (node.getType() != null);
        ret.setType(node.getType());
        return ret;
    }

    @Override
    public IRNode visitAllocNewObject(AllocNewObject node) {
        // TODO: Scoping!!!
        if (node.getConstructor().isVar() && node.getConstructor().asVar().getIdentifier().equals("Array")) {
            // The Array closure at the top level is the 1-argument version.  Desugar 0-arg into an
            // empty array literal.
            if (node.getArguments().size() == 0) {
                AllocArrayLiteral arr = mkAllocArrayLiteral(((ArrayType)node.getType()).elemType());
                arr.setType(node.getType());
                return arr;
            }
            // rewrite call to array constructor
            FunctionCall fcall = mkFunctionCall(node.getConstructor().asVar());
            for (Expression e : node.getArguments()) {
                fcall.addArgument(e.accept(this).asExpression());
            }
            fcall.setType(node.getType());
            return fcall;
        } else if (node.getConstructor().isVar() && 
                    node.getConstructor().asVar().getIdentifier().equals("String")) {
            // TODO: Is this constructor overloaded???
            return node.getArguments().get(0).accept(this).asExpression();
        } else {
            return super.visitAllocNewObject(node);
        }
    }
}
