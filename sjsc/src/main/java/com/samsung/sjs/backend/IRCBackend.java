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
/**
 * Last-stage transformation of a transformed and lowered IR to
 * a representation of C code suitable for direct output.
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend;

import com.samsung.sjs.backend.asts.ir.*;
import com.samsung.sjs.backend.asts.c.*;
import com.samsung.sjs.backend.asts.c.types.*;
import com.samsung.sjs.types.*;
import com.samsung.sjs.CompilerOptions;
import com.samsung.sjs.FFILinkage;
import com.samsung.sjs.JSEnvironment;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class IRCBackend extends CBackend {

    private static Logger logger = LoggerFactory.getLogger(IRCBackend.class);

    private Script program;
    private boolean debug;
    private Stack<Function> functions;
    private Stack<FunctionDeclaration> compiled_functions;
    private FFILinkage ffi;
    private JSEnvironment toplevel;

    private CompilerOptions options;

    private SlowPathGenerator slowgen;
    public IRCBackend(Script r,
                    CompilerOptions opts,
                    IRFieldCollector.FieldMapping m,
                    FFILinkage ffi,
                    JSEnvironment env) {
        program = r;
        this.debug = opts.debug();
        this.options = opts;
        field_codes = m;
        assert (m != null);
        functions = new Stack<Function>();
        compiled_functions = new Stack<FunctionDeclaration>();
        this.ffi = ffi;
        this.toplevel = env;
        slowgen = new SlowPathGenerator();
    }

    public com.samsung.sjs.backend.asts.c.Statement generateObjectMap(String name, List<String> properties) {
        if (debug) {
            System.err.println("Generating object map for "+ name + ": "+properties);
            System.err.println("Using field coding...");
            System.err.println(field_codes.toString());
        }
        int[] vt = new int[field_codes.size()];
        java.util.Arrays.fill(vt, -1);
        int physical_index = 0;
        for (String prop : properties) {
            if (debug) {
                System.err.println("Looking up index of "+prop);
            }
            vt[field_codes.indexOf(prop)] = physical_index++;
        }
        CArrayLiteral arr = new CArrayLiteral();
        for (int x = 0; x < vt.length; x++) {
            arr.addElement(new com.samsung.sjs.backend.asts.c.IntLiteral(vt[x]));
        }
        // TODO: Refactor so we're not doing this hideous "int <name>[n] = " gen here
        com.samsung.sjs.backend.asts.c.VariableDeclaration vd =
            //new com.samsung.sjs.backend.asts.c.VariableDeclaration(false, new VTablePseudoType());
            new com.samsung.sjs.backend.asts.c.VariableDeclaration(false, new CInteger());
        //vd.addVariable(new Variable(name), arr);
        vd.addVariable(new Variable(name+"[]"), arr);
        return vd;
    }

    public static void exportPropertyOffsets(CompilationUnit ccode, IRFieldCollector.FieldMapping m) {
        for (Map.Entry<String,Integer> kv : m) {
            ccode.declarePropertyConstant(kv.getKey(), kv.getValue());
        }
    }

    public CompilationUnit compile() {
        CompilationUnit ccode = new CompilationUnit();
        ccode.addStatement(new IncludeDirective("runtime.h"));
        ccode.addStatement(new IncludeDirective("ffi.h"));
        ccode.addStatement(new IncludeDirective("globals.h"));
        ccode.addStatement(new IncludeDirective("map.h"));

        if (options.eflEnabled()) {
            ccode.addStatement(new IncludeDirective("Elementary.h"));
        }

        if (options.interopEnabled()) {
            ccode.addStatement(new IncludeDirective("interop.h"));
        }

        ccode.exportString("#ifdef __cplusplus");
        ccode.exportString("extern \"C\" {");
        ccode.exportString("#endif // __cplusplus");
        if (options.isGuestRuntime()) {
            ccode.exportString("extern int __sjs_main(int);");
        }

        // Process vtables exported to other runtime modules (e.g., for console)
        for (Map.Entry<String,List<String>> table_req : ffi.getTablesToGenerate()) {
            ccode.addStatement(generateObjectMap(table_req.getKey(), table_req.getValue()));
            ccode.exportIndirectionMap(table_req.getKey());
        }

        com.samsung.sjs.backend.asts.c.CompoundStatement vtables =
            new com.samsung.sjs.backend.asts.c.CompoundStatement();
        ccode.addStatement(vtables);

        ccode.addStatement(new IncludeDirective("array.h"));
        BackPatchDeclarations bpd = new BackPatchDeclarations();
        ccode.addStatement(bpd);

        // insert extern declarations for FFI entities
        for (Map.Entry<String,FFILinkage.LinkEntry> extern : ffi.entrySet()) {
            Type t = toplevel.get(extern.getKey());
            if (t == null) {
                System.err.println("BAD: FFI linkage declaration for ["+extern.getKey()+"], but toplevel has no type for it");
            }
            if (t.isIntersectionType()) {
                continue;
                // TODO: Implement runtime representation for intersection of multiple types
            }
            CType ct = getTypeConverter().convert(t);
            if (extern.getValue().boxed) {
                ccode.addStatement(new com.samsung.sjs.backend.asts.c.ExpressionStatement(
                    new InlineCCode("extern value_t* "+extern.getKey())));
            } else {
                ccode.addStatement(new com.samsung.sjs.backend.asts.c.ExpressionStatement(
                    new InlineCCode("extern "+ct.toSource()+" "+extern.getKey())));
            }
        }

        // This emits field #defines in the C code and header
        exportPropertyOffsets(ccode, field_codes);

        // Make space for string literal decls
        com.samsung.sjs.backend.asts.c.CompoundStatement strlits =
            new com.samsung.sjs.backend.asts.c.CompoundStatement();
        ccode.addStatement(strlits);
        this.string_literal_decls = strlits;

        // Rhino seems to do a lot of casting from Node to AstNode
        //for (Node n : sourcetree) {
        for (IRNode n : program) {
            CNode result = n.accept(this);
            if (debug) {
                System.err.println("Converting ["+n.toSource(0)+"]");
                System.err.println(">>> ["+result.toSource(0)+"]");
            }
            if (result instanceof FunctionDeclaration) {
                bpd.preDeclare((FunctionDeclaration)result);
            }
            ccode.addStatement((com.samsung.sjs.backend.asts.c.Statement)result);
        }
        // Now that we've observed all anonymous C types, we can backtrack to generate some typedefs
        String anondefs = getTypeConverter().tn.genTypeDefs();
        bpd.setTypeDefs(anondefs);

        ccode.exportString(anondefs);
        ccode.exportString("#ifdef __cplusplus");
        ccode.exportString("} // extern C");
        ccode.exportString("#endif // __cplusplus");

        for (Map.Entry<Integer,Set<Pair<int[],Integer>>> entry : vtables_by_hash.entrySet()) {
            for (Pair<int[],Integer> vt_and_id : entry.getValue()) {
                int[] vt = vt_and_id.getKey();
                int i = vt_and_id.getValue();
                CArrayLiteral arr = new CArrayLiteral();
                for (int x = 0; x < vt.length; x++) {
                    arr.addElement(new com.samsung.sjs.backend.asts.c.IntLiteral(vt[x]));
                }
                // TODO: Refactor so we're not doing this hideous "int <name>[n] = " gen here
                com.samsung.sjs.backend.asts.c.VariableDeclaration vd =
                    //new com.samsung.sjs.backend.asts.c.VariableDeclaration(false, new VTablePseudoType());
                    new com.samsung.sjs.backend.asts.c.VariableDeclaration(false, new CInteger());
                vd.addVariable(new Variable("__vtable_id_"+i+"[]"), arr);
                vtables.addStatement(vd);
            }
        }

        return ccode;
    }

    @Override
    public com.samsung.sjs.backend.asts.c.Expression visitVar(Var node) {
        Variable v = new Variable(node.getIdentifier());
        boolean should_unbox = false;
        if (lval_capture) {
            should_unbox = true;
        }
        if (shouldUnbox(node)) {
            if (debug) {
                System.err.println(">>>> Unboxing use of "+node.getIdentifier()+" in function "+functions.peek().getName());
            }
            should_unbox = true;
        }
        if (should_unbox) {
            return v;
        } else {
            // TODO: Rename Unbox, since it's really looking inside a box, not unboxing
            return new Unbox(v);
        }
    }

    @Override
    public com.samsung.sjs.backend.asts.c.Expression visitArrayIndex(ArrayIndex node) {
        return visitArrayIndex(node, false);
    }
    public com.samsung.sjs.backend.asts.c.Expression visitArrayIndex(ArrayIndex node, boolean lval_position) {
        if (node.getArray().getType() instanceof EnvironmentType) {
            // Indexing into an environment, which is represented as a literal C array
            Var envname = (Var)node.getArray();
            // TODO: Right now this only works with constant offsets, as for environment access
            if (!(envname.getType() instanceof EnvironmentType)) {
                System.err.println("Variable "+envname.toSource(0)+" is not an EnvironmentType");
            }
            assert (envname.getType() instanceof EnvironmentType);
            com.samsung.sjs.backend.asts.ir.IntLiteral offset = (com.samsung.sjs.backend.asts.ir.IntLiteral)node.getOffset();
            int ioffset = (int)offset.getValue();
            ArrayIndexing ind = new ArrayIndexing(new Variable(envname.getIdentifier()), ioffset);
            if (lval_capture) {
                return ind;
            } else {
                return new Unbox(ind);
                // need to cast the box when it's a pointer
                //com.samsung.sjs.backend.asts.c.Expression access = null;
                //access = new ValueAs(new Unbox(ind), node.getType());
                //if (lval_position) {
                //    return access;
                //} else {
                //    return new CastExpression(getTypeConverter().convert(node.getType()), access);
                //}
            }
        } else if (node.getArray().getType() instanceof StringType) {
            return new ValueCoercion(Types.mkString(),
                    new com.samsung.sjs.backend.asts.c.FunctionCall(
                            new Variable("__str__charAt"),
                            node.getArray().accept(this).asExpression().inType(Types.mkString()),
                            node.getOffset().accept(this).asExpression().inType(Types.mkInt())),
                    false);
        } else {
            Type atype = node.getArray().getType();
            if (!(atype.isArray() || atype.isMap())) {
                System.err.println("??? have an array that is not array or map, after handling env and string");
                System.err.println(node.toSource(0)+" :: "+node.getType());
            }
            assert (atype.isArray() || atype.isMap());
            // Typing a JSArray access
            Type elemType = null;
            Type keyType = null;
            if (atype.isArray()) {
                ArrayType arrty = (ArrayType)node.getArray().getType();
                elemType = arrty.elemType();
                keyType = Types.mkInt();
            } else {
                MapType mapty = (MapType)atype;
                elemType = mapty.elemType();
                keyType = Types.mkString();
            }

            if (node.getArray().isPure() && node.getOffset().isPure()) {
                com.samsung.sjs.backend.asts.c.Expression access = null;
                com.samsung.sjs.backend.asts.c.FunctionCall f =
                    new com.samsung.sjs.backend.asts.c.FunctionCall(atype.isArray() ? "array_get" : "__map_access");
                f.addActualArgument(node.getArray().accept(this).asExpression().inType(node.getArray().getType()));
                f.addActualArgument(node.getOffset().accept(this).asExpression().inType(node.getOffset().getType()));
                //if (elemType instanceof PrimitiveType) {
                //    access = new ValueAs(f, elemType);
                //} else {
                //    access = new CastExpression(getTypeConverter().convert(elemType), new ValueAs(f, elemType));
                //}
                //return access;
                return f;
            } else {
                // Note that array_get is a macro, and will duplicate both expressions many times
                Variable arr_tmp = newTempVariable(atype);
                Variable i_tmp = newTempVariable(keyType);

                Assignment asgn_arr = new Assignment(arr_tmp, "=", node.getArray().accept(this).asExpression().asValue(node.getArray().getType()));
                Assignment asgn_i = new Assignment(i_tmp, "=", node.getOffset().accept(this).asExpression().asValue(node.getOffset().getType()));

                com.samsung.sjs.backend.asts.c.FunctionCall f =
                    new com.samsung.sjs.backend.asts.c.FunctionCall(atype.isArray() ? "array_get" : "__map_access");
                //f.addActualArgument(node.getArray().accept(this).asExpression());
                //f.addActualArgument(node.getOffset().accept(this).asExpression());
                f.addActualArgument(arr_tmp.inType(node.getArray().getType()));
                f.addActualArgument(i_tmp.inType(node.getOffset().getType()));

                com.samsung.sjs.backend.asts.c.Expression access = null;
                access = f;
                //return new CastExpression(getTypeConverter().convert(elemType), f);
                //if (elemType instanceof PrimitiveType) {
                //    access = new ValueAs(f, elemType);
                //} else {
                //    access = new CastExpression(getTypeConverter().convert(elemType), new ValueAs(f, elemType));
                //}
                return new BinaryInfixExpression(asgn_arr, ",",

                        new BinaryInfixExpression(asgn_i, ",",
                            access));
            }
        }
    }

    private boolean shouldUnbox(Var node) {
        //return (!functions.isEmpty() // Not in top-level
        //        && !functions.peek().getCaptured().contains(node.getIdentifier()) // Not captured in a closure
        //        && !program.getScope().isLocallyBound(node) // Don't unbox ffi objects
        //        );
        if (functions.isEmpty() /* top level ; TODO: Why? */
                || functions.peek().getCaptured().contains(node.getIdentifier()) // captured in later closure
                ) {
            return false;
        }
        if (program.getScope().isLocallyBound(node)) {
            // bound in global context: check ffi linkage
            FFILinkage.LinkEntry l = ffi.get(node.getIdentifier());
            if (l != null) {
                return !l.boxed;
            } else {
                // If we haven't found linkage information for a top-level variable, that's bad
                throw new IllegalArgumentException("Missing linkage information for global variable ["+node.getIdentifier()+"]; did we forget to pass a *.linkage.json file?");
            }
        }
        // not top-level, not captured, and not FFI.  unbox it.
        return true;
    }

    @Override
    public com.samsung.sjs.backend.asts.c.Statement visitVarDecl(VarDecl node) {
        // We'll see at most one VarDecl for a given variable in some JS scope, so we should
        // always go ahead and prefix the hoisted declaration.
        CType decltype = getTypeConverter().convert(node.getType());
        com.samsung.sjs.backend.asts.c.VariableDeclaration d =
            new com.samsung.sjs.backend.asts.c.VariableDeclaration(false, /*decltype*/new Value());
        com.samsung.sjs.backend.asts.c.Expression var = node.getVar().accept(this).asExpression();
        // var may be a box macro, or it may be a value-projection
        if (var instanceof ValueAs) {
            var = ((ValueAs)var).expr();
        }
        // NOTE: It's tempting to check in here if something is a temporary variable introduced by
        // the ThreeAddress pass, and not hoist in that case.  But the temp
        // variables must be function-scoped so they can be shared across the type-correct and
        // type-checking paths in interop mode.  Solution: function scope for interop intermediates,
        // local scope here.  The dirty flag check must then be generated in a context of which
        // locals are in scope, copy their values to the function-scoped checking-path
        // intermediates, and then transition.  This means the cost of making intermediates
        // function-scoped causes C optimizer issues in the "slower" path, not the faster path.
        // TODO: replace with proper temp var flag
        if (node.getVar().asVar().getIdentifier().startsWith("__tmp")) {
            // This is a compiler-introduced intermediate (sort of SSA) variable, which has two
            // important properties we can't assume about most source-level variables:
            //   1. It is always written before being read
            //   2. Its initialization is well-defined; there's no cyclic definition mess to
            //      untangle.
            // As a result, we do *not* need to hoist this to the top of the C function, and we
            // don't need to fabricate an undefined initialization!  To boot, these are never
            // captured in closures because they name intermediate expressions.  Let's emit C-block-scope declarations so the C compiler can do a better job reasoning.
            //
            // Note, however, that for interop mode, when generating the second body, we need to
            // duplicate (under a different but related name) this temp var, and hoist *that*
            // declaration, so the clean-to-dirty transition can copy temporaries appropriately.
            d.addVariable(var, node.getInitialValue().accept(this).asExpression().asValue(node.getInitialValue().getType()));
            return d;
        }
        if (node.getType() instanceof FloatType) {
            d.addVariable(var,
                           shouldUnbox(node.getVar()) ?
                           new com.samsung.sjs.backend.asts.c.DoubleLiteral(Double.NaN).asValue(Types.mkFloat()) :
                           new AllocBox(new com.samsung.sjs.backend.asts.c.DoubleLiteral(Double.NaN).asValue(Types.mkFloat()), decltype)
                          );
        } else {
            d.addVariable(var,
                           shouldUnbox(node.getVar()) ?
                           new com.samsung.sjs.backend.asts.c.IntLiteral(0).asValue(Types.mkInt()) :
                           new AllocBox(new com.samsung.sjs.backend.asts.c.IntLiteral(0).asValue(Types.mkInt()), decltype)
                          );
        }
        compiled_functions.peek().getBody().prefixStatement(d);

        // TODO: stop generating extra writes if there's an initial value...
        com.samsung.sjs.backend.asts.c.Expression initval = node.getInitialValue().accept(this).asExpression().asValue(node.getInitialValue().getType());
        if (node.getInitialValue().getType() instanceof IntegerType && node.getVar().getType() instanceof FloatType) {
            // coercion
            initval = coerceIntToFloat(initval);
        }
        Assignment wr = new Assignment(var,
                                      "=",
                                      initval);
                      //new AllocBox(
                      //    node.getType() instanceof PrimitiveType ?
                      //    node.getInitialValue().accept(this).asExpression() :
                      //    new CastExpression(new CVoid(1), node.getInitialValue().accept(this).asExpression())
                      //    ,
                      //             decltype));

        return new com.samsung.sjs.backend.asts.c.ExpressionStatement(wr);
    }

    public com.samsung.sjs.backend.asts.c.Expression visitNonBooleanOr(BinaryOp node) {
        // we convert the left side as a value, to preserve possible undefinedness
        // TODO: the 3-addr conversion needs to decompose this when there are effects...
        com.samsung.sjs.backend.asts.c.Expression left = node.getLeft().accept(this).asExpression().asValue(node.getLeft().getType());
        com.samsung.sjs.backend.asts.c.Expression right = node.getRight().accept(this).asExpression().asValue(node.getRight().getType());
        ConditionalExpression condexpr = new ConditionalExpression();
        com.samsung.sjs.backend.asts.c.FunctionCall test = new com.samsung.sjs.backend.asts.c.FunctionCall("!val_is_falsy");
        test.addActualArgument(left);
        condexpr.setTest(test);
        condexpr.setTrueBranch(left);
        condexpr.setFalseBranch(right);
        return condexpr;
    }

    @Override
    public com.samsung.sjs.backend.asts.c.Expression visitBinaryOp(BinaryOp node) {
        if (node.getType() == null) {
            System.err.println("BAD: binop without overall result type: "+node.toSource(0));
        }
        assert(node.getType() != null);
        if (node.getLeft().getType() == null) {
            System.err.println("BAD: binop lhs w/o type: "+node.getLeft().toSource(0));
        }
        if (node.getOp().equals("||") && !(node.getLeft().getType() instanceof BooleanType)) {
            return visitNonBooleanOr(node);
        }
        com.samsung.sjs.backend.asts.c.Expression left = node.getLeft().accept(this).asExpression().inType(node.getLeft().getType());
        com.samsung.sjs.backend.asts.c.Expression right = node.getRight().accept(this).asExpression().inType(node.getRight().getType());
        switch (node.getOp()) {
            case "|":
            case "&":
            case "^":
            case ">>":
            case ">>>":
            case "<<":
                // For bitwise operators, insert float -> int32 coercion for float operands
                if (node.getLeft().getType() instanceof FloatType) {
                    left = new com.samsung.sjs.backend.asts.c.FunctionCall("___int_of_float", left);
                }
                if (node.getRight().getType() instanceof FloatType) {
                    right = new com.samsung.sjs.backend.asts.c.FunctionCall("___int_of_float", right);
                }
                break;
            default:
                break;
        }
        if (node.getOp().equals(">>>")) {
            // zero-fill right shift, as opposed to signed shift.
            return new InlineCCode("int_as_val((int32_t)(((unsigned int)"+left.toSource(0)+") >> "+right.toSource(0)+"))");
        }
        if (node.getOp().equals("/") && node.getLeft().getType() instanceof IntegerType && node.getRight().getType() instanceof IntegerType) {
            // To avoid silently introducing integer division imprecision in a language that doesn't
            // have it, we must cast one of the operands to double
            right = new CastExpression(new CDouble(), right);
        }
        // TODO: Between here and above, we're clearly double-casting, but even with this lower one
        // removed, we're double-casting elsewhere...
        return new BinaryInfixExpression(left,
                                         lower_op(node.getOp()),
                                         right).asValue(node.getType());
    }

    protected com.samsung.sjs.backend.asts.c.Expression coerceIntToFloat(com.samsung.sjs.backend.asts.c.Expression e) {
        CastExpression ce = new CastExpression(new CDouble(), e.inType(Types.mkInt()));
        return ce.asValue(Types.mkFloat());
    }

    public com.samsung.sjs.backend.asts.c.Expression visitAssignment(com.samsung.sjs.backend.asts.ir.Expression left,
                                                                     String op,
                                                                     com.samsung.sjs.backend.asts.ir.Expression right) {
        boolean intfloat_coercion = false;
        if (left.getType() instanceof FloatType && right.getType() instanceof IntegerType) {
            intfloat_coercion = true;
        }
        // Remember that (modulo a future unboxing pass),
        // object fields are boxed (for inheritance), as well as variables (for capture)
        // By default, the target, if translated naively, is translated as an access to
        // the box contents, so we shouldn't need to treat anything specially here
        // until we do an unboxing optimization.
        if (left instanceof ArrayIndex) {
        // TODO: Ignore other calls to merge Var, Field, and Array assignments.  Keep them separate,
        // which will keep the code paths clean.  Finish splitting them.
            ArrayIndex node = (ArrayIndex) left;
            if (node.getArray().getType().isArray() || node.getArray().getType().isMap()) {
                Type elemType = null;
                boolean ismap = false;
                if (node.getArray().getType().isArray()) {
                    ArrayType arrty = (ArrayType)node.getArray().getType();
                    elemType = arrty.elemType();
                } else {
                    ismap = true;
                    MapType mapty = (MapType)node.getArray().getType();
                    elemType = mapty.elemType();
                }
                com.samsung.sjs.backend.asts.c.FunctionCall f =
                    new com.samsung.sjs.backend.asts.c.FunctionCall(ismap ? "__map_store" : "array_put");
                f.addActualArgument(node.getArray().accept(this).asExpression().inType(node.getArray().getType()));
                f.addActualArgument(node.getOffset().accept(this).asExpression().inType(node.getOffset().getType()));
                com.samsung.sjs.backend.asts.c.Expression rhs = right.accept(this).asExpression();
                if (intfloat_coercion) {
                    rhs = coerceIntToFloat(rhs);
                }
                if (op.length() == 2) {
                    assert (node.getArray().isPure() && node.getOffset().isPure()); // TODO: memoize as elsewhere!
                    f.addActualArgument(new BinaryInfixExpression(
                                    left.accept(this).asExpression().inType(node.getType()), String.valueOf(op.charAt(0)),
                                    rhs.inType(left.getType())
                                ).asValue(node.getType()));
                } else {
                    f.addActualArgument(rhs.asValue(right.getType()));
                    //f.addActualArgument(new CastExpression(new Value(), right.accept(this).asExpression()));
                }
                //return new CastExpression(getTypeConverter().convert(elemType), f);
                //return new ValueAs(f, elemType);
                return f;
            }
        }

        com.samsung.sjs.backend.asts.c.Expression val = right.accept(this).asExpression();
        if (intfloat_coercion) {
            val = coerceIntToFloat(val);
        }

        // TODO: return value shifting for floats in interop
        if (left instanceof FieldRead) {
            FieldRead fr = (FieldRead)left;
            com.samsung.sjs.backend.asts.c.FunctionCall f =
                new com.samsung.sjs.backend.asts.c.FunctionCall("FIELD_READ_WRITABLE");
            com.samsung.sjs.backend.asts.c.Expression obj = fr.getObject().accept(this).asExpression().inType(fr.getObject().getType());
            f.addActualArgument(new CastExpression(new ObjectPseudoType(), obj));
            f.addActualArgument(new com.samsung.sjs.backend.asts.c.IntLiteral(
                    field_codes.indexOf(fr.getField())));
            Type rhs_type = null;
            if (left.getType() instanceof FloatType && right.getType() instanceof IntegerType) {
                rhs_type = Types.mkFloat();
            } else {
                rhs_type = right.getType();
            }
            switch (op) {
                case "=":
                    return new BinaryInfixExpression(f, op, val.asValue(right.getType())).asValue(left.getType());
                case "+=":
                    return new BinaryInfixExpression(f, "=", new BinaryInfixExpression(f.inType(fr.getType()), "+", val.inType(rhs_type)).asValue(left.getType())).asValue(left.getType());
                case "-=":
                    return new BinaryInfixExpression(f, "=", new BinaryInfixExpression(f.inType(fr.getType()), "-", val.inType(rhs_type)).asValue(left.getType())).asValue(left.getType());
                case "*=":
                    return new BinaryInfixExpression(f, "=", new BinaryInfixExpression(f.inType(fr.getType()), "*", val.inType(rhs_type)).asValue(left.getType())).asValue(left.getType());
                case "/=":
                    return new BinaryInfixExpression(f, "=", new BinaryInfixExpression(f.inType(fr.getType()), "/", val.inType(rhs_type)).asValue(left.getType())).asValue(left.getType());
                default:
                    throw new IllegalArgumentException("Unsupported binary operator for fields: "+op);
            }
        } else if (left.isVar() && left.getType() instanceof FloatType) {
            if (op.equals("+=") || op.equals("-=") || op.equals("/=") || op.equals("*=")) {
                val = new BinaryInfixExpression(left.accept(this).asExpression().inType(left.getType()),
                                                String.valueOf(op.charAt(0)),
                                                val.inType(intfloat_coercion ? left.getType() : right.getType())).asValue(left.getType());
                // TODO: Float shifting when in interop mode
                op = "=";
            }
        }

        com.samsung.sjs.backend.asts.c.Expression target = left.accept(this).asExpression();
        if (target instanceof CastExpression) {
            target = ((CastExpression)target).getBaseExpression();
        }

        // Prior to a type violation, the tag for a variable should be correct from initialization
        // onwards, so for non-pointers (i.e., no low tags) we can optimize local variable accesses to only touch the appropriate
        // projection
        if (left.isVar() && (left.getType() instanceof IntegerType || left.getType() instanceof ObjectType)) {
            return new ValueCoercion(left.getType(), new com.samsung.sjs.backend.asts.c.Assignment(
                    new InlineCCode(target.toSource(0)+"."+ValueAs.value_field_of(left.getType())), op, val.inType(right.getType())), false);
        }

        // fix for type-based modifying operations
        String newop = null;
        switch(op) {
            case "&=":
                newop = "&"; break;
            case "|=":
                newop = "|"; break;
            case "^=":
                newop = "^"; break;
            case "+=":
                newop = "+"; break;
            case "-=":
                newop = "-"; break;
            case "*=":
                newop = "*"; break;
            case "/=":
                newop = "/"; break;
            case "%=":
                newop = "%"; break;
            case "<<=":
                newop = "<<"; break;
            case ">>=":
                newop = ">>"; break;
            default:
        }
        if (newop != null) {
            val = new BinaryInfixExpression(target.inType(left.getType()), newop, val.inType(right.getType()));
            op = "=";
        }
        return new com.samsung.sjs.backend.asts.c.Assignment(target, op, val.asValue(right.getType()));
    }
    @Override
    public com.samsung.sjs.backend.asts.c.Expression visitVarAssignment(VarAssignment node) {
        return visitAssignment(node.getAssignedVar(), node.getOperator(), node.getAssignedValue());
    }
    @Override
    public com.samsung.sjs.backend.asts.c.Expression visitFieldAssignment(FieldAssignment node) {
        // TODO Ctor.prototype.m = ... is permitted, where Ctor is a function... Need to generate
        // partner objects for any function used in such a manner
        // But that would prohibit higher-order use of constructors (func(C, x) { return new C(x); })
        if (!(node.getObject().getType() instanceof ObjectType)) {
            assert (node.getObject().getType() != null);
            assert (node.getObject().getType().isConstructor());
            assert (node.getField().equals("prototype"));
            assert (node.getValue().getType().isObject());
            com.samsung.sjs.backend.asts.c.Expression ctor = node.getObject().accept(this).asExpression().inType(node.getObject().getType());
            ctor = new CastExpression(getTypeConverter().convert(node.getObject().getType()), ctor);
            com.samsung.sjs.backend.asts.c.Expression proto = node.getValue().accept(this).asExpression().inType(node.getValue().getType());
            return new InlineCCode(ctor.toSource(0)+"->proto = "+proto.toSource(0));
        }
        FieldRead fr = new FieldRead(node.getObject(), node.getField());
        fr.setType(((ObjectType)node.getObject().getType()).findMemberType(node.getField()));
        return visitAssignment(fr, node.getOperator(), node.getValue()).asValue(fr.getType());
    }
    @Override
    public com.samsung.sjs.backend.asts.c.Expression visitPredictedFieldAssignment(PredictedFieldAssignment node) {
        // TODO Ctor.prototype.m = ... is permitted, where Ctor is a function... Need to generate
        // partner objects for any function used in such a manner
        // But that would prohibit higher-order use of constructors (func(C, x) { return new C(x); })
        assert (node.getObject().getType() instanceof ObjectType);
        PredictedFieldRead fr = mkPredictedFieldRead(node.getObject(), node.getField(), node.getBoxPointerOffset());
        fr.setType(((ObjectType)node.getObject().getType()).findMemberType(node.getField()));
        String op = node.getOperator();

        com.samsung.sjs.backend.asts.c.Expression val = node.getValue().accept(this).asExpression();

        // TODO: return value shifting if we store a double....
        // In this case we know the field is local since we're writing to it, which means it's
        // not boxed, and since we've predicted the box offset we can emit an lval
        com.samsung.sjs.backend.asts.c.FunctionCall f =
            new com.samsung.sjs.backend.asts.c.FunctionCall("INLINE_BOX_ACCESS");
        f.addActualArgument(fr.getObject().accept(this).asExpression().inType(fr.getObject().getType()));
        f.addActualArgument(new com.samsung.sjs.backend.asts.c.IntLiteral(fr.getOffset()));
        com.samsung.sjs.backend.asts.c.Expression lhs = new ValueAs(f, fr.getType());
        // TODO: float shifting in interop
        //if (fr.getType() instanceof FloatType) {
        //    // TODO: The call f constructed below should be reusable in place of fr.accept... except
        //    // the current impl also handle casts and shifts.  Should replace the call with a
        //    // (direct) predicted field read
        //    lhs = new InlineCCode(f.toSource(0)+".box");
        //    com.samsung.sjs.backend.asts.c.FunctionCall shift =
        //        new com.samsung.sjs.backend.asts.c.FunctionCall("shift_double");
        //    switch (op) {
        //        case "=":
        //            shift.addActualArgument(val.inType(node.getValue().getType()));
        //            break;
        //        case "+=":
        //            shift.addActualArgument(new BinaryInfixExpression(
        //                        fr.accept(this).asExpression().inType(fr.getType()), // This unshifts during the read; TODO: optimize since it's writable
        //                        "+",
        //                        val.inType(node.getValue().getType())));
        //            break;
        //        case "-=":
        //            shift.addActualArgument(new BinaryInfixExpression(
        //                        fr.accept(this).asExpression().inType(fr.getType()), // This unshifts during the read; TODO: optimize since it's writable
        //                        "-",
        //                        val.inType(node.getValue().getType())));
        //            break;
        //        default:
        //            throw new IllegalArgumentException("Unsupported read-write operator for doubles: "+op);
        //    }
        //    val = shift;
        //    op = "=";
        //    return new com.samsung.sjs.backend.asts.c.Assignment(lhs, op, val);
        //}
        // TODO: Inline this behavior as well?  Or a general field-write specific variant
        return visitAssignment(fr, node.getOperator(), node.getValue());
    }

    @Override
    public FunctionDeclaration visitFunction(Function node) {
        functions.push(node);
        CodeType ftype = (CodeType)node.getType();
        Type retty = ftype.returnType();
        FunctionDeclaration fn = null;
        // Must initialize GC before any other actions
        if ((node.getName().equals("main") || node.getName().equals("__sjs_main")) && options.getMMScheme() == CompilerOptions.MMScheme.GC) {
            fn = new FunctionDeclaration(node.getName(), getTypeConverter().convert(retty));

            // set up efl environment
            if (options.eflEnabled()) {
                com.samsung.sjs.backend.asts.c.FunctionCall f = new com.samsung.sjs.backend.asts.c.FunctionCall("elm_init");
                assert (node.nargs() > 0);
                f.addActualArgument(new Variable(node.argName(0).getIdentifier()));
                f.addActualArgument(new com.samsung.sjs.backend.asts.c.NullLiteral());
                fn.addBodyStatement(new com.samsung.sjs.backend.asts.c.ExpressionStatement(f));

                f = new com.samsung.sjs.backend.asts.c.FunctionCall("elm_policy_set");
                f.addActualArgument(new Variable("ELM_POLICY_QUIT"));
                f.addActualArgument(new Variable("ELM_POLICY_QUIT_LAST_WINDOW_CLOSED"));
                fn.addBodyStatement(new com.samsung.sjs.backend.asts.c.ExpressionStatement(f));
            }

            fn.addBodyStatement(
                    new com.samsung.sjs.backend.asts.c.ExpressionStatement(
                        new com.samsung.sjs.backend.asts.c.FunctionCall("GC_INIT")));
        } else {
            fn = new FunctionDeclaration(node.getName(), retty instanceof VoidType ? new CVoid() : new Value() /*getTypeConverter().convert(retty)*/);
        }
        //fn.addArgument(node.getEnvironmentName(), new EnvironmentPseudoType(node.getEnvLayout()));
        for (int i = 0; i < node.nargs(); i++) {
            Var name = node.argName(i);
            Type ty = node.argType(i);
            if (ty == null) {
                System.err.println("ERROR: visiting function ["+node.getName()+"], no type on argument "+i);
                System.err.println("Full function:\n"+node.toSource(0));
            }
            assert (ty != null);
            if (ty instanceof EnvironmentType) {
                assert (i == 0);
                fn.addArgument(node.getEnvironmentName(), new EnvironmentPseudoType(node.getEnvLayout()));
            } else if (ty instanceof CRuntimeArray) {
                // We need a box, but for a regular JS array, and we need to populate it with
                // a conversion function.
                CRuntimeArray cra = (CRuntimeArray)ty;
                CType argt = getTypeConverter().convert(ty);
                fn.addArgument("__"+name.getIdentifier(), argt);
                CType cellt = getTypeConverter().convert(cra.elemType());
                com.samsung.sjs.backend.asts.c.VariableDeclaration vd =
                    new com.samsung.sjs.backend.asts.c.VariableDeclaration(false, new JSArray());
                com.samsung.sjs.backend.asts.c.FunctionCall f =
                    new com.samsung.sjs.backend.asts.c.FunctionCall("c_to_js_array");
                f.addActualArgument(new Variable("__"+name.getIdentifier()));
                vd.addVariable(new Unbox(new Variable(name.getIdentifier())),
                               new AllocBox(new CastExpression(new CVoid(1), f), new JSArray()));
                fn.addBodyStatement(vd);
            } else {
                CType t = new Value(); // was, when type-specializing: getTypeConverter().convert(ty);
                if ((node.getName().equals("main") || node.getName().equals("__sjs_main")) && options.getMMScheme() == CompilerOptions.MMScheme.GC) {
                    // main() needs the right signature
                    t = getTypeConverter().convert(ty);
                }
                if (shouldUnbox(name)) { // IMPORTANT: We've pushed the function we're generating onto the functions stack already
                    fn.addArgument(name.getIdentifier(), t);
                } else {
                    fn.addArgument("__"+name.getIdentifier(), t);
                    // Now deal with creating a fresh box for a passed-by-value argument
                    com.samsung.sjs.backend.asts.c.VariableDeclaration vd =
                        new com.samsung.sjs.backend.asts.c.VariableDeclaration(false, t);
                    vd.addVariable(new Unbox(new Variable(name.getIdentifier())),
                                   new AllocBox(
                                       new Variable("__"+name.getIdentifier())
                                       , t));
                    fn.addBodyStatement(vd);
                }
            }
        }

        // For constructors, copy down inherited field pointers.  If the prototype is a native
        // wrapper, then also instantiate an SJS subclass
        if (node.isConstructor()) {
            logger.debug("Compiling constructor {} with result type: {}", node.getName(), retty);
            logger.debug(((ObjectType)retty).inheritedProperties().size()+" inherited properties");
            for (Property p : ((ObjectType)retty).inheritedProperties()) {
                int vt_off = field_codes.indexOf(p.getName());
                com.samsung.sjs.backend.asts.c.FunctionCall exp =
                    new com.samsung.sjs.backend.asts.c.FunctionCall("INHERIT_FIELD_COMPRESSED");
                exp.addActualArgument(new InlineCCode("__this.obj"));
                exp.addActualArgument(new InlineCCode("__this.obj->__proto__"));
                exp.addActualArgument(new InlineCCode(String.valueOf(vt_off)));
                fn.addBodyStatement(new com.samsung.sjs.backend.asts.c.ExpressionStatement(exp));
            }

            // Generate CPP proxy if needed; notice this (intentionally) may clobber a naive field
            // inheritance of _____cpp_receiver and _____gen_cpp_proxy!
            fn.addBodyStatement(new com.samsung.sjs.backend.asts.c.ExpressionStatement(new InlineCCode(
                            "MAYBE_GEN_CPP_PROXY(__this.obj)")));

            ObjectType oretty = (ObjectType)retty;
            // TODO: native prototype support
            //for (Property p : oretty.properties()) {
            //    // TODO: We don't necessarily know, currently, if the prefix of this object will
            //    // match the layout of the parent, statically.  In fact, it often won't.  But we
            //    // should figure out how to make that the case.
            //    // TODO: Inside a constructor, we should know exactly the vtable, and should be able
            //    // to generate better code
            //    if (p.getPropertySetId() == PropertySetId.RO) {
            //        // inhertied property
            //        int vt_off = field_codes.indexOf(p.getName());
            //        InlineCCode exp = new InlineCCode(
            //                "__this->fields[__this->vtbl["+vt_off+
            //                "]].ptr = ((uint64_t)&__this->__proto__->fields[__this->__proto__->vtbl["+vt_off+"]]) | 0x1;");
            //        fn.addBodyStatement(new com.samsung.sjs.backend.asts.c.ExpressionStatement(exp));
            //    }
            //}
        }

        Block body = (Block)node.getBody();
        compiled_functions.push(fn);

        BlockStatement typed = new BlockStatement();

        for (com.samsung.sjs.backend.asts.ir.Statement n : body) {
            CNode s = n.accept(this);
            // TODO: UNDO THIS TERRIBLE HACK
            if (s instanceof com.samsung.sjs.backend.asts.c.Expression) {
                typed.addStatement(new com.samsung.sjs.backend.asts.c.ExpressionStatement((com.samsung.sjs.backend.asts.c.Expression)s));
            } else {
                // the efl function calls should be run before the return statement
                if(s instanceof com.samsung.sjs.backend.asts.c.ReturnStatement) {
                    if (options.eflEnabled() && node.getName().equals("main")) {
                        typed.addStatement(
                            new com.samsung.sjs.backend.asts.c.ExpressionStatement(
                                new com.samsung.sjs.backend.asts.c.FunctionCall("elm_run")));
                        typed.addStatement(
                            new com.samsung.sjs.backend.asts.c.ExpressionStatement(
                                new com.samsung.sjs.backend.asts.c.FunctionCall("elm_shutdown")));
                    }
                }
                typed.addStatement((com.samsung.sjs.backend.asts.c.Statement)s);
            }
        }

        if (node.isConstructor()) {
            typed.addStatement(new ReturnStatement(new ThisPseudoLiteral()));
        } else if (retty instanceof VoidType) {
            typed.addStatement(new ReturnStatement());
        }

        if (options.interopEnabled()) {
            // generate ill-typed body here
            BlockStatement untyped = new BlockStatement();
            IfStatement fbody = new IfStatement(new UnaryExpression(new Variable("__dirty"), "!", false), typed, untyped);
            for (com.samsung.sjs.backend.asts.ir.Statement n : body) {
                // Remember to use the other backend for these statements
                CNode s = n.accept(this.slowgen);
                // TODO: UNDO THIS TERRIBLE HACK
                if (s instanceof com.samsung.sjs.backend.asts.c.Expression) {
                    untyped.addStatement(new com.samsung.sjs.backend.asts.c.ExpressionStatement((com.samsung.sjs.backend.asts.c.Expression)s));
                } else {
                    untyped.addStatement((com.samsung.sjs.backend.asts.c.Statement)s);
                }
            }
            if (node.isConstructor()) {
                untyped.addStatement(new ReturnStatement(new ThisPseudoLiteral()));
            }
            fn.addBodyStatement(fbody);
        } else {
            fn.addBodyStatement(typed);
        }

        functions.pop();
        compiled_functions.pop();
        return fn;
    }

    @Override
    public CNode visitAllocNewObject(AllocNewObject node) {
        // TODO: This doesn't support .prototype access yet, either for reads or setting
        // inherited methods.  For now, we won't even support inheriting from Object.

        // Getting this to work before there are proper constructor types and AST nodes is too
        // brittle; it requires spreading around quite a few special cases, or multiple
        // re-traversals of the AST to re-type function references as constructor references.
        // For now, we'll keep the code, but won't use it.
        com.samsung.sjs.backend.asts.c.FunctionCall f = new com.samsung.sjs.backend.asts.c.FunctionCall("construct_object");
        assert (node.getVTable() != null);
        CType c_ctor_type = getTypeConverter().convert(node.getConstructor().getType());
        // TODO: The construction and use of this vtable are more subtle than for object literals,
        // because we have to deal with ordering wrt inherited fields as well.
        // TODO: Constructor code gen can always statically resolve field offsets of the receiver
        int indir_id = memo_vtable(node.getVTable());
        // TODO: This breakdown is wrong.  The constructor should have control of the vtable, not
        // the allocation site
        f.addActualArgument(new Variable("__vtable_id_"+indir_id));
        Variable vtmp = newTempVariable(node.getConstructor().getType());
        Assignment asgn = new Assignment(vtmp, "=", node.getConstructor().accept(this).asExpression());
        //f.addActualArgument(new com.samsung.sjs.backend.asts.c.NullLiteral()); // TODO: get proto from the closure
        f.addActualArgument(new InlineCCode(new CastExpression(c_ctor_type, vtmp.inType(node.getConstructor().getType())).toSource(0)+"->proto")); // get proto from the closure
        ObjectType ty = (ObjectType)node.getType();
        assert (ty != null);

        f.addActualArgument(new com.samsung.sjs.backend.asts.c.IntLiteral(ty.properties().size()));

        // f allocates the empty object, with no initialized fields --- not even boxes
        // TODO: Inheritance!  Actually, that should be done in codegen for the constructor, which
        // should assume no boxes exist.

        com.samsung.sjs.backend.asts.c.FunctionCall ctor_call =
            new ClosureCall(vtmp.inType(node.getConstructor().getType()),
                            c_ctor_type);
        // construct_object returns object_t*
        ctor_call.addActualArgument(new ValueCoercion(ty, f, options.encodeVals()));
        int argnum = 0;
        for (com.samsung.sjs.backend.asts.ir.Expression e : node.getArguments()) {
            Type formal_param = ((CodeType)node.getConstructor().getType()).paramTypes().get(argnum);
            boolean intfloat_coercion = false;
            if (e.getType() instanceof IntegerType && formal_param instanceof FloatType) {
                intfloat_coercion = true;
            }
            com.samsung.sjs.backend.asts.c.Expression carg = e.accept(this).asExpression();
            ctor_call.addActualArgument(intfloat_coercion ? coerceIntToFloat(carg) : carg);
            argnum++;
        }
        return new com.samsung.sjs.backend.asts.c.BinaryInfixExpression(asgn, ",", ctor_call);
    }

    @Override
    public CNode visitFunctionCall(com.samsung.sjs.backend.asts.ir.FunctionCall node) {
        /* XXX: HACK!
         * We set a flag when something is calling a primitive, as those are dispatched directly,
         * and require variation from the value_t calling convention to pass C's typechecker.
         * On 64-bit, this should work okay since passing something smaller still consumes 64 bits,
         * but it will break in weird and terrible ways on 32-bit.  The correct solution is to
         * change the (whole) runtime implementation (and IDL toolchain) for the new calling
         * convention.  But this is sufficient to shake out codegen bugs.
         */
        boolean hack = false;
        com.samsung.sjs.backend.asts.c.Expression ret = null;
        com.samsung.sjs.backend.asts.c.FunctionCall f;
        if (node.getTarget().toSource(0).equals("assert")) {
            hack = true;
            f = new com.samsung.sjs.backend.asts.c.FunctionCall("assert");
        } else if (node.getTarget().toSource(0).equals("_____type_violation")) {
            hack = true;
            f = new com.samsung.sjs.backend.asts.c.FunctionCall("_____type_violation");
        } else if (node.getTarget().toSource(0).equals("print")
                   || node.getTarget().toSource(0).equals("printString") ) {
            // Hack in an assert for testing purposes
            hack = true;
            f = new com.samsung.sjs.backend.asts.c.FunctionCall("print");
        } else if (node.getTarget().toSource(0).equals("parseFloat")) {
            hack = true;
            f = new com.samsung.sjs.backend.asts.c.FunctionCall("parseFloat");
        } else if (node.getTarget().toSource(0).equals("parseInt")) {
            hack = true;
            // TODO: Notice that this arity hack is even worse than most here
            f = new com.samsung.sjs.backend.asts.c.FunctionCall(
                    node.getArguments().size() == 1 ? "parseInt_noradix" : "parseInt");
        } else if (node.getTarget().toSource(0).equals("readline")) {
            hack = true;
            // Hack in an assert for testing purposes
            f = new com.samsung.sjs.backend.asts.c.FunctionCall("__readline");
        } else if (node.getTarget().toSource(0).equals("printInt")) {
            hack = true;
            // Hack in an assert for testing purposes
            f = new com.samsung.sjs.backend.asts.c.FunctionCall("printInt");
        } else if (node.getTarget().toSource(0).equals("printFloat")) {
            hack = true;
            // Hack in an assert for testing purposes
            f = new com.samsung.sjs.backend.asts.c.FunctionCall("printFloat");
        } else if (node.getTarget().toSource(0).equals("printFloat10")) {
            hack = true;
            // Hack in an assert for testing purposes
            f = new com.samsung.sjs.backend.asts.c.FunctionCall("printFloat10");
        } else if (node.getTarget().toSource(0).equals("string_of_int")) {
            hack = true;
            f = new com.samsung.sjs.backend.asts.c.FunctionCall("string_of_int");
        } else if (node.isDirectCall()) {
            // Direct recursive call, possibly throuh a number of stack frames
            // Note that the direct call target is the PRE-closure-conversion target,
            // and closure conversion already renamed the target
            f = new com.samsung.sjs.backend.asts.c.FunctionCall(((Var)node.getTarget()).getIdentifier());
            assert(!functions.isEmpty());
            com.samsung.sjs.backend.asts.ir.Expression env = null;
            if (node.getDirectCallTarget().getEnvironmentName().equals(functions.peek().getEnvironmentName())) {
                env = new Var((node.getDirectCallTarget().getEnvironmentName()));
            } else {
                Var envname = mkVar(functions.peek().getEnvironmentName());
                envname.setType(new EnvironmentType());
                env = new ArrayIndex(envname,
                                     new com.samsung.sjs.backend.asts.ir.IntLiteral(
                                         functions.peek().getEnvLayout().getOffset(
                                            node.getDirectCallTarget().getEnvironmentName())));
            }
            lval_capture = true;
            f.addActualArgument(env.accept(this).asExpression());
            lval_capture = false;
            f.addActualArgument(new com.samsung.sjs.backend.asts.c.NullLiteral().asValue(Types.mkObject(new LinkedList<>())));
        } else if (node.getTarget() instanceof IntrinsicName) {
            hack = true;
            f = new com.samsung.sjs.backend.asts.c.FunctionCall(((IntrinsicName)node.getTarget()).getIdentifier());
        } else {
            if (node.getTarget().getType() == null) {
                System.err.println("Function call target missing type: "+node.toSource(0));
            }
            assert (node.getTarget().getType() != null);
            Type clos_type = node.getTarget().getType();
            if (clos_type.isIntersectionType()) {
                clos_type = ((IntersectionType)clos_type).findFunctionType(node.getArguments().size());
            }
            if (node.getTarget().isPure()) {
                // var evaluation is pure; duplicating the dereference is harmless (more work for C
                // optimizer)
                f = new ClosureCall(node.getTarget().accept(this).asExpression().inType(node.getTarget().getType()),
                                    getTypeConverter().convert(clos_type));
                ret = null;
            } else {
                // fn expression may have side effects, need to memoize to avoid duplicating effects
                Variable vtmp = newTempVariable(clos_type);
                Assignment asgn = new Assignment(vtmp, "=", node.getTarget().accept(this).asExpression().asValue(clos_type));
                ClosureCall ccall = new ClosureCall(vtmp.inType(node.getTarget().getType()),
                                                    getTypeConverter().convert(clos_type));
                f = ccall;
                ret = new BinaryInfixExpression(asgn, ",", ccall);
            }
            f.addActualArgument(new com.samsung.sjs.backend.asts.c.NullLiteral().asValue(Types.mkObject(new LinkedList<>())));
        }

        Type clos_type = node.getTarget().getType();
        if (clos_type != null && clos_type.isIntersectionType()) {
            clos_type = ((IntersectionType)clos_type).findFunctionType(node.getArguments().size());
        }
        int argnum = 0;
        for (com.samsung.sjs.backend.asts.ir.Expression arg : node.getArguments()) {
            //f.addActualArgument(hack ? new ValueAs(arg.accept(this).asExpression(), arg.getType()) : arg.accept(this).asExpression());
            // TODO: Currently, many intrinsics introduced in earlier phases aren't decorated with
            // types...
            Type formal_param = clos_type == null ? null : ((CodeType)clos_type).paramTypes().get(argnum);
            boolean intfloat_coercion = false;
            if (arg.getType() instanceof IntegerType && formal_param != null && formal_param instanceof FloatType) {
                intfloat_coercion = true;
            }
            com.samsung.sjs.backend.asts.c.Expression prearg = arg.accept(this).asExpression();
            if (intfloat_coercion) {
                prearg = coerceIntToFloat(prearg);
            }
            Type castType = formal_param != null ? formal_param : arg.getType();
            com.samsung.sjs.backend.asts.c.Expression carg = hack ? prearg.inType(castType) : prearg.asValue(castType);
            f.addActualArgument(carg);
            argnum++;
        }
        if (ret == null) {
            ret = f;
        }

        if (hack) {
            // If the type is unknown (eg. not populated in
            // IntrinsicsInliningPass) or the return type is not void, then we
            // perform this extra cast b/c of primitives like e.g. wcslen,
            // which returns a size_t (unsigned) that cannot be cast to value_t.

            // TODO: This is awful; IntrinsicNames don't get types set in IntrinsicsInliningPass...
            // mkIntrinsicName should take 2 args: expr and type
            if (node.getTarget().getType() == null) {
                ret = new CastExpression(getTypeConverter().convert(node.getType()), ret).asValue(node.getType());
            }
            else {
                boolean returnIsDefinitelyVoid = false;
                if (node.getTarget().getType() instanceof IntersectionType) {
                    boolean hasNonVoid = false;
                    for (Type t : ((IntersectionType)node.getTarget().getType()).getTypes()) {
                        if (t instanceof CodeType && !(((CodeType)t).returnType() instanceof VoidType))
                            hasNonVoid = true;
                    }
                    returnIsDefinitelyVoid = !hasNonVoid;
                } else if ((((CodeType)node.getTarget().getType()).returnType() instanceof VoidType)) {
                    returnIsDefinitelyVoid = true;
                }
                if (!returnIsDefinitelyVoid) {
                    ret = new CastExpression(getTypeConverter().convert(node.getType()), ret).asValue(node.getType());
                }
            }
        }
        return ret;
    }

    // temp variables can always be unboxed
    private Variable newTempVariable(Type t) {
        assert(!functions.isEmpty());
        Scope s = functions.peek().getScope();
        Var v = freshVar(s, t);
        s.declareVariable(v, t);
        CType cty = getTypeConverter().convert(t);
        com.samsung.sjs.backend.asts.c.VariableDeclaration decl = new VariableDeclaration(false, new Value()/*cty*/);
        Variable cvar = new Variable(v.getIdentifier());
        decl.addVariable(cvar, new com.samsung.sjs.backend.asts.c.IntLiteral(0).asValue(Types.mkInt()));
        compiled_functions.peek().getBody().prefixStatement(decl);
        return cvar;
    }

    @Override
    public CNode visitMethodCall(MethodCall node) {
        com.samsung.sjs.backend.asts.c.Expression obj = node.getTarget().accept(this).asExpression().inType(node.getTarget().getType());
        String slot = node.getField();

        if (!(node.getTarget().getType() instanceof PropertyContainer)) {
            System.err.println("PANIC: Tried to cast non-PropertyContainer type as method dispatch target in compiling: "+node.toSource(0));
        }
        PropertyContainer receiver_type = (PropertyContainer)node.getTarget().getType();
        CodeType slot_type = (CodeType)receiver_type.getTypeForProperty(slot);
        CType c_slot_type = getTypeConverter().convert(slot_type, !options.useConstraints()); // mangle to closure

        obj = new CastExpression(new ObjectPseudoType(), obj);

        com.samsung.sjs.backend.asts.c.Expression o = null;
        Assignment asgn = null;
        if (node.getTarget().isPure()) {
            o = obj;
        } else {
            Variable vtmp = newTempVariable(receiver_type);
            System.err.println("Generating temp for method call receiver ["+obj.toSource(0)+"] of type "+receiver_type.toString());
            // Prepare to generate some really ugly C code
            // First, store the object into the temp variable (it may be an expression w/ side effects)
            // TODO: Same transformation for field read/write
            // TODO: The address of the box storing an object field's value never changes, which opens
            // the door for an optimization that could be a big win.  Similarly for the boxes for
            // globals.
            asgn = new Assignment(vtmp, "=", obj.asValue(node.getTarget().getType()));
            o = new CastExpression(new ObjectPseudoType(), vtmp.inType(receiver_type));
        }
        // Then, generate a cast expression casting the field read to the correct type
        MemberRead mr = new MemberRead(o, c_slot_type, slot, field_codes.indexOf(slot));
        mr.setDoNotCast();
        com.samsung.sjs.backend.asts.c.Expression method_lookup = //mr.inType(slot_type);
            new CastExpression(c_slot_type, new ValueAs(mr, slot_type));

        // And call the method lookup
        com.samsung.sjs.backend.asts.c.FunctionCall f =
            new com.samsung.sjs.backend.asts.c.FunctionCall("INVOKE_CLOSURE");
            //TODO: new ClosureCall(method_lookup, c_slot_type);
        f.addActualArgument(method_lookup);
        //f.addActualArgument(new CastExpression(new ObjectPseudoType(), o));
        f.addActualArgument(o.asValue(node.getTarget().getType()));

        int argnum = 0;
        for (com.samsung.sjs.backend.asts.ir.Expression arg : node.getArguments()) {
            boolean intfloat_coercion = false;
            if (arg.getType() instanceof IntegerType && slot_type.paramTypes().get(argnum) instanceof FloatType) {
                intfloat_coercion = true;
            }
            com.samsung.sjs.backend.asts.c.Expression carg = arg.accept(this).asExpression();
            f.addActualArgument(intfloat_coercion ? coerceIntToFloat(carg) : carg);
            argnum++;
        }
        com.samsung.sjs.backend.asts.c.Expression ret = f; //new ValueAs(f, ((CodeType)slot_type).returnType());

        if (o == obj) { // pure
            assert (asgn == null);
            return ret;
            //return node.getTarget().getType().isArray() && !(c_slot_type.getReturnType() instanceof CVoid)
            //    ? new ValueAs(f, slot_type.returnType()) : f;
        } else {
            // But only after executing the store to the temp variable!
            return new BinaryInfixExpression(asgn, ",", ret);
                    //node.getTarget().getType().isArray()  && !(c_slot_type.getReturnType() instanceof CVoid)
                    //? new ValueAs(f, slot_type.returnType()) : f);
        }
    }

    @Override
    public CNode visitReturn(Return node) {
        if (node.hasResult()) {
            if (functions.peek().getName().equals("main") || functions.peek().getName().equals("__sjs_main")) {
                return new ReturnStatement(node.getResult().accept(this).asExpression().inType(Types.mkInt()));
            }
            return new ReturnStatement(node.getResult().accept(this).asExpression().asValue(node.getResult().getType()));
        } else {
            return new ReturnStatement();
        }
    }

    @Override
    public CNode visitUnaryOp(UnaryOp node) {
        if (node.getOp().equals("void")) {
            return generateVoidOp(node.getExpression());
        }
        com.samsung.sjs.backend.asts.c.Expression exp = null;
        boolean mut = node.getOp().equals("++") || node.getOp().equals("--");
        // Field reads are usually no longer lvals, but if we're modifying we can emit a field
        // access that *is* an lval
        if (mut && node.getExpression() instanceof PredictedFieldRead) {
            PredictedFieldRead fr = (PredictedFieldRead)node.getExpression();
            // We've predicted the slot where this lives, and it's mutable so we know it's local,
            // not boxed
            com.samsung.sjs.backend.asts.c.FunctionCall f =
                new com.samsung.sjs.backend.asts.c.FunctionCall("INLINE_BOX_ACCESS");
            com.samsung.sjs.backend.asts.c.Expression oexpr = fr.getObject().accept(this).asExpression().inType(fr.getObject().getType());
            f.addActualArgument(new CastExpression(new ObjectPseudoType(), oexpr));
            f.addActualArgument(new InlineCCode(fr.getOffset()+" /* "+fr.getField()+","+field_codes.indexOf(fr.getField())+" */"));
            String op = node.getOp();
            String intrinsic = "UNARY_"+(node.isPostfix() ? "POST" : "PRE")+(op.equals("++") ? "_INC" : "_DEC");
            if (node.getExpression().getType() instanceof FloatType) {
                intrinsic = "FLOAT_"+intrinsic;
            }
            com.samsung.sjs.backend.asts.c.FunctionCall intr =
                new com.samsung.sjs.backend.asts.c.FunctionCall(intrinsic);
            intr.addActualArgument(f); // relies on FIELD_READ_WRITABLE being an lval
            // these macros don't return value_ts
            return new ValueCoercion(fr.getType(), intr, false); // TODO: float shifting in interop
        } else if (mut && node.getExpression() instanceof FieldRead) {
            FieldRead fr = (FieldRead)node.getExpression();
            com.samsung.sjs.backend.asts.c.FunctionCall f =
                new com.samsung.sjs.backend.asts.c.FunctionCall("FIELD_READ_WRITABLE");
            com.samsung.sjs.backend.asts.c.Expression oexpr = fr.getObject().accept(this).asExpression().inType(fr.getObject().getType());
            f.addActualArgument(new CastExpression(new ObjectPseudoType(), oexpr));
            f.addActualArgument(new InlineCCode(field_codes.indexOf(fr.getField())+" /* "+fr.getField()+" */"));
            String op = node.getOp();
            String intrinsic = "UNARY_"+(node.isPostfix() ? "POST" : "PRE")+(op.equals("++") ? "_INC" : "_DEC");
            if (node.getExpression().getType() instanceof FloatType) {
                intrinsic = "FLOAT_"+intrinsic;
            }
            com.samsung.sjs.backend.asts.c.FunctionCall intr =
                new com.samsung.sjs.backend.asts.c.FunctionCall(intrinsic);
            intr.addActualArgument(f); // relies on FIELD_READ_WRITABLE being an lval
            // these macros don't return value_ts
            return new ValueCoercion(fr.getType(), intr, false); // TODO: float shifting in interop
        } else if (mut && (node.getExpression().isVar() || (node.getExpression() instanceof ArrayIndex && ((ArrayIndex)node.getExpression()).getArray().getType() instanceof EnvironmentType))) {
            String op = node.getOp();
            String intrinsic = "UNARY_"+(node.isPostfix() ? "POST" : "PRE")+(op.equals("++") ? "_INC" : "_DEC");
            if (node.getExpression().getType() instanceof FloatType) {
                intrinsic = "FLOAT_"+intrinsic;
            }
            com.samsung.sjs.backend.asts.c.FunctionCall f =
                new com.samsung.sjs.backend.asts.c.FunctionCall(intrinsic);
            f.addActualArgument(node.getExpression().accept(this).asExpression());
            // these macros don't return value_ts
            return new ValueCoercion(node.getExpression().getType(), f, false); // TODO: float shifting in interop
        } else if (mut && node.getExpression() instanceof ArrayIndex) {
            // desugar array inc/dec since we don't generate lvals anymore
            // TODO: specialize these operations, since the get and put duplicate some work
            ArrayIndex ai = (ArrayIndex)node.getExpression();
            // We're still abusing VarAssignment for array writes in the IR
            com.samsung.sjs.backend.asts.ir.IntLiteral one =  mkIntLiteral(1);
            one.setType(Types.mkInt());
            BinaryOp b = mkBinaryOp(ai, String.valueOf(node.getOp().charAt(0)), one);
            b.setType(ai.getType());
            VarAssignment va = mkVarAssignment(ai, "=", b);
            va.setType(ai.getType());
            return va.accept(this).asExpression().asValue(ai.getType());
        } else {
            exp = node.getExpression().accept(this).asExpression();
        }
        if (node.getOp().equals("~") && node.getExpression().getType() instanceof FloatType) {
            // need to do the double->int conversion
            com.samsung.sjs.backend.asts.c.FunctionCall f = new com.samsung.sjs.backend.asts.c.FunctionCall("___int_of_float");
            f.addActualArgument(exp.inType(node.getExpression().getType()));
            // underlying primitive returns a real int
            return new UnaryExpression(new ValueCoercion(Types.mkInt(), f, false).inType(Types.mkInt()),
                                       node.getOp(),
                                       node.isPostfix()).asValue(node.getType());
        }
        return new UnaryExpression(exp.inType(node.getExpression().getType()),
                                   node.getOp(),
                                   node.isPostfix()).asValue(node.getType());
    }

    @Override
    public CNode visitFieldRead(FieldRead node) {
        // TODO: make temporaries when the object expr may not be idempotent!
        // see array access and method invocation for reference
        String field = node.getField();
        if (node.getObject().getType().isConstructor()) {
            com.samsung.sjs.backend.asts.c.Expression ctor = new CastExpression(getTypeConverter().convert(node.getObject().getType()), node.getObject().accept(this).asExpression().inType(node.getObject().getType()));
            return new ValueCoercion(node.getType(), new InlineCCode(ctor.toSource(0)+"->proto"), false);
        }
        if (node.getObject().getType().isArray() && field.equals("length")) {
            com.samsung.sjs.backend.asts.c.Expression exp = node.getObject().accept(this).asExpression();
            return new ValueCoercion(node.getType(), new CastExpression(new CInteger(), new InlineCCode("__array_len("+exp.toSource(0)+")")), false);
        }
        // get object as void*
        com.samsung.sjs.backend.asts.c.Expression obj = node.getObject().accept(this).asExpression().inType(node.getObject().getType());
        MemberRead mr = new MemberRead(new CastExpression(new ObjectPseudoType(), obj),
                              getTypeConverter().convert(node.getType()),
                              field,
                              field_codes.indexOf(field));
        mr.setDoNotCast();
        // If the field is writable, we can skip emitting anything considering the prototype, so the
        // C compiler can do a better job optimizing
        if (((ObjectType)node.getObject().getType()).getProperty(field).isRW()) {
            mr.setWritable();
        }
        return mr;
    }

    @Override
    public CNode visitPredictedFieldRead(PredictedFieldRead node) {
        // TODO: make temporaries when the object expr may not be idempotent!
        // see array access and method invocation for reference
        String field = node.getField();
        com.samsung.sjs.backend.asts.c.Expression obj = node.getObject().accept(this).asExpression().inType(node.getObject().getType());
        PredictedMemberRead mr = new PredictedMemberRead(new CastExpression(new ObjectPseudoType(), obj),
                              getTypeConverter().convert(node.getType()),
                              field,
                              field_codes.indexOf(field),
                              node.getOffset());
        if (node.isDirect()) {
            mr.setDirect();
        }
        mr.setDoNotCast();
        return mr;
    }

    @Override
    public CNode visitForInLoop(ForInLoop node) {
        com.samsung.sjs.backend.asts.c.CompoundStatement result =
            new com.samsung.sjs.backend.asts.c.CompoundStatement();

        com.samsung.sjs.backend.asts.c.Expression map = node.getIteratee().accept(this).asExpression();

        CType celemtype = getTypeConverter().convert(node.getIteratedType());

        Variable v = new Variable(node.getVariable().getIdentifier());

        com.samsung.sjs.backend.asts.c.VariableDeclaration idecl =
            new com.samsung.sjs.backend.asts.c.VariableDeclaration(false, new CMapIterator(celemtype));
        Scope s = functions.peek().getScope();
        Var iter = freshVar(s, Types.mkMapIteratorType(node.getIteratedType()));
        Variable iterv = new Variable(iter.getIdentifier());
        idecl.addVariable(iterv,
                          new com.samsung.sjs.backend.asts.c.FunctionCall(new Variable("__get_map_iterator"), map.inType(node.getIteratee().getType())));
        result.addStatement(idecl);

        com.samsung.sjs.backend.asts.c.WhileLoop loop =
            new com.samsung.sjs.backend.asts.c.WhileLoop(
                    new com.samsung.sjs.backend.asts.c.FunctionCall(new Variable("__map_iterator_has_next"), iterv));
        loop.addStatement(new com.samsung.sjs.backend.asts.c.ExpressionStatement(
                            new Assignment(v, "=",
                            new CastExpression(new CString(), new com.samsung.sjs.backend.asts.c.FunctionCall(new Variable("__map_iterator_get_next"), iterv)).asValue(Types.mkString()))));
        loop.addStatement(node.getBody().accept(this).asStatement());
        result.addStatement(loop);

        return result;
    }

    /**
     * A class for generating the slow path.  This handles statements and expressions, but relies
     * heavily on the state of the enclosing IRCBackend instance to coordinate.
     *
     * SlowPathGenerator is subordinate to the CBackend.  For each function body, the CBackend is
     * run *first*, generating the type-correct code with escape checks, and as a side effect
     * accumulating a bunch of meta-information that is consumed by the SlowPathGenerator.  This
     * information includes:
     *   - Label names for jumping from (after) a failing operation in the typed path to the
     *     corresponding point on the slow path.  It is up to the SlowPathGenerator to actually emit
     *     the appropriate labels.
     *   - ...
     *
     * The CBackend tracks which temp variables variables, are live at each possible failure
     * location, and is responsible itself for generating function-scoped temporaries to share with
     * the slow path, and for shunting state from the fast-path-local temporary variables to the
     * corresponding function-scoped temps.
     */
    public class SlowPathGenerator extends CBackend {
        @Override
        public com.samsung.sjs.backend.asts.c.Expression visitVar(Var node) {
            if (node.getIdentifier().startsWith("__tmp")) {
                // this needs to be translated
                return new Variable("__slow_"+node.getIdentifier());
            } else {
                return IRCBackend.this.visitVar(node);
            }
        }
        @Override
        public com.samsung.sjs.backend.asts.c.Statement visitVarDecl(VarDecl node) {
            // function-scoped locals were already generated by the CBackend.  We only need to add
            // new function-scoped temp variables for exchange with the fast-path
            // TODO: should only need a fixed # of these per function body, reused across multiple
            // transition edges
            com.samsung.sjs.backend.asts.c.Expression var = node.getVar().accept(this).asExpression();
            if (node.getVar().getIdentifier().startsWith("__tmp")) {
                CType decltype = IRCBackend.this.getTypeConverter().convert(node.getType());
                com.samsung.sjs.backend.asts.c.VariableDeclaration d =
                    new com.samsung.sjs.backend.asts.c.VariableDeclaration(false, /*decltype*/new Value());
                d.addVariable(var, getUndef());
                IRCBackend.this.compiled_functions.peek().getBody().prefixStatement(d);

            }
            Assignment wr = new Assignment(var, "=", node.getInitialValue().accept(this).asExpression().asValue(node.getInitialValue().getType()));
            return new com.samsung.sjs.backend.asts.c.ExpressionStatement(wr);
        }
        @Override
        public FunctionDeclaration visitFunction(Function node) {
            throw new IllegalArgumentException("SlowPathGenerator should never directly emit a function; this should be orchestrated by the general C backend.");
        }
        @Override
        public com.samsung.sjs.backend.asts.c.Expression visitArrayIndex(ArrayIndex node) {
            throw new UnsupportedOperationException();
        }
        @Override
        public CNode visitPredictedFieldRead(PredictedFieldRead node) {
            // TODO: Can we do any real specialization here?
            return visitFieldRead(node);
        }
        @Override
        public CNode visitFieldRead(FieldRead node) {
            throw new UnsupportedOperationException();
        }
        @Override
        public com.samsung.sjs.backend.asts.c.Expression visitPredictedFieldAssignment(PredictedFieldAssignment node) {
            // TODO: Can we do any real specialization here?
            return visitFieldAssignment(node);
        }
        @Override
        public com.samsung.sjs.backend.asts.c.Expression visitFieldAssignment(FieldAssignment node) {
            throw new UnsupportedOperationException();
        }
        @Override
        public CNode visitReturn(Return node) {
            return IRCBackend.this.visitReturn(node);
        }
        @Override
        public CNode visitForInLoop(ForInLoop node) {
            throw new UnsupportedOperationException();
        }
        @Override
        public com.samsung.sjs.backend.asts.c.Expression visitVarAssignment(VarAssignment node) {
            // TODO: Note that writes to arrays also show up here...
            if (node.getAssignedVar() instanceof ArrayIndex) {
                throw new UnsupportedOperationException();
            }
            return new Assignment(node.getAssignedVar().accept(this).asExpression(),
                                  node.getOperator(),
                                  node.getAssignedValue().accept(this).asExpression().asValue(node.getAssignedValue().getType()));
        }
        @Override
        public CNode visitUnaryOp(UnaryOp node) {
            throw new UnsupportedOperationException();
        }
        @Override
        public com.samsung.sjs.backend.asts.c.Expression visitBinaryOp(BinaryOp node) {
            com.samsung.sjs.backend.asts.c.FunctionCall f = null;
            switch (node.getOp()) {
                case "|":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("BITOR");
                    break;
                case "^":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("BITXOR");
                    break;
                case "&":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("BITAND");
                    break;
                case "==":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("EQ");
                    break;
                case "!=":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("NE");
                    break;
                case "===":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("STRICTEQ");
                    break;
                case "!==":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("STRICTNE");
                    break;
                case "<":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("LT");
                    break;
                case ">":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("GT");
                    break;
                case "<=":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("LE");
                    break;
                case ">=":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("GE");
                    break;
                case "+":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("ADD");
                    break;
                case "-":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("SUB");
                    break;
                case "*":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("MUL");
                    break;
                case "/":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("DIV");
                    break;
                case "%":
                    f = new com.samsung.sjs.backend.asts.c.FunctionCall("MOD");
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            f.addActualArgument(node.getLeft().accept(this).asExpression());
            f.addActualArgument(node.getRight().accept(this).asExpression());
            return f;
        }
        @Override
        public CNode visitAllocNewObject(AllocNewObject node) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CNode visitMethodCall(MethodCall node) {
            throw new UnsupportedOperationException();
        }
        @Override
        public CNode visitFunctionCall(com.samsung.sjs.backend.asts.ir.FunctionCall node) {
            if (node.getArguments().size() > 0 || !(((CodeType)node.getTarget().getType()).returnType() instanceof VoidType)) {
                throw new UnsupportedOperationException();
            }
            // TODO temporary hack; won't treat arguments/returns correctly
            // since this will trip IRCBackend's visitVar, we need to rewrite the dispatch target
            com.samsung.sjs.backend.asts.ir.Expression e = node.getTarget();
            com.samsung.sjs.backend.asts.ir.Expression target2 = node.getTarget();
            com.samsung.sjs.backend.asts.ir.FunctionCall node2 = null;
            if (e.isVar()) {
                Var target = node.getTarget().asVar();
                target2 = mkVar("__slow_"+target.getIdentifier());
                target2.setType(e.getType());
                node2 = new com.samsung.sjs.backend.asts.ir.FunctionCall(target2);
                node2.setType(node.getType());
            } else {
                // should be call to intrinsic
                node2 = node;
            }
            return IRCBackend.this.visitFunctionCall(node2);
        }

    }
}
