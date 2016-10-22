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
 * Base class for transforming IR trees into new IR trees, with default plumbing
 * that allows leaf nodes to be shared between trees.
 * We use covariant returns in a few places where it doesn't seem to
 * overconstrain compiler passes, but reduces casting.
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

import java.util.*;

public class IRTransformer extends IRVisitor<IRNode> {

    protected Scope curScope; // The scope being *built*
    protected Scope oldScope; // The scope being *consumed*
    protected Block currentBlock;

    public IRTransformer(Script s) {
        curScope = s.getScope();
        oldScope = s.getScope();
        currentBlock = null;
    }

    @Override public IRNode visitFunctionCall(FunctionCall node) { 
        FunctionCall f = mkFunctionCall(node.getTarget().accept(this).asExpression());
        for (Expression e : node.getArguments()) {
            f.addArgument(e.accept(this).asExpression());
        }
        f.setType(node.getType());
        // This points from the new tree into the old, but is used only for naming and diagnostics
        f.setDirectCall(node.getDirectCallTarget());
        return f; 
    }
    @Override public IRNode visitMethodCall(MethodCall node) { 
        MethodCall m = mkMethodCall(node.getTarget().accept(this).asExpression(), node.getField());
        for (Expression e : node.getArguments()) {
            m.addArgument(e.accept(this).asExpression());
        }
        m.setType(node.getType());
        return m; 
    }
    @Override public IRNode visitAllocClosure(AllocClosure node) {
        // We don't transform the code here, because it's only a reference, and we only use it for
        // layouts
        AllocClosure a = mkAllocClosure(node.getCode());
        for (Expression e : node.getCapturedVars()) {
            a.addCapturedVariable(e.accept(this).asExpression());
        }
        a.setType(node.getType());
        a.setVTable(node.getVTable());
        return a;
    }
    @Override public IRNode visitLetIn(LetIn node) {
        // TODO: Implement, handling scopes correctly
        //LetIn l = mkLetIn(visitVar(node.getVar()),
        //                  node.getBoundExpression().accept(this).asExpression(),
        //                  node.getOpenExpression().accept(this).asExpression());
        //l.setType(node.getType());
        //return l;
        throw new UnsupportedOperationException();
    }
    @Override public IRNode visitAllocObjectLiteral(AllocObjectLiteral node) {
        AllocObjectLiteral aol = mkAllocObjectLiteral();
        for (AllocObjectLiteral.TypedSlot s : node) {
            aol.addSlot(s.name, s.val.accept(this).asExpression(), s.ty);
        }
        aol.setVTable(node.getVTable());
        aol.setType(node.getType());
        return aol;
    }
    @Override public IRNode visitAllocMapLiteral(AllocMapLiteral node) {
        AllocMapLiteral aml = mkAllocMapLiteral(node.getRangeType());
        for (AllocMapLiteral.KVPair p : node) {
            aml.addEntry(p.name, p.val.accept(this).asExpression());
        }
        aml.setType(node.getType());
        return aml;
    }
    @Override public IRNode visitBinaryOp(BinaryOp node) {
        BinaryOp b = mkBinaryOp(node.getLeft().accept(this).asExpression(),
                          node.getOp(),
                          node.getRight().accept(this).asExpression());
        b.setType(node.getType());
        return b;
    }
    @Override public IRNode visitUnaryOp(UnaryOp node) {
        UnaryOp u = mkUnaryOp(node.getExpression().accept(this).asExpression(), node.getOp(), node.isPostfix());
        u.setType(node.getType());
        return u;
    }
    @Override public IRNode visitCondExpr(CondExpr node) {
        CondExpr c = mkCondExpr(node.getTestExpr().accept(this).asExpression(),
                                node.getYesExpr().accept(this).asExpression(),
                                node.getNoExpr().accept(this).asExpression());
        c.setType(node.getType());
        return c;
    }
    @Override public IRNode visitVarAssignment(VarAssignment node) {
        VarAssignment va = mkVarAssignment(node.getAssignedVar().accept(this).asExpression(),
                                           node.getOperator(),
                                           node.getAssignedValue().accept(this).asExpression());
        va.setType(node.getType());
        return va;
    }
    @Override public IRNode visitPredictedFieldAssignment(PredictedFieldAssignment node) {
        FieldAssignment fa = mkPredictedFieldAssignment(node.getObject().accept(this).asExpression(),
                                               node.getField(),
                                               node.getBoxPointerOffset(),
                                               node.getOperator(),
                                               node.getValue().accept(this).asExpression());
        fa.setType(node.getType());
        return fa;
    }
    @Override public IRNode visitFieldAssignment(FieldAssignment node) {
        FieldAssignment fa = mkFieldAssignment(node.getObject().accept(this).asExpression(),
                                               node.getField(),
                                               node.getOperator(),
                                               node.getValue().accept(this).asExpression());
        fa.setType(node.getType());
        return fa;
    }
    @Override public Block visitBlock(Block node) {
        Block oldBlock = currentBlock;
        Block b = mkBlock();
        currentBlock = b;
        for (Statement s : node) {
            b.addStatement(s.accept(this).asStatement());
        }
        currentBlock = oldBlock;
        return b;
    }
    @Override public IRNode visitVarDecl(VarDecl node) {
        curScope.declareVariable(node.getVar(), node.getType());
        VarDecl vd = mkVarDecl((Var)visitVar(node.getVar()),
                               node.getType(),
                               node.getInitialValue().accept(this).asExpression());
        return vd;
    }
    @Override public IRNode visitIfThenElse(IfThenElse node) {
        IfThenElse ite = mkIfThenElse(node.getTestExpr().accept(this).asExpression(),
                                      node.getTrueBranch().accept(this).asStatement(),
                                      (node.getFalseBranch() != null ? node.getFalseBranch().accept(this).asStatement() : null));
        return ite;
    }
    @Override public IRNode visitReturn(Return node) {
        if (node.hasResult()) {
            return mkReturn(node.getResult().accept(this).asExpression());
        } else {
            return mkReturn();
        }
    }
    @Override public IRNode visitCompoundStatement(CompoundStatement node) {
        CompoundStatement cs = mkCompoundStatement();
        for (Statement s : node) {
            cs.addStatement(s.accept(this).asStatement());
        }
        return cs;
    }
    @Override public IRNode visitPredictedFieldRead(PredictedFieldRead node) {
        PredictedFieldRead fr = mkPredictedFieldRead(node.getObject().accept(this).asExpression(),
                                   node.getField(),
                                   node.getOffset());
        if (node.isDirect()) {
            fr.setDirect();
        }
        fr.setType(node.getType());
        return fr;
    }
    @Override public IRNode visitFieldRead(FieldRead node) {
        FieldRead fr = mkFieldRead(node.getObject().accept(this).asExpression(),
                                   node.getField());
        fr.setType(node.getType());
        return fr;
    }
    @Override public IRNode visitArrayIndex(ArrayIndex node) {
        ArrayIndex ai = mkArrayIndex(node.getArray().accept(this).asExpression(),
                                     node.getOffset().accept(this).asExpression());
        ai.setType(node.getType());
        return ai;
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
        currentBlock = oldBlock;
        curScope = curScope.getParentScope();
        oldScope = oldScope.getParentScope();
        return f;
    }
    @Override public IRNode visitExpressionStatement(ExpressionStatement node) {
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
        AllocArrayLiteral lit = mkAllocArrayLiteral(node.getCellType());
        lit.setType(node.getType());
        for (Expression e : node) {
            lit.addElement(e.accept(this).asExpression());
        }
        return lit;
    }

