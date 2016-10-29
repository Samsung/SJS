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
 * Compiler pass to optimize field accesses when offsets can be statically resolved.
 *
 * TODO: Update this for new object layout!!!
 *
 * This compiler pass assumes the layout of an object is as follows:
 *
 *  +-----------------------------------------------------------------------------+
 *  | vtbl ptr | field_ptr0 | ... | field_ptr n | field_box_0 | ... | field_box_m |
 *  +-----------------------------------------------------------------------------+
 *
 * Where the field pointers for inherited fields point to wherever, and the field pointers for
 * local (i.e., writable) fields point to the address of one of the field_boxes.
 *
 * The normal field access path (ignoring use of a value_t at a specific type) is, for accessing
 * field f (resolved to gloabl offset f_id) of object o, approximately:
 *
 *  int* o_vtbl = o->vtbl;
 *  int fptr_offset = o_vtbl[f_id];
 *  value_t* f_box_addr = o->fields[fptr_offset];
 *  value_t box_contents = *f_box_addr;
 *
 * That's 4 memory accesses per field access.  This is ridiculously expensive compared to accessing
 * a field of a C struct or Java class in memory, and is actually even worse than the overhead of
 * unoptimized virtual field/method access in Java/C++ because the contents are boxed.
 *
 * In contrast, a JIT can do inline caching of the form:
 *
 *  if (o->vtbl == profiled_vtable) {
 *    field_contents = o->fields[profiled_offset];
 *  } else {
 *    goto redo_JIT;
 *  }
 *
 * This is two accesses when prediction is successful, and when multiple fields of the same object
 * are accessed in succession, multiple field offsets may be cached, predicated on the vtable check.
 *
 * The goal of this compiler pass is to get SJS close to those numbers using type-based
 * specialization, exploiting two properties of SJS object layout:
 *   1. If all vtables with a field f map its box pointer to the same physical offset in an object
 *      representation, then we can simply skip the field offset lookup, and directly access the box
 *      pointer.  (all field accesses)
 *   2. If we can uniquely resolve the vtable based on the field present in the type at the point of
 *      access (e.g., only one vtable with field 'foo' or one with writable 'bar'), then for
 *      writable field access (include reads of writable fields) we can directly predict the offset
 *      of the _box itself_ from the object pointer.  (all accesses to fields writable *in the
 *      unique vtable*)
 *
 * The latter requires being able to predict physical layout from a vtable.
 * Right now (December 2014), the boxes for writable fields are layed out in the same order as the
 * box pointers themselves.  At the moment, we're also not implementing inheritance, so we can
 * additionally assume that the offset of the box pointer into the pointer region is the same as the
 * offset of the box into the box region.
 *
 * This will require careful revisitation when we implement the readable/writable split in object
 * types.
 */
package com.samsung.sjs.backend;

import com.samsung.sjs.backend.asts.ir.*;
import com.samsung.sjs.types.*;
import com.samsung.sjs.CompilerOptions;

import java.util.*;

import static com.samsung.sjs.backend.asts.ir.IRManipulator.*;
import static com.samsung.sjs.types.Types.*;

public final class FieldAccessOptimizer extends IRTransformer {

    private CompilerOptions opts;
    private IRFieldCollector.FieldMapping field_codes;
    // This needs to be computed by RhinoToIR, which generates the vtables themselves
    private Map<String, Set<int[]> > vtables_with_field;

    public FieldAccessOptimizer(Script s,
                       CompilerOptions opts,
                       IRFieldCollector.FieldMapping field_offsets,
                       Map<String, Set<int[]> > vtables_by_field) {
        super(s);
        this.field_codes = field_offsets;
        this.vtables_with_field = vtables_by_field;
        this.opts = opts;
    }

    @Override
    public IRNode visitPredictedFieldRead(PredictedFieldRead node) {
        throw new IllegalArgumentException("FieldAccessOptimizer should not see already-optimized field accesses!");
    }

