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
 * Convert IR to three-address form.
 *
 * This pass converts IR to a "three-address" subset of the IR language.  For example:
 *
 *     var y = x + z + 2; ==>
 *     var tmp3 = x + z;
 *     var y = tmp3 + 2;
 *
 * It's really three-address-like, since a method call for example just turns into 
 *     tmp9 = tmp8.method(tmp4, ...);
 *
 *
 * The structure of the pass is to decompose each function body into two regions: a set of temp
 * variable declarations, and a series of three-address-like statements.
 *
 * Each expression visit:
 *      1. recursively visits component expressions
 *      2. generates a new temp variable for the result (say metavariable X)
 *      3. appends a statement X = [op](args...) to the end of the function thus far
 *      4. returns the variable node for X
 * Constants / literals simply return themselves, since it's silly to do otherwise.
 *
 * Statements simply work as normal, but assert for consistency purposes that each recursive call
 * produces a variable or constant.
 *
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend;

import com.samsung.sjs.backend.asts.ir.*;
import com.samsung.sjs.types.*;

import java.util.*;

public class ThreeAddressConversion extends IRTransformer {

    private Block currentBlock;

    private static int temp = 2349;

    private Var mkFreshTmp(Type t) {
        // TODO: This is subtly inappropriate for the interop mode, because this emits the
        // declaration at the top of the block we emit the statement in.  What we really want,
        // eventually, is to emit the declaration at the top of the enclosing JS scope block,
        // and emit the statement in a possibly-different (but at least nested) block.
        Var v = new Var("__tmp"+temp++);
        v.setType(t);
        currentBlock.prefixStatement(mkVarDecl(v, t, mkIntLiteral(0)));
        curScope.declareVariable(v, t);
        return v;
    }

    // TODO: replace explicit calls to mkFreshTmp with uses of the new serialize
    private Expression serialize(Type t, Expression e) {
        // Constants and intrinsics needn't be serialized.  Environments don't convert, and are
        // immutable in a given scope.
        // The intersection check is a bit of a hack; it means we're accessing Array, which is
        // essentially constant...
        if (e.isConst() || e instanceof IntrinsicName || t instanceof EnvironmentType || t instanceof IntersectionType) {
            return e;
        }
        Var v = new Var("__tmp"+temp++);
        v.setType(t);
        curScope.declareVariable(v,t);
        // inline decl
        currentBlock.addStatement(mkVarDecl(v, t, e));
        return v;
    }
    private Expression serialize(Expression e) {
        return serialize(e.getType(), e);
    }

    public ThreeAddressConversion(Script s) {
        super(s);
        currentBlock = null;
    }

