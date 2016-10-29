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
import com.samsung.sjs.typeconstraints.ObjectLiteralTerm;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.Property;

/**
 * specifically updates the MRO/MRW sets of an object literal term
 * @author m.sridharan
 *
 */
public class MROMRWToObjLitOperator extends
        UnaryOperator<TypeInfSolverVariable> {

    final static Logger logger = LoggerFactory.getLogger(MROMRWToObjLitOperator.class);
    private final TypeConstraintFixedPointSolver solver;
    /**
     * the term inducing this operator
     */
    private final ObjectLiteralTerm term;
    private final Cause reason;

    public MROMRWToObjLitOperator(TypeConstraintFixedPointSolver solver,
                                  ObjectLiteralTerm term, Cause reason) {
        super();
        this.solver = solver;
        this.term = term;
        this.reason = reason;
    }

    @Override
    public byte evaluate(TypeInfSolverVariable lhs, TypeInfSolverVariable rhs) {
        TypeConstraintSolverVariable lhsVar = (TypeConstraintSolverVariable) lhs;
        MROMRWVariable rhsVar = (MROMRWVariable) rhs;
        ObjectType oType = (ObjectType) lhsVar.getType();
        Cause cause = Cause.derived(reason, lhs.reasonForCurrentValue, rhs.reasonForCurrentValue);
        for (Property p: oType.properties()) {
            ConstraintUtil.equatePropertyTypeWithMROMRWSets(rhsVar, p, solver, cause, logger);
        }
        return NOT_CHANGED;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((term == null) ? 0 : term.hashCode());
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
        MROMRWToObjLitOperator other = (MROMRWToObjLitOperator) obj;
        if (term == null) {
            if (other.term != null)
                return false;
        } else if (!term.equals(other.term))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MROMRWToObjLitOperator [term=" + term + "]";
    }


}