    @Override 
    public IRNode visitForLoop(ForLoop node) {
        // TODO(cns): what is considered the "current block" for the condition
        // and increment?
        IRNode init = null;
        Expression cond = null, incr = null;
        Statement body;
        if (node.getInitializer() != null)
            init = node.getInitializer().accept(this);
        if (node.getCondition() != null)
            cond = node.getCondition().accept(this).asExpression();
        if (node.getIncrement() != null)
            incr = node.getIncrement().accept(this).asExpression();
        body = node.getBody().accept(this).asStatement();

        ForLoop loop = mkForLoop(init, cond, incr);
        loop.setBody(body);

        return loop;
    }
    @Override
    public IRNode visitForInLoop(ForInLoop node) {
        Expression expr = node.getIteratee().accept(this).asExpression();
        ForInLoop loop = mkForInLoop(node.getVariable(), expr, node.getIteratedType());
        Statement body = node.getBody().accept(this).asStatement();
        loop.setBody(body);
        return loop;
    }

    @Override public IRNode visitWhileLoop(WhileLoop node) { 
        return mkWhileLoop(node.getCondition().accept(this).asExpression(),
                           node.getBody().accept(this).asStatement());
    }
    @Override public IRNode visitDoLoop(DoLoop node) {
        return mkDoLoop(node.getCondition().accept(this).asExpression(),
                        node.getBody().accept(this).asStatement());
    }

