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
 * Base class for IR to C compilation.  Contains some reusable code for leaf nodes and structural
 * nodes that don't have any actual variation between general (typed) and slow-path codegen.
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend;

import com.samsung.sjs.backend.asts.ir.*;
import com.samsung.sjs.backend.asts.c.*;
import com.samsung.sjs.backend.asts.c.types.*;
import com.samsung.sjs.types.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public abstract class CBackend extends IRVisitor<CNode> {

    protected static int str_const_cnt = 0;
    protected static com.samsung.sjs.backend.asts.c.CompoundStatement string_literal_decls = null;
    protected IRFieldCollector.FieldMapping field_codes;

    // This is static because both the IRCBackend and SlowPathGenerator need to use the same
    // memoization of indirection maps.
    protected static Map<Integer,Set<Pair<int[],Integer>>> vtables_by_hash = new HashMap<Integer,Set<Pair<int[],Integer>>>();
    protected static int next_vtable_id = 0;
    /**
     * Returns a new ID for a new vtable, or the id of an existing
     * vtable if argument vt is identical to a previous vtable array.
     */
    protected int memo_vtable(int[] vt) {
        int result = -1;
        int hash = Arrays.hashCode(vt);
        int oldsize = vtables_by_hash.size();
        int oldsetsize = -1;
        boolean collision = false;
        if (vtables_by_hash.containsKey(hash)) {
            Set<Pair<int[],Integer>> possible_matches = vtables_by_hash.get(hash);
            assert (possible_matches != null);
            for (Pair<int[],Integer> test : possible_matches) {
                if (Arrays.equals(test.getKey(), vt)) {
                    collision = true;
                    result = test.getValue();
                }
            }
            if (!collision) {
                // We hit an existing has bucket, but don't match
                result = next_vtable_id++;
                Pair<int[],Integer> newpair = Pair.of(vt, result);
                oldsetsize = possible_matches.size();
                possible_matches.add(newpair);
                assert(possible_matches.size() > oldsetsize);
            }
        } else {
            // We don't match any existing bucket
            result = next_vtable_id++;
            Pair<int[],Integer> newpair = Pair.of(vt, result);
            Set<Pair<int[],Integer>> newset = new HashSet<Pair<int[],Integer>>();
            newset.add(newpair);
            vtables_by_hash.put(hash, newset);
            assert (vtables_by_hash.size() >= oldsize);
        }
        assert (result >= 0); // we initialize next_vtable_id to 0, so -1 is invalid
        return result;
    }

    protected com.samsung.sjs.backend.asts.c.Expression alignStringConstant(String s) {
        // TODO: Should really memoize these literals so we don't emit giant tables of redundant
        // string constants...
        Variable v = new Variable("strlit"+str_const_cnt++);
        AlignedStringConstant asc = new AlignedStringConstant(v, new CStringLiteral(s));
        // insert declaration
        string_literal_decls.addStatement(asc);
        // most variables are bound as values, but not hoisted string constant names
        return new ValueCoercion(Types.mkString(), v, false);
    }

    protected SJSTypeConverter conv;
    protected boolean lval_capture;
    protected TypeTagSerializer tts;
    public SJSTypeConverter getTypeConverter() { return conv; }

    public CBackend(TypeTagSerializer tts) {
        conv = new SJSTypeConverter();
        lval_capture = false;
        this.tts = tts;
    }

    @Override
    public CNode visitBreak(Break node) {
        return new BreakStatement();
    }
    @Override
    public CNode visitSwitch(Switch node) {
        SwitchStatement sw = new SwitchStatement(node.getDiscriminee().accept(this).asExpression().inType(node.getDiscriminee().getType()));
        for (Case c : node.getCases()) {
            sw.addCaseStatement((CaseStatement)c.accept(this).asStatement());
        }
        return sw;
    }
    @Override
    public CNode visitCase(Case node) {
        CaseStatement cs = new CaseStatement(node.getValue() != null ? node.getValue().accept(this).asExpression().inType(node.getValue().getType()) : null);
        for (com.samsung.sjs.backend.asts.ir.Statement st : node.getStatements()) {
            cs.addStatement(st.accept(this).asStatement());
        }
        return cs;
    }

    @Override
    public com.samsung.sjs.backend.asts.c.Expression visitIntLiteral(com.samsung.sjs.backend.asts.ir.IntLiteral node) {
        // Sometimes we abuse int literals as other types... don't assume type Integer
        return new com.samsung.sjs.backend.asts.c.IntLiteral((long)node.getValue()).asValue(node.getType() != null ? node.getType() : Types.mkInt());
    }

    @Override
    public com.samsung.sjs.backend.asts.c.Expression visitFloatLiteral(com.samsung.sjs.backend.asts.ir.FloatLiteral node) {
        return new com.samsung.sjs.backend.asts.c.DoubleLiteral(node.getValue()).asValue(Types.mkFloat());
    }

    @Override
    public com.samsung.sjs.backend.asts.c.Expression visitBoolLiteral(com.samsung.sjs.backend.asts.ir.BoolLiteral node) {
        return new com.samsung.sjs.backend.asts.c.BoolLiteral(node.getValue()).asValue(Types.mkBool());
    }

    @Override
    public com.samsung.sjs.backend.asts.c.Expression visitNullLiteral(com.samsung.sjs.backend.asts.ir.NullLiteral node) {
        return new com.samsung.sjs.backend.asts.c.NullLiteral().asValue(node.getType());
    }

    @Override
    public com.samsung.sjs.backend.asts.c.Expression visitThisLiteral(ThisLiteral node) {
        return new com.samsung.sjs.backend.asts.c.ThisPseudoLiteral().asValue(node.getType());
    }

    @Override public com.samsung.sjs.backend.asts.c.Expression visitIntrinsicName(IntrinsicName node) {
        // TODO: mangle intrinsic names, link against other versions to avoid collisions w/ program
        // local names
        //return new Variable("____"+node.getIdentifier());
        Variable v = new Variable(node.getIdentifier());
        v.makeIntrinsic();
        return v;
    }

    // TODO: support! (or delete unnecessary nodes)
    @Override public CNode visitScript(Script node) { throw new UnsupportedOperationException(); }
    @Override public CNode visitContinue(Continue node) { 
        return new ContinueStatement();
    }
    @Override public CNode visitFunDecl(FunDecl node) { throw new UnsupportedOperationException(); }
    @Override public CNode visitNamedLambda(NamedLambda node) { throw new UnsupportedOperationException(); }
    @Override public CNode visitLambda(Lambda node) { throw new UnsupportedOperationException(); }
    @Override public CNode visitLetIn(LetIn node) { throw new UnsupportedOperationException(); }

    @Override
    public CNode visitIfThenElse(IfThenElse node) {
        com.samsung.sjs.backend.asts.c.Expression test = node.getTestExpr().accept(this).asExpression().inType(node.getTestExpr().getType());
        if (!(node.getTestExpr().getType() instanceof BooleanType)) {
            com.samsung.sjs.backend.asts.c.FunctionCall testf = new com.samsung.sjs.backend.asts.c.FunctionCall("!val_is_falsy");
            testf.addActualArgument(test.asValue(node.getTestExpr().getType()));
            test = testf;
        }
        com.samsung.sjs.backend.asts.c.Statement thenpart = node.getTrueBranch().accept(this).asStatement();
        com.samsung.sjs.backend.asts.c.Statement elsepart = null;
        if (node.getFalseBranch() != null) {
            elsepart = node.getFalseBranch().accept(this).asStatement();
        }
        return new IfStatement(test, thenpart, elsepart);
    }

    @Override
    public CNode visitBlock(Block node) {
        BlockStatement b = new BlockStatement();
        for (com.samsung.sjs.backend.asts.ir.Statement s : node) {
            b.addStatement(s.accept(this).asStatement());
        }
        return b;
    }

    @Override
    public CNode visitCompoundStatement(com.samsung.sjs.backend.asts.ir.CompoundStatement node) {
        com.samsung.sjs.backend.asts.c.CompoundStatement cs =
            new com.samsung.sjs.backend.asts.c.CompoundStatement();
        for (com.samsung.sjs.backend.asts.ir.Statement s : node) {
            cs.addStatement(s.accept(this).asStatement());
        }
        return cs;
    }

    @Override
    public CNode visitAllocArrayLiteral(AllocArrayLiteral node) {
        com.samsung.sjs.backend.asts.c.FunctionCall f =
            new com.samsung.sjs.backend.asts.c.FunctionCall("array___lit");
        String tag = tts != null ? node.getType().generateTag(tts) : "NULL";
        f.addActualArgument(new InlineCCode(tag));
        f.addActualArgument(new com.samsung.sjs.backend.asts.c.IntLiteral(node.nelems()));
        Type elemType = ((ArrayType)node.getType()).elemType();
        for (com.samsung.sjs.backend.asts.ir.Expression e : node) {
            assert (e != null);
            assert (e.getType() != null);
            if (e.getType() == null) {
                System.err.println("WARNING: Pushing array element type down into array literal entry ["+e.toSource(0)+"] of ["+node.toSource(0)+"]");
                e.setType(((ArrayType)node.getType()).elemType());
            }
            if (elemType instanceof FloatType && e.getType() instanceof IntegerType) {
                f.addActualArgument(
                        new com.samsung.sjs.backend.asts.c.FunctionCall("double_as_val_noenc",
                        new com.samsung.sjs.backend.asts.c.FunctionCall("(double)",
                            e.accept(this).asExpression().inType(e.getType()))));
            } else {
                f.addActualArgument(e.accept(this).asExpression().asValue(e.getType()));
            }
            //// Must cast to value_t when calling variadics on 32-bit platforms
            //if (e.getType().isPrimitive() && !e.getType().isFunction()) {
            //    f.addActualArgument(new CastExpression(new Value(), e.accept(this).asExpression()));
            //} else {
            //    f.addActualArgument(new CastExpression(new Value(),
            //                        new CastExpression(new CVoid(1), e.accept(this).asExpression())));
            //}
        }
        //return f.asValue(node.getType());
        // array lit alloc primitive returns object_t* for C++ compat...
        return new ValueCoercion(node.getType(), f, false);
    }

    public static String lower_op(String op) {
        if (op.equals("===")) {
            return "==";
        } else if (op.equals("!==")) {
            return "!=";
        } else {
            return op;
        }
    }

    public InlineCCode getUndef() { 
        return new InlineCCode("((value_t){ .box = 0xFFFF000700000000 })");
    }

    @Override
    public CNode visitUndef(Undefined node) {
        return getUndef();
    }

    @Override
    public CNode visitStr(Str node) {
        return alignStringConstant(node.getValue()).asValue(Types.mkString());
    }

    @Override
    public CNode visitForLoop(com.samsung.sjs.backend.asts.ir.ForLoop node) {
        CNode init = null;
        com.samsung.sjs.backend.asts.c.Expression cond = null, incr = null;
        com.samsung.sjs.backend.asts.c.Statement body;
        // TODO: Rework the loop structures to ensure these fields are always non-null
        // (possibly empty expressions)
        if (node.getInitializer() != null)
            init = node.getInitializer().accept(this);
        if (node.getCondition() != null) {
            cond = node.getCondition().accept(this).asExpression();
            // If cond is bool, specialize
            if (node.getCondition().getType().rep() == RepresentationSort.BOOL) {
                cond = cond.inType(Types.mkBool());
            } else {
                // falsyness
                com.samsung.sjs.backend.asts.c.FunctionCall testf = new com.samsung.sjs.backend.asts.c.FunctionCall("!val_is_falsy");
                testf.addActualArgument(cond.asValue(node.getCondition().getType()));
                cond = testf;
            }
        }
        if (node.getIncrement() != null)
            incr = node.getIncrement().accept(this).asExpression();
        body = node.getBody().accept(this).asStatement();

        com.samsung.sjs.backend.asts.c.ForLoop loop =
            new com.samsung.sjs.backend.asts.c.ForLoop(init, cond, incr);
        if (body instanceof BlockStatement) {
            BlockStatement b = (BlockStatement)body;
            for (com.samsung.sjs.backend.asts.c.Statement s : b.getStatements()) {
                loop.addStatement(s);
            }
        } else {
            loop.addStatement(body);
        }

        return loop;
    }

    @Override
    public CNode visitWhileLoop(com.samsung.sjs.backend.asts.ir.WhileLoop node) {
        com.samsung.sjs.backend.asts.c.WhileLoop result =
            new com.samsung.sjs.backend.asts.c.WhileLoop(node.getCondition().accept(this).asExpression().inType(node.getCondition().getType()));
        result.addStatement(node.getBody().accept(this).asStatement());
        return result;
    }

    @Override public CNode visitDoLoop(com.samsung.sjs.backend.asts.ir.DoLoop node) {
        com.samsung.sjs.backend.asts.c.DoLoop result =
            new com.samsung.sjs.backend.asts.c.DoLoop(node.getCondition().accept(this).asExpression().inType(node.getCondition().getType()));
        result.addStatement(node.getBody().accept(this).asStatement());
        return result;
    }

    @Override
    public CNode visitCondExpr(CondExpr node) {
        ConditionalExpression c = new ConditionalExpression();
        c.setTest(node.getTestExpr().accept(this).asExpression().inType(node.getTestExpr().getType()));
        if (!(node.getTestExpr().getType() instanceof BooleanType)) {
            com.samsung.sjs.backend.asts.c.FunctionCall test = new com.samsung.sjs.backend.asts.c.FunctionCall("!val_is_falsy");
            test.addActualArgument(c.getTest().asValue(node.getTestExpr().getType()));
            c.setTest(test);
        }
        c.setTrueBranch(node.getYesExpr().accept(this).asExpression().asValue(node.getYesExpr().getType()));
        c.setFalseBranch(node.getNoExpr().accept(this).asExpression().asValue(node.getNoExpr().getType()));
        return c.asValue(node.getType());
    }

    public CNode generateVoidOp(com.samsung.sjs.backend.asts.ir.Expression e) {
        // void operator evaluates arg, returns undefined.  So we'll do this with the comma
        // operator.
        return new BinaryInfixExpression(e.accept(this).asExpression(), ",", getUndef()).asValue(e.getType());
    }

    @Override
    public CNode visitAllocClosure(AllocClosure node) {
        com.samsung.sjs.backend.asts.c.FunctionCall f =
            new com.samsung.sjs.backend.asts.c.FunctionCall(node.getType().isConstructor() ? "ALLOC_CTOR_CLOSURE" : "ALLOC_CLOSURE");
        String fname = node.getCode().getName();
        lval_capture = true; // No need to follow a stack discipline, since env allocations don't nest
        //visit(fptr);
        //f.addActualArgument((com.samsung.sjs.backend.asts.c.Expression)lastChildResult); // function pointer
        String tag = tts != null ? node.getType().generateTag(tts) : "NULL";
        f.addActualArgument(new InlineCCode("(void*)"+fname)); // function pointer
        f.addActualArgument(new InlineCCode(tag));
        if (node.getType().isConstructor()) {
            // TODO: if we can prove the constructor isn't used before the prototype is set, don't
            // bother allocating.
            ConstructorType ctor = ((ConstructorType)node.getType());
            ObjectType ptype = (ObjectType)ctor.getPrototype();
            if (node.getVTable() != null) {
                com.samsung.sjs.backend.asts.c.FunctionCall proto = new com.samsung.sjs.backend.asts.c.FunctionCall("blank_obj_vtable");
                proto.addActualArgument(new com.samsung.sjs.backend.asts.c.IntLiteral(ptype.ownProperties().size()));
                int indir_id = memo_vtable(node.getVTable());
                proto.addActualArgument(new Variable("__vtable_id_"+indir_id));
                f.addActualArgument(proto);
            } else {
                f.addActualArgument(new com.samsung.sjs.backend.asts.c.NullLiteral());
            }
        }
        com.samsung.sjs.backend.asts.c.IntLiteral count = new com.samsung.sjs.backend.asts.c.IntLiteral(node.environmentSize());
        f.addActualArgument(count);
        for (com.samsung.sjs.backend.asts.ir.Expression e : node.getCapturedVars()) {
            // These will always be pointer-sized --- value_t* --- so no special care is required
            // for platform-sensitivity of variadic functions in C
            f.addActualArgument(e.accept(this).asExpression()); // environment capture
        }
        lval_capture = false;
        return new CastExpression(new CVoid(1),f); // XXX: hack: we generate anonymous closure types, whose pointers can't be cast directly to value_t
    }

    @Override
    public CNode visitAllocMapLiteral(AllocMapLiteral node) {
        com.samsung.sjs.backend.asts.c.FunctionCall f =
            new com.samsung.sjs.backend.asts.c.FunctionCall("__alloc_map_literal");
        String tag = tts != null ? node.getType().generateTag(tts) : "NULL";
        f.addActualArgument(new InlineCCode(tag));
        f.addActualArgument(new com.samsung.sjs.backend.asts.c.IntLiteral(node.nentries()));
        for (AllocMapLiteral.KVPair p : node) {
            // Must cast BOTH to value_t for this to be 32/64 portable
            f.addActualArgument(alignStringConstant(p.name).asValue(Types.mkString()));
            f.addActualArgument(p.val.accept(this).asExpression().asValue(p.val.getType()));
        }
        // alloc_map_literal returns map_t, which needs to be changed to object_t
        return new CastExpression(new ObjectPseudoType(), f).asValue(node.getType());
    }

    @Override
    public CNode visitAllocObjectLiteral(AllocObjectLiteral node) {
        /* Allocating an object requires not only the memory allocation of the object
         * itself, but also lvalue slots, and the indirection map.
         */
        com.samsung.sjs.backend.asts.c.FunctionCall f = new com.samsung.sjs.backend.asts.c.FunctionCall("alloc_object_lit");
        assert (node.getVTable() != null);
        int indir_id = memo_vtable(node.getVTable());
        f.addActualArgument(new Variable("__vtable_id_"+indir_id));
        String tag = tts != null ? node.getType().generateTag(tts) : "NULL";
        f.addActualArgument(new InlineCCode(tag));
        f.addActualArgument(new com.samsung.sjs.backend.asts.c.IntLiteral(node.nslots()));
        // reorder slot values according to vtable
        com.samsung.sjs.backend.asts.c.Expression[] fields = new com.samsung.sjs.backend.asts.c.Expression[node.nslots()];
        for (AllocObjectLiteral.TypedSlot slot : node) {
            // Must cast to value_t when calling variadics on 32-bit platforms
            // If it's a pointer type generated by this compilation run (i.e., a closure), we need
            // to first cast to void*
            com.samsung.sjs.backend.asts.c.Expression val = slot.val.accept(this).asExpression().asValue(slot.val.getType());
            // Do the virtual lookup to find the right physical offset
            fields[node.getVTable()[field_codes.indexOf(slot.name)]] = val;
        }
        for (int i = 0; i < fields.length; i++) {
            f.addActualArgument(fields[i]);
        }
        return new ValueCoercion(node.getType(), f, false);
    }

    @Override
    public CNode visitRequire(Require node) {
        com.samsung.sjs.backend.asts.c.FunctionCall f = 
            new com.samsung.sjs.backend.asts.c.FunctionCall("__untyped_import_"+node.getPath());
        // TODO: Fix this to change the dispatch target based on the basename...
        return f;
    }


}
