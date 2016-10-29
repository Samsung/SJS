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
import com.samsung.sjs.types.Type;
import java.util.List;
public class IRManipulator {
    public static IntrinsicName mkIntrinsicName(String s) { return new IntrinsicName(s); }
    public static IntLiteral mkIntLiteral(int i) { return new IntLiteral(i); }
    public static BoolLiteral mkBoolLiteral(boolean b) { return new BoolLiteral(b); }
    public static ThisLiteral mkThisLiteral() { return new ThisLiteral(); }
    public static NullLiteral mkNullLiteral() { return new NullLiteral(); }
    public static Undefined mkUndefined() { return new Undefined(); }
    public static Function mkFunction(Scope s, String name, Type ret) {
        return new Function(s, name, ret);
    }
    public static FunctionCall mkFunctionCall(Expression f, Expression... args) {
        return new FunctionCall(f, args); 
    }
    public static FunctionCall mkFunctionCall(Expression f, List<Expression> args) {
        FunctionCall call = new FunctionCall(f); 
        for (Expression e : args) {
            call.addArgument(e);
        }
        return call;
    }
    public static MethodCall mkMethodCall(Expression obj, String f, Expression... args) {
        return new MethodCall(obj, f, args); 
    }
    public static AllocClosure mkAllocClosure(Function f) {
        return new AllocClosure(f); 
    }
    public static LetIn mkLetIn(Scope s, Var x, Type t, Expression v, Expression e) {
        return new LetIn(s, x, t, v, e);
    }
    public static AllocObjectLiteral mkAllocObjectLiteral() { return new AllocObjectLiteral(); }
    public static AllocMapLiteral mkAllocMapLiteral(Type t) { return new AllocMapLiteral(t); }
    public static AllocNewObject mkAllocNewObject(Expression ctor) { return new AllocNewObject(ctor); }
    public static Lambda mkLambda() { return new Lambda(); }
    public static NamedLambda mkNamedLambda() { return new NamedLambda(); }
    public static BinaryOp mkBinaryOp(Expression l, String op, Expression r) { 
        return new BinaryOp(l, op, r); 
    }
    public static UnaryOp mkUnaryOp(Expression e, String op, boolean postfix) {
        return new UnaryOp(e, op, postfix); 
    }
    public static CondExpr mkCondExpr(Expression a, Expression b, Expression c) {
        return new CondExpr(a, b, c); 
    }
    public static Var mkVar(String s) { return new Var(s); }
    public static VarAssignment mkVarAssignment(Expression x, String op, Expression e) { 
        return new VarAssignment(x, op, e); 
    }
    public static PredictedFieldAssignment mkPredictedFieldAssignment(Expression o, String f, int offset, String op, Expression v) { 
        return new PredictedFieldAssignment(o, f, offset, op, v);
    }
    public static FieldAssignment mkFieldAssignment(Expression o, String f, String op, Expression v) { 
        return new FieldAssignment(o, f, op, v);
    }
    public static DoLoop mkDoLoop(Expression cond, Statement body) { return new DoLoop(cond, body); }
    public static WhileLoop mkWhileLoop(Expression cond, Statement body) { return new WhileLoop(cond, body); }
    public static ForLoop mkForLoop(IRNode init, Expression cond, Expression incr) { 
        return new ForLoop(init, cond, incr);
    }
    public static ForInLoop mkForInLoop(Var v, Expression e, Type t) { return new ForInLoop(v, e, t); }
    public static Block mkBlock() { return new Block(); }
    public static VarDecl mkVarDecl(Var v, Type t, Expression e) {
        return new VarDecl(v, t, e);
    }
    public static FunDecl mkFunDecl() { return new FunDecl(); }
    public static IfThenElse mkIfThenElse(Expression a, Statement b, Statement c) {
        return new IfThenElse(a, b, c);
    }
    public static Return mkReturn() { return new Return(); }
    public static Return mkReturn(Expression e) { return new Return(e); }
    public static Break mkBreak() { return new Break(); }
    public static Continue mkContinue() { return new Continue(); }
    public static CompoundStatement mkCompoundStatement() { return new CompoundStatement(); }

    private static long varCounter = 0;

    public static Var freshVar(Scope s, String prefix, Type t) { 
        Var x = new Var("___var"+prefix+varCounter++);
        while (s.isBound(x)) {
            x = new Var("___var"+prefix+varCounter++);
        }
        //s.declareVariable(x, t);
        x.setType(t);
        return x;
    }
    public static Var freshVar(Scope s, Type t) { 
        return freshVar(s, "anon", t);
    }
    public static Str mkStr(String s) { return new Str(s); }
    public static FieldRead mkFieldRead(Expression o, String f) { return new FieldRead(o, f); }
    public static PredictedFieldRead mkPredictedFieldRead(Expression o, String f, int i) { 
        return new PredictedFieldRead(o, f, i); 
    }
    public static ExpressionStatement mkExpressionStatement(Expression e) {
        return new ExpressionStatement(e);
    }
    public static ArrayIndex mkArrayIndex(Expression arr, Expression index) {
        return new ArrayIndex(arr, index);
    }
    public static AllocArrayLiteral mkAllocArrayLiteral(Type t) {
        return new AllocArrayLiteral(t);
    }
    public static Switch mkSwitch(Expression e) { return new Switch(e); }
    public static Case mkCase(Expression e) { return new Case(e); }
}