    @Override public IRNode visitFunctionCall(FunctionCall node) { 
        // Functions may dispatch to untyped code, so we need to memoize the context
        Expression func = node.getTarget().accept(this).asExpression();
        // If we hoist temporaries for direct calls, we introduce a lot of perf overhead from
        // packing/unpacking
        if (!node.isDirectCall()) {
            func = serialize(func);
        }
        FunctionCall f = mkFunctionCall(func);
        for (Expression e : node.getArguments()) {
            f.addArgument(serialize(e.accept(this).asExpression()));
        }
        f.setType(node.getType());
        // This points from the new tree into the old, but is used only for naming and diagnostics
        f.setDirectCall(node.getDirectCallTarget());
        return f;
    }
    @Override public IRNode visitMethodCall(MethodCall node) { 
        // Methods may dispatch to untyped code, so we need to memoize the context
        MethodCall m = mkMethodCall(serialize(node.getTarget().accept(this).asExpression()), node.getField());
        for (Expression e : node.getArguments()) {
            m.addArgument(serialize(e.accept(this).asExpression()));
        }
        m.setType(node.getType());
        return m;
    }
    // Inherit this: alloc closure captures lvals, so there's no failing subexpr
    //@Override public IRNode visitAllocClosure(AllocClosure node) {
    //    if (!node.mustSaveIntermediates()) {
    //        return super.visitAllocClosure(node);
    //    }
    //    AllocClosure a = mkAllocClosure(node.getCode());
    //    for (Expression e : node.getCapturedVars()) {
    //        // We don't want to memoize these, because we want to capture these variables, not
    //        // copies!!!
    //        if (!e.isVar()) {System.err.println(e.getClass().toString());}
    //        assert (e.isVar() || e instanceof ArrayIndex); // This is messy, handling capture of variable from outer scopes via current env
    //        a.addCapturedVariable(e);
    //    }
    //    a.setType(node.getType());
    //    return serialize(a);
    //}
    @Override public IRNode visitAllocObjectLiteral(AllocObjectLiteral node) {
        AllocObjectLiteral aol = mkAllocObjectLiteral();
        for (AllocObjectLiteral.TypedSlot s : node) {
            aol.addSlot(s.name, serialize(s.val.accept(this).asExpression()), s.ty);
        }
        aol.setVTable(node.getVTable());
        aol.setType(node.getType());
        return aol;
    }
    @Override public IRNode visitAllocMapLiteral(AllocMapLiteral node) {
        if (!node.mustSaveIntermediates()) {
            return super.visitAllocMapLiteral(node);
        }
        AllocMapLiteral aml = mkAllocMapLiteral(node.getRangeType());
        for (AllocMapLiteral.KVPair p : node) {
            aml.addEntry(p.name, serialize(p.val.accept(this).asExpression()));
        }
        aml.setType(node.getType());
        return aml;
    }
    @Override public IRNode visitBinaryOp(BinaryOp node) {
        if (!node.mustSaveIntermediates()) {
            return super.visitBinaryOp(node);
        }
        // lazy operators need special treatment
        if (node.getOp().equals("||")) {
            // TODO: does this violate any memory management assumptions about
            // not sharing types between objects?
            Var res = mkFreshTmp(node.getType());
            Expression left = serialize(node.getLeft().accept(this).asExpression());
            VarAssignment lhs = mkVarAssignment(res, "=", left);
            lhs.setType(res.getType());

            Block rhs = new Block();
            Block oldblock = currentBlock;
            currentBlock = rhs;
            Expression right = serialize(node.getRight().accept(this).asExpression());
            VarAssignment rasgn = mkVarAssignment(res, "=", right);
            rasgn.setType(res.getType());
            rhs.addStatement(mkExpressionStatement(rasgn));
            currentBlock = oldblock;

            currentBlock.addStatement(mkIfThenElse(left, mkExpressionStatement(lhs), rhs));
            return res;

        } else if (node.getOp().equals("&&")) {
            // TODO: does this violate any memory management assumptions about
            // not sharing types between objects?
            Var res = mkFreshTmp(node.getType());
            Expression left = serialize(node.getLeft().accept(this).asExpression());
            VarAssignment lhs = mkVarAssignment(res, "=", left);
            lhs.setType(res.getType());

            left = mkUnaryOp(left, "!", false); // negate condition
            left.setType(node.getLeft().getType());

            Block rhs = new Block();
            Block oldblock = currentBlock;
            currentBlock = rhs;
            Expression right = serialize(node.getRight().accept(this).asExpression());
            VarAssignment rasgn = mkVarAssignment(res, "=", right);
            rasgn.setType(res.getType());
            rhs.addStatement(mkExpressionStatement(rasgn));
            currentBlock = oldblock;

            currentBlock.addStatement(mkIfThenElse(left, mkExpressionStatement(lhs), rhs));
            return res;

        }
        // the comma operator also needs special treatment
        else if (node.getOp().equals(",")) {
            currentBlock.addStatement(mkExpressionStatement(node.getLeft().accept(this).asExpression()));
            return node.getRight().accept(this);
        }
        BinaryOp b = mkBinaryOp(node.getLeft().accept(this).asExpression(),
                          node.getOp(),
                          node.getRight().accept(this).asExpression());
        b.setType(node.getType());
        return (b);
    }
    @Override public IRNode visitUnaryOp(UnaryOp node) {
        // if the expression is a field value or array element, the naive translation fails for
        // mutating unary ops (++, --) because we increment the read result rather than the aggregate member
        // TODO: split unary op applied to field, predicted field, array element, and variable as
        // separate IR nodes?
        if (!node.mustSaveIntermediates()) {
            return super.visitUnaryOp(node);
        }
        Expression subject = null;
        boolean mut = (node.getOp().equals("++") || node.getOp().equals("--"));
        if (mut && node.getExpression() instanceof PredictedFieldRead) {
            PredictedFieldRead fr = (PredictedFieldRead)node.getExpression();
            Expression target = fr.getObject().accept(this).asExpression();
            String field = fr.getField();
            int offset = fr.getOffset();
            PredictedFieldRead fr2 = mkPredictedFieldRead(target, field, offset);
            fr2.setType(fr.getType());
            subject = fr2;
        } else if (mut && node.getExpression() instanceof FieldRead) {
            FieldRead fr = (FieldRead)node.getExpression();
            Expression target = fr.getObject().accept(this).asExpression();
            String field = fr.getField();
            FieldRead fr2 = mkFieldRead(target, field);
            fr2.setType(fr.getType());
            subject = fr2;
        } else if (!node.getExpression().isVar() && mut) {
            throw new UnsupportedOperationException("mutating unary ops on array elts not yet supported");
        } else {
            subject = node.getExpression().accept(this).asExpression();
        }
        UnaryOp u = mkUnaryOp(subject, node.getOp(), node.isPostfix());
        u.setType(node.getType());
        return serialize(u);
    }
    @Override public IRNode visitCondExpr(CondExpr node) {
        // We have to decompose this to preserve lazy evaluation....
        if (!node.mustSaveIntermediates()) {
            return super.visitCondExpr(node);
        }
        Expression test = node.getTestExpr().accept(this).asExpression();
        // at least one nontrivial branch, so laziness matters
        Var result = mkFreshTmp(node.getType());
        Block tbranch = new Block();
        Block fbranch = new Block();
        IfThenElse ite = mkIfThenElse(test, tbranch, fbranch);
        Block oldblock = currentBlock;
        // emit true branch
        currentBlock = tbranch;
        Expression tresult = serialize(node.getYesExpr().accept(this).asExpression());
        VarAssignment vat = mkVarAssignment(result, "=", tresult);
        vat.setType(node.getType());
        tbranch.addStatement(mkExpressionStatement(vat));
        // emit false branch
        currentBlock = fbranch;
        Expression fresult = serialize(node.getNoExpr().accept(this).asExpression());
        VarAssignment vaf = mkVarAssignment(result, "=", fresult);
        vaf.setType(node.getType());
        fbranch.addStatement(mkExpressionStatement(vaf));

        currentBlock = oldblock;
        currentBlock.addStatement(ite);
        return result;
    }
    @Override public IRNode visitVarAssignment(VarAssignment node) {
        // Note that this includes assignment to arrays and environments
        if (!node.mustSaveIntermediates()) {
            return super.visitVarAssignment(node);
        }
        if (node.getAssignedVar().isVar()) {
            VarAssignment va = mkVarAssignment(node.getAssignedVar().accept(this).asExpression(),
                                               node.getOperator(),
                                               serialize(node.getAssignedValue().accept(this).asExpression()));
            va.setType(node.getType());
            // This might seem silly, but we have actual benchmark code that uses the result of an
            // assignment expression...
            return va;
        } else {
            // This should really be a distinct node type
            ArrayIndex ai = (ArrayIndex)node.getAssignedVar();
            Expression array = (ai.getArray().accept(this).asExpression());
            Expression index = (ai.getOffset().accept(this).asExpression());
            if (!ai.getArray().isConst() || !ai.getOffset().isConst()) {
                array = serialize(array);
                index = serialize(index);
            }
            Expression val = serialize(node.getAssignedValue().accept(this).asExpression());
            ArrayIndex ai2 = mkArrayIndex(array, index);
            ai2.setType(ai.getType());
            VarAssignment va = mkVarAssignment(ai2, node.getOperator(), val);
            va.setType(node.getType());
            return va;
        }
    }
    @Override public IRNode visitPredictedFieldAssignment(PredictedFieldAssignment node) {
        if (!node.mustSaveIntermediates()) {
            return super.visitPredictedFieldAssignment(node);
        }
        FieldAssignment fa = mkPredictedFieldAssignment(serialize(node.getObject().accept(this).asExpression()),
                                               node.getField(),
                                               node.getBoxPointerOffset(),
                                               node.getOperator(),
                                               serialize(node.getValue().accept(this).asExpression()));
        fa.setType(node.getType());
        return (fa);
    }
    @Override public IRNode visitFieldAssignment(FieldAssignment node) {
        if (!node.mustSaveIntermediates()) {
            return super.visitFieldAssignment(node);
        }
        FieldAssignment fa = mkFieldAssignment(serialize(node.getObject().accept(this).asExpression()),
                                               node.getField(),
                                               node.getOperator(),
                                               serialize(node.getValue().accept(this).asExpression()));
        fa.setType(node.getType());
        return (fa);
    }
    @Override public Block visitBlock(Block node) {
        Block b = mkBlock();
        Block oldBlock = currentBlock;
        currentBlock = b;
        for (Statement s : node) {
            b.addStatement(s.accept(this).asStatement());
        }
        currentBlock = oldBlock;
        return b;
    }

