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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NewExpression;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.StringLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.samsung.sjs.BasicSourceLocation;
import com.samsung.sjs.SourceLocation;
import com.samsung.sjs.constraintgenerator.ConstraintFactory;
import com.samsung.sjs.constraintgenerator.ConstraintGenUtil;
import com.samsung.sjs.constraintgenerator.ConstraintGenerator;
import com.samsung.sjs.typeconstraints.ArrayLiteralTerm;
import com.samsung.sjs.typeconstraints.ExpressionTerm;
import com.samsung.sjs.typeconstraints.FunctionCallTerm;
import com.samsung.sjs.typeconstraints.FunctionParamTerm;
import com.samsung.sjs.typeconstraints.FunctionReturnTerm;
import com.samsung.sjs.typeconstraints.IConstraint;
import com.samsung.sjs.typeconstraints.ITypeConstraint;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.typeconstraints.IndexedTerm;
import com.samsung.sjs.typeconstraints.InheritPropsConstraint;
import com.samsung.sjs.typeconstraints.MROMRWConstraint;
import com.samsung.sjs.typeconstraints.MapLiteralTerm;
import com.samsung.sjs.typeconstraints.MethodReceiverTerm;
import com.samsung.sjs.typeconstraints.ObjectLiteralTerm;
import com.samsung.sjs.typeconstraints.PropertyAccessTerm;
import com.samsung.sjs.typeconstraints.ProtoConstraint;
import com.samsung.sjs.typeconstraints.ProtoParentTerm;
import com.samsung.sjs.typeconstraints.ProtoTerm;
import com.samsung.sjs.typeconstraints.SubTypeConstraint;
import com.samsung.sjs.typeconstraints.TypeConstantTerm;
import com.samsung.sjs.typeconstraints.TypeEqualityConstraint;
import com.samsung.sjs.typeconstraints.TypeVariableTerm;
import com.samsung.sjs.typeconstraints.UpperBoundConstraint;
import com.samsung.sjs.types.AnyType;
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.CodeType;
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.FunctionType;
import com.samsung.sjs.types.IntegerType;
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.MapType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.TypeVar;
import com.samsung.sjs.types.UnattachedMethodType;
import com.samsung.sjs.types.UnknownIndexableType;

/**
 * Directional solver for type constraints, based on WALA's constraint solving engine.
 *
 * Created by schandra on 3/11/15.
 */
public class DirectionalConstraintSolver {

    private final Set<ITypeConstraint> constraints;

    private final Set<MROMRWConstraint> mroMRWConstraints = HashSetFactory.make();

    private final TypeConstraintFixedPointSolver fixedpointSolver;

    private final ConstraintFactory factory;

    private final Map<IConstraint,Set<Integer>> sourceMapping;


    @SuppressWarnings("unused")
    private final Map<ITypeTerm,Set<Integer>> termMapping;

    private Cause currentCause;
    public final Map<ITypeConstraint, Cause> causesByConstraint = new LinkedHashMap<>();

    private static Logger logger = LoggerFactory.getLogger(DirectionalConstraintSolver.class);

    public DirectionalConstraintSolver(Set<ITypeConstraint> constraints, ConstraintFactory factory, ConstraintGenerator generator) {
        this.constraints = constraints;
        this.factory = factory;
        this.sourceMapping = HashMapFactory.make(generator.getSourceMapping());
        this.termMapping = HashMapFactory.make(generator.getTermMapping());
        this.fixedpointSolver = new TypeConstraintFixedPointSolver(constraints, causesByConstraint, mroMRWConstraints, factory, sourceMapping);
    }

