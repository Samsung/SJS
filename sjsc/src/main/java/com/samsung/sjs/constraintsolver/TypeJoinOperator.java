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
import java.util.Set;
import java.util.function.BiConsumer;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.PropertyGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.samsung.sjs.constraintgenerator.ConstraintGenUtil;
import com.samsung.sjs.constraintsolver.TypeOperatorException.OperatorType;
import com.samsung.sjs.typeconstraints.ExpressionTerm;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.typeconstraints.ObjectLiteralTerm;
import com.samsung.sjs.typeconstraints.PropertyAccessTerm;
import com.samsung.sjs.typeconstraints.ThisTerm;
import com.samsung.sjs.typeconstraints.TypeConstantTerm;
import com.samsung.sjs.types.AbstractMethodType;
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.AttachedMethodType;
import com.samsung.sjs.types.BottomType;
import com.samsung.sjs.types.CodeType;
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.FloatType;
import com.samsung.sjs.types.FunctionType;
import com.samsung.sjs.types.IntegerType;
import com.samsung.sjs.types.MapType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.ObjectUnionType;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.BottomReferenceType;
import com.samsung.sjs.types.TopReferenceType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.TypeVar;
import com.samsung.sjs.types.Types;
import com.samsung.sjs.types.UnattachedMethodType;
import com.samsung.sjs.types.UnknownIndexableType;
import com.samsung.sjs.types.VoidType;

/**
 * Created by schandra on 3/17/15.
 */
public class TypeJoinOperator extends UnaryOperator<TypeInfSolverVariable> {

    private static Logger logger = LoggerFactory.getLogger(TypeJoinOperator.class);

    private final TypeConstraintFixedPointSolver solver;

    /**
     * the term corresponding to the left-hand side of the join operation,
     * i.e., the type being updated
     */
    private final ITypeTerm targetTerm;

    private final Cause reason;

    public TypeJoinOperator(TypeConstraintFixedPointSolver solver, ITypeTerm targetTerm, Cause reason) {
        this.solver = solver;
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
        TypeJoinOperator other = (TypeJoinOperator) obj;
        if (targetTerm == null) {
            if (other.targetTerm != null)
                return false;
        } else if (!targetTerm.equals(other.targetTerm))
            return false;
        return true;
    }


    @Override
    public String toString() {
        return "TypeJoinOperator [targetTerm=" + targetTerm + "]";
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
        logger.debug("called on {} and {}", lhs, rhs);
        Type rhsType = rhs.getType();
        if (rhsType instanceof TopReferenceType) {
            System.err.println("doh");
        }
        if (Types.isEqual(old, rhsType)) {
            // do nothing
        } else if (old instanceof BottomType) {
            // initialize to the RHS type
            ITypeTerm termForType = rhs.getTermForType();
            if (termForType instanceof TypeConstantTerm) {
                // the type constant term is not useful for diagnosis. Use the
                // expression
                // term representing the constant value
                termForType = lhs.getOrigTerm();
            }
            if (rhsType instanceof UnattachedMethodType && targetTerm instanceof PropertyAccessTerm) {
                PropertyAccessTerm targetAccess = (PropertyAccessTerm) targetTerm;
                if (isValidMethodUpdateTarget(targetAccess)) {
                    // since we're writing into a property, make the type an
                    // attached method
                    UnattachedMethodType umt = (UnattachedMethodType) rhsType;
                    rhsType = new AttachedMethodType(umt.paramTypes(),
                            umt.paramNames(), umt.returnType());
                } else {
                    // TODO source location!!!
                    AstNode node = targetTerm.getNode();
                    if (node != null) {
                        throw new CoreException("cannot update " + node.toSource() + " with a method (line " + node.getLineno() + ")",
                                Cause.derived(lhs.reasonForCurrentValue, rhs.reasonForCurrentValue, reason));
                    } else {
                        throw new CoreException("cannot update " + targetTerm + " with a method",
                                Cause.derived(lhs.reasonForCurrentValue, rhs.reasonForCurrentValue, reason));
                    }
                }
            }
            lhs.setTypeAndTerm(rhsType, termForType,
                    Cause.derived(lhs.reasonForCurrentValue, rhs.reasonForCurrentValue, reason));
            changed = true;
        } else if (rhsType instanceof BottomType) {
            // do nothing
        } else {
            changed = joinType(lhs, rhs);
        }
        logger.debug("Changed = {}, result: lhs is now {}", (changed?"Y":"N"), lhs);
        return changed ? FixedPointConstants.CHANGED : FixedPointConstants.NOT_CHANGED;
    }