    @Override public IRNode visitCase(Case node) {
        // TODO: This will break for effectful computations in the case discriminee... need to
        // implement desugaring to if-then-else
        Case c = mkCase(node.getValue() != null ? node.getValue().accept(this).asExpression() : null);
        Block oldblock = currentBlock;
        Block b = mkBlock();
        currentBlock = b;
        for (Statement s : node.getStatements()) {
            b.addStatement(s.accept(this).asStatement());
        }
        c.addStatement(b);
        currentBlock = oldblock;
        return c;
    }

    // this is basically a block.... this routine flattens it
    @Override public IRNode visitCompoundStatement(CompoundStatement node) {
        for (Statement s : node) {
            currentBlock.addStatement(s.accept(this).asStatement());
        }
        return mkExpressionStatement(null);
    }
    @Override public IRNode visitPredictedFieldRead(PredictedFieldRead node) {
        if (!node.mustSaveIntermediates()) {
            return super.visitPredictedFieldRead(node);
        }
        PredictedFieldRead fr = mkPredictedFieldRead(serialize(node.getObject().accept(this).asExpression()),
                                   node.getField(),
                                   node.getOffset());
        if (node.isDirect()) {
            fr.setDirect();
        }
        fr.setType(node.getType());
        return (fr);
    }
    @Override public IRNode visitFieldRead(FieldRead node) {
        if (!node.mustSaveIntermediates()) {
            return super.visitFieldRead(node);
        }
        FieldRead fr = mkFieldRead(serialize(node.getObject().accept(this).asExpression()),
                                   node.getField());
        fr.setType(node.getType());
        return serialize(fr);
    }
    @Override public IRNode visitArrayIndex(ArrayIndex node) {
        // generally serialize both parts; the macros this turns into duplicate the expressions
        // But if the expressions are pure, the peephole optimization from leaving this inline is
        // worth more than the burden on the C compiler to do a good job of value numbering
        if (node.getArray().isConst() && node.getOffset().isConst()) {
            return super.visitArrayIndex(node);
        }
        ArrayIndex ai = mkArrayIndex(serialize(node.getArray().accept(this).asExpression()),
                                     serialize(node.getOffset().accept(this).asExpression()));
        ai.setType(node.getType());
        return (ai);
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
        oldScope = node.getScope();
        curScope = f.getScope();
        Block oldBlock = currentBlock;
        currentBlock = f.getBody();
        for (Statement s : node.getBody()) {
            f.addBodyStatement((Statement)s.accept(this));
        }
        curScope = curScope.getParentScope();
        oldScope = oldScope.getParentScope();
        currentBlock = oldBlock;
        return f;
    }
    @Override public IRNode visitExpressionStatement(ExpressionStatement node) {
        // Don't serialize; the result of the expression is unused
        Expression e = node.getExpression();
        if (e != null) {
            return mkExpressionStatement(node.getExpression().accept(this).asExpression());
        } else {
            return mkExpressionStatement(null);
        }
    }
    @Override public Script visitScript(Script node) {
        // This is the entry point for most visitors
        Script s = new Script(visitBlock(node.getBody()), node.getScope());
        return s;
    }
    @Override
    public IRNode visitAllocArrayLiteral(AllocArrayLiteral node) {
        if (!node.mustSaveIntermediates()) {
            return super.visitAllocArrayLiteral(node);
        }
        AllocArrayLiteral lit = mkAllocArrayLiteral(node.getCellType());
        lit.setType(node.getType());
        for (Expression e : node) {
            lit.addElement(serialize(e.accept(this).asExpression()));
        }
        return (lit);
    }

