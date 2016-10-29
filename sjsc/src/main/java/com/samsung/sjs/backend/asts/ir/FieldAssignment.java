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
 * FieldAssignment
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

public class FieldAssignment extends Expression {
    Expression o;
    String f;
    Expression v;
    String op;
    public FieldAssignment(Expression o, String f, String op, Expression v) {
        super(Tag.FieldAssignment);
        this.o = o;
        this.f = f;
        this.v = v;
        this.op = op;
    }
    public Expression getObject() { return o; }
    public String getField() { return f; }
    public Expression getValue() { return v; }
    public String getOperator() { return op; }
    @Override
    public String toSource(int x) {
        return "("+o.toSource(0)+"."+f+" :"+op+" "+v.toSource(0)+")";
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitFieldAssignment(this);
    }
    @Override
    public boolean mustSaveIntermediates() {
        // inherited by predicted writes, too
        return o.mustSaveIntermediates() || v.mustSaveIntermediates();
    }
}
