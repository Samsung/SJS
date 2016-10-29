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
 * Accessor for values reinterpreted as a certain type.
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;

import com.samsung.sjs.types.*;

public class ValueAs extends Expression {
    private Expression val;
    private Type type;
    private boolean encode;
    public ValueAs(Expression val, Type t) {
        assert(t != null);
        this.val = val;
        this.type = t;
        this.encode = false;
    }
    public ValueAs(Expression val, Type t, boolean encode) {
        assert(t != null);
        this.val = val;
        this.type = t;
        this.encode = encode;
    }
    public static String value_field_of(Type t) {
        if (t instanceof PrimitiveType) {
            if (t instanceof IntegerType) {
                return "i";
            } else if (t instanceof FloatType) {
                return "dbl";
            } else if (t instanceof StringType) {
                return "str";
            } else if (t instanceof BooleanType) {
                return "b";
            } else {
                return "box";
            }
        } else {
            if (t instanceof ObjectType) {
                return "obj";
            }
            // Reference type
            return "ptr";
        }
    }
    public String toSource(int x) {
        //String field = value_field_of(type);
        //return "("+val.toSource(0)+"."+field+")";
        if (type.rep() == RepresentationSort.UNREPRESENTABLE) {
            System.err.println("??? have unrepresentable expression ["+val.toSource(0)+"] of type "+type);
        }
        String res = "("+getCoercion(type.rep(), encode)+"("+val.toSource(0)+"))";
        // TODO: Casts for other types: closures, etc.
        if (type.isMap()) {
            return "((map_t*)"+res+")";
        }
        return res;
    }

    public Expression expr() { return this.val; }

    @Override
    public Expression asValue(Type t) {
        return expr();
    }
    public static String getCoercion(RepresentationSort s, boolean encode) {
        switch (s) {
            case INT:
                return "val_as_int";
            case BOOL:
                return "val_as_boolean";
            case STRING:
                return "val_as_string";
            case FLOAT:
                if (encode) {
                    return "val_as_double";
                } else {
                    return "val_as_double_noenc";
                }
            case OBJECT:
                return "val_as_object";
            case CODE:
                return "val_as_pointer"; // need an additional cast
            default:
                String msg = "INTERNAL COMPILER ERROR: Tried to coerce non-representable type "+s+" to a value!";
                System.err.println(msg);
                throw new IllegalArgumentException(msg);
        }
    }
}