    @Override public IRNode visitAllocNewObject(AllocNewObject node) {
        AllocNewObject alloc = mkAllocNewObject(node.getConstructor().accept(this).asExpression());
        for (Expression arg : node.getArguments()) {
            alloc.addArgument(arg.accept(this).asExpression());
        }
        alloc.setType(node.getType());
        alloc.setVTable(node.getVTable());
        return alloc;
    }

    @Override public IRNode visitSwitch(Switch node) {
        Switch s = mkSwitch(node.getDiscriminee().accept(this).asExpression());
        for (Case c : node.getCases()) {
            s.addCase((Case)c.accept(this).asStatement());
        }
        return s;
    }
    @Override public IRNode visitCase(Case node) {
        Case c = mkCase(node.getValue() != null ? node.getValue().accept(this).asExpression() : null);
        Block oldBlock = currentBlock;
        Block b = mkBlock();
        currentBlock = b;
        for (Statement s : node.getStatements()) {
            b.addStatement(s.accept(this).asStatement());
        }
        c.addStatement(b);
        currentBlock = oldBlock;

        return c;
    }

    // Leaf nodes
    @Override public IRNode visitIntLiteral(IntLiteral node) { return node; }
    @Override public IRNode visitFloatLiteral(FloatLiteral node) { return node; }
    @Override public IRNode visitBoolLiteral(BoolLiteral node) { return node; }
    @Override public IRNode visitThisLiteral(ThisLiteral node) { return node; }
    @Override public IRNode visitNullLiteral(NullLiteral node) { return node; }
    @Override public IRNode visitStr(Str node) { return node; }
    @Override public IRNode visitUndef(Undefined node) { return node; }
    @Override public IRNode visitBreak(Break node) { return node; }
    @Override public IRNode visitContinue(Continue node) { return node; }
    @Override public IRNode visitVar(Var node) {
        return node;
    }
    @Override public IRNode visitIntrinsicName(IntrinsicName node) { return node; }

    // TODO: Not implemented yet
    @Override public IRNode visitNamedLambda(NamedLambda node) { throw new UnsupportedOperationException(); }
    @Override public IRNode visitLambda(Lambda node) {
        throw new UnsupportedOperationException();
    }
    @Override public IRNode visitFunDecl(FunDecl node) {
        throw new UnsupportedOperationException();
    }
    @Override public IRNode visitUntyped(UntypedAccess node) {
        UntypedAccess ua = new UntypedAccess(node.untypedVariable().accept(this).asExpression());
        ua.setType(node.getType());
        return ua;
    }
    @Override public IRNode visitRequire(Require node) {
        return node;
    }
}
