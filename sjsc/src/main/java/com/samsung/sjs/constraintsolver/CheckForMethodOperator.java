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
import com.samsung.sjs.types.AnyType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.UnattachedMethodType;

public class CheckForMethodOperator extends
        UnaryOperator<TypeInfSolverVariable> {

    private final static Logger logger = LoggerFactory.getLogger(CheckForMethodOperator.class);
    private final TypeConstraintFixedPointSolver solver;
    private final ITypeTerm containingObjectTerm;
    private final Cause reason;

    public CheckForMethodOperator(TypeConstraintFixedPointSolver solver,
                                  ITypeTerm containingObjectTerm, Cause reason) {
        super();
        this.solver = solver;
        this.containingObjectTerm = containingObjectTerm;
        this.reason = reason;
    }

    @Override
    public byte evaluate(TypeInfSolverVariable lhs, TypeInfSolverVariable rhs) {
        TypeConstraintSolverVariable rhsVar = (TypeConstraintSolverVariable) rhs;
        Type rhsType = rhsVar.getType();
        // TODO any other cases here??
        if (rhsType instanceof UnattachedMethodType) {
            UnattachedMethodType umt = (UnattachedMethodType) rhsType;
            logger.debug("found method type {}", umt);
            Type receiverType = umt.receiverType();
            assert !(receiverType instanceof AnyType);
            // generate a new constraint connecting the upper bound of the receiver type
            // to the MROMRW variable of the containing object term
            TypeConstraintSolverVariable receiverUpperBound = solver.upperBounds.get(solver.factory.getTermForType(receiverType));
            MROMRWVariable objTermMROMRWVar = solver.getMROMRWVarForTerm(containingObjectTerm);
            Cause cause = Cause.derived(reason, rhsVar.reasonForCurrentValue);
            solver.newStatement(objTermMROMRWVar, new CopyFromUpperBoundOperator(solver, containingObjectTerm, cause), receiverUpperBound, true, false);
        }
        return NOT_CHANGED;
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
        CheckForMethodOperator other = (CheckForMethodOperator) obj;
        if (containingObjectTerm == null) {
            if (other.containingObjectTerm != null)
                return false;
        } else if (!containingObjectTerm.equals(other.containingObjectTerm))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CheckForMethodOperator [containingObjectTerm="
                + containingObjectTerm + "]";
    }


}
