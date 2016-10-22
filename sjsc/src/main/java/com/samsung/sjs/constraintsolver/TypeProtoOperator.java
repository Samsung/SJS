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
import com.samsung.sjs.typeconstraints.ProtoTerm;
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.BottomType;
import com.samsung.sjs.types.Type;

public class TypeProtoOperator extends
        UnaryOperator<TypeInfSolverVariable> {

    private static Logger logger = LoggerFactory.getLogger(TypeProtoOperator.class);

    private final TypeConstraintFixedPointSolver solver;

    private final ProtoTerm protoTerm;
    private final Cause reason;

    public TypeProtoOperator(TypeConstraintFixedPointSolver solver,
                             ProtoTerm protoTerm, Cause reason) {
        super();
        this.solver = solver;
        this.protoTerm = protoTerm;
        this.reason = reason;
    }

    @Override
    public byte evaluate(TypeInfSolverVariable lhsVar,
            TypeInfSolverVariable rhsVar) {
        TypeConstraintSolverVariable rhs = (TypeConstraintSolverVariable) rhsVar;
        Type rhsType = rhs.getType();
        Cause cause = Cause.derived(reason, rhsVar.reasonForCurrentValue);
        if (rhsType instanceof BottomType) {
            // do nothing
        } else if (rhsType instanceof ConstructorType) {
            ConstructorType constType = (ConstructorType) rhsType;
            Type protoType = constType.getPrototype();
            if (protoType != null) {
                logger.debug("equating {} and {}", protoTerm, protoType);
                solver.equateTypes(solver.factory.getTermForType(protoType), protoTerm, cause);
            }
        } else {
            throw new CoreException("non-constructor flowing unexpectedly", cause);
        }
        return NOT_CHANGED;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + protoTerm.hashCode();
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
        TypeProtoOperator other = (TypeProtoOperator) obj;
        return protoTerm.equals(other.protoTerm);
    }

    @Override
    public String toString() {
        return "TypeProtoOperator [protoTerm=" + protoTerm + "]";
    }

}