    // Can't just inherit loops, because it will screw up side effects and reexecution of bound
    // checks and counter increments
    @Override 
    public IRNode visitForLoop(ForLoop node) {
        boolean basic_transform = true;
        // initializer is a statement
        if (node.getCondition() != null && node.getCondition().mustSaveIntermediates()) {
            basic_transform = false;
        } else if (node.getIncrement() != null && node.getIncrement().mustSaveIntermediates()) {
            basic_transform = false;
        }
        if (basic_transform) {
            return super.visitForLoop(node);
        }
        // TODO: this is a pretty heavyweight transformation; is there any way to retain more of the
        // natural loop structure?
        IRNode init = null;
        Expression cond = null, incr = null;
        Statement body;
        if (node.getInitializer() != null) {
            if (node.getInitializer() instanceof Statement)
                node.getInitializer().accept(this).asStatement(); // serializes statement earlier
            else
                init = serialize(node.getInitializer().accept(this).asExpression());
        }
        if (node.getCondition() != null)
            cond = serialize(node.getCondition().accept(this).asExpression());
        // cond now holds null OR a variable for the initial bounds check
        body = node.getBody().accept(this).asStatement();
        Block oldblock = currentBlock;
        currentBlock = (Block)body;
        if (node.getIncrement() != null)
            incr = serialize(node.getIncrement().accept(this).asExpression());
        if (cond != null) {
            Expression cond2 = node.getCondition().accept(this).asExpression();
            // we've now re-executed the condition at the end of the loop iteration, need to feed
            // back to start of loop
            VarAssignment asgn = mkVarAssignment(cond, "=", cond2);
            asgn.setType(cond.getType());
            currentBlock.addStatement(mkExpressionStatement(asgn));
        }
        
        currentBlock = oldblock;
        ForLoop loop = mkForLoop(init, cond, null);
        loop.setBody(body);
        return loop;
    }

