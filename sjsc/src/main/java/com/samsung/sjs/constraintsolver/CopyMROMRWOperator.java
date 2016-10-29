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
import com.samsung.sjs.types.Property;

public class CopyMROMRWOperator extends UnaryOperator<TypeInfSolverVariable> {

    private static final Logger logger = LoggerFactory.getLogger(CopyMROMRWOperator.class);

    private final TypeConstraintFixedPointSolver solver;
    private final Cause reason;

    public CopyMROMRWOperator(TypeConstraintFixedPointSolver solver, Cause reason) {
        super();
        this.solver = solver;
        this.reason = reason;
    }


    @Override
    public byte evaluate(TypeInfSolverVariable lhs, TypeInfSolverVariable rhs) {
        MROMRWVariable lhsVar = (MROMRWVariable) lhs;
        MROMRWVariable rhsVar = (MROMRWVariable) rhs;
        logger.debug("{}: {} <-- {}: {}", lhsVar.getTerm(), lhsVar, rhsVar.getTerm(), rhsVar);
        Cause cause = Cause.derived(reason, lhsVar.reasonForCurrentValue, rhsVar.reasonForCurrentValue);
        boolean changed = false;
        for (Property p: rhsVar.getMRO()) {
            boolean updated = ConstraintUtil.copyIntoMRO(p, lhsVar, logger, solver, cause);
            changed = changed || updated;
        }
        for (Property p: rhsVar.getMRW()) {
            boolean updated = ConstraintUtil.copyIntoMRW(p, lhsVar, logger, solver, cause);
            changed = changed || updated;
        }
        return changed ? CHANGED : NOT_CHANGED;
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CopyMROMRWOperator;
    }


    @Override
    public String toString() {
        return "CopyMROMRWOperator";
    }

}
