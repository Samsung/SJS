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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.util.collections.Pair;
import com.samsung.sjs.constraintsolver.TypeOperatorException.OperatorType;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.typeconstraints.TypeConstantTerm;
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.TopReferenceType;
import com.samsung.sjs.types.TopType;
import com.samsung.sjs.types.CodeType;
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.FloatType;
import com.samsung.sjs.types.IndexableType;
import com.samsung.sjs.types.IntegerType;
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.PrimitiveType;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.PropertyNotFoundException;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.Types;
import com.samsung.sjs.types.UnknownIndexableType;

/**
 * Created by schandra on 3/17/15.
 */
public class TypeMeetOperator extends UnaryOperator<TypeInfSolverVariable> {

    private static Logger logger = LoggerFactory.getLogger(TypeMeetOperator.class);

    private TypeConstraintFixedPointSolver solver;

    private final BiConsumer<Type, Type> typeEquator;

    /**
     * the term corresponding to the left-hand side of the meet operation,
     * i.e., the type being updated
     */
    private final ITypeTerm targetTerm;

    private final Cause reason;

    public TypeMeetOperator(TypeConstraintFixedPointSolver solver, ITypeTerm targetTerm, Cause reason) {
        this.solver = solver;
        typeEquator = (Type ltyVar, Type rtyVar) -> {
            logger.debug("equate {} to {}", ltyVar, rtyVar);
            solver.equateTypes(ltyVar, rtyVar, reason);
        };
        this.targetTerm = targetTerm;
        this.reason = reason;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((targetTerm == null) ? 0 : targetTerm.hashCode());
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
        TypeMeetOperator other = (TypeMeetOperator) obj;
        if (targetTerm == null) {
            if (other.targetTerm != null)
                return false;
        } else if (!targetTerm.equals(other.targetTerm))
            return false;
        return true;
    }


    @Override
    public String toString() {
        return "MEET [targetTerm=" + targetTerm + "]";
    }

    /**
     * meet two object types.
     * @param lTy
     * @param rTy
     * @return <O,b> where O is the meeted type, and b is true if this type differs from lTy.  If b is false, O is null.
     */
    private Pair<ObjectType,Boolean> meetObjectTypes(ObjectType lTy, ObjectType rTy) {
        boolean changed = false;
        List<Property> props = new ArrayList<Property>();
        for (Property p : rTy.properties()) {
            String propName = p.getName();
            if (lTy.hasProperty(propName)) {

                logger.debug("Common property: {}", propName);

                Property lProp = lTy.getProperty(propName);
                Property rProp = rTy.getProperty(propName);
                typeEquator.accept(lProp.getType(), rProp.getType());
                if (lProp.isRO() && rProp.isRW()) {
                    props.add(new Property(lProp.getName(), lProp.getType(), false, rProp.getSourceLoc()));
                    changed = true;
                } else {
                    props.add(lProp);
                }

            } else {
                logger.debug("Adding property: {}", propName);
                props.add(p);
                changed = true;
            }
        }
        if (changed) {
            // create a fresh ObjectType for the meet result
            // first we need to add the lTy properties that are not on rTy
            List<Property> lTyPropsToKeep = lTy.properties().stream()
                    .filter((p) -> {
                        return !rTy.hasProperty(p.getName());
                    }).collect(Collectors.toList());
            props.addAll(lTyPropsToKeep);
            ObjectType meetResult = new ObjectType(props);
            return Pair.make(meetResult,changed);
        }
        return Pair.make(null,changed);

    }

    private boolean meetIsectAndObjType(IntersectionType isectType,
            ObjectType objType) {
        ObjectType lhsObj = isectType.findObjectType();
        List<Type> isectTypes = isectType.getTypes();
        if (lhsObj != null) {
            Pair<ObjectType, Boolean> res = meetObjectTypes(lhsObj, objType);
            if (res.snd) {
                // need to replace object type in intersection
                for (int i = 0; i < isectTypes.size(); i++) {
                    if (isectTypes.get(i).equals(lhsObj)) {
                        isectTypes.set(i, res.fst);
                        break;
                    }
                }
                return true;
            }
            return false;
            // UGH.  need to write test for when something is used both as an object and a function.  sigh.
        } else {
            // add object type as a case
            isectTypes.add(objType);
            // we changed the type, so return true
            return true;
        }
    }




