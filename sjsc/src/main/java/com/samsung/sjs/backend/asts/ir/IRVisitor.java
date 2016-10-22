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
package com.samsung.sjs.backend.asts.ir;
public abstract class IRVisitor<R> extends IRManipulator {
    public abstract R visitIntLiteral(IntLiteral node);
    public abstract R visitFloatLiteral(FloatLiteral node);
    public abstract R visitBoolLiteral(BoolLiteral node);
    public abstract R visitThisLiteral(ThisLiteral node);
    public abstract R visitNullLiteral(NullLiteral node);
    public abstract R visitFunctionCall(FunctionCall node);
    public abstract R visitMethodCall(MethodCall node);
    public abstract R visitAllocClosure(AllocClosure node);
    public abstract R visitLetIn(LetIn node);
    public abstract R visitAllocObjectLiteral(AllocObjectLiteral node);
    public abstract R visitAllocMapLiteral(AllocMapLiteral node);
    public abstract R visitAllocNewObject(AllocNewObject node);
    public abstract R visitLambda(Lambda node);
    public abstract R visitNamedLambda(NamedLambda node);
    public abstract R visitBinaryOp(BinaryOp node);
    public abstract R visitUnaryOp(UnaryOp node);
    public abstract R visitCondExpr(CondExpr node);
    public abstract R visitVar(Var node);
    public abstract R visitVarAssignment(VarAssignment node);
    public abstract R visitFieldAssignment(FieldAssignment node);
    public abstract R visitPredictedFieldAssignment(PredictedFieldAssignment node);
    public abstract R visitDoLoop(DoLoop node);
    public abstract R visitWhileLoop(WhileLoop node);
    public abstract R visitForLoop(ForLoop node);
    public abstract R visitForInLoop(ForInLoop node);
    public abstract R visitBlock(Block node);
    public abstract R visitVarDecl(VarDecl node);
    public abstract R visitFunDecl(FunDecl node);
    public abstract R visitIfThenElse(IfThenElse node);
    public abstract R visitReturn(Return node);
    public abstract R visitBreak(Break node);
    public abstract R visitContinue(Continue node);
    public abstract R visitCompoundStatement(CompoundStatement node);
    public abstract R visitFieldRead(FieldRead node);
    public abstract R visitPredictedFieldRead(PredictedFieldRead node);
    public abstract R visitStr(Str node);
    public abstract R visitArrayIndex(ArrayIndex node);
    public abstract R visitFunction(Function node);
    public abstract R visitExpressionStatement(ExpressionStatement node);
    public abstract R visitScript(Script node);
    public abstract R visitAllocArrayLiteral(AllocArrayLiteral node);
    public abstract R visitIntrinsicName(IntrinsicName node);
    public abstract R visitSwitch(Switch node);
    public abstract R visitCase(Case node);
    public abstract R visitUndef(Undefined node);
    public abstract R visitUntyped(UntypedAccess node);
    public abstract R visitRequire(Require node);
}