    /**
     * only allow method updates in three cases:
     * 1. property of object literal
     * 2. property of 'this' inside a constructor
     * 3. property 'prototype' of a constructor
     * @param t
     * @return
     */
    private boolean isValidMethodUpdateTarget(PropertyAccessTerm t) {
        ITypeTerm base = t.getBase();
        if (base instanceof ObjectLiteralTerm) {
            return true;
        }
        if (base instanceof ThisTerm) {
            AstNode node = base.getNode();
            return ConstraintGenUtil.isConstructor(ConstraintGenUtil.findEnclosingFunction(node));
        }
        if (base instanceof ExpressionTerm) {
            AstNode node = base.getNode();
            if (node instanceof PropertyGet) {
                PropertyGet pg = (PropertyGet) node;
                return pg.getProperty().getIdentifier().equals("prototype");
            }
        }
        // otherwise, invalid
        return false;
    }

    private boolean fail(String message, TypeConstraintSolverVariable lhsVar, TypeConstraintSolverVariable rhsVar) throws TypeOperatorException {
        throw new TypeOperatorException(
                message + ": " + lhsVar
                        + " and " + rhsVar, lhsVar.getTermForType(),
                rhsVar.getTermForType(), targetTerm, OperatorType.JOIN,
                Cause.derived(lhsVar.reasonForCurrentValue, rhsVar.reasonForCurrentValue, reason));
    }

    /**
     * computes join of types. if there is a change from existing type in
     * lhsVar, updates lhsVar and returns true. otherwise, returns false
     *
     */
    public boolean joinType(TypeConstraintSolverVariable lhsVar, TypeConstraintSolverVariable rhsVar) {
        Type lhs = lhsVar.getType();
        Type rhs = rhsVar.getType();
        Cause derivedReason = Cause.derived(lhsVar.reasonForCurrentValue, rhsVar.reasonForCurrentValue, reason);
        BiConsumer<Type, Type> typeEquator = (Type ltyVar, Type rtyVar) -> {
            logger.debug("equate {} to {}", ltyVar, rtyVar);

            solver.equateTypes(ltyVar, rtyVar, derivedReason);
        };
        if (lhs instanceof ObjectType && rhs instanceof ObjectType) {
            ObjectType lTy = (ObjectType) lhs;
            ObjectType rTy = (ObjectType) rhs;
            TypeVar lProto = (TypeVar) lTy.getPrototypeParent();
            TypeVar rProto = (TypeVar) rTy.getPrototypeParent();
            if (lProto != null && rProto != null
                    && lProto.toString().equals(rProto.toString())) {
                // if both types have the same prototype parent
                assert sameOwnProperties(lTy, rTy);
                Set<Property> rInherited = rTy.inheritedProperties();
                Set<Property> lInherited = lTy.inheritedProperties();
                assert rInherited.containsAll(lInherited)
                        || lInherited.containsAll(rInherited) : "This should not be possible";
                if (!rInherited.equals(lInherited)
                        && rInherited.size() > lInherited.size()) {
                    // update the type
                    lhsVar.setType(rTy, derivedReason);
                    return true;
                } else {
                    // we know lInherited.containsAll(rInherited) from the
                    // assertion above
                    return false;
                }
            } else {
                // defer and create ObjectUnionType.  even if we have
                // no prototype inheritance, we cannot know if the properties
                // will have the same type
                lhsVar.setType(new ObjectUnionType(lTy, rTy), derivedReason);
                return true;
            }
        } else if (lhs instanceof ObjectType && rhs instanceof ObjectUnionType) {
            ObjectUnionType result = joinObjectIntoUnion((ObjectUnionType)rhs, (ObjectType)lhs);
            lhsVar.setType(result, derivedReason);
            return true;
        } else if (lhs instanceof ObjectUnionType && rhs instanceof ObjectType) {
            ObjectUnionType result = joinObjectIntoUnion((ObjectUnionType)lhs, (ObjectType)rhs);
            if (result != lhs) {
                lhsVar.setType(result, derivedReason);
                return true;
            }
            return false;
        } else if (lhs instanceof ObjectUnionType && rhs instanceof ObjectUnionType) {
            ObjectUnionType result = (ObjectUnionType) lhs;
            ObjectUnionType newUnion = (ObjectUnionType) rhs;
            for (ObjectType ot : newUnion) {
                result = joinObjectIntoUnion(result, ot);
            }
            if (result != lhs) {
                lhsVar.setType(result, derivedReason);
                return true;
            }
            return false;
        } else if (lhs instanceof ArrayType && rhs instanceof ArrayType) {
            // equate the element type
            TypeVar ltyElem = (TypeVar) ((ArrayType)lhs).elemType();
            TypeVar rtyElem = (TypeVar) ((ArrayType)rhs).elemType();
            typeEquator.accept(ltyElem, rtyElem);
            // no change
            return false;
        } else if (lhs instanceof MapType && rhs instanceof MapType) {
            // TODO kill code duplication?
            // equate the element type
            TypeVar ltyElem = (TypeVar) ((MapType)lhs).elemType();
            TypeVar rtyElem = (TypeVar) ((MapType)rhs).elemType();
            typeEquator.accept(ltyElem, rtyElem);
            // no change
            return false;
        } else if (lhs instanceof UnknownIndexableType && rhs instanceof UnknownIndexableType) {
            UnknownIndexableType lMapOrArray = (UnknownIndexableType) lhs;
            UnknownIndexableType rMapOrArray = (UnknownIndexableType) rhs;
            typeEquator.accept(lMapOrArray.keyType(), rMapOrArray.keyType());
            typeEquator.accept(lMapOrArray.elemType(), rMapOrArray.elemType());
            // no change
            return false;
        }  else if (lhs instanceof CodeType && rhs instanceof CodeType) {
            logger.debug("equate {} to {}", lhs, rhs);
            boolean success = solver.equateFunctionTypes((CodeType)rhs, (CodeType)lhs, derivedReason);
            if (!success) {
                return fail("could not join function types", lhsVar, rhsVar);
            }
            // if one is a constructor, the other should be too
            if ((lhs instanceof ConstructorType && !(rhs instanceof ConstructorType))
                    || (rhs instanceof ConstructorType && !(lhs instanceof ConstructorType))) {
                return fail("could not join constructor with non-constructor", lhsVar, rhsVar);
            }
            // if lhs is a function and rhs is a method, switch to method type
            if (lhs instanceof FunctionType && rhs instanceof AbstractMethodType) {
                lhsVar.setType(rhs, derivedReason);
                return true;
            }
            return false;
        } else if (lhs instanceof VoidType && rhs instanceof VoidType) {
            // do nothing
            return false;
        } else if ((lhs instanceof FloatType && rhs instanceof IntegerType)
                || (rhs instanceof FloatType && lhs instanceof IntegerType)) {
            FloatType ft = (FloatType)((lhs instanceof FloatType) ? lhs : rhs);
            lhsVar.setType(ft, derivedReason);
            return true;
        } else if (lhs instanceof BottomReferenceType && Types.isRefType(rhs)) {
            lhsVar.setType(rhs, derivedReason);
            return true;
        } else if (rhs instanceof BottomReferenceType && Types.isRefType(lhs)) {
            return false;
        } else {
            // TODO still need to handle joining MapOrArrayType with ArrayType or MapType
            // TODO deal with cases other than object types
            return fail("join not defined on types", lhsVar, rhsVar);
        }
    }