    @Override
    public byte evaluate(TypeInfSolverVariable lhsVar, TypeInfSolverVariable rhsVar) {
        TypeConstraintSolverVariable lhs = (TypeConstraintSolverVariable) lhsVar;
        TypeConstraintSolverVariable rhs = (TypeConstraintSolverVariable) rhsVar;
        if (lhs == null) {
            throw new IllegalArgumentException("lhs == null");
        }
        Type old = lhs.getType();
        boolean changed = false;

        logger.debug("called on {} and {}", lhs, rhs); // lhs = lhs \/ rhs
        Type rhsType = rhs.getType();
        if (old == rhsType) {
            // do nothing
        } else if (old instanceof TopType) {
            ITypeTerm termForType = rhs.getTermForType();
            if (termForType instanceof TypeConstantTerm) {
                // the type constant term is not useful for diagnosis.  Use the expression
                // term representing the constant value
                if (lhs.getOrigTerm().getNode() != null) {
                    termForType = lhs.getOrigTerm();
                }
            }
            lhs.setTypeAndTerm(rhsType, termForType,
                    Cause.derived(lhs.reasonForCurrentValue, rhs.reasonForCurrentValue, reason));
            changed = true;
        } else if (rhsType instanceof TopType) {
            // do nothing
        } else {
            changed = meetType(lhs, rhs);
        }

        logger.debug("Changed = {}, result: lhs is now {}", (changed ? "Y"
                : "N"), lhs);
        return changed ? FixedPointConstants.CHANGED : FixedPointConstants.NOT_CHANGED;
    }

    private boolean fail(String message, TypeConstraintSolverVariable lhsVar, TypeConstraintSolverVariable rhsVar) throws TypeOperatorException {
        throw new TypeOperatorException(
                message + ": " + lhsVar
                        + " and " + rhsVar, lhsVar.getTermForType(),
                rhsVar.getTermForType(), targetTerm, OperatorType.MEET,
                Cause.derived(lhsVar.reasonForCurrentValue, rhsVar.reasonForCurrentValue, reason));
    }

