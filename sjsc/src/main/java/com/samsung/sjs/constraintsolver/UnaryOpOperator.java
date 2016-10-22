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

import com.ibm.wala.fixpoint.AbstractOperator;
import com.samsung.sjs.constraintsolver.OperatorModel.UnOpTypeCase;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.BottomType;
import com.samsung.sjs.types.FloatType;
import com.samsung.sjs.types.FunctionType;
import com.samsung.sjs.types.IntegerType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.Types;

public class UnaryOpOperator extends
        AbstractOperator<TypeInfSolverVariable> {

    private final static Logger logger = LoggerFactory.getLogger(UnaryOpOperator.class);

    private final TypeConstraintFixedPointSolver solver;
    private final String operator;
    private final boolean isPrefix;
    private final int lineNumber;
    private final Cause reason;

    public UnaryOpOperator(TypeConstraintFixedPointSolver solver, String operator, boolean isPrefix, int lineNumber, Cause reason) {
        this.operator = operator;
        this.isPrefix = isPrefix;
        this.solver = solver;
        this.lineNumber = lineNumber;
        this.reason = reason;
    }
    @Override
    public byte evaluate(TypeInfSolverVariable lhs,
            TypeInfSolverVariable[] rhs) {
        // TODO Auto-generated method stub
        logger.debug("called on {}", lhs);
        TypeConstraintSolverVariable lhsVar = (TypeConstraintSolverVariable) lhs;
        TypeConstraintSolverVariable rightLo = (TypeConstraintSolverVariable) rhs[0];

        Type origType = lhsVar.getType();
        Type operandType = rightLo.getType();
        if (!(operandType instanceof BottomType)) {
            Cause derivedReason = Cause.derived(lhsVar.reasonForCurrentValue, rightLo.reasonForCurrentValue, reason);
            try {
                UnOpTypeCase matchingCase = solver.operatorModel.getTypeOfUnaryExpression(operator, operandType, isPrefix);
                addUpperBoundConstraint(matchingCase.operandType, rightLo.getOrigTerm(), derivedReason);
                Type resultType = matchingCase.resultType;
                if (!Types.isEqual(origType, resultType)) {
                    logger.debug("setting type to {}", resultType);
                    lhsVar.setType(resultType, derivedReason);
                    return CHANGED;
                }
            } catch (IllegalArgumentException e) {
                throw new CoreException(e.getMessage() + " (line " + lineNumber + ")", derivedReason);
            }
        }
        return NOT_CHANGED;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isPrefix ? 1231 : 1237);
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
        UnaryOpOperator other = (UnaryOpOperator) obj;
        if (isPrefix != other.isPrefix)
            return false;
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
        return "UnaryOpOperator [operator=" + operator + ", isPrefix="
                + isPrefix + ", lineNumber=" + lineNumber + "]";
    }


    private void addUpperBoundConstraint(Type type, ITypeTerm term, Cause reason) {
        if (type instanceof FunctionType || type instanceof ArrayType) {
            // don't bother
            return;
        }
        if (type instanceof IntegerType) {
            solver.addSubtypeConstraints(term, solver.factory.getTermForType(FloatType.make()), true, reason);
        } else {
            solver.addSubtypeConstraints(term, solver.factory.getTermForType(type), true, reason);
        }
    }


}
