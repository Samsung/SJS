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
 * Indexing an array
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

public class ArrayIndex extends Expression {
    Expression o, offset;
    public ArrayIndex(Expression o, Expression offset) {
        super(Tag.ArrayIndex);
        this.o = o;
        this.offset = offset;
    }
    public Expression getArray() { return o; }
    public Expression getOffset() { return offset; }
    @Override
    public String toSource(int x) {
        return "("+o.toSource(0)+"["+offset.toSource(0)+"])";
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitArrayIndex(this);
    }

    @Override
    public boolean isPure() { return o.isPure() && offset.isPure(); }
    @Override
    public boolean mustSaveIntermediates() {
        return o.mustSaveIntermediates() || offset.mustSaveIntermediates();
    }
}