    /**
     * TODO remove duplication between this code and {@link TypeJoinOperator#joinType(Type, Type)}
     * @param lhs
     * @param rhs
     * @return
     */
    private boolean meetType(TypeConstraintSolverVariable lhsVar, TypeConstraintSolverVariable rhsVar) { // potentially destructive update inside lhs
        Cause derivedReason = Cause.derived(lhsVar.reasonForCurrentValue, rhsVar.reasonForCurrentValue, reason);
        Type lhs = lhsVar.getType();
        Type rhs = rhsVar.getType();
        if (lhs instanceof ObjectType && rhs instanceof ObjectType) {
            ObjectType lTy = (ObjectType) lhs;
            ObjectType rTy = (ObjectType) rhs;
            Pair<ObjectType, Boolean> res = meetObjectTypes(lTy, rTy);
            if (res.snd) {
                lhsVar.setType(res.fst, derivedReason);
            }
            return res.snd;
        } else if (lhs instanceof IndexableType && rhs instanceof IndexableType) {
            // either the types need to be the same, or one type should be
            // unknownindexable
            if (!(lhs.getClass().equals(rhs.getClass())
                    || lhs instanceof UnknownIndexableType || rhs instanceof UnknownIndexableType)) {
                return fail("MEET not defined on indexable types", lhsVar, rhsVar);
            }
            // unify key and elem types
            IndexableType lhsInd = (IndexableType) lhs;
            IndexableType rhsInd = (IndexableType) rhs;
            typeEquator.accept(lhsInd.keyType(), rhsInd.keyType());
            typeEquator.accept(lhsInd.elemType(), rhsInd.elemType());
            // update lhs type if it is unknown and the other is not
            if (lhs instanceof UnknownIndexableType && !(rhs instanceof UnknownIndexableType)) {
                lhsVar.setType(rhs, derivedReason);
                return true;
            } else {
                return false;
            }
        } else if (lhs instanceof UnknownIndexableType && rhs instanceof ObjectType) {
            UnknownIndexableType newType = meetUnknownIndexableAndObj((UnknownIndexableType)lhs, (ObjectType)rhs);
            boolean changed = newType != lhs;
            if (changed) {
                lhsVar.setType(newType, reason);
            }
            return changed;
        } else if (lhs instanceof ObjectType && rhs instanceof UnknownIndexableType) {
            rhs = meetUnknownIndexableAndObj((UnknownIndexableType)rhs, (ObjectType)lhs);
            lhsVar.setType(rhs, reason);
            return true;
        } else if (lhs instanceof IntersectionType && rhs instanceof ObjectType) {
            // exactly *one* case of the intersection type should be an object type
            // if so, meet on that type
            IntersectionType lhsIsect = (IntersectionType) lhs;
            ObjectType rhsObj = (ObjectType) rhs;
            return meetIsectAndObjType(lhsIsect, rhsObj);
        } else if (lhs instanceof ObjectType && rhs instanceof IntersectionType) {
            IntersectionType isect = (IntersectionType) rhs;
            ObjectType obj = (ObjectType) lhs;
            meetIsectAndObjType(isect, obj);
            // update result to intersection type
            lhsVar.setType(isect, reason);
            return true;
        } else if (lhs instanceof ObjectType && rhs instanceof ArrayType) {
            solver.equateObjAndArrayOrStringType((ObjectType)lhs, (ArrayType) rhs, derivedReason);
            lhsVar.setType(rhs, derivedReason);
            return true;
        } else if (lhs instanceof ArrayType && rhs instanceof ObjectType) {
            solver.equateObjAndArrayOrStringType((ObjectType)rhs, (ArrayType) lhs, derivedReason);
            return false;
        } else if (lhs instanceof PrimitiveType && rhs instanceof ObjectType) {
            try {
                solver.equateObjAndPrimType((ObjectType) rhs, (PrimitiveType) lhs, derivedReason);
                return false;
            } catch (CoreException e) {
                return fail("MEET not defined on types", lhsVar, rhsVar);
            }
        } else if (lhs instanceof ObjectType && rhs instanceof PrimitiveType) {
        	try {
                solver.equateObjAndPrimType((ObjectType) lhs, (PrimitiveType) rhs, derivedReason);
                lhsVar.setType(rhs, derivedReason);
                return true;        		
        	} catch (CoreException e) {
        		return fail("MEET not defined on types", lhsVar, rhsVar);
        	}
        } else if (lhs instanceof CodeType && rhs instanceof CodeType) {
            logger.debug("equate {} to {}", lhs, rhs);
            boolean sameArity = solver.equateFunctionTypes((CodeType)lhs, (CodeType)rhs, derivedReason);
            if (!sameArity) {
                // we need to create a new intersection type here, and update the LHS
                IntersectionType isectType = new IntersectionType(lhs, rhs);
                lhsVar.setType(isectType, derivedReason);
                return true;

            }
            return false;
        } else if (lhs instanceof CodeType && rhs instanceof IntersectionType) {
            meetTypeAndIntersection((CodeType)lhs, (IntersectionType)rhs, derivedReason);
            lhsVar.setType(rhs, derivedReason);
            return true;
        } else if (lhs instanceof IntersectionType && rhs instanceof CodeType) {
            return meetTypeAndIntersection((CodeType)rhs, (IntersectionType)lhs, derivedReason);
        } else if (lhs instanceof IntersectionType && rhs instanceof IntersectionType) {
            // TODO optimize this
            IntersectionType result = (IntersectionType)lhs;
            boolean changed = false;
            IntersectionType newIsectType = (IntersectionType)rhs;
            for (Type t : newIsectType.getTypes()) {
                changed = changed || meetTypeAndIntersection(t, result, derivedReason);
            }
            return changed;
        } else if (lhs instanceof CodeType && rhs instanceof ObjectType) {
            CodeType codeType = (CodeType) lhs;
            ObjectType objType = (ObjectType) rhs;
            Type newType = meetCodeAndObjTypes(codeType,objType, derivedReason);
            if (newType != codeType) {
                lhsVar.setType(newType, derivedReason);
                return true;
            }
            return false;
        } else if (lhs instanceof ObjectType && rhs instanceof CodeType) {
            CodeType codeType = (CodeType) rhs;
            ObjectType objType = (ObjectType) lhs;
            Type newType = meetCodeAndObjTypes(codeType,objType, derivedReason);
            if (newType != objType) {
                lhsVar.setType(newType, derivedReason);
                return true;
            }
            return false;
        } else if ((lhs instanceof FloatType && rhs instanceof IntegerType)
                || (rhs instanceof FloatType && lhs instanceof IntegerType)) {
            // meet of integer and float is integer
            IntegerType newType = (IntegerType)((lhs instanceof IntegerType) ? lhs : rhs);
            lhsVar.setType(newType, derivedReason);
            return true;
        } else if (Types.isEqual(lhs,rhs)) {
            // do nothing
            return false;
        } else if (lhs instanceof TopReferenceType && Types.isRefType(rhs)) {
            lhsVar.setType(rhs, derivedReason);
            return true;
        } else if (rhs instanceof TopReferenceType && Types.isRefType(lhs)) {
            return false;
        } else {
            return fail("MEET not defined on types", lhsVar, rhsVar);
        }
    }