    @Override public IRNode visitWhileLoop(WhileLoop node) { 
        // Can't just inherit this, because if the condition is side-effecting we need to duplicate
        // its effects at the end of the loop.
        // In fact, we can't do the naive generation at all in general --- if the condition reads a
        // variable that is modified in the loop but memoized when emitting the conditional, the
        // conditional expression's code will not observe the updates...

        // initial condition check
        Expression cond = serialize(node.getCondition().accept(this).asExpression());
        Block body = (Block)node.getBody().accept(this).asStatement();
        // This is for evaluating the condition after each iteration
        // This only applies if the loop condition isn't constant... (e.g., not while(true) {...} )
        if (cond.isVar()) {
            Block oldblock = currentBlock;
            currentBlock = body;
            Var cond2 = serialize(node.getCondition().accept(this).asExpression()).asVar();
            VarAssignment asgn = mkVarAssignment(cond.asVar(), "=", cond2);
            asgn.setType(cond.getType());
            body.addStatement(mkExpressionStatement(asgn));
            currentBlock = oldblock;
        }
        return mkWhileLoop(cond, body);
    }
    @Override public IRNode visitDoLoop(DoLoop node) {
        // do-while loops already evaluate conditions at the end of the loop body,
        // but we're in the odd situation of needing to declare the SSA variable for the condition
        // check outside the loop, so it can be read after what C considers to be the end of the
        // loop....  So we'll do some of the serialize(..) call ourselves
        if (!node.getCondition().mustSaveIntermediates()) {
            // This case is actually important for correctness --- C's analysis recognizes 
            // do { ... } while (true); in its termination (return on all paths) analysis 
            return mkDoLoop(node.getCondition().accept(this).asExpression(),
                            node.getBody().accept(this).asStatement());
        }
        Var condvar = mkFreshTmp(Types.mkBool());
        Block body = (Block)node.getBody().accept(this).asStatement();
        Block oldblock = currentBlock;
        currentBlock = body;
        VarAssignment va = mkVarAssignment(condvar, "=", node.getCondition().accept(this).asExpression());
        va.setType(Types.mkBool());
        body.addStatement(mkExpressionStatement(va));
        currentBlock = oldblock;
        return mkDoLoop(condvar, body);
    }

    @Override public IRNode visitAllocNewObject(AllocNewObject node) {
        // Constructors may go to untyped code..
        AllocNewObject alloc = mkAllocNewObject(serialize(node.getConstructor().accept(this).asExpression()));
        for (Expression arg : node.getArguments()) {
            alloc.addArgument(serialize(arg.accept(this).asExpression()));
        }
        alloc.setType(node.getType());
        alloc.setVTable(node.getVTable());
        return serialize(alloc);
    }
    // TODO: Should new/fcall/mcall always serialize all components?  It's the *call* that could
    // fail, not necessarily the subexpressions.  I think we can do better codegen if we
    // check argument/target mustSaveIntermediates(); the calls themselves will always say the
    // context must save intermediates that may be used after the call

}
