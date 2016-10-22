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
package com.samsung.sjs.constraintsolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.fixpoint.UnaryOperator;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.types.TopType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.Type;

public class CopyFromUpperBoundOperator extends
        UnaryOperator<TypeInfSolverVariable> {

    private final static Logger logger = LoggerFactory.getLogger(CopyFromUpperBoundOperator.class);

    private final TypeConstraintFixedPointSolver solver;
    private final ITypeTerm containingObjectTerm;
    private final Cause reason;


    public CopyFromUpperBoundOperator(TypeConstraintFixedPointSolver solver,
                                      ITypeTerm containingObjectTerm, Cause reason) {
        super();
        this.solver = solver;
        this.containingObjectTerm = containingObjectTerm;
        this.reason = reason;
    }

    @Override
    public byte evaluate(TypeInfSolverVariable lhs, TypeInfSolverVariable rhs) {
        MROMRWVariable lhsVar = (MROMRWVariable) lhs;
        logger.debug("{}: {} <-- {}", lhsVar.getTerm(), lhs, rhs);
        TypeConstraintSolverVariable rhsVar = (TypeConstraintSolverVariable) rhs;
        Type type = rhsVar.getType();
        if (type instanceof TopType) {
            // no upper bound yet
            return NOT_CHANGED;
        }
        ObjectType rhsType = (ObjectType) type;
        boolean changed = false;
        // add RO properties to MRO, unless they are already in MRW
        for (Property p: rhsType.getROProperties()) {
            boolean updated = ConstraintUtil.copyIntoMRO(p, lhsVar, logger, solver, reason);
            changed = changed || updated;
        }
        // handle RW properties
        for (Property p: rhsType.getRWProperties()) {
            boolean updated = ConstraintUtil.copyIntoMRW(p, lhsVar, logger, solver, reason);
            changed = changed || updated;
        }
        return changed ? CHANGED : NOT_CHANGED;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((containingObjectTerm == null) ? 0 : containingObjectTerm
                        .hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CopyFromUpperBoundOperator other = (CopyFromUpperBoundOperator) obj;
        if (containingObjectTerm == null) {
            if (other.containingObjectTerm != null)
                return false;
        } else if (!containingObjectTerm.equals(other.containingObjectTerm))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CopyFromUpperBoundOperator [containingObjectTerm="
                + containingObjectTerm + "]";
    }



}
