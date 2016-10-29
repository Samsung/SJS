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
 * IfThenElse
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

public class IfThenElse extends Statement {
    Expression test;
    Statement yes, no;
    public IfThenElse(Expression a, Statement b, Statement c) {
        super(Tag.IfThenElse);
        test = a;
        if (b instanceof Block) {
            yes = b;
        } else {
            Block bl = new Block();
            bl.addStatement(b);
            yes = bl;
        }
        if (c == null || c instanceof Block) {
            no = c;
        } else {
            Block bl = new Block();
            bl.addStatement(c);
            no = bl;
        }
    }
    public Expression getTestExpr() { return test; }
    public Statement getTrueBranch() { return yes; }
    public Statement getFalseBranch() { return no; }
    @Override
    public String toSource(int x) {
        StringBuilder b = new StringBuilder();
        b.append("if ("+test.toSource(0)+") {"+yes.toSource(0)+"}");
        if (no != null) {
            b.append(" else { "+no.toSource(0)+" }");
        }
        return b.toString();
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitIfThenElse(this);
    }
}
