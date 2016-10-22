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
 * Serialize a Java representation of an SJS type as a type tag.
 *
 * @author colin.gordon
 */

package com.samsung.sjs.types;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

// TODO: Hoist this structure out of the backend and/or have the classes in the types package depend
// on an interface to the type tag serializer, and move this implementation to the backend package
import com.samsung.sjs.backend.IRFieldCollector;

// This class has embedded knowledge about the structure of type tags; see
// src/main/resources/backend/runtime.h
public final class TypeTagSerializer {
    private List<String> fwd_decls;
    private List<String> tag_decls;

    // Objects and code require auxilliary allocations for field arrays, property names, argument
    // types
    private List<String> code_decls;
    private List<String> proparray_decls;
    private List<String> fieldarray_decls;
    private List<String> argarray_decls;

    private IRFieldCollector.FieldMapping field_codes;

    private int next_code;
    private int next_tag;
    
    private Map<Type,String> memo;

    // TODO: Accept an FFI linkage, and generate tags for FFI objects
    // TODO: Memoize code and type tags to reduce memory
    public TypeTagSerializer(IRFieldCollector.FieldMapping codes) {
        fwd_decls = new LinkedList<>();
        tag_decls = new LinkedList<>();
        code_decls = new LinkedList<>();
        proparray_decls = new LinkedList<>();
        fieldarray_decls = new LinkedList<>();
        argarray_decls = new LinkedList<>();

        field_codes = codes;

        // Set up primitives
        fwd_decls.add("extern type_tag_t __int_tag;");
        fwd_decls.add("extern type_tag_t __bool_tag;");
        fwd_decls.add("extern type_tag_t __float_tag;");
        fwd_decls.add("extern type_tag_t __string_tag;");
        fwd_decls.add("extern type_tag_t __void_tag;");
        fwd_decls.add("extern type_tag_t __top_tag;");
        tag_decls.add("type_tag_t __int_tag = { TYPE_INT, NULL };");
        tag_decls.add("type_tag_t __bool_tag = { TYPE_BOOL, NULL };");
        tag_decls.add("type_tag_t __float_tag = { TYPE_FLOAT, NULL };");
        tag_decls.add("type_tag_t __string_tag = { TYPE_STRING, NULL };");
        tag_decls.add("type_tag_t __void_tag = { TYPE_VOID, NULL };");
        tag_decls.add("type_tag_t __top_tag = { TYPE_TOP, NULL };");

        next_code = 1;
        next_tag = 1;

        memo = new LinkedHashMap<>();
    }
    public List<String> getForwardDecls() { return fwd_decls; }
    public List<String> getPropArrayDecls() { return proparray_decls; }
    public List<String> getFieldArrayDecls() { return fieldarray_decls; }
    public List<String> getArgArrayDecls() { return argarray_decls; }
    public List<String> getCodeDecls() { return code_decls; }
    public List<String> getTagDecls() { return tag_decls; }

    public String memoizeInt() { return "&__int_tag"; }
    public String memoizeBool() { return "&__bool_tag"; }
    public String memoizeFloat() { return "&__float_tag"; }
    public String memoizeString() { return "&__string_tag"; }
    public String memoizeVoid() { return "&__void_tag"; }
    public String memoizeTop() { return "&__top_tag"; }

