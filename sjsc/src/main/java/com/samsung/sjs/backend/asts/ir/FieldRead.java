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
 * Reading a field of an object
 *
 * @author colin.gordon
 *
 */
package com.samsung.sjs.backend.asts.ir;
public class FieldRead extends Expression {
    protected Expression o;
    protected String f;
    public FieldRead(Expression o, String f) {
        super(Tag.FieldRead);
        this.o = o;
        this.f = f;
    }
    public Expression getObject() { return o; }
    public String getField() { return f; }
    @Override
    public String toSource(int x) {
        return "("+o.toSource(0)+"."+f+")";
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitFieldRead(this);
    }

    @Override
    public boolean isPure() { return o.isPure(); }
    @Override
    public boolean mustSaveIntermediates() {
        return o.mustSaveIntermediates(); // this is inherited by predicted field reads as well
    }
}
