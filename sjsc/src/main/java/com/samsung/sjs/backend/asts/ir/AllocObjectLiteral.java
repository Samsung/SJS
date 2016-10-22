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
 * AllocObjectLiteral
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;
import java.util.*;
import com.samsung.sjs.types.Type;
public class AllocObjectLiteral extends Expression implements Iterable<AllocObjectLiteral.TypedSlot> {

    public static class TypedSlot {
        public final String name;
        public final Expression val;
        public final Type ty;
        public TypedSlot(String s, Expression v, Type t) {
            name = s;
            val = v;
            ty = t;
        }
    }

    public Iterator<TypedSlot> iterator() {
        return slots.iterator();
    }

    private int[] vtable;
    private ArrayList<TypedSlot> slots;
    public AllocObjectLiteral() {
        super(Tag.AllocObjectLiteral);
        slots = new ArrayList<TypedSlot>();
    }
    public int nslots() { return slots.size(); }
    public void setVTable(int[] vt) { vtable = vt; }
    public int[] getVTable() { return vtable; }
    public void addSlot(String n, Expression v, Type t) {
        slots.add(new TypedSlot(n, v, t));
    }
    @Override
    public String toSource(int x) {
        StringBuilder sb = new StringBuilder();
        sb.append("<objalloc>{ ");
        for (TypedSlot slot : slots) {
            sb.append(slot.name+" : "+slot.ty.toString()+" = "+slot.val.toSource(0));
            sb.append("; ");
        }
        sb.append("}");
        return sb.toString();
    }
    @Override
    public <R> R accept(IRVisitor<R> v) {
        return v.visitAllocObjectLiteral(this);
    }

    @Override
    public boolean mustSaveIntermediates() {
        for (TypedSlot slot : slots) {
            if (slot.val.mustSaveIntermediates()) {
                return true;
            }
        }
        return false;
    }
}