    private int predict_box_ptr_offset(ObjectType t, String f) {
        // TODO: At least partially memoize these results.  If f only occurs in one vtable, then we
        // should cache it instead of recomputing.  If it occurs in multiple, then we should at
        // least cache that fact.
        Set<int[]> vtables_with_f = vtables_with_field.get(f);
        // null is an okay return value; we could in principle compile code accessing a type for
        // which we don't have an allocation
        if (vtables_with_f == null || vtables_with_f.size() < 1) {
            System.err.println("Found 0 vtables with field "+f);
            return -1;
        }
        boolean found = false;
        boolean multiple = false;
        int ptr_offset = 0;
        for (int[] vt : vtables_with_f) {
            if (found) {
                if (ptr_offset != vt[field_codes.indexOf(f)]) {
                    System.err.println("Found (at least) offsets for ["+f+"] at "+ptr_offset+" and "+vt[field_codes.indexOf(f)]);
                    //return -1; // multiple offsets for field f
                    multiple = true;
                }
            } else {
                found = true;
                ptr_offset = vt[field_codes.indexOf(f)];
            }
        }
        // We found multiple vtables with the field we're trying to optimize
        if (multiple) {
            System.err.println("Trying to disambiguate offsets based on co-occurrence of fields with ["+f+"]...");
            Set<int[]> candidates = new HashSet<>();
            Set<int[]> removals = new HashSet<>();
            // candidates will (initially) hold all vtables containing f
            candidates.addAll(vtables_with_f);
            // For each property in the object type we have for the receiver, remove any candidate vtables
            // missing that property
            for (String curprop : t.propertyNames()) {
                if (curprop.equals(f))
                    continue;
                int curprop_code = field_codes.indexOf(curprop);
                for (int[] vt : candidates) {
                    if (vt[curprop_code] == -1) {
                        System.err.println("-- Removing candidate with property ["+f+"] but no property ["+curprop+"]");
                        removals.add(vt);
                    } else {
                        System.err.println("== Candidate has property ["+f+"] AND ["+curprop+"]");
                    }
                }
                candidates.removeAll(removals);
                removals.clear();
                if (candidates.size() == 1)
                    break;
            }
            // If we've narrowed it down to one, then we win!
            if (candidates.size() == 1) {
                System.err.println("SUCCESS! Minimized candidate offsets for ["+f+"]");
                for (int[] vt : candidates) {
                    return vt[field_codes.indexOf(f)];
                }
            } else {
                // iterate through the remaining candidates hoping that with fewer vtables in play,
                // it's more likely the vtables we haven't ruled out agree on the physicall field
                // offset
                found = false;
                multiple = false;
                for (int[] vt : candidates) {
                    if (found) {
                        if (ptr_offset != vt[field_codes.indexOf(f)]) {
                            System.err.println("FAILED: Found (at least) offsets for ["+f+"] at "+ptr_offset+" and "+vt[field_codes.indexOf(f)]);
                            return -1; // multiple offsets for field f
                        }
                    } else {
                        found = true;
                        ptr_offset = vt[field_codes.indexOf(f)];
                    }
                }
            }

        }
        return ptr_offset;
    }

    @Override
    public IRNode visitFieldRead(FieldRead node) {
        if (!node.getObject().getType().isObject()) {
            return super.visitFieldRead(node);
        }
        ObjectType t = (ObjectType)node.getObject().getType();
        int box_ptr_offset = predict_box_ptr_offset(t, node.getField());
        if (box_ptr_offset != -1) {
            PredictedFieldRead n = mkPredictedFieldRead(node.getObject(), node.getField(), box_ptr_offset);
            n.setType(node.getType());
            if (t.hasOwnProperty(node.getField())) {
                n.setDirect();
            }
            return n;
        } else {
            System.err.println("Field access opt failed: "+node.toSource(0));
            return super.visitFieldRead(node);
        }
    }

    @Override
    public IRNode visitFieldAssignment(FieldAssignment node) {
        if (!node.getObject().getType().isObject()) {
            return super.visitFieldAssignment(node);
        }
        ObjectType t = (ObjectType)node.getObject().getType();
        int box_ptr_offset = predict_box_ptr_offset(t, node.getField());
        if (box_ptr_offset != -1) {
            IRNode n = mkPredictedFieldAssignment(node.getObject(), node.getField(), box_ptr_offset,
                                                  node.getOperator(),
                                                  node.getValue().accept(this).asExpression());
            n.setType(node.getType());
            return n;
        } else {
            System.err.println("Field write opt failed: "+node.toSource(0));
            return super.visitFieldAssignment(node);
        }
    }

    //TODO: accesses to hard-coded vtables like console, 
    //TODO: physical box (as opposed to box ptr) prediction, both read and write

}
