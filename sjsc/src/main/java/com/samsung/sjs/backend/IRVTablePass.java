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
 * This pass uses a collection of field names to populate the vtables in the
 * IR.  The pass consists of two steps: traversing the IR, and also building
 * vtables for the foreign function interface using {@code IRVTablePass.convert()}.
 *
 * @author Cole Schlesinger (cschles1@gmail.com)
 */
package com.samsung.sjs.backend;

import com.samsung.sjs.CompilerOptions;
import com.samsung.sjs.FFILinkage;
import com.samsung.sjs.backend.asts.ir.*;
import com.samsung.sjs.types.*;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// NOTE: Static import
import static com.samsung.sjs.backend.asts.ir.IRManipulator.*;

public class IRVTablePass extends VoidIRVisitor {

    private static Logger logger = LoggerFactory.getLogger(IRVTablePass.class);

    private boolean debug;
    private IRFieldCollector.FieldMapping field_codes;
    private Map<String, Set<int[]>> vtables_by_field = new HashMap<>();
    private Set<PropertyContainer> visited;

    public IRVTablePass(CompilerOptions opts, IRFieldCollector.FieldMapping field_codes, FFILinkage ffi) {
        this.debug = opts.debug();
        this.field_codes = field_codes;
        // populate native vtables:
        for (Map.Entry<String,List<String>> table_req : ffi.getTablesToGenerate()) {
            generateIndirectionMap(table_req.getValue());
        }
        this.visited = new HashSet<>();
    }

    public Map<String, Set<int[]>> getVtablesByFieldMap() {
        return vtables_by_field;
    }

    public void generateVTables(Type t) {
        if (t.isIntersectionType()) {
            for (Type ty : ((IntersectionType)t).getTypes()) {
                generateVTables(ty);
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
                generateIndirectionMap(pc);
                for (String name : pc.propertyNames()) {
                    generateVTables(pc.findMemberType(name));
                }
            }
            if (t instanceof CodeType) {
                CodeType ct = (CodeType)t;
                generateVTables(ct.returnType());
                for (Type ty : ct.paramTypes()) {
                    generateVTables(ty);
                }
                // TODO: unattached method receivers
            }
        }
    }

    protected int[] generateIndirectionMap(AllocObjectLiteral node) {
        int[] map = new int[field_codes.size()];
        java.util.Arrays.fill(map, -1);
        int nextoffset = 0;
        for (AllocObjectLiteral.TypedSlot slot : node) {
            String name = slot.name;
            map[field_codes.indexOf(name)] = nextoffset++;
            if (vtables_by_field.containsKey(name)) {
                logger.debug("Adding vtable for {}", name);
                Set<int[]> vts = vtables_by_field.get(name);
                vts.add(map);
            } else {
                logger.debug("Adding vtable for {}", name);
                Set<int[]> vts = new HashSet<>();
                vts.add(map);
                vtables_by_field.put(name, vts);
            }
        }
        if (debug) {
            System.err.print(">>> For object literal ["+node.toSource(0)+"], object map: ");
            for (int i = 0; i < map.length; i++) {
                System.err.print(map[i]+" ");
            }
            System.err.println();
        }
        return map;
    }

    protected int[] generateIndirectionMap(List<String> properties) {
        // TODO: This is half-duplicate of some work in IRCBackend, which recomputes these vtables
        // at a lower level.  Merge these somehow.  We need the information here for field access
        // optimizations to work (and be sound)
        int[] vt = new int[field_codes.size()];
        java.util.Arrays.fill(vt, -1);
        int physical_index = 0;
        for (String prop : properties) {
            if (debug) {
                System.err.println("Looking up index of "+prop);
            }
            // TODO(cns): how do FFI field names get collected?
            vt[field_codes.indexOf(prop)] = physical_index++;
            if (vtables_by_field.containsKey(prop)) {
                Set<int[]> vts = vtables_by_field.get(prop);
                vts.add(vt);
            } else {
                Set<int[]> vts = new HashSet<>();
                vts.add(vt);
                vtables_by_field.put(prop, vts);
            }
        }
        return vt;
    }

