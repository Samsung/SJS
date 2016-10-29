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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.fixpoint.AbstractOperator;
import com.ibm.wala.fixpoint.FixedPointConstants;
import com.samsung.sjs.constraintsolver.OperatorModel.InfixOpTypeCase;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.BottomType;
import com.samsung.sjs.types.FunctionType;
import com.samsung.sjs.types.MapType;
import com.samsung.sjs.types.TopType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.TypeVar;
import com.samsung.sjs.types.Types;


/**
 * Created by schandra on 3/20/15.
 */
public class TypeOperatorOperator extends AbstractOperator<TypeInfSolverVariable> implements FixedPointConstants {

    private final static Logger logger = LoggerFactory.getLogger(TypeOperatorOperator.class);

    private TypeConstraintFixedPointSolver solver;
    private String operator;
    private final int lineNumber;
    private final Cause reason;

    private TypeVar mapElemTypeVar = null;

    private TypeOperatorOperator(TypeConstraintFixedPointSolver solver, String operator, int lineNumber, Cause reason) {
        this.operator = operator;
        assert !operator.equals("||");
        this.solver = solver;
        this.lineNumber = lineNumber;
        this.reason = reason;
    }

    public static TypeOperatorOperator make(TypeConstraintFixedPointSolver solver, String operator, int lineNumber, Cause reason) {
        return new TypeOperatorOperator(solver, operator, lineNumber, reason);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + lineNumber;
        result = prime * result
                + ((operator == null) ? 0 : operator.hashCode());
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
        TypeOperatorOperator other = (TypeOperatorOperator) obj;
        if (lineNumber != other.lineNumber)
            return false;
        if (operator == null) {
            if (other.operator != null)
                return false;
        } else if (!operator.equals(other.operator))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TypeOperatorOperator [operator=" + operator + ", lineNumber="
                + lineNumber + "]";
    }

    @Override
    public byte evaluate(TypeInfSolverVariable lhs, TypeInfSolverVariable[] rhs) {
        logger.debug("called on {}", lhs);

        TypeConstraintSolverVariable leftLo = (TypeConstraintSolverVariable) rhs[0];
        TypeConstraintSolverVariable rightLo = (TypeConstraintSolverVariable) rhs[1];
        TypeConstraintSolverVariable opUpper = (TypeConstraintSolverVariable) rhs[2];

        Type origType = ((TypeConstraintSolverVariable) lhs).getType();
        // NOTE for simplicity, we do not use upper bounds here.
        // if we want to do truly bottom-up type inference, we should
        // use the upper bounds, but then we'll have to handle tricky
        // cases like an expression with no lower bound whose upper bound
        // is just { length: X0 }, which could be an object, array, or string.
        Type left = null;
        if (!(leftLo.getType() instanceof BottomType)) {
            left = leftLo.getType();
        }/* else if (!(leftHi.getType() instanceof BottomType)) {
            left = leftHi.getType();
        }*/

        Type right = null;
        if (!(rightLo.getType() instanceof BottomType)) {
            right = rightLo.getType();
        }/* else if (!(rightHi.getType() instanceof BottomType)) {
            right = rightHi.getType();
        }*/

        Type resultType = null;
        if (!(opUpper.getType() instanceof TopType)) {
            resultType = opUpper.getType();
        }

        Cause derivedReason = Cause.derived(
                leftLo.reasonForCurrentValue,
                rightLo.reasonForCurrentValue,
                opUpper.reasonForCurrentValue,
                reason);

        List<InfixOpTypeCase> cases = solver.operatorModel.getInfixCases(operator, left, right, resultType);
        logger.debug("cases[" + operator + " @ " + lineNumber + "] --> " + cases);
        if (cases.size() == 0) {
            throw new CoreException("no possible overloading for " +
                    (left != null ? left : "_") + ' ' + operator + ' ' + (right != null ? right : "_") + " -> " + (resultType != null ? resultType : "_")
                    + " (line " + lineNumber + ')', derivedReason);
        } else {
            Type leftBound = cases.stream().map(c -> c.leftType).reduce(Types::coarseUpperBound).orElseThrow(IllegalStateException::new);
            Type rightBound = cases.stream().map(c -> c.rightType).reduce(Types::coarseUpperBound).orElseThrow(IllegalStateException::new);
            if (rightBound instanceof MapType) {
                // we don't care about the type of the values in the map, so create a fresh type variable
                if (mapElemTypeVar == null) {
                    mapElemTypeVar = solver.factory.freshTypeVar();
                }
                rightBound = new MapType(mapElemTypeVar);
            }
            Type resultBound = cases.stream().map(c -> c.resultType).reduce(Types::coarseLowerBound).orElseThrow(IllegalStateException::new);
            logger.debug("computed case: " + leftBound + ' ' + operator + ' ' + rightBound + " = " + resultBound);

            addSubtypeConstraint(leftLo.getOrigTerm(), leftBound);
            addSubtypeConstraint(rightLo.getOrigTerm(), rightBound);

            return resultBound instanceof BottomType ?
                    NOT_CHANGED :
                    updateLHSTypeIfNeeded(lhs, origType, resultBound, derivedReason);
        }
    }

    private void addSubtypeConstraint(ITypeTerm subTerm, Type superType) {
        if (superType instanceof TopType) {
            return;
        }
        if (superType instanceof ArrayType || superType instanceof FunctionType) {
            // This case is subtle:
            // we don't want to constrain the type to be some fake array or function type
            // use the actual type that we used to infer this type case is the correct one

            // Fortunately, this will never happen! (see operators.json)
            throw new UnsupportedOperationException();
        }
        logger.debug("adding constraint {} <: {}", subTerm, superType);
        solver.addSubtypeConstraints(subTerm, solver.factory.getTermForType(superType), true, reason);
    }

    private byte updateLHSTypeIfNeeded(TypeInfSolverVariable lhs,
            Type origType, Type t, Cause reason) {
        if (!Types.isEqual(origType, t)) {
            logger.debug("setting type to {}", t);
            ((TypeConstraintSolverVariable)lhs).setType(t, reason);
            return FixedPointConstants.CHANGED;
        } else {
            return FixedPointConstants.NOT_CHANGED;
        }
    }
}