    private boolean sameOwnProperties(ObjectType lTy, ObjectType rTy) {
        List<Property> lOwnProperties = lTy.ownProperties();
        List<Property> rOwnProperties = rTy.ownProperties();
        return (lOwnProperties.size() == rOwnProperties.size()
                && lOwnProperties.containsAll(rOwnProperties));
    }

    private ObjectUnionType joinObjectIntoUnion(ObjectUnionType unionType,
            ObjectType objType) {
        // dumb for now
        // if we find an exact match, return the extant union type
        // otherwise, add another case
        logger.debug("joining {} into {}", objType, unionType);
        TypeVar protoParent = (TypeVar) objType.getPrototypeParent();
        ObjectType matchingCase = null;
        for (ObjectType objCase: unionType) {
            // equate type properties for all cases
            TypeVar objCaseParent = (TypeVar) objCase.getPrototypeParent();
            if (Types.isEqual(objType, objCase)
                    || (protoParent != null && objCaseParent != null && protoParent
                            .toString().equals(objCaseParent.toString()))) {
                assert matchingCase == null;
                matchingCase = objCase;
            }
        }
        if (matchingCase != null) {
            logger.debug("matching case {}", matchingCase);
            if (Types.isEqual(matchingCase, objType)) {
                return unionType;
            } else {
                // we need to copy over any new inherited properties
                Set<Property> objTypeInherited = objType.inheritedProperties();
                Set<Property> matchingCaseInherited = matchingCase.inheritedProperties();
                assert objTypeInherited.containsAll(matchingCaseInherited)
                        || matchingCaseInherited.containsAll(objTypeInherited) : "bad inherited properties "
                        + objTypeInherited + " " + matchingCaseInherited;
                if (!matchingCaseInherited.containsAll(objTypeInherited)) {
                    // update the type
                    return replaceCaseInObjUnionType(unionType, matchingCase, objType);
                } else {
                    return unionType;
                }
            }
        } else {
            // add a new case
            List<ObjectType> newCases = new ArrayList<>(unionType.getObjTypes());
            newCases.add(objType);
            return new ObjectUnionType(newCases);
        }
    }

    private ObjectUnionType replaceCaseInObjUnionType(
            ObjectUnionType unionType, ObjectType caseToReplace,
            ObjectType newCase) {
        List<ObjectType> newCases = new ArrayList<>(unionType.getObjTypes());
        int ind = newCases.indexOf(caseToReplace);
        newCases.set(ind, newCase);
        return new ObjectUnionType(newCases);
    }
}
