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
 * VarAssignment
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

// TODO: Remove the Var/Field/Array-Assignment distinction, and put together a single
// solution for deciding whether an lvalue is boxed or not

public class VarAssignment extends Expression {
    Expression x;
    Expression e;
    String op;
    public VarAssignment(Expression x, String op, Expression e) {
        super(Tag.VarAssignment);
        this.x = x;
        this.e = e;
        this.op = op;
    }
    public Expression getAssignedVar() { return x; }
    public Expression getAssignedValue() { return e; }
    public String getOperator() { return op; }
    @Override
    public String toSource(int d) { return parens(x.toSource(0)+" :"+op+" "+e.toSource(0)); }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitVarAssignment(this);
    }
    @Override
    public boolean mustSaveIntermediates() {
        if (x.isVar()) {
            return e.mustSaveIntermediates();
        }
        return super.mustSaveIntermediates();
    }
}