    public String memoizeObject(ObjectType t) {
        if (memo.containsKey(t)) {
            return memo.get(t);
        }
        int tag = next_tag++;
        // Need to put this in *now* so recursive types work correctly.
        memo.put(t, "&__tagid"+tag);
        // build the property list
        List<String> props = new ArrayList<>();
        List<String> ftypes = new ArrayList<>();

        /* After reading the following allocation and loop, you may be wondering why we're doing
         * this weird iteration and reordering of properties, instead of simply iterating over
         * t.properties().  Great question!  The answer is that this code is currently duplicating
         * logic from IRVTablePass.generateIndirectionMap(PropertyContainer).  The order of
         * properties in the tag is taken as indicative of physical layout, so we must match the
         * code here to generate the tag against the vtable layout pass, which generates all vtables
         * statically (and therefore forms the basis for field access optimization).  If this order
         * and the one generated by IRVTablePass differ, field accesses will be mis-optimized.
         */
        List<Property> layout = new ArrayList<>(t.inheritedProperties());
        for (Property p : t.ownProperties()) {
            layout.add(p);
        }
        for (Property p : layout) {
            props.add(field_codes.indexOf(p.getName())+" /*"+p.getName()+"*/");
            ftypes.add(p.getType().generateTag(this));
        }
        ftypes.add("NULL");


        // TODO: Need to rip out the proto part of this representation, which hasn't been used in a
        // long time

        // now generate the nested components: field and props tags
        fwd_decls.add("extern type_tag_t __tagid"+tag+";");
        tag_decls.add("type_tag_t __tagid"+tag+" = { TYPE_OBJECT, .body = { .object_sig = { __tagfields"+tag+", __tagprops"+tag+", NULL } } };");

        proparray_decls.add("uint32_t __tagprops"+tag+"[] = { "+String.join(", ", props)+" };");
        fieldarray_decls.add("type_tag_t* __tagfields"+tag+"[] = { "+String.join(", ", ftypes)+" };");

        return "&__tagid"+tag;
    }

    public String memoizeMap(MapType t) {
        if (memo.containsKey(t)) {
            return memo.get(t);
        }
        int tag = next_tag++;
        fwd_decls.add("extern type_tag_t __tagid"+tag+";");
        tag_decls.add("type_tag_t __tagid"+tag+" = { TYPE_MAP, "+t.elemType().generateTag(this)+" };");
        memo.put(t, "&__tagid"+tag);
        return "&__tagid"+tag;
    }

    public String memoizeArray(ArrayType t) {
        if (memo.containsKey(t)) {
            return memo.get(t);
        }
        int tag = next_tag++;
        fwd_decls.add("extern type_tag_t __tagid"+tag+";");
        tag_decls.add("type_tag_t __tagid"+tag+" = { TYPE_ARRAY, "+t.elemType().generateTag(this)+" };");
        memo.put(t, "&__tagid"+tag);
        return "&__tagid"+tag;
    }

    public static enum ClosureType { FUNCTION, METHOD, CTOR; }

    public String memoizeClosure(ClosureType ct, Type result, Type receiver, List<Type> args) {
        // TODO: Why isn't this taking a CodeType?  Probably so the caller (which knows its variety)
        // can choose the receiver, but this (slightly) hurts memoization
        int tag = next_tag++;
        fwd_decls.add("extern type_tag_t __tagid"+tag+";");
        tag_decls.add("type_tag_t __tagid"+tag+" = { TYPE_CLOSURE, &__codetag"+tag+" };");
        String codetype = null;
        switch (ct) {
            case FUNCTION: codetype = "CODE_FUNCTION"; break;
            case METHOD: codetype = "CODE_METHOD"; break;
            case CTOR: codetype = "CODE_CTOR"; break;
        }
        code_decls.add("code_type_t __codetag"+tag+" = { "+codetype+", "+(args.size()+1)+", "+result.generateTag(this)+", "+
                (receiver != null ? receiver.generateTag(this) : "NULL")+", __argarray"+tag+" };");
        // TODO: note that we're dumping the receiver into the prototype slot above... fix this
        StringBuilder argbuilder = new StringBuilder();
        boolean leadcomma = false;
        argbuilder.append("type_tag_t *__argarray"+tag+"[] = { ");
        // TODO: where's the reciever? it's dumped out above, which is wrong
        argbuilder.append(receiver != null ? receiver.generateTag(this) : memoizeTop());
        leadcomma = true;
        for (Type arg : args) {
            if (leadcomma) argbuilder.append(", ");
            leadcomma = true;
            argbuilder.append(arg.generateTag(this));
        }
        argbuilder.append(" };");
        argarray_decls.add(argbuilder.toString());
        return "&__tagid"+tag;
    }
}