    private Type meetCodeAndObjTypes(CodeType codeType, ObjectType objType, Cause reason) {
        if (codeType instanceof ConstructorType) {
            // NOTE we assume that constructor types cannot appear inside intersection types for now
            // so, in this case, the *only* property allowed on objType should be named 'prototype'
            List<Property> properties = objType.properties();
            if (properties.size() != 1) {
                throw new CoreException("cannot meet object type " + objType + " with constructor type " + codeType, reason);
            }
            Property p = properties.get(0);
            if (!p.getName().equals("prototype")) {
                throw new CoreException("cannot meet object type " + objType + " with constructor type " + codeType, reason);
            }
            ConstructorType consType = (ConstructorType) codeType;
            solver.equateTypes(p.getType(), consType.getPrototype(), reason);
            return codeType;
        } else {
            // create a fresh intersection type
            IntersectionType isectType = new IntersectionType(codeType, objType);
            return isectType;
        }
    }

    private boolean meetTypeAndIntersection(Type t, IntersectionType isectType, Cause reason) {
        if (t instanceof CodeType) {
            return meetFunTypeAndIntersection((CodeType) t, isectType);
        } else if (t instanceof ObjectType) {
            return meetIsectAndObjType(isectType, (ObjectType) t);
        } else {
            throw new CoreException("unexpected type " + t + " in intersection", reason);
        }
    }

    private boolean meetFunTypeAndIntersection(CodeType funType,
            IntersectionType isectType) {
        List<Type> paramTypes = funType.paramTypes();
        Type returnType = funType.returnType();
        int arity = paramTypes.size();
        CodeType existing = isectType.findFunctionType(arity);
        if (existing == null) {
            existing = isectType.findMethodType(arity);
            if (existing == null) {
                existing = isectType.findConstructorType(arity);
            }
        }
        if (existing != null) {
            // unify type variables.
            List<Type> existingParamTypes = existing.paramTypes();
            for (int i = 0; i < arity; i++) {
                Type newParamType = paramTypes.get(i);
                Type existingParamType = existingParamTypes.get(i);
                // generate an equality constraint
                logger.debug("equating {} and {}", existingParamType, newParamType);
                solver.equateTypes(
                        solver.factory.getTermForType(existingParamType),
                        solver.factory.getTermForType(newParamType),
                        reason);
            }
            Type existingReturnType = existing.returnType();
            logger.debug("equating {} and {}", existingReturnType, returnType);
            solver.equateTypes(
                    solver.factory.getTermForType(existingReturnType),
                    solver.factory.getTermForType(returnType),
                    reason);
            return false;
        } else {
            // we have a new case.  mutate the intersection type
            List<Type> extantTypes = isectType.getTypes();
            extantTypes.add(funType);
            return true;
        }
    }

    private UnknownIndexableType meetUnknownIndexableAndObj(UnknownIndexableType unknownIndType, ObjectType objType) {
        // in this case, we have an array or a string, so constrain the key type to be an integer
        solver.equateTypes(solver.factory.getTermForType(unknownIndType.keyType()), solver.factory.getTermForType(IntegerType.make()), reason);
        // add the properties to the indexable type.  if we added anything, indicate that something has changed
        boolean changed = false;
        List<Property> unknownIndProps = unknownIndType.properties();
        for (Property p: objType.properties()) {
            if (unknownIndType.hasProperty(p.getName())) {
                // constrain types to be equal
                solver.equateTypes(p.getType(), unknownIndType.getTypeForProperty(p.getName()), reason);
            } else {
                unknownIndProps.add(p);
                changed = true;
            }
        }
        return changed ? new UnknownIndexableType(unknownIndType.keyType(),
                unknownIndType.elemType(), unknownIndProps) : unknownIndType;
    }
}
