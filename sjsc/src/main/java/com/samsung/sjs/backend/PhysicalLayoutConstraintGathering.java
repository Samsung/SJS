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
 * A pass to collect data on the sets of vtables that should be generated, so a global optimization
 * problem can be solved to optimize the applicability of the field access optimizations.
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend;


import com.samsung.sjs.CompilerOptions;
import com.samsung.sjs.FFILinkage;
import com.samsung.sjs.backend.asts.ir.*;
import com.samsung.sjs.types.*;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhysicalLayoutConstraintGathering extends VoidIRVisitor {

    private static Logger logger = LoggerFactory.getLogger(PhysicalLayoutConstraintGathering.class);

    private IRFieldCollector.FieldMapping field_codes;
    // For now, we can start by just trying to optimize based on picking a random set of fields to
    // give up on aligning.  Down the road, we can also accumulate different weighting statistics to
    // choose to e.g., try really hard to optimizes accesses to 'foo' because that's 30% of all
    // property accesses in the source, or based on profiling information...
    private int[][] cooccurrence_table;
    private FFILinkage ffi;

    public PhysicalLayoutConstraintGathering(CompilerOptions opts, IRFieldCollector.FieldMapping field_codes, FFILinkage ffi) {
        this.field_codes = field_codes;

        cooccurrence_table = new int[field_codes.size()][field_codes.size()];

        // The FFI interface defines some vtables that may *not* be modified by the compiler.  It
        // dictates the physical offsets of some properties in some objects that aren't under
        // control of the compiler, and must be accounted for in a solution.
        this.ffi = ffi;
    }

    protected void processObject(ObjectType t) {
        // Note that we have the luxury of assuming the writable properties are *exactly* the
        // properties physically present on the object, though that won't matter until we stop doing
        // copy-down inheritance...
        
        List<Property> props = t.properties();
        for (Property p1 : props) {
            for (Property p2 : props) {
                if (!p1.getName().equals(p2.getName())) {
                    int off1 = field_codes.indexOf(p1.getName());
                    int off2 = field_codes.indexOf(p2.getName());
                    cooccurrence_table[off1][off2]++;
                    cooccurrence_table[off2][off1]++;
                }
            }
        }
    }

    @Override
    public Void visitAllocObjectLiteral(AllocObjectLiteral node) {
        processObject((ObjectType)node.getType());
        return super.visitAllocObjectLiteral(node);
    }

    @Override
    public Void visitAllocNewObject(AllocNewObject node) {
        processObject((ObjectType)node.getType());
        return super.visitAllocNewObject(node);
    }

    public int[] getVTable(ObjectType t) {
        // TODO: compute and cache results on first call
        throw new UnsupportedOperationException();
    }
}
