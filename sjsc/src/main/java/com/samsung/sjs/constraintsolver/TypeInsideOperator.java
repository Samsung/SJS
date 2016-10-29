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
import com.samsung.sjs.constraintsolver.TypeOperatorException.OperatorType;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.PropertyNotFoundException;
import com.samsung.sjs.types.BottomReferenceType;
import com.samsung.sjs.types.TopReferenceType;
import com.samsung.sjs.types.TopType;
import com.samsung.sjs.types.CodeType;
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.FloatType;
import com.samsung.sjs.types.FunctionType;
import com.samsung.sjs.types.IntegerType;
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.MapType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.ObjectUnionType;
import com.samsung.sjs.types.PrimitiveType;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.StringType;
import com.samsung.sjs.types.BottomType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.TypeVar;
import com.samsung.sjs.types.Types;
import com.samsung.sjs.types.UnknownIndexableType;

/**
 * Created by schandra on 3/20/15.
 */
public class TypeInsideOperator extends AbstractOperator<TypeInfSolverVariable> {

    private final static Logger logger = LoggerFactory.getLogger(TypeInsideOperator.class);
    private final TypeConstraintFixedPointSolver solver;
    /**
     * the term inducing this operator
     */
    private final ITypeTerm term;
    private final Cause reason;

    private TypeInsideOperator(TypeConstraintFixedPointSolver solver, ITypeTerm term, Cause reason) {
        this.solver = solver;
        this.term = term;
        this.reason = reason;
    }

    public static TypeInsideOperator make(TypeConstraintFixedPointSolver solver, ITypeTerm term, Cause reason) {
        return new TypeInsideOperator(solver, term, reason);
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
        TypeInsideOperator other = (TypeInsideOperator) obj;
        if (term == null) {
            if (other.term != null)
                return false;
        } else if (!term.equals(other.term))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "INSIDE";
    }

    private void fail(Type lowerTy, Type upperTy, TypeConstraintSolverVariable lowerVar, TypeConstraintSolverVariable upperVar) {
        ITypeTerm firstSource = lowerVar.getTermForType();
        ITypeTerm secondSource = upperVar.getTermForType();
        throw new TypeOperatorException("type " + lowerTy + " flows into "
                + term + ", which is used as type " + upperTy, firstSource,
                secondSource, term, OperatorType.INSIDE,
                Cause.derived(lowerVar.reasonForCurrentValue, upperVar.reasonForCurrentValue, reason));
    }