    protected int[] generateIndirectionMap(PropertyContainer ty) {
        logger.debug("Generating indirection map for: {}", ty);
        int[] map = new int[field_codes.size()];
        java.util.Arrays.fill(map, -1);
        int nextoffset = 0;
        Stack<PropertyContainer> st = new Stack<>();
        // Push all levels of the prototype heirarchy onto the stack, with the top of the heirarchy
        // on top of the stack.  Then add all properties in order, which (tends to) put inherited
        // properties earlier in the object.
        // TODO: If we only use the inherited but not-overridden properties here, we'll often screw
        // up the all-F-at-same-offset optimization for overridden properties
        // TODO: If this logic changes for ObjectTypes (i.e., will produce a different property
        // order), also update the unfortunately-duplicated logic in
        // TypeTagSerializer.memoizeObject.
        List<Property> props = null;
        if (ty instanceof ObjectType) {
            ObjectType tyo = (ObjectType)ty;
            props = new ArrayList<>(tyo.inheritedProperties());
            for (Property p : tyo.ownProperties()) {
                props.add(p);
            }
        } else {
            props = ty.properties();
        }
        // Fix runtime-assumed offsets --- this is a matter of correctness, not optimization
        if (ty.hasProperty("_____cpp_receiver")) {
            map[field_codes.indexOf("_____cpp_receiver")] = nextoffset++;
        }
        if (ty.hasProperty("_____gen_cpp_proxy")) {
            map[field_codes.indexOf("_____gen_cpp_proxy")] = nextoffset++;
        }
        for (Property prop : props) {
            int off = field_codes.indexOf(prop.getName());
            // If a property is inherited, or already fixed, we only want to keep the first offset chosen
            if (map[off] == -1) {
                logger.debug("Placing property [{}] at offset: {}", prop.getName(), nextoffset);
                map[off] = nextoffset++;
                if (vtables_by_field.containsKey(prop.getName())) {
                    Set<int[]> vts = vtables_by_field.get(prop.getName());
                    vts.add(map);
                } else {
                    Set<int[]> vts = new HashSet<>();
                    vts.add(map);
                    vtables_by_field.put(prop.getName(), vts);
                }
            }
        }
        if (debug) {
            System.err.print(">>> For object type ["+ty.toString()+"], object map: ");
            for (int i = 0; i < map.length; i++) {
                System.err.print(map[i]+" ");
            }
            System.err.println();
        }
        return map;
    }

    @Override
    public Void visitAllocObjectLiteral(AllocObjectLiteral node) {
        node.setVTable(generateIndirectionMap(node));
        return super.visitAllocObjectLiteral(node);
    }

    @Override
    public Void visitAllocNewObject(AllocNewObject node) {
        // Only makes sense for non-intrinsics:
        node.setVTable(generateIndirectionMap((PropertyContainer)node.getType()));
        return super.visitAllocNewObject(node);
    }

    @Override
    public Void visitAllocClosure(AllocClosure node) {
        assert (node.getType() instanceof CodeType);
        logger.debug("IRVTPass looking at {} :: {}", node.toSource(0), node.getType());
        if (node.getType().isConstructor()) {
            logger.debug("Generating vtable...");
            ConstructorType ctor = ((ConstructorType)node.getType());
            if (ctor.getPrototype() != null) {
                node.setVTable(generateIndirectionMap((PropertyContainer)ctor.getPrototype()));
                assert (node.getVTable() != null);
            }
        }
        return super.visitAllocClosure(node);
    }

    @Override public Void visitUntyped(UntypedAccess node) {
        // TODO: For now, we'll only handle really simple export objects, nothing higher order
        Type t = node.getType();
        if (t.isObject()) {
            ObjectType o = (ObjectType)t;
            // TODO: Need to generate this local map, plus all nested maps in positive (i.e., return
            // from untyped code) positions in the type.  Right now we just generate them all, which
            // in principle will end up nerfing some layout optimizations
            generateIndirectionMap(o);
        } else {
            throw new IllegalArgumentException("Temporarily, backend can't handle importing non-objects (module ["+node.untypedVariable().toSource(0)+"])");
        }
        return super.visitUntyped(node);
    }
}
