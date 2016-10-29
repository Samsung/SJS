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
 * AllocArrayLiteral
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;
import java.util.*;
import com.samsung.sjs.types.Type;
public class AllocArrayLiteral extends Expression implements Iterable<Expression> {

    private ArrayList<Expression> slots;
    private Type cellType;
    public AllocArrayLiteral(Type cellType) {
        super(Tag.AllocArrayLiteral);
        slots = new ArrayList<Expression>();
        this.cellType = cellType;
    }
    public Type getCellType() { return cellType; }
    public int nelems() { return slots.size(); }
    public void addElement(Expression v) {
        slots.add(v);
    }
    public Iterator<Expression> iterator() {
        return slots.iterator();
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        sb.append("<array:"+cellType.toString()+">[");
        for (Expression slot : slots) {
            sb.append(slot.toSource(0));
            sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitAllocArrayLiteral(this);
    }
    @Override
    public boolean mustSaveIntermediates() {
        for (Expression slot : slots) {
            if (slot.mustSaveIntermediates()) {
                return true;
            }
        }
        return false;
    }
}