    @Override
    public byte evaluate(TypeInfSolverVariable lhs, TypeInfSolverVariable[] rhs) {
        // lower bound is in rhs[0], upper bound in rhs[1]
        TypeConstraintSolverVariable lowerBoundVar = (TypeConstraintSolverVariable)rhs[0];
        Type lowerTy = lowerBoundVar.getType();
        TypeConstraintSolverVariable upperBoundVar = (TypeConstraintSolverVariable)rhs[1];
        Type upperTy = upperBoundVar.getType();
        MROMRWVariable mroMRWVar = (MROMRWVariable) rhs[2];
        logger.debug("INSIDE called on {} , {} MRO/MRW {}", rhs[0], rhs[1], rhs[2]);

        Cause derivedReason = Cause.derived(
                lowerBoundVar.reasonForCurrentValue,
                upperBoundVar.reasonForCurrentValue,
                mroMRWVar.reasonForCurrentValue,
                reason);

        if (Types.isEqual(lowerTy, upperTy) || lowerTy instanceof BottomType || upperTy instanceof TopType) {
            // nothing to do here
        } else if (upperTy instanceof TopReferenceType && Types.isRefType(lowerTy)) {
            // nothing to do here
        } else if (lowerTy instanceof BottomReferenceType && Types.isRefType(upperTy)) {
            // nothing to do here
        } else if (lowerTy instanceof IntegerType && upperTy instanceof FloatType) {
            // this is fine; an integer can be implicitly converted to a float
        } else if (lowerTy instanceof ObjectType && upperTy instanceof ObjectType) {

            ObjectType l = (ObjectType) lowerTy;
            ObjectType r = (ObjectType) upperTy;

            insideForObjectTypes(l, r, mroMRWVar, derivedReason);
        } else if (lowerTy instanceof ObjectUnionType && upperTy instanceof ObjectType) {
            ObjectUnionType lowerUnion = (ObjectUnionType) lowerTy;
            ObjectType r = (ObjectType) upperTy;
            for (ObjectType t : lowerUnion.getObjTypes()) {
                insideForObjectTypes(t, r, mroMRWVar, derivedReason);
            }
        } else if (upperTy instanceof UnknownIndexableType) {
            UnknownIndexableType mapOrArrType = (UnknownIndexableType) upperTy;
            Type otherType = lowerTy;
            if (otherType instanceof ArrayType) {
                // constrain the element type of the MapOrArrayType to be the same as that of the ArrayType
                ArrayType arrType = (ArrayType) otherType;
                equateTypes(solver.factory.findOrCreateTypeVariableTerm((TypeVar)arrType.elemType()), solver.factory.findOrCreateTypeVariableTerm((TypeVar)mapOrArrType.elemType()), derivedReason);
                // the key type of the MapOrArrayType must be integer
                equateTypes(solver.factory.findOrCreateTypeVariableTerm((TypeVar) mapOrArrType.keyType()), solver.factory.findOrCreateTypeTerm(IntegerType.make()), derivedReason);
                solver.equateObjAndArrayOrStringType(mapOrArrType, arrType, derivedReason);
            } else if (otherType instanceof MapType) {
                // constrain the element type of the MapOrArrayType to be the same as that of the MapType
                MapType mapType = (MapType) otherType;
                equateTypes(solver.factory.findOrCreateTypeVariableTerm((TypeVar)mapType.elemType()), solver.factory.findOrCreateTypeVariableTerm((TypeVar)mapOrArrType.elemType()), derivedReason);
                // the key type of the MapOrArrayType must be string
                equateTypes(solver.factory.findOrCreateTypeVariableTerm((TypeVar) mapOrArrType.keyType()), solver.factory.findOrCreateTypeTerm(StringType.make()), derivedReason);
            } else if (otherType instanceof StringType) {
                solver.equateTypes(mapOrArrType.keyType(), IntegerType.make(), derivedReason);
                solver.equateTypes(mapOrArrType.elemType(), StringType.make(), derivedReason);
                solver.equateObjAndArrayOrStringType(mapOrArrType, (StringType)otherType, derivedReason);
            } else {
                fail(lowerTy, upperTy, lowerBoundVar, upperBoundVar);
            }
        } else if (lowerTy instanceof PrimitiveType && upperTy instanceof ObjectType) {
            logger.debug("INSIDE: equate {} to {}", lowerTy, upperTy);
            try {
                solver.equateObjAndPrimType((ObjectType)upperTy, (PrimitiveType)lowerTy, derivedReason);
            } catch (PropertyNotFoundException e) {
                fail(lowerTy, upperTy, lowerBoundVar, upperBoundVar);
            }
        } else if (lowerTy instanceof ConstructorType && upperTy instanceof ObjectType) {
            // this can only occur of upperTy has one property named "prototype"
            ObjectType objType = (ObjectType) upperTy;
            List<Property> properties = objType.properties();
            if (properties.size() != 1) {
                throw new CoreException("cannot inside object type " + objType + " with constructor type " + lowerTy,
                        Cause.derived(lowerBoundVar.reasonForCurrentValue, upperBoundVar.reasonForCurrentValue, derivedReason));
            }
            Property p = properties.get(0);
            if (!p.getName().equals("prototype")) {
                throw new CoreException("cannot inside object type " + objType + " with constructor type " + lowerTy,
                        Cause.derived(lowerBoundVar.reasonForCurrentValue, upperBoundVar.reasonForCurrentValue, derivedReason));
            }
            ConstructorType consType = (ConstructorType) lowerTy;
            solver.equateTypes(p.getType(), consType.getPrototype(), derivedReason);
        } else if (lowerTy instanceof CodeType && upperTy instanceof CodeType) {
            // for now, just equate the function types, since we're not yet
            // handling function subtyping
            logger.debug("INSIDE: equate {} to {}", lowerTy, upperTy);
            solver.equateFunctionTypes((CodeType)lowerTy, (CodeType)upperTy, derivedReason);
        } else if (lowerTy instanceof ArrayType) {
            ArrayType lowerArr = (ArrayType)lowerTy;
            if (upperTy instanceof ObjectType) {
                // in this case, every property in the upper type should already be on the lower type
                // for each such property, add an equality constraint
                ObjectType upperObj = (ObjectType)upperTy;
                solver.equateObjAndArrayOrStringType(upperObj, lowerArr, derivedReason);
            } else if (upperTy instanceof ArrayType) {
                // equate the element types
                equateTypes(solver.factory.getTermForType(lowerArr.elemType()), solver.factory.getTermForType(((ArrayType) upperTy).elemType()), derivedReason);
            } else {
                fail(lowerTy, upperTy, lowerBoundVar, upperBoundVar);
            }
        } else if (upperTy instanceof IntersectionType) {
            if (lowerTy instanceof CodeType) {
                equateFunWithIntersection((CodeType)lowerTy, (IntersectionType)upperTy, derivedReason);
            } else if (lowerTy instanceof IntersectionType) {
                for (Type t : ((IntersectionType)lowerTy).getTypes()) {
                    equateTypeWithIntersection(t, (IntersectionType)upperTy, mroMRWVar, derivedReason);
                }
            } else {
                fail(lowerTy, upperTy, lowerBoundVar, upperBoundVar);
            }
        } else if (lowerTy instanceof IntersectionType) {
            if (upperTy instanceof CodeType) {
                equateFunWithIntersection((CodeType)upperTy, (IntersectionType)lowerTy, derivedReason);
            } else {
                fail(lowerTy, upperTy, lowerBoundVar, upperBoundVar);
            }
        } else if (upperTy instanceof MapType) {
            if (lowerTy instanceof MapType) {
                equateTypes(solver.factory.getTermForType(((MapType) lowerTy)
                        .elemType()),
                        solver.factory.getTermForType(((MapType) upperTy)
                                .elemType()), derivedReason);
            } else {
                fail(lowerTy, upperTy, lowerBoundVar, upperBoundVar);
            }
        } else {
            fail(lowerTy, upperTy, lowerBoundVar, upperBoundVar);
        }
        return FixedPointConstants.NOT_CHANGED;
    }

