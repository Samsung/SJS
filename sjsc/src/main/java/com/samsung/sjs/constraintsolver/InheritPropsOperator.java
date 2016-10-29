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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.fixpoint.AbstractOperator;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.BottomType;
import com.samsung.sjs.types.Type;

/**
 * Propagates properties from prototype parent to child.  Also equates property
 * types between parent and child.
 * @author m.sridharan
 *
 */
public class InheritPropsOperator extends
        AbstractOperator<TypeInfSolverVariable> {

    private static Logger logger = LoggerFactory.getLogger(InheritPropsOperator.class);

    private final TypeConstraintFixedPointSolver solver;
    private final Cause reason;

    public InheritPropsOperator(TypeConstraintFixedPointSolver solver, Cause reason) {
        super();
        this.solver = solver;
        this.reason = reason;
    }

    @Override
    public byte evaluate(TypeInfSolverVariable lhs,
            TypeInfSolverVariable rhs[]) {
        // rhs is the lower bound of the prototype term, lhs is the lower bound of the child term
        Type childType = ((TypeConstraintSolverVariable)rhs[0]).getType();
        TypeConstraintSolverVariable protoParentVar = (TypeConstraintSolverVariable)rhs[1];
        Type protoParentType = protoParentVar.getType();
        logger.debug("INHERIT checking child {} parent {}", lhs, rhs[1]);
        if (protoParentType instanceof BottomType) {
            // do nothing
        } else if (protoParentType instanceof ObjectType) {
            if (childType instanceof BottomType) {
                // do nothing
            } else if (childType instanceof ObjectType) {
                ObjectType childObj = (ObjectType) childType;
                ObjectType parentObj = (ObjectType) protoParentType;
                // add all parent properties and parent inherited properties
                List<Property> propsToAdd = parentObj.properties().stream()
                        .filter((p) -> {
                            return shouldAddInheritedProperty(p, childObj);
                        })
                        .map((p) -> {
                            return p.isRO()
                                    ? p : new Property(p.getName(), p.getType(), true);

                        })
                        .collect(Collectors.toList());
                if (!propsToAdd.isEmpty()) {
                    ArrayList<Property> ownProps = new ArrayList<>(childObj.ownProperties());
                    List<Property> inheritedProps = Stream.concat(childObj.inheritedProperties().stream(),
                            propsToAdd.stream()).collect(Collectors.toList());
                    ObjectType newObjType = new ObjectType(childObj.getPrototypeParent(), ownProps, inheritedProps);
                    ((TypeConstraintSolverVariable)lhs).setType(newObjType,
                            Cause.derived(rhs[0].reasonForCurrentValue, rhs[1].reasonForCurrentValue, reason));
                    logger.debug("new object type {}", newObjType);
                    return CHANGED;
                }
                return NOT_CHANGED;
            } else {
                throw new CoreException("unexpected type to have a prototype", Cause.derived(rhs[0].reasonForCurrentValue, rhs[1].reasonForCurrentValue, reason));
            }
        } else {
            throw new CoreException("unexpected type " + protoParentType + " flows to term " + protoParentVar.getOrigTerm() + ", used as a prototype",
                    Cause.derived(rhs[0].reasonForCurrentValue, rhs[1].reasonForCurrentValue, reason));
        }
        return NOT_CHANGED;
    }

    private boolean shouldAddInheritedProperty(Property p, ObjectType childObj) {
        String propName = p.getName();
        if (!childObj.hasInheritedProperty(propName)) {
            if (childObj.hasProperty(propName)) {
                // constrain property types to be the same
                logger.debug("INHERIT equating {} and {}", p.getType(), childObj.getTypeForProperty(propName));
                solver.equateTypes(p.getType(),
                        childObj.getTypeForProperty(propName),
                        reason);
            } else {
                logger.debug("INHERIT adding property {} to {}", p, childObj);
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InheritPropsOperator;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return null;
    }

}
