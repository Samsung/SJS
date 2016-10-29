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
 * Generate code for read-access to an object slot
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import com.samsung.sjs.backend.*;
import com.samsung.sjs.types.*;
import com.samsung.sjs.backend.asts.c.types.*;

public class PredictedMemberRead extends Expression {
    private Expression target;
    private CType slot_type;
    private String slotname;
    private int offset;
    private int ptr_off;
    private boolean docast;
    private boolean isdirect;

    public PredictedMemberRead(Expression t, CType ty, String s, int o, int ptr_off) {
        target = t;
        slot_type = ty;
        slotname = s;
        offset = o;
        this.ptr_off = ptr_off;
        docast = true;
        isdirect = false;
    }
    // If we know the field is local and not inherited:
    public boolean isDirect() { return isdirect; }
    public void setDirect() {
        isdirect = true;
    }
    // When this AST node is (ab)used as an lval, C doesn't permit the cast
    public void setDoNotCast() { docast = false; }
    @Override
    public String toSource(int x) {
        String base = null;
        if (isdirect) {
            base = "(INLINE_BOX_ACCESS("+target.toSource(0)+", "+ptr_off+" /* "+slotname+","+offset+" */))";
        } else {
            base = "(FIELD_ACCESS("+target.toSource(0)+", "+ptr_off+" /* "+slotname+","+offset+" */))";
        }
        if (docast) {
            return "(("+(slot_type).toSource()+")"+base;
        } else {
            return base;
        }
    }
    @Override
    public Expression asValue(Type t) {
        // this is already a value
        return this;
    }
}