    private void insideForObjectTypes(ObjectType lower, ObjectType upper, MROMRWVariable mroMRWVar, Cause reason) {
        // for any property of upper present on lower, equate the property types
        for (Property p : upper.properties()) {
            String name = p.getName();
            Type rty = upper.getTypeForProperty(name);
            if (lower.hasProperty(name)) {
                Type lty = lower.getTypeForProperty(name);
                logger.debug("INSIDE: equate {} to {} for property {}", lty, rty, name);
                solver.equateTypes(lty, rty, reason);
            }
            ConstraintUtil.equatePropertyTypeWithMROMRWSets(mroMRWVar, p, solver, reason, logger);
        }
        // find any lower property not on upper, and equate with mro/mrw property if needed
        lower.properties().stream().filter((p) -> {
            return !upper.hasProperty(p.getName());
        }).forEach((p)-> {
            ConstraintUtil.equatePropertyTypeWithMROMRWSets(mroMRWVar, p, solver, reason, logger);
        });
    }



    private void equateTypeWithIntersection(Type lowerTy, IntersectionType upperTy, MROMRWVariable mroMRWVar, Cause reason) {
        if (lowerTy instanceof CodeType) {
            equateFunWithIntersection((CodeType) lowerTy, upperTy, reason);
        } else if (lowerTy instanceof ObjectType) {
            ObjectType lowerObj = (ObjectType) lowerTy;
            ObjectType upperObj = upperTy.findObjectType();
            if (upperObj != null) {
                insideForObjectTypes(lowerObj, upperObj, mroMRWVar, Cause.derived(mroMRWVar.reasonForCurrentValue, reason));
            }
        }
    }
    private void equateFunWithIntersection(CodeType lowerTy,
            IntersectionType upperTy, Cause reason) {
        FunctionType upperFunType = upperTy.findFunctionType(lowerTy.nrParams());
        if (upperFunType != null) {
            List<Type> lowerParamTypes = lowerTy.paramTypes();
            List<Type> upperParamTypes = upperFunType.paramTypes();
            for (int i = 0; i < lowerTy.nrParams(); i++) {
                Type lowerParamType = lowerParamTypes.get(i);
                Type upperParamType = upperParamTypes.get(i);
                equateTypes(solver.factory.getTermForType(lowerParamType), solver.factory.getTermForType(upperParamType), reason);
            }
            equateTypes(solver.factory.getTermForType(lowerTy.returnType()), solver.factory.getTermForType(upperFunType.returnType()), reason);
        }
    }

    private void equateTypes(ITypeTerm lty, ITypeTerm rty, Cause reason) {
        logger.debug("INSIDE: equate {} to {}", lty, rty);

        solver.equateTypes(lty, rty, reason);
    }
}
