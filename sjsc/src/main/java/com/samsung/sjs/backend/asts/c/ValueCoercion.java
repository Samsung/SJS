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
/* Coerce a C expression into a value_t
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import com.samsung.sjs.types.*;
import com.samsung.sjs.backend.asts.c.types.*;

public class ValueCoercion extends Expression {
    private Expression subject;
    private String name;
    private Type srcTy;
    private boolean encode;
    public ValueCoercion(String name, Expression subj) {
        this.name = name;
        subject = subj;
        srcTy = null;
        this.encode = false;
    }
    public ValueCoercion(Type src, Expression subj, boolean encode) {
        subject = subj;
        name = null;
        srcTy = src;
        this.encode = encode;
    }
    public Expression getSubject() { return subject; }
    @Override
    public String toSource(int x) {
        if (name != null) {
            return "("+name+"("+subject.toSource(0)+"))";
        } else {
            if (srcTy == null) {
                System.err.println("ASERASFGASDFASFAS DFASDF "+subject.toSource(0));
            }
            if (srcTy.isAny()) {
                assert (subject instanceof NullLiteral);
                return "(object_as_val(NULL))";
            }
            return "("+getCoercion(srcTy.rep(), encode)+"("+subject.toSource(0)+"))";
        }
    }
    @Override
    public Expression asValue(Type t) {
        return this;
    }
    @Override
    public Expression inType(Type t) {
        // We don't want to check that the coercion is to *exactly* the same type, because objects
        // and code type are subject to subtyping.  And such checks on composite types are
        // expensive.  Instead, check that at least the rep type is the same, when present.
        if (srcTy != null) {
            if (t.rep() != srcTy.rep() && t.rep() != RepresentationSort.BOOL) {
                System.err.println("ERROR: trying to coerce expression "+subject.toSource(0)+" of type "+srcTy.toString()+" to type "+t.toString());
            }
            assert (t.rep() == srcTy.rep() || t.rep() == RepresentationSort.BOOL);
        }
        return subject;
    }

    public static String getCoercion(RepresentationSort s, boolean encode) {
        switch (s) {
            case INT:
                return "int_as_val";
            case BOOL:
                return "boolean_as_val";
            case STRING:
                return "string_as_val";
            case FLOAT:
                if (encode) {
                    return "double_as_val"; // shifting
                } else {
                    return "double_as_val_noenc"; // no shift
                }
            case OBJECT:
                return "object_as_val";
            case CODE:
                return "closure_as_val";
            default:
                String msg = "INTERNAL COMPILER ERROR: Tried to coerce non-representable type "+s+" to a value!";
                System.err.println(msg);
                throw new IllegalArgumentException(msg);
        }
    }
}
