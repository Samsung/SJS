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

public class MemberRead extends Expression {
    private Expression target;
    private CType slot_type;
    private String slotname;
    private int offset;
    private boolean docast;
    private boolean is_writable;
    public MemberRead(Expression t, CType ty, String s, int o) {
        target = t;
        slot_type = ty;
        slotname = s;
        offset = o;
        docast = true;
        is_writable = false;
    }
    public Expression getTarget() { return target; }
    public int getOffset() { return offset; }
    // When this AST node is (ab)used as an lval, C doesn't permit the cast
    public void setDoNotCast() { docast = false; }
    public void setWritable() { is_writable = true; }
    @Override
    public String toSource(int x) {
        String access = is_writable ? "FIELD_READ_WRITABLE" : "FIELD_READ";
        if (docast) {
            return "(("+(slot_type).toSource()+")"+
                   access+"("+target.toSource(0)+", "+offset+" /* "+slotname+" */))";
        } else {
            return "("+access+"("+target.toSource(0)+", "+offset+" /* "+slotname+" */))";
        }
    }
    @Override
    public Expression asValue(Type t) {
        // this is already a value
        return this;
    }
}
