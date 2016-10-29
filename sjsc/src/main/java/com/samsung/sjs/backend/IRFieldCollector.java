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
 * Visitor to collect property names used, and compute indirection maps.  Field
 * names are collected from the types annotating field reads and object
 * literals.
 *
 * @author Cole Schlesinger (cschles1@gmail.com)
 *
 */
package com.samsung.sjs.backend;

import java.util.*;
import com.samsung.sjs.JSEnvironment;
import com.samsung.sjs.types.*;
import com.samsung.sjs.backend.asts.ir.*;
import com.samsung.sjs.backend.*;

public final class IRFieldCollector extends VoidIRVisitor {

    private SortedSet<String> fields;
    private Set<PropertyContainer> visited;

    public IRFieldCollector(JSEnvironment env) {
        fields = new TreeSet<String>();
        visited = new HashSet<PropertyContainer>();
        for (Map.Entry<String,Type> kv : env.entrySet()) {
            // the key here is the name of something in the global environment
            Type t = kv.getValue();
            collectFields(t);
        }

        /* Patch array properties.  The toplevel doesn't contain any actual arrays with populated
           property maps for the array type --- the solver populates array properties lazily!
           But since array properties mention array types, eager population produces a stack overflow.
           Once we have proper support for recursive types, all should be well.
           We could in principle do the following now:
            for (String s : ArrayType.getParameterizedProperties().keySet()) {
                fields.add(s);
            } 
           except the array properties supported by the runtime and the array properties supported by
           the frontend are incomparable right now.
        */
        fields.add("reverse");
        fields.add("length");
        fields.add("pop");
        fields.add("push");
        fields.add("shift");
        fields.add("unshift");
        fields.add("join");

        // Patch primitive properties
        // TODO: Use JSEnvironment intrinsic maps
        fields.add("isFinite");
        fields.add("isNaN");
        fields.add("toString");
        fields.add("_____cpp_receiver"); // <-- FFI IDL processor generates this
        fields.add("_____gen_cpp_proxy"); // <-- FFI IDL processor generates this
        fields.add("_____env"); // <-- closure representation
        fields.add("_____code"); // <-- closure representation
        fields.add("prototype"); // <-- constructor representation
    }

    public void collectFields(Type t) {
        if (t.isIntersectionType()) {
            for (Type ty : ((IntersectionType)t).getTypes()) {
                collectFields(ty);
            }
        } else {
            // The following cases aren't mutually exclusive
            if (t instanceof PropertyContainer) {
                PropertyContainer pc = (PropertyContainer)t;
                // Avoid infinite recursion on cyclic (recursive) types
                if (visited.contains(pc)) {
                    return;
                }
                visited.add(pc);
                for (String name : pc.propertyNames()) {
                    fields.add(name);
                    collectFields(pc.findMemberType(name));
                }
            }
            if (t instanceof CodeType) {
                CodeType ct = (CodeType)t;
                collectFields(ct.returnType());
                for (Type ty : ct.paramTypes()) {
                    collectFields(ty);
                }
            }
        }
    }

    public FieldMapping getResults() {
        return new FieldMapping(fields);
    }

    @Override
    public Void visitFieldRead(FieldRead node) {
        fields.add(node.getField());
        return super.visitFieldRead(node);
    }

    @Override
    public Void visitFieldAssignment(FieldAssignment node) {
        fields.add(node.getField());
        return super.visitFieldAssignment(node);
    }

    @Override
    public Void visitPredictedFieldAssignment(PredictedFieldAssignment node) {
        fields.add(node.getField());
        return super.visitPredictedFieldAssignment(node);
    }

    @Override
    public Void visitAllocObjectLiteral(AllocObjectLiteral node) {
        for (AllocObjectLiteral.TypedSlot s : node) {
            fields.add(s.name);
        }
        return super.visitAllocObjectLiteral(node);
    }

    public static class FieldMapping implements Iterable<Map.Entry<String,Integer>> {
        private final int nfields;
        private final String[] indexToName;
        private final Map<String,Integer> nameToIndex;

        public FieldMapping(SortedSet<String> fs) {
            nfields = fs.size();
            indexToName = new String[nfields];
            int i = 0;
            for (String s : fs) {
                indexToName[i] = s;
                i++;
            }
            nameToIndex = new HashMap<String,Integer>();
            for (i = 0; i < nfields; i++) {
                nameToIndex.put(indexToName[i], i);
            }
        }
        public String nameOf(int index) {
            return indexToName[index];
        }
        public int indexOf(String name) {
            Integer i = nameToIndex.get(name);
            if (i == null) throw new IllegalArgumentException(name);
            return i;
        }
        public int size() {
            return nfields;
        }
        @Override
        public Iterator<Map.Entry<String,Integer>> iterator() {
            return nameToIndex.entrySet().iterator();
        }
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Object layout:\n");
            for (int i = 0; i < nfields; i++) {
                sb.append("\toffset "+i+" <- "+indexToName[i]+"\n");
            }
            return sb.toString();
        }
    }
}
