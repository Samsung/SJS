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
 * Base class for visitors which only decorate ASTs or collect stats, without
 * directly using recursive results.  This class does the plumbing for recursive
 * visitation, so subclasses can only override method where they do something different
 * instead of reinventing their own plumbing.
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

public class VoidIRVisitor extends IRVisitor<Void> {

    @Override public Void visitFunctionCall(FunctionCall node) { 
        node.getTarget().accept(this);
        for (Expression e : node.getArguments()) {
            e.accept(this);
        }
        return null; 
    }
    @Override public Void visitMethodCall(MethodCall node) { 
        node.getTarget().accept(this);
        for (Expression e : node.getArguments()) {
            e.accept(this);
        }
        return null; 
    }
    @Override public Void visitAllocClosure(AllocClosure node) {
        visitFunction(node.getCode());
        for (Expression e : node.getCapturedVars()) {
            e.accept(this);
        }
        return null;
    }
    @Override public Void visitLetIn(LetIn node) {
        visitVar(node.getVar());
        node.getBoundExpression().accept(this);
        node.getOpenExpression().accept(this);
        return null;
    }
    @Override public Void visitAllocObjectLiteral(AllocObjectLiteral node) {
        for (AllocObjectLiteral.TypedSlot s : node) {
            s.val.accept(this);
        }
        return null;
    }
    @Override public Void visitBinaryOp(BinaryOp node) {
        node.getLeft().accept(this);
        node.getRight().accept(this);
        return null;
    }
    @Override public Void visitUnaryOp(UnaryOp node) {
        node.getExpression().accept(this);
        return null;
    }
    @Override public Void visitCondExpr(CondExpr node) {
        node.getTestExpr().accept(this);
        node.getYesExpr().accept(this);
        node.getNoExpr().accept(this);
        return null;
    }
    @Override public Void visitVarAssignment(VarAssignment node) {
        node.getAssignedVar().accept(this);
        node.getAssignedValue().accept(this);
        return null;
    }
    @Override public Void visitPredictedFieldAssignment(PredictedFieldAssignment node) {
        node.getObject().accept(this);
        node.getValue().accept(this);
        return null;
    }
    @Override public Void visitFieldAssignment(FieldAssignment node) {
        node.getObject().accept(this);
        node.getValue().accept(this);
        return null;
    }
    @Override public Void visitBlock(Block node) {
        for (Statement s : node) {
            s.accept(this);
        }
        return null;
    }
    @Override public Void visitVarDecl(VarDecl node) {
        visitVar(node.getVar());
        node.getInitialValue().accept(this);
        return null;
    }
    @Override public Void visitIfThenElse(IfThenElse node) {
        node.getTestExpr().accept(this);
        node.getTrueBranch().accept(this);
        if (node.getFalseBranch() != null)
            node.getFalseBranch().accept(this);
        return null;
    }
    @Override public Void visitReturn(Return node) {
        if (node.hasResult()) {
            node.getResult().accept(this);
        }
        return null;
    }
    @Override public Void visitCompoundStatement(CompoundStatement node) {
        for (Statement s : node) {
            s.accept(this);
        }
        return null;
    }
    @Override public Void visitPredictedFieldRead(PredictedFieldRead node) {
        node.getObject().accept(this);
        return null;
    }
    @Override public Void visitFieldRead(FieldRead node) {
        node.getObject().accept(this);
        return null;
    }
    @Override public Void visitArrayIndex(ArrayIndex node) {
        node.getArray().accept(this);
        node.getOffset().accept(this);
        return null;
    }
    @Override public Void visitFunction(Function node) {
        visitBlock(node.getBody());
        return null;
    }
    @Override public Void visitExpressionStatement(ExpressionStatement node) {
        if (node.getExpression() != null) {
            node.getExpression().accept(this);
        }
        return null;
    }
    @Override public Void visitScript(Script node) {
        // This is the entry point for most visitors
        visitBlock(node.getBody());
        return null;
    }
    @Override public Void visitAllocArrayLiteral(AllocArrayLiteral node) {
        for (Expression e : node) {
            e.accept(this);
        }
        return null;
    }

    @Override public Void visitAllocNewObject(AllocNewObject node) {
        node.getConstructor().accept(this);
        for (Expression e : node.getArguments()) {
            e.accept(this);
        }
        return null;
    }

    // Leaf nodes
    @Override public Void visitIntLiteral(IntLiteral node) { return null; }
    @Override public Void visitFloatLiteral(FloatLiteral node) { return null; }
    @Override public Void visitBoolLiteral(BoolLiteral node) { return null; }
    @Override public Void visitThisLiteral(ThisLiteral node) { return null; }
    @Override public Void visitNullLiteral(NullLiteral node) { return null; }
    @Override public Void visitStr(Str node) { return null; }
    @Override public Void visitBreak(Break node) { return null; }
    @Override public Void visitContinue(Continue node) { return null; }
    @Override public Void visitUndef(Undefined node) { return null; }
    @Override public Void visitVar(Var node) {
        return null;
    }
    @Override public Void visitIntrinsicName(IntrinsicName node) { return null; }

    @Override
    public Void visitForLoop(ForLoop node) {
        if (node.getInitializer() != null) node.getInitializer().accept(this);
        if (node.getCondition() != null) node.getCondition().accept(this);
        if (node.getIncrement() != null) node.getIncrement().accept(this);
        node.getBody().accept(this);
        return null;
    }
    @Override
    public Void visitForInLoop(ForInLoop node) {
        node.getIteratee().accept(this);
        node.getBody().accept(this);
        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoop node) {
        if (node.getCondition() != null) node.getCondition().accept(this);
        node.getBody().accept(this);
        return null;
    }

    @Override
    public Void visitDoLoop(DoLoop node) {
        node.getBody().accept(this);
        if (node.getCondition() != null) node.getCondition().accept(this);
        return null;
    }

    @Override
    public Void visitAllocMapLiteral(AllocMapLiteral node) {
        for (AllocMapLiteral.KVPair pair : node) {
            pair.val.accept(this);
        }
        return null;
    }
    @Override public Void visitSwitch(Switch node) {
        node.getDiscriminee().accept(this);
        for (Case c : node.getCases()) {
            c.accept(this);
        }
        return null;
    }
    @Override public Void visitCase(Case node) {
        if (node.getValue() != null) {
            node.getValue().accept(this);
        }
        for (Statement s : node.getStatements()) {
            s.accept(this);
        }
        return null;
    }
    @Override public Void visitUntyped(UntypedAccess node) {
        node.untypedVariable().accept(this);
        return null;
    }
    @Override public Void visitRequire(Require node) { return null; }

    // TODO: Not implemented yet
    @Override public Void visitNamedLambda(NamedLambda node) { throw new UnsupportedOperationException(); }
    @Override public Void visitLambda(Lambda node) {
        throw new UnsupportedOperationException();
    }
    @Override public Void visitFunDecl(FunDecl node) {
        throw new UnsupportedOperationException();
    }
}
