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
 * Convert SJS types to C AST types
 * @author colin.gordon
 */
package com.samsung.sjs.backend;


import com.samsung.sjs.backend.asts.c.*;
import com.samsung.sjs.backend.asts.c.types.*;
import com.samsung.sjs.types.*;
import com.samsung.sjs.backend.asts.ir.CRuntimeArray;

public class SJSTypeConverter {

    public final TypeNormalizer tn = new TypeNormalizer();

    public CType convert(Type t) {
        return convert(t, false);
    }
    public CType convert(Type t, boolean isCtor) {
        if (t.isIntersectionType()) {
            System.err.println("ERROR: Cannot convert arbitrary intersection types yet: "+t);
        }
        assert (!t.isIntersectionType());
        // Right now, we haven't fleshed out the Type API enough to
        // write this without instanceof
        // TODO: Rewrite this when we have more isX() methods declared
        if (isCtor) { assert (t.isFunction() || t.isAttachedMethod() || t.isUnattachedMethod()); }
        if (t.isFunction()) {
            // C function pointer
            final CodeType jstype = (CodeType)t;
            CFunctionType ctype = new CFunctionType("");
            ctype.setReturn(jstype.returnType() instanceof VoidType ? new CVoid() : new Value());
            ctype.addParameterType(new EnvironmentPseudoType());
            // All function/code bodies use a single calling convention, so we add a parameter for
            // "self"
            ctype.addParameterType(new Value());
            for (Type arg : jstype.paramTypes()) {
                ctype.addParameterType(new Value());
            }
            // TODO: instead of closure pseudo type, return the raw c type
            return new ClosurePseudoType(ctype, tn);
        } else if (t instanceof BooleanType) {
            return new CBool();
        } else if (t instanceof FloatType) {
            return new CDouble();
        } else if (t instanceof VoidType) {
            return new CVoid();
        } else if (t instanceof IntegerType) {
            return new CInteger();
        } else if (t instanceof StringType) {
            return new CString();
        } else if (t.isAttachedMethod()) {
            // C function pointer consuming an object + args
            final AttachedMethodType jstype = (AttachedMethodType)t;
            CFunctionType ctype = new CFunctionType("");
            ctype.setReturn(jstype.returnType() instanceof VoidType ? new CVoid() : new Value());
            ctype.addParameterType(new EnvironmentPseudoType());
            ctype.addParameterType(new Value());
            for (Type arg : jstype.paramTypes()) {
                ctype.addParameterType(new Value());
            }
            // TODO: instead of closure pseudo type, return the raw c type
            return new ClosurePseudoType(ctype, tn);
        }  else if (t.isUnattachedMethod()) {
            // C function pointer consuming an object + args
            final UnattachedMethodType jstype = (UnattachedMethodType)t;
            CFunctionType ctype = new CFunctionType("");
            ctype.setReturn(jstype.returnType() instanceof VoidType ? new CVoid() : new Value());
            ctype.addParameterType(new EnvironmentPseudoType());
            ctype.addParameterType(new Value());
            for (Type arg : jstype.paramTypes()) {
                ctype.addParameterType(new Value());
            }
            // TODO: instead of closure pseudo type, return the raw c type
            return new ClosurePseudoType(ctype, tn);
        } else if (t.isConstructor()) {
            // C function pointer consuming an object + args
            final ConstructorType jstype = (ConstructorType)t;
            CFunctionType ctype = new CFunctionType("");
            ctype.setReturn(new Value());
            ctype.addParameterType(new EnvironmentPseudoType());
            ctype.addParameterType(new Value());
            for (Type arg : jstype.paramTypes()) {
                ctype.addParameterType(new Value());
            }
            // TODO: instead of closure pseudo type, return the raw c type
            return new ClosurePseudoType(ctype, tn, true);
        } else if (t.isMap()) {
            MapType mt = (MapType)t;
            return new CHashTable(convert(mt.elemType()));
        } else {
            if (t instanceof CRuntimeArray) {
                return new CArray(convert(((CRuntimeArray)t).elemType()));
            }
            if (t.isArray()) {
                //return new CArray(convert(((ArrayType)t).elemType()));
                return new JSArray();
            }
            if (t instanceof AnyType) {
                return new CVoid();
            }
            if (t instanceof Types.MapIteratorType) {
                return new CMapIterator(convert(((Types.MapIteratorType)t).elemType()));
            }
            if (!t.isObject()) {
                System.err.println("Type conversion failure: need object type, got "+t.toString());
            }
            assert (t.isObject());
            return new ObjectPseudoType();
        }
    }

}