    public TypeAssignment solve() {
        augmentConstraints();
        try {
            return fixedpointSolver.solve();
        } catch (CancelException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Augment the initial constraints. This should move into the previous passes
     *
     */
    public void augmentConstraints() {
        Set<ITypeConstraint> moreConstraints = new LinkedHashSet<ITypeConstraint>();
        Consumer<IConstraint> constraintAdder = (IConstraint c) -> {
            logger.debug("adding {}", c);
            if (c instanceof ITypeConstraint) {
                moreConstraints.add((ITypeConstraint) c);
                causesByConstraint.put((ITypeConstraint) c, currentCause);
            } else {
                mroMRWConstraints.add((MROMRWConstraint) c);
            }
        };
        // terms for which we have already added the appropriate index type constraints
        Set<ITypeTerm> constrainedIndexTerms = HashSetFactory.make();
        // terms for which we have already added the appropriate prop term constraints
        Map<PropertyAccessTerm,TypeVariableTerm> propTerm2TypeVar = HashMapFactory.make();
        // terms for which we have already added the appropriate function type constraints
        // we need a term *and* an arity, due to intersection types
        Set<Pair<ITypeTerm,Integer>> constrainedFunctionTerms = HashSetFactory.make();
        Set<ProtoTerm> handledProtoTerms = HashSetFactory.make();
        Set<ProtoParentTerm> handledProtoParentTerms = HashSetFactory.make();
        logger.debug("Adding constraints ...");
        // HACK do ProtoTerms first.  We do this since we do not want to automatically
        // generate a prototype variable for all constructors, since some come from
        // the library and are not prototypable
        // TODO clean this up
        for (ITypeConstraint constraint: constraints) {
            currentCause = Cause.src(constraint);
            if (constraint.getLeft() instanceof ProtoTerm) {
                handleProtoTerm((ProtoTerm)constraint.getLeft(), constraintAdder, handledProtoTerms);
            }
            if (constraint.getRight() instanceof ProtoTerm) {
                handleProtoTerm((ProtoTerm)constraint.getRight(), constraintAdder, handledProtoTerms);
            }
        }
        for (ITypeConstraint constraint : constraints) {
            currentCause = Cause.src(constraint);
            logger.debug("Checking constraint {}", constraint);
            if (constraint.getLeft() instanceof ProtoParentTerm) {
                handleProtoParentTerm((ProtoParentTerm)constraint.getLeft(), constraintAdder, handledProtoParentTerms);
            }
            if (constraint.getRight() instanceof ProtoParentTerm) {
                handleProtoParentTerm((ProtoParentTerm)constraint.getRight(), constraintAdder, handledProtoParentTerms);
            }
            if (constraint.getLeft() instanceof IndexedTerm
                    || constraint.getRight() instanceof IndexedTerm) {
                handleIndexedTerm(constraintAdder, constrainedIndexTerms,
                        constrainedFunctionTerms, constraint);
            }
            if (constraint.getLeft() instanceof MapLiteralTerm) {
                handleMapLiteralTerm(constraintAdder, constraint);
            }
            if (constraint.getLeft() instanceof FunctionParamTerm
                    || constraint.getRight() instanceof FunctionParamTerm) {
                handleFunctionParamTerm(constraintAdder,
                        constrainedFunctionTerms, constraint);
            }
            if (constraint.getLeft() instanceof FunctionReturnTerm
                    || constraint.getRight() instanceof FunctionReturnTerm) {
                handleFunctionReturnTerm(constraintAdder,
                        constrainedFunctionTerms, constraint);
            }
            if (constraint.getRight() instanceof MethodReceiverTerm) {
                assert constraint instanceof TypeEqualityConstraint;
                handleReceiverTerm((MethodReceiverTerm) constraint.getRight(), constraintAdder);
            }
            if (constraint.getLeft().getType() instanceof IntersectionType) {
                handleIntersectionType((IntersectionType)constraint.getLeft().getType(), constraintAdder);
            }
            if (constraint.getRight().getType() instanceof IntersectionType) {
                handleIntersectionType((IntersectionType)constraint.getRight().getType(), constraintAdder);
            }
            if (constraint instanceof SubTypeConstraint) {
                handleSubtypeConstraint(constraintAdder, constraint, propTerm2TypeVar);
            }
            if (constraint instanceof TypeEqualityConstraint
                    && ConstraintGenUtil.isNullUndefinedLitOrVoidOp(constraint.getLeft())) {
                // this is a type equality, but it was generated for assigning null/undefined
                // into some location.  for the purposes of augmenting constraints, treat
                // it as a subtype constraint
                handleSubtypeConstraint(constraintAdder, constraint, propTerm2TypeVar);
            }
        }
        for (ITypeConstraint constraint: constraints) {
            currentCause = Cause.src(constraint);
            if (constraint instanceof TypeEqualityConstraint) {
                logger.debug("Re-checking constraint {}", constraint);
                handleTypeEqualityConstraint(constraintAdder, constraint, propTerm2TypeVar);
            }
        }
        constraints.addAll(moreConstraints);
    }

    private void handleReceiverTerm(MethodReceiverTerm receiverTerm,
            Consumer<IConstraint> constraintAdder) {
        UnattachedMethodType methodType = (UnattachedMethodType) receiverTerm.getFunctionTerm().getType();
        TypeVar receiverTypeVar = makeFreshTypeVar();
        methodType.setReceiverType(receiverTypeVar);
        constraintAdder.accept(new TypeEqualityConstraint(receiverTerm, factory.getTermForType(receiverTypeVar)));
    }

    private void handleSubtypeConstraint(
            Consumer<IConstraint> constraintAdder,
            ITypeConstraint constraint, Map<PropertyAccessTerm,TypeVariableTerm> propTerm2TypeVar) {
        /* handle e <= prop(e', foo)
         * we need to introduce a var x for the type of object's foo field, and
         * then x = prop(e', foo)
         */

        // we have some serious code duplication now, but
        // these cases will diverge.
        // TODO extract the common code
        SourceLocation loc = getSourceLoc(sourceMapping.get(constraint));
        if (constraint.getLeft() instanceof PropertyAccessTerm) {
            PropertyAccessTerm propTerm = (PropertyAccessTerm) constraint.getLeft();
            if (!propTerm2TypeVar.containsKey(propTerm)) {
                ITypeTerm baseTerm = propTerm.getBase();
                TypeVar tyVar = makeFreshTypeVar();
                TypeVariableTerm tvTerm =  factory.findOrCreateTypeVariableTerm(tyVar);

                assert !(baseTerm instanceof ObjectLiteralTerm);
                // create a fresh object type as the upper bound
                // here, since the property is being read (since it is on the LHS
                // of the subtype constraint), make the property read-only
                ObjectType ot = new ObjectType(null, Collections.emptyList(),
                        Collections.singletonList(new Property(propTerm
                                .getPropertyName(), tyVar, true, loc)));

                ITypeConstraint c = new SubTypeConstraint(baseTerm,
                        factory.getTermForType(ot));
                constraintAdder.accept(c);

                ITypeConstraint c1 = new SubTypeConstraint(tvTerm, propTerm);
                propTerm2TypeVar.put(propTerm, tvTerm);
                constraintAdder.accept(c1);
            }
        }

        if (constraint.getRight() instanceof PropertyAccessTerm) {
            PropertyAccessTerm propTerm = (PropertyAccessTerm) constraint.getRight();
            ITypeTerm baseTerm = propTerm.getBase();
            if (!propTerm2TypeVar.containsKey(propTerm)) {
                TypeVar tyVar = makeFreshTypeVar();
                TypeVariableTerm tvTerm =  factory.findOrCreateTypeVariableTerm(tyVar);

                if (baseTerm instanceof ObjectLiteralTerm) {
                    ObjectLiteralTerm olTerm = (ObjectLiteralTerm) propTerm.getBase();
                    ObjectType ot = (ObjectType) olTerm.getType();
                    ot.setProperty(propTerm.getPropertyName(), tyVar);
                } else {
                    // create a fresh object type as the upper bound
                    // since this is a write, add the property as RW
                    ObjectType ot = new ObjectType(null,
                            Collections.singletonList(new Property(propTerm
                                    .getPropertyName(), tyVar,
                                    false, loc)),
                            Collections.emptyList());

                    ITypeConstraint c = new SubTypeConstraint(baseTerm,  factory.getTermForType(ot));
                    constraintAdder.accept(c);
                }

                ITypeConstraint c1 = new TypeEqualityConstraint(tvTerm, propTerm);
                propTerm2TypeVar.put(propTerm, tvTerm);
                constraintAdder.accept(c1);
            }

            // generate MRO-MRW upper bound constraint
            ITypeTerm leftTerm = constraint.getLeft();
            if (possiblyAMethodTerm(leftTerm)) {
                constraintAdder.accept(new UpperBoundConstraint(leftTerm, baseTerm, Cause.src(constraint)));
            }
        }

    }


    /**
     * conservative check that returns false only for terms that obviously do not represent methods
     *
     * TODO move this code inside ITypeTerm??
     * @param t
     * @return
     */
    private boolean possiblyAMethodTerm(ITypeTerm t) {
        if (ConstraintGenUtil.isNullUndefinedLitOrVoidOp(t)) {
            return false;
        }
        if (t instanceof ExpressionTerm) {
            ExpressionTerm et = (ExpressionTerm) t;
            AstNode node = et.getNode();
            if (node != null) {
                return !(node instanceof NumberLiteral
                        || node instanceof StringLiteral);
            }
        }
        return !(t instanceof ArrayLiteralTerm
                || t instanceof MapLiteralTerm
                || t instanceof ObjectLiteralTerm
                || t instanceof TypeConstantTerm);
    }

    private SourceLocation getSourceLoc(Set<Integer> sourceLines) {
        int lineNum = -1;
        if (sourceLines != null) {
            assert sourceLines.size() == 1;
            lineNum = sourceLines.iterator().next();
        }
        SourceLocation loc = new BasicSourceLocation(lineNum);
        return loc;
    }

    private void handleTypeEqualityConstraint(
            Consumer<IConstraint> constraintAdder,
            ITypeConstraint constraint, Map<PropertyAccessTerm, TypeVariableTerm> propTerm2TypeVar) {
        /*
         * We only deal with |a.foo| = prop( |a|, foo) here
         *
         * We add
         * |a| <: ObjTy( { foo: X0 } )
         * |a.foo| = X0
         * where X0 is new
         */
        ITypeTerm dotTerm = null;
        PropertyAccessTerm propTerm = null;
        if (constraint.getLeft() instanceof PropertyAccessTerm) {
            dotTerm = constraint.getRight();
            propTerm = (PropertyAccessTerm) constraint.getLeft();
        } else if (constraint.getRight() instanceof PropertyAccessTerm) {
            dotTerm = constraint.getLeft();
            propTerm = (PropertyAccessTerm) constraint.getRight();
        }
        if (propTerm != null) {
            ITypeTerm baseTerm = propTerm.getBase();
            TypeVariableTerm tvTerm = propTerm2TypeVar.get(propTerm);
            if (tvTerm == null) {
                // this occurs when the prop term never appears in a subtype constraint,
                // e.g., for reads of length from arrays.  Since it must be a read, upper
                // bound has a read-only property
                SourceLocation loc = getSourceLoc(sourceMapping.get(constraint));
                TypeVar tyVar = makeFreshTypeVar();
                ObjectType ot = new ObjectType(null, Collections.emptyList(),
                        Collections.singletonList(new Property(propTerm
                                .getPropertyName(), tyVar, true, loc)));
                ITypeConstraint c1 = new SubTypeConstraint(baseTerm, factory.getTermForType(ot));
                constraintAdder.accept(c1);
                ITypeConstraint c2 = new TypeEqualityConstraint(dotTerm, factory.findOrCreateTypeVariableTerm(tyVar));
                constraintAdder.accept(c2);
                if (baseTerm instanceof ObjectLiteralTerm) {
                    // get rid of lingering any types
                    ObjectLiteralTerm baseOLT = (ObjectLiteralTerm) baseTerm;
                    ObjectType baseOLTType = (ObjectType) baseOLT.getType();
                    for (Property p: baseOLTType.properties()) {
                        if (p.getType() instanceof AnyType) {
                            baseOLTType.setProperty(p.getName(), makeFreshTypeVar(), p.isRO());
                        }
                    }
                }
            }
        }
    }

    private void handleFunctionReturnTerm(
            Consumer<IConstraint> constraintAdder,
            Set<Pair<ITypeTerm, Integer>> constrainedFunctionTerms,
            ITypeConstraint constraint) {
        FunctionReturnTerm returnTerm = (FunctionReturnTerm)(constraint.getLeft() instanceof FunctionReturnTerm ? constraint.getLeft() : constraint.getRight());
        ITypeTerm otherTerm = constraint.getLeft() instanceof FunctionReturnTerm ? constraint.getRight() : constraint.getLeft();
        ITypeTerm functionTerm = returnTerm.getFunctionTerm();
        int nrParams = returnTerm.getNrParams();
        // TODO make sure we handle constructors with parameters
        boolean isConstructorCall = otherTerm instanceof FunctionCallTerm &&
                ((FunctionCallTerm)otherTerm).getFunctionCall() instanceof NewExpression;
        doConstraintsForFunctionTerm(constraintAdder, constrainedFunctionTerms, functionTerm, nrParams, isConstructorCall);
    }

    private void handleFunctionParamTerm(
            Consumer<IConstraint> constraintAdder,
            Set<Pair<ITypeTerm, Integer>> constrainedFunctionTerms,
            ITypeConstraint constraint) {
        FunctionParamTerm paramTerm = (FunctionParamTerm)(constraint.getLeft() instanceof FunctionParamTerm ? constraint.getLeft() : constraint.getRight());
        // create type variables for return type and parameters
        ITypeTerm functionTerm = paramTerm.getFunctionTerm();
        int nrParams = paramTerm.getNrParams();
        doConstraintsForFunctionTerm(constraintAdder,
                constrainedFunctionTerms, functionTerm, nrParams, false);
    }

    private void handleMapLiteralTerm(
            Consumer<IConstraint> constraintAdder,
            ITypeConstraint constraint) {
        MapLiteralTerm mlt = (MapLiteralTerm) constraint.getLeft();
        MapType mapType = (MapType) mlt.getType();
        if (mapType.elemType() instanceof AnyType) {
            // introduce a fresh type variable
            TypeVar elemTypeVar = makeFreshTypeVar();
            TypeVariableTerm elemTypeVarTerm = factory.findOrCreateTypeVariableTerm(elemTypeVar);
            mapType.setElemType(elemTypeVar);
            constraintAdder.accept(new TypeEqualityConstraint(elemTypeVarTerm, factory.findOrCreateIndexedTerm(mlt)));
        }
    }

    private void handleIndexedTerm(Consumer<IConstraint> constraintAdder,
            Set<ITypeTerm> constrainedIndexTerms,
            Set<Pair<ITypeTerm, Integer>> constrainedFunctionTerms,
            ITypeConstraint constraint) {
        IndexedTerm indexedTerm = (IndexedTerm)(constraint.getLeft() instanceof IndexedTerm ? constraint.getLeft() : constraint.getRight());
        ITypeTerm base = indexedTerm.getBase();
        // TODO temporary hack until we handle function types; remove this condition!
//                if (base instanceof FunctionReturnTerm) continue;
        if (!constrainedIndexTerms.contains(base)) {
            ITypeTerm other = constraint.getLeft() instanceof IndexedTerm ? constraint.getRight() : constraint.getLeft();
            TypeVar elemTypeVar = makeFreshTypeVar();
            TypeVariableTerm elemTypeVarTerm = factory.findOrCreateTypeVariableTerm(elemTypeVar);
            if (base instanceof ArrayLiteralTerm) {
                // mutate the array type to use our elements type variable
                ArrayType arrType = (ArrayType)((ArrayLiteralTerm)base).getType();
                arrType.setElemType(elemTypeVar);
            } else if (base instanceof MapLiteralTerm) {
                // mutate to use our elements type variable
                MapType mapType = (MapType)((MapLiteralTerm)base).getType();
                mapType.setElemType(elemTypeVar);
            } else if (other.toString().startsWith("TP(")) { // TODO HACK!
                // we know we're talking about an array in this case, so
                // don't introduce a type variable for the key
                ArrayType arrayType = new ArrayType(elemTypeVar);
                constraintAdder.accept(new SubTypeConstraint(base, factory.getTermForType(arrayType)));
                // hack!  make sure return type of "push" is an int
                PropertyAccessTerm pushTerm = factory.findOrCreatePropertyAccessTerm(base, "push", null);
                FunctionReturnTerm pushRetTerm = factory.findOrCreateFunctionReturnTerm(pushTerm, 1);
                constraintAdder.accept(new TypeEqualityConstraint(
                        pushRetTerm, factory.getTermForType(IntegerType
                                .make())));
                // another hack!  need this to handle Array function
                if (base instanceof FunctionReturnTerm
                        && base.toString().equals("ret(|Array|)")) {
                    doConstraintsForFunctionTerm(constraintAdder,
                            constrainedFunctionTerms,
                            ((FunctionReturnTerm) base)
                                    .getFunctionTerm(),
                            ((FunctionReturnTerm) base).getNrParams(), false);
                }
            } else {
                TypeVar keyTypeVar = makeFreshTypeVar();
                TypeVariableTerm keyTypeVarTerm = factory.findOrCreateTypeVariableTerm(keyTypeVar);
                UnknownIndexableType mapOrArrayType = new UnknownIndexableType(keyTypeVar, elemTypeVar);
                constraintAdder.accept(new SubTypeConstraint(base, factory.getTermForType(mapOrArrayType)));
                constraintAdder.accept(new TypeEqualityConstraint(keyTypeVarTerm, factory.findOrCreateKeyTerm(base)));
            }
            // always constrain the element type
            constraintAdder.accept(new TypeEqualityConstraint(elemTypeVarTerm, indexedTerm));
            constrainedIndexTerms.add(base);
        }
    }

    private void handleProtoParentTerm(ProtoParentTerm protoParentTerm,
            Consumer<IConstraint> constraintAdder,
            Set<ProtoParentTerm> handled) {
        if (handled.add(protoParentTerm)) {
            constraintAdder.accept(new InheritPropsConstraint(protoParentTerm));
        }

    }

    private void handleProtoTerm(ProtoTerm protoTerm,
            Consumer<IConstraint> constraintAdder, Set<ProtoTerm> handled) {
        if (handled.add(protoTerm)) {
            logger.debug("handling proto term {}", protoTerm);
            ITypeTerm baseTerm = protoTerm.getTerm();
            if (baseTerm.getType() instanceof ConstructorType) {
                ConstructorType cType = (ConstructorType) baseTerm.getType();
                TypeVar prototypeVar = makeFreshTypeVar();
                ITypeTerm protoVarTerm = factory.getTermForType(prototypeVar);
                if (cType.getPrototype() != null) {
                    // this must be due to initialization of individual prototype properties
                    ObjectType protoType = (ObjectType) cType.getPrototype();
                    for (Property p : protoType.properties()) {
                        if (p.getType() instanceof AnyType) {
                            protoType.setProperty(p.getName(), makeFreshTypeVar());
                        }
                    }
                    constraintAdder.accept(new TypeEqualityConstraint(
                            protoVarTerm, factory.getTermForType(protoType)));
                }
                cType.setPrototype(prototypeVar);
                ProtoTerm consProto = factory.findOrCreateProtoTerm(baseTerm);
                constraintAdder.accept(new TypeEqualityConstraint(consProto,
                        protoVarTerm));
                // also put it on the return type
                Type returnType = typeVarsForReturnType(constraintAdder, cType,
                        prototypeVar);
                FunctionReturnTerm returnTerm = factory
                        .findOrCreateFunctionReturnTerm(baseTerm,
                                cType.nrParams());
                constraintAdder.accept(new TypeEqualityConstraint(returnTerm,
                        factory.getTermForType(returnType)));
            } else {
                // the upper bound of the base term should be a constructor
                // type, but we don't know its arity! so we can't
                // immediately generate an upper bound. instead, handle this
                // case during solving with a ProtoConstraint
                constraintAdder.accept(new ProtoConstraint(protoTerm));
            }
        }

    }

    /**
     * special-case hack to get rid of AnyType inside the type of Array.
     * TODO don't generate AnyType any more, and use type variables instead
     */
    private void handleIntersectionType(IntersectionType isectType, Consumer<IConstraint> constraintAdder) {
        for (Type t: isectType.getTypes()) {
            if (t instanceof FunctionType) {
                FunctionType ft = (FunctionType) t;
                // check for Array<any> in return type, and for any in parameter types
                List<Type> paramTypes = ft.paramTypes();
                for (int i = 0; i < paramTypes.size(); i++) {
                    if (paramTypes.get(i) instanceof AnyType) {
                        // replace with fresh type variable
                        ft.setParamType(factory.freshTypeVar(), i);
                    }
                }
                Type returnType = ft.returnType();
                if (returnType instanceof ArrayType) {
                    ArrayType arrType = (ArrayType) returnType;
                    if (arrType.elemType() instanceof AnyType) {
                        arrType.setElemType(factory.freshTypeVar());
                        // TODO constrain indexed term?
                    }
                }
            }
        }
    }

    private void doConstraintsForFunctionTerm(
            Consumer<IConstraint> constraintAdder,
            Set<Pair<ITypeTerm, Integer>> constrainedFunctionTerms,
            ITypeTerm functionTerm, int nrParams, boolean isConstructor) {
        Pair<ITypeTerm, Integer> key = Pair.make(functionTerm, nrParams);
        if (!constrainedFunctionTerms.contains(key)) {
            constrainedFunctionTerms.add(key);
            // this call ensures that we have an entry for the function
            // the final type mapping.  see endtoend test iife.js
            fixedpointSolver.initBounds(functionTerm, currentCause);
            List<Type> paramTypes = null;
            Type returnType = null;
            Type type = functionTerm.getType();
            if (type instanceof CodeType) {
                // update fnType with new type variables as needed
                CodeType fnType = (CodeType) type;
                paramTypes = fnType.paramTypes();
                for (int i = 0; i < nrParams; i++) {
                    Type curParamType = paramTypes.get(i);
                    if (curParamType instanceof AnyType) {
                        fnType.setParamType(makeFreshTypeVar(), i);
                    }
                }
                returnType = typeVarsForReturnType(constraintAdder, fnType, null);
                logger.debug("updated type of {} to {}", functionTerm, fnType);
            } else {
                paramTypes = new ArrayList<>(nrParams);
                for (int i = 0; i < nrParams; i++) {
                    paramTypes.add(makeFreshTypeVar());
                }
                returnType = makeFreshTypeVar();
                if (!isConstructor) {
                    // create a fresh function type, and make type a subtype
                    // TODO passing bogus names in here; hopefully they won't be
                    // used. clean this up
                    FunctionType fnType = new FunctionType(paramTypes,
                            Collections.<String> emptyList(), returnType);
                    constraintAdder.accept(new SubTypeConstraint(functionTerm,
                            factory.getTermForType(fnType)));
                } else {
                    TypeVar protoType = makeFreshTypeVar();
                    ConstructorType cType = new ConstructorType(paramTypes, Collections.<String>emptyList(), returnType, protoType);
                    constraintAdder.accept(new SubTypeConstraint(functionTerm, factory.getTermForType(cType)));
                    // also equate protoType appropriately to a term
                    ProtoTerm protoTerm = factory.findOrCreateProtoTerm(functionTerm);
                    constraintAdder.accept(new TypeEqualityConstraint(protoTerm, factory.getTermForType(protoType)));
                }
            }
            // add equality constraints for FunctionParamTerms and
            // FunctionReturnTerm
            for (int i = 0; i < nrParams; i++) {
                FunctionParamTerm curParamTerm = factory
                        .findOrCreateFunctionParamTerm(functionTerm, i,
                                nrParams);
                constraintAdder
                        .accept(new TypeEqualityConstraint(
                                curParamTerm,
                                factory.getTermForType(paramTypes.get(i))));
            }
            FunctionReturnTerm returnTerm = factory
                    .findOrCreateFunctionReturnTerm(functionTerm, nrParams);
            constraintAdder.accept(new TypeEqualityConstraint(returnTerm,
                    factory.getTermForType(returnType)));
        }
    }

    /**
     * Introduce type variables for the return type of fnType as needed
     * @param constraintAdder
     * @param fnType
     * @param prototypeVar
     * @return
     */
    private Type typeVarsForReturnType(
            Consumer<IConstraint> constraintAdder, CodeType fnType, TypeVar prototypeVar) {
        Type returnType = fnType.returnType();
        if (!((returnType instanceof AnyType) || (returnType instanceof ObjectType))) {
            return returnType;
        }
        TypeVar returnTypeVar = makeFreshTypeVar();
        if (returnType instanceof ObjectType) {
            ObjectType retObj = (ObjectType) returnType;
            for (Property p : retObj.properties()) {
                if (p.getType() instanceof AnyType) {
                    retObj.setProperty(p.getName(), makeFreshTypeVar());
                }
            }
            if (prototypeVar != null) {
                retObj = new ObjectType(prototypeVar, retObj.ownProperties(), Collections.emptyList());
            }
            constraintAdder.accept(new TypeEqualityConstraint(factory
                    .getTermForType(returnTypeVar), factory
                    .getTermForType(retObj)));
        }
        fnType.setReturnType(returnTypeVar);
        return returnTypeVar;
    }

    private TypeVar makeFreshTypeVar() {
        TypeVar freshVar = factory.freshTypeVar();
        return freshVar;
    }

}
