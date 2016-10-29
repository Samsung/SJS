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
 * CondExpr
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

public class CondExpr extends Expression {
    Expression test, yes, no;
    public CondExpr(Expression a, Expression b, Expression c) {
        super(Tag.CondExpr);
        test = a;
        yes = b;
        no = c;
    }
    public Expression getTestExpr() { return test; }
    public Expression getYesExpr() { return yes; }
    public Expression getNoExpr() { return no; }
    @Override
    public String toSource(int x) {
        return "("+test.toSource(0)+" ? "+yes.toSource(0)+" : "+no.toSource(0)+")";
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitCondExpr(this);
    }

    @Override
    public boolean isPure() { return test.isPure() && yes.isPure() && no.isPure(); }
    @Override
    public boolean mustSaveIntermediates() {
        return test.mustSaveIntermediates() || yes.mustSaveIntermediates() || no.mustSaveIntermediates();
    }
}
