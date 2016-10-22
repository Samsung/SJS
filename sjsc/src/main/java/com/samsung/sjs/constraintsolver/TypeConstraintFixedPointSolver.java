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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.samsung.sjs.typeconstraints.CheckArityConstraint;
import com.samsung.sjs.typeconstraints.FunctionCallTerm;
import org.apache.commons.lang3.tuple.Pair;
import org.mozilla.javascript.ast.AstNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.fixedpoint.impl.DefaultFixedPointSolver;
import com.ibm.wala.fixpoint.AbstractStatement;
import com.ibm.wala.fixpoint.UnaryStatement;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.samsung.sjs.SourceLocation;
import com.samsung.sjs.constraintgenerator.ConstraintFactory;
import com.samsung.sjs.constraintgenerator.ConstraintGenUtil;
import com.samsung.sjs.typeconstraints.ATerm;
import com.samsung.sjs.typeconstraints.ArrayLiteralTerm;
import com.samsung.sjs.typeconstraints.ConcreteConstraint;
import com.samsung.sjs.typeconstraints.EnvironmentDeclarationTerm;
import com.samsung.sjs.typeconstraints.FunctionParamTerm;
import com.samsung.sjs.typeconstraints.FunctionReturnTerm;
import com.samsung.sjs.typeconstraints.FunctionTerm;
import com.samsung.sjs.typeconstraints.IConstraint;
import com.samsung.sjs.typeconstraints.ITypeConstraint;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.typeconstraints.IndexedTerm;
import com.samsung.sjs.typeconstraints.InheritPropsConstraint;
import com.samsung.sjs.typeconstraints.KeyTerm;
import com.samsung.sjs.typeconstraints.MROMRWConstraint;
import com.samsung.sjs.typeconstraints.MapLiteralTerm;
import com.samsung.sjs.typeconstraints.ObjectLiteralTerm;
import com.samsung.sjs.typeconstraints.OperatorTerm;
import com.samsung.sjs.typeconstraints.PropertyAccessTerm;
import com.samsung.sjs.typeconstraints.ProtoConstraint;
import com.samsung.sjs.typeconstraints.ProtoParentTerm;
import com.samsung.sjs.typeconstraints.ProtoTerm;
import com.samsung.sjs.typeconstraints.SubTypeConstraint;
import com.samsung.sjs.typeconstraints.TypeConstantTerm;
import com.samsung.sjs.typeconstraints.TypeEqualityConstraint;
import com.samsung.sjs.typeconstraints.TypeVariableTerm;
import com.samsung.sjs.typeconstraints.UnaryOperatorTerm;
import com.samsung.sjs.typeconstraints.UpperBoundConstraint;
import com.samsung.sjs.types.AnyType;
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.AttachedMethodType;
import com.samsung.sjs.types.BottomReferenceType;
import com.samsung.sjs.types.BottomType;
import com.samsung.sjs.types.CodeType;
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.DefaultType;
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.MapType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.ObjectUnionType;
import com.samsung.sjs.types.PrimitiveType;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.PropertyContainer;
import com.samsung.sjs.types.PropertyNotFoundException;
import com.samsung.sjs.types.StringType;
import com.samsung.sjs.types.TopReferenceType;
import com.samsung.sjs.types.TopType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.TypeVar;
import com.samsung.sjs.types.Types;
import com.samsung.sjs.types.UnattachedMethodType;
import com.samsung.sjs.types.UnknownIndexableType;

/**
 * Created by schandra on 3/11/15.
 */
public class TypeConstraintFixedPointSolver extends DefaultFixedPointSolver<TypeInfSolverVariable> {

    private final Set<ITypeConstraint> constraints;

    private final Set<MROMRWConstraint> mroMRWConstraints;

    protected final Set<ITypeTerm> terms;

    /**
     * terms that have been constrained to be concrete
     */
    private final Set<Pair<ITypeTerm, Cause>> concreteTerms = HashSetFactory.make();

    public final Map<ITypeTerm, TypeConstraintSolverVariable> upperBounds;

    public final Map<ITypeTerm, TypeConstraintSolverVariable> lowerBounds;

    public final OperatorModel operatorModel;

    protected final ConstraintFactory factory;

    private static Logger logger = LoggerFactory.getLogger(TypeConstraintFixedPointSolver.class);

    private final Map<IConstraint,Set<Integer>> sourceMapping;
    private final Map<ITypeConstraint, Cause> causes;
    private final Map<ITypeTerm, Cause> termExistence;
    private final Map<ITypeTerm, CheckArityConstraint> aritiesToCheck;

    public TypeConstraintFixedPointSolver(Set<ITypeConstraint> constraints, Map<ITypeConstraint, Cause> causes, Set<MROMRWConstraint> mroMRWConstraints, ConstraintFactory factory, Map<IConstraint, Set<Integer>> sourceMapping) {
        this.constraints = constraints;
        this.mroMRWConstraints = mroMRWConstraints;
        this.terms = new LinkedHashSet<>();
        this.upperBounds = new LinkedHashMap<>();
        this.lowerBounds = new LinkedHashMap<>();
        this.operatorModel = new OperatorModel();
        this.factory = factory;
        this.sourceMapping = sourceMapping;
        this.causes = causes;
        this.termExistence = new LinkedHashMap<>();
        this.aritiesToCheck = new LinkedHashMap<>();
    }

    private Cause getCause(ITypeConstraint c) {
        Cause cause = causes.get(c);
        return cause != null ? cause : Cause.src(c);
    }

    @Override
    protected TypeInfSolverVariable[] makeStmtRHS(int size) {
        return new TypeInfSolverVariable[size];
    }

    private boolean isTypeSource(ITypeTerm term) {
        return term instanceof TypeConstantTerm
                || term instanceof ObjectLiteralTerm
                || term instanceof ArrayLiteralTerm
                || term instanceof MapLiteralTerm
                || term instanceof FunctionTerm
                || term instanceof EnvironmentDeclarationTerm;
    }

    @Override
    protected void initializeVariables() {
        for (ITypeConstraint c : constraints) {

            logger.debug("Examining constraint: {}", c);
           ITypeTerm left = c.getLeft();
            initBounds(left, getCause(c));

            logger.debug("Left: {} <= {} <= {}", lowerBounds.get(left), left,
                    upperBounds.get(left));

            ITypeTerm right = c.getRight();
            initBounds(right, getCause(c));

            logger.debug("Right: {} <= {} <= {}", lowerBounds.get(right), right,
                    upperBounds.get(right));

        }

//         {
//            for (ITypeTerm t : lowerBounds.keySet()) {
//                logger.debug(t + "-->" + lowerBounds.get(t));
//            }
//        }
    }

    void initBounds(ITypeTerm t, Cause reason) {
        assert t != null : "null term!!";
        if (! terms.contains(t)) {
            terms.add(t);
            termExistence.put(t, reason);
            if (isTypeSource(t)) {
                Type type = t.getType();
                TypeConstraintSolverVariable hiVar = new TypeConstraintSolverVariable(t, "HI", type);
                hiVar.setTypeAndTerm(type, t, reason);
                upperBounds.put(t, hiVar);
                TypeConstraintSolverVariable loVar = new TypeConstraintSolverVariable(t, "LO", type);
                loVar.setTypeAndTerm(type, t, reason);
                lowerBounds.put(t, loVar);
            } else if (ConstraintGenUtil.isNullConstant(t)) {
                // make BottomReferenceType the lower bound of null
                Type bottomRefType = BottomReferenceType.make();
                TypeConstraintSolverVariable loVar = new TypeConstraintSolverVariable(t, "LO", bottomRefType);
                loVar.setTypeAndTerm(bottomRefType, t, reason);
                lowerBounds.put(t, loVar);
                upperBounds.put(t, new TypeConstraintSolverVariable(t, "HI", TopType.SINGLETON));
            } else {
                upperBounds.put(t, new TypeConstraintSolverVariable(t, "HI", TopType.SINGLETON));
                lowerBounds.put(t, new TypeConstraintSolverVariable(t, "LO", BottomType.SINGLETON));
            }
        }
    }

    @Override
    protected void initializeWorkList() {

        logger.debug("Initializing work list");
        generateSolverTypeConstraints();

        connectUpperAndLowerBounds();

        generateSolverOperatorConstraints();

        generateMROMRWConstraints();
        addSourcesToWorklist();

        logger.debug("solving constraints...");
    }

    /**
     * add statements to the solver for MRO/MRW constraints
     */
    private void generateMROMRWConstraints() {
        for (MROMRWConstraint constraint: mroMRWConstraints) {
            if (constraint instanceof UpperBoundConstraint) {
                UpperBoundConstraint ubc = (UpperBoundConstraint) constraint;
                ITypeTerm possMethodTerm = ubc.getPossibleMethodTerm();
                ITypeTerm containingObjectTerm = ubc.getContainingObjectTerm();
                TypeInfSolverVariable lowerVar = lowerBounds.get(possMethodTerm);
                // NOTE the lhs var passed here doesn't matter, since the
                // operator only generates new constraints
                newStatement(lowerVar, new CheckForMethodOperator(this,containingObjectTerm, constraint.getReason()), lowerVar, false, false);
            } else {
                throw new RuntimeException("need to handle MROMRWConstraint " + constraint);
            }
        }
    }

    private void addSourcesToWorklist() {
        @SuppressWarnings("unchecked")
        Iterator<AbstractStatement<?,?>> i = getStatements();
        while (i.hasNext()) {
            AbstractStatement<?,?> curStmt = i.next();
            if (curStmt instanceof UnaryStatement<?>) {
                TypeInfSolverVariable rhsVar = (TypeInfSolverVariable) ((UnaryStatement<?>)curStmt).getRightHandSide();
                checkForTypeSource(curStmt, rhsVar);
            } else {
                TypeInfSolverVariable[] rhs =(TypeInfSolverVariable[]) curStmt.getRHS();
                for (TypeInfSolverVariable rhsVar: rhs) {
                    checkForTypeSource(curStmt, rhsVar);
                }
            }
        }
    }

    private void connectUpperAndLowerBounds() {
        /*
         * Connect the upper and lower bounds for each term.
         * All the type variables that we introduced are also present in the constraint system
         * in the form of TypeVariableTerm.
         *
         * TODO: actually, the "lhs" of this statement is never directly changed in this operation.  We just pass
         * something as a dummy.
         */
        for (ITypeTerm t: terms) {
            if (! isTypeSource(t)) {
                MROMRWVariable mromrwVar = getMROMRWVarForTerm(t);
                newStatement(lowerBounds.get(t), TypeInsideOperator.make(this, t, contrib(t)), lowerBounds.get(t), upperBounds.get(t), mromrwVar, false, false);
            } else if (t instanceof ObjectLiteralTerm) {
                MROMRWVariable mromrwVar = getMROMRWVarForTerm(t);
                newStatement(lowerBounds.get(t), new MROMRWToObjLitOperator(this,(ObjectLiteralTerm) t, contrib(t)), mromrwVar, false, false);
            }
        }
    }

    private void generateSolverOperatorConstraints() {

        constraints.stream().filter(c -> c.getRight() instanceof OperatorTerm).forEach(c -> {
            OperatorTerm ot = (OperatorTerm) c.getRight();
            Cause cause = getCause(c);
            initBounds(ot.getLeft(), cause);
            initBounds(ot.getRight(), cause);
            TypeConstraintSolverVariable[] a = {
                    lowerBounds.get(ot.getLeft()),
                    lowerBounds.get(ot.getRight()),
                    upperBounds.get(ot)
            };
            String operator = ot.getOperator();
            int lineNumber = this.sourceMapping.get(c).iterator().next();
            if (operator.equals("||")) {
                addSubtypeConstraints(ot.getLeft(), ot, false, cause);
                addSubtypeConstraints(ot.getRight(), ot, false, cause);
            } else {
                newStatement(lowerBounds.get(ot), TypeOperatorOperator.make(this, operator, lineNumber, cause), a, true, false);
            }
        });

        constraints.stream().filter(c -> c.getRight() instanceof UnaryOperatorTerm).forEach(c -> {
            Cause cause = getCause(c);
            UnaryOperatorTerm uot = (UnaryOperatorTerm) c.getRight();
            initBounds(uot.getOperand(), cause);
            TypeConstraintSolverVariable[] a = { lowerBounds.get(uot.getOperand()), upperBounds.get(uot.getOperand()) };
            int lineNumber = this.sourceMapping.get(c).iterator().next();
            newStatement(lowerBounds.get(uot), new UnaryOpOperator(this, uot.getOperator(), uot.isPrefix(), lineNumber, cause), a, false, false);

        });
    }

    private void generateSolverTypeConstraints() {
        for (ITypeConstraint c : constraints) {
            Cause cause = getCause(c);
            logger.debug("Examining constraint: {}", c);
            if (c instanceof SubTypeConstraint) {
                ITypeTerm left = c.getLeft();
                ITypeTerm right = c.getRight();

                addSubtypeConstraints(left, right, false, getCause(c));
            } else if (c instanceof TypeEqualityConstraint) {

                ITypeTerm left = c.getLeft();
                ITypeTerm right = c.getRight();

                /* simulate equating by 2 directional constraints */
                equateTypes(left, right, false, getCause(c));
            } else if (c instanceof ProtoConstraint) {
                ProtoTerm protoTerm = (ProtoTerm) c.getLeft();
                ITypeTerm baseTerm = protoTerm.getTerm();
                initBounds(baseTerm, cause);
                newStatement(lowerBounds.get(baseTerm), new TypeProtoOperator(this,protoTerm, cause), lowerBounds.get(baseTerm), false, false);
            } else if (c instanceof InheritPropsConstraint) {
                ProtoParentTerm protoParentTerm = (ProtoParentTerm) c.getLeft();
                ITypeTerm baseTerm = protoParentTerm.getTerm();
                initBounds(protoParentTerm, cause);
                initBounds(baseTerm, cause);
                // lower bound of base term is LHS, and lower bound of parent term is RHS
                newStatement(lowerBounds.get(baseTerm),
                        new InheritPropsOperator(this, cause),
                        lowerBounds.get(baseTerm),
                        lowerBounds.get(protoParentTerm), false, false);
                // we also need to inherit the MRO/MRW from the prototype parent
                newStatement(getMROMRWVarForTerm(baseTerm), new CopyMROMRWOperator(this, cause), getMROMRWVarForTerm(protoParentTerm), false, false);
            } else if (c instanceof ConcreteConstraint) {
                concreteTerms.add(Pair.of(((ConcreteConstraint) c).getTerm(), cause));
            } else if (c instanceof CheckArityConstraint) {
                CheckArityConstraint ca = (CheckArityConstraint)c;
                aritiesToCheck.put(ca.getTerm(), ca);
            } else {
                assert false: c.getClass();
            }
        }
    }

    private void equateMROMRW(ITypeTerm left, ITypeTerm right, boolean toWorklist, Cause reason) {
        /* also equate the MRO/MRW */
        MROMRWVariable leftMROMRW = getMROMRWVarForTerm(left);
        MROMRWVariable rightMROMRW = getMROMRWVarForTerm(right);
        newStatement(leftMROMRW, new CopyMROMRWOperator(this, reason), rightMROMRW, toWorklist, false);
        newStatement(rightMROMRW, new CopyMROMRWOperator(this, reason), leftMROMRW, toWorklist, false);
    }

    /**
     * if rhsVar corresponds to a type source, add curStmt to the worklist
     * @param curStmt
     * @param rhsVar
     */
    private void checkForTypeSource(AbstractStatement<?, ?> curStmt,
            TypeInfSolverVariable rhsVar) {
        if (rhsVar instanceof TypeConstraintSolverVariable) {
            ITypeTerm origTerm = ((TypeConstraintSolverVariable)rhsVar).getOrigTerm();
            if (isTypeSource(origTerm) || ConstraintGenUtil.isNullConstant(origTerm)) {
                addToWorkList(curStmt);
            }
        }
    }

    void addSubtypeConstraints(ITypeTerm left, ITypeTerm right, boolean toWorklist, Cause reason) {
        if (ConstraintGenUtil.isNullUndefinedLitOrVoidOp(left) && !isTypeSource(right)) {
            // anytime we see a null, undefined, or void expression on the LHS of a subtype constraint,
            // we generate an equality constraint instead, to ensure that we get the "right" type for the
            // expression
            equateTypes(left, right, toWorklist, reason);
            return;
        }
        // we init the bounds here, in case we haven't seen the terms before
        initBounds(left, reason);
        initBounds(right, reason);
        if (! isTypeSource(left)) {
            /* left.hi <- left.hi \/ right.hi */
            newStatement(upperBounds.get(left), new TypeMeetOperator(this, left, reason), upperBounds.get(right), toWorklist, false);
        }
        if (! isTypeSource(right)) {
            /* right.lo <- left.lo /\ right.lo */
            newStatement(lowerBounds.get(right), new TypeJoinOperator(this, right, reason), lowerBounds.get(left), toWorklist, false);
        }
    }

    private static enum Context {
        CONSTRUCTOR_PROTO, RECEIVER, OTHER
    }


    private static void traverseAndUpdateType(Type type, final Set<Type> inProgress, final BiFunction<Type, Context, Type> typeUpdater) {
        BiFunction<Type, Context, Type> updateAndRecurse = (t, context) -> {
            Type newType = typeUpdater.apply(t,context);
            traverseAndUpdateType(newType, inProgress, typeUpdater);
            return newType;
        };
        if (inProgress.contains(type)) {
            logger.trace("already handling type {}", type);
            return;
        }
        logger.trace("handling type {}", type);
        inProgress.add(type);
        if (type instanceof ObjectType) {
            ObjectType ot = (ObjectType) type;
            for (Property p : ot.properties()) {
                Type ptype = p.getType();
                Type newType = updateAndRecurse.apply(ptype, Context.OTHER);
                ot.setProperty(p.getName(), newType, p.isRO(), p.getSourceLoc());
            }
        } else if (type instanceof ArrayType) {
            ArrayType at = (ArrayType) type;
            Type elemType = at.elemType();
            at.setElemType(updateAndRecurse.apply(elemType, Context.OTHER));
        } else if (type instanceof MapType) {
            MapType mt = (MapType) type;
            Type elemType = mt.elemType();
            mt.setElemType(updateAndRecurse.apply(elemType, Context.OTHER));
        } else if (type instanceof CodeType) {
            CodeType ft = (CodeType) type;
            List<Type> paramTypes = ft.paramTypes();
            for (int i = 0; i < paramTypes.size(); i++) {
                Type curParamType = paramTypes.get(i);
                ft.setParamType(updateAndRecurse.apply(curParamType, Context.OTHER), i);
            }
            Type returnType = ft.returnType();
            ft.setReturnType(updateAndRecurse.apply(returnType, Context.OTHER));
            if (type instanceof ConstructorType) {
                ConstructorType cType = (ConstructorType) type;
                Type proto = cType.getPrototype();
                Type updated = updateAndRecurse.apply(proto, Context.CONSTRUCTOR_PROTO);
                cType.setPrototype(updated);
            }
            if (type instanceof UnattachedMethodType) {
                UnattachedMethodType umType = (UnattachedMethodType) type;
                Type recv = umType.receiverType();
                Type updatedRecv = updateAndRecurse.apply(recv, Context.RECEIVER);
                umType.setReceiverType(updatedRecv);
            }
        } else if (type instanceof IntersectionType) {
            // immutable, so just traverse
            for (Type t : ((IntersectionType) type).getTypes()) {
                traverseAndUpdateType(t, inProgress, typeUpdater);
            }
        } else if (type instanceof ObjectUnionType) {
            // immutable, so just traverse
            for (Type t: ((ObjectUnionType)type)) {
                traverseAndUpdateType(t, inProgress, typeUpdater);
            }
        }
        inProgress.remove(type);
    }

    private static Type fallbackType() {
        return DefaultType.SINGLETON;
    }

    public static void replaceNestedAny(Type type) {
        logger.debug("replacing any types appearing in {}", type);
        traverseAndUpdateType(type, HashSetFactory.make(), (t,context) -> {
            if (t instanceof AnyType) {
                switch (context) {
                case CONSTRUCTOR_PROTO:
                    return null;
                case RECEIVER:
                    return new ObjectType();
                case OTHER:
                    return fallbackType();
                }
            }
            return t;
        });
    }
    /**
     * Substitutes type solutions for type variables where applicable.
     * Also, replaces appearances of the Any type with Integer, to
     * aid in code generation
     * @param type
     * @param inProgress
     */
    public void substituteTypeVars(Type type) {
        logger.debug("substituting type variables in {}", type);
        final HashSet<Type> inProgress = HashSetFactory.make();
        traverseAndUpdateType(type, inProgress, (t, context) -> {
            if (t instanceof TypeVar) {
                logger.trace("substituting for type variable {}", t);
                TypeVariableTerm tvarTerm = factory
                        .findOrCreateTypeVariableTerm((TypeVar)t);
                Type result = tvarTerm.getType();
                logger.trace("subst result: {}", result);
                return result;
            } else if (t instanceof ObjectType && !inProgress.contains(t)) {
                // This is subtle.  In some cases, we have many ObjectType objects
                // representing equivalent types in the final solution.  Performing
                // the same substitutions on these types repeatedly can be a performance
                // bottleneck.  So here, if we detect an equivalent object type, we return
                // the representative type we already encountered, reducing the number of
                // substitutions performed.
                // TODO devise a more principled solution to this problem, likely involving
                // making ObjectTypes truly immutable
                for (Type t2: inProgress) {
                    if (Types.isEqual(t, t2)) {
                        return t2;
                    }
                }
                return t;
            } else {
                return t;
            }
        });
    }

    /**
     * ensure that each property in upper is present in lower or its transitive prototypes
     * @param lower
     * @param upper
     */
    private void checkPropertyPresence(ObjectType lower, ObjectType upper, ITypeTerm t, Cause reason) {
        for (String propName : upper.propertyNames()) {
            Property upperProp = upper.getProperty(propName);
            SourceLocation loc = upperProp.getSourceLoc();
            if (!lower.hasProperty(propName)) {
                substituteTypeVars(lower);
                String message = "could not find property " + propName
                        + " in type " + lower + " for term " + t;
                if (loc != null && loc.getStartLine() != -1) {
                    message += "\nproperty accessed on line " + loc.getStartLine();
                }
                throw new CoreException(message, reason);
            }
            Property lowerProp = lower.getProperty(propName);
            // if we write a read-only property, report an error
            if (upperProp.isRW()
                    && lowerProp.isRO()) {
                logger.debug("lower type {}", lower);
                logger.debug("upper type {}", upper);
                String message = "writing read-only property " + propName;
                if (loc != null && loc.getStartLine() != -1) {
                    message += " on line " + loc.getStartLine();
                }
                throw new CoreException(message, reason);
            }

        }

    }


    /* TODO: Revisit this.
     * PropertyAccessTerm's set type actually sets the type of its baseType
     * causing confusion.
     *
     * OTOH, a getType on a PropertyAccessTerm will consult its base type
     * so we do not really need to worry about assigning its 'type' here.
     */
    private boolean findFinalSolutions() {
        boolean flag = setTermTypes();
        for (ITypeTerm t : terms) {
            try {
                if (t instanceof FunctionReturnTerm && aritiesToCheck.containsKey(t)) {
                    SolutionChecker.checkFunctionRet((FunctionReturnTerm) t);
                } else if (t instanceof FunctionCallTerm) {
                    SolutionChecker.checkFunctionCall((FunctionCallTerm) t);
                }
            } catch (SolverException e) {
                Cause cause = contrib(t);
                if (t instanceof FunctionReturnTerm && aritiesToCheck.containsKey(t)) {
                    cause = Cause.derived(cause, Cause.src(aritiesToCheck.get(t)));
                }
                throw new CoreException(e.getMessage(), cause);
            }
        }
        substituteAllTypeVars();
        checkObjectTypeUpperLower();
        checkConcrete();
        return flag;
    }

    private void substituteAllTypeVars() {
        for (ITypeTerm t : terms) {
            if (! (t instanceof PropertyAccessTerm)) {
                Type type = t.getType();
                if (type instanceof AnyType) {
                    if (!(t instanceof ProtoTerm || t instanceof ProtoParentTerm || t instanceof TypeVariableTerm || t instanceof KeyTerm || t instanceof IndexedTerm)) {
                        t.setType(fallbackType());
                    }
                } else {
                    substituteTypeVars(type);
                    replaceNestedAny(type);
                }
            }
        }
    }

    Collection<ITypeTerm> subterms(ITypeTerm t) {
        // TODO: do we need to add other cases here?
        if (t instanceof FunctionReturnTerm) {
            return Collections.singleton(((FunctionReturnTerm) t).getFunctionTerm());
        } else if (t instanceof ProtoTerm) {
            return Collections.singleton(((ProtoTerm) t).getTerm());
        } else if (t instanceof PropertyAccessTerm) {
            return Collections.singleton(((PropertyAccessTerm) t).getBase());
        } else if (t instanceof FunctionCallTerm) {
            return Collections.singleton(((FunctionCallTerm) t).getTarget());
        }
        return Collections.emptySet();
    }

    Cause contrib(ITypeTerm t) {
        TypeConstraintSolverVariable v1 = lowerBounds.get(t);
        TypeConstraintSolverVariable v2 = upperBounds.get(t);
        Cause cause = Cause.derived(
                v1 == null ? Cause.noReason() : v1.reasonForCurrentValue,
                v2 == null ? Cause.noReason() : v2.reasonForCurrentValue,
                termExistence.getOrDefault(t, Cause.noReason()));
        for (ITypeTerm tt : subterms(t)) {
            cause = Cause.derived(cause, contrib(tt));
        }
        return cause;
    }

    /**
     * check that all terms constrained to be concrete have appropriate concrete types
     */
    private void checkConcrete() {
        for (Pair<ITypeTerm, Cause> pair : concreteTerms) {
            ITypeTerm t = pair.getLeft();
            Cause cause = pair.getRight();
            Type type = t.getType();
            if (type instanceof ObjectType) {
                ObjectType ot = (ObjectType) type;
                Set<Property> roProps = ot.getROProperties();
                List<Property> rwProps = ot.getRWProperties();
                MROMRWVariable mroMRW = getMROMRWVarForTerm(t);
                // all MRO variables must be in RO or RW, and
                // all MRW variables must be in RW
                List<Property> missingMRO = mroMRW
                        .getMRO()
                        .stream()
                        .filter((p) -> {
                            return !ConstraintUtil.getPropWithName(p.getName(),
                                    roProps).isPresent()
                                    && !ConstraintUtil.getPropWithName(
                                            p.getName(), rwProps).isPresent();
                        }).collect(Collectors.toList());
                List<Property> missingMRW = mroMRW
                        .getMRW()
                        .stream()
                        .filter((p) -> {
                            return !ConstraintUtil.getPropWithName(p.getName(),
                                    rwProps).isPresent();
                        }).collect(Collectors.toList());
                if (!missingMRO.isEmpty() || !missingMRW.isEmpty()) {
                    if (logger.isDebugEnabled()) {
                        // print out all MROMRW info
                        for (ITypeTerm term : terms) {
                            if (term2MROMRW.containsKey(term)) {
                                MROMRWVariable mroMRWVar = term2MROMRW.get(term);
                                if (mroMRWVar.nonEmpty()) {
                                    logger.debug("MROMRW {} -> {}", term, mroMRWVar);
                                }
                            }
                        }
                    }
                    // TODO better message!!!
                    throw new CoreException(t + " is missing some MRO/MRW property, and hence cannot appear here\n"
                            + "line " + t.getNode().getLineno() + "\n"
                            + "type " + type + "\n"
                            + mroMRW + "\n"
                            + (missingMRO.isEmpty() ? "" : ("missing MRO: " + missingMRO.stream().map((p) -> p.getName()).collect(Collectors.joining(",","[","]")) + "\n"))
                            + (missingMRW.isEmpty() ? "" : ("missing MRW: " + missingMRW.stream().map((p) -> p.getName()).collect(Collectors.joining(",","[","]")) + "\n")),
                            Cause.derived(contrib(t), mroMRW.reasonForCurrentValue, cause));
                }
            } else if (type instanceof AttachedMethodType && !ConstraintGenUtil.isNullUndefinedLitOrVoidOp(t)) {
                // trying to detach a method
                // we explicitly allow null/undefined/void here, since those expressions only get an
                // attached method type due to the hackish equality constraints we generate for them
                throw new CoreException("cannot detach method from object\n"
                        + "line " + t.getNode().getLineno() + "\n"
                        + "expression " + t,
                        Cause.derived(contrib(t), cause));
            }
        }
    }

    /**
     * check if all properties in type upper bounds are present in corresponding lower bounds
     */
    private void checkObjectTypeUpperLower() {
        for (ITypeTerm t: terms) {
            TypeConstraintSolverVariable lowerVar = lowerBounds.get(t);
            TypeConstraintSolverVariable upperVar = upperBounds.get(t);
            Type lowerBound = lowerVar.getType();
            Type upperBound = upperVar.getType();
            Cause reason = contrib(t);
            try {
                if (upperBound instanceof ObjectType) {
                    ObjectType rTy = (ObjectType) upperBound;
                    if (lowerBound instanceof ObjectType) {
                        ObjectType lTy = (ObjectType) lowerBound;
                        checkPropertyPresence(lTy, rTy, t, reason);
                    } else if (lowerBound instanceof ObjectUnionType) {
                        if (t.getType() instanceof ObjectUnionType) {
                            ObjectType merged = computeMergedType((ObjectUnionType) t.getType(), reason);
                            checkPropertyPresence(merged, rTy, t, reason);
                            t.setType(merged);
                        } else {
                            // we've already done the merge on t's type
                            // HACK in some strange cases involving arrays, t's type
                            // may not be an ObjectType.  Ignore this for now, but
                            // we *really* need to bring some sanity to our Array handling
                            if (t.getType() instanceof ObjectType) {
                                ObjectType merged = (ObjectType) t.getType();
                                checkPropertyPresence(merged, rTy, t, reason);
                            }
                        }
                    }
                } else if (t.getType() instanceof ObjectUnionType) {
                    t.setType(computeMergedType((ObjectUnionType) t.getType(), reason));
                }
				// need to merge any nested ObjectUnionTypes
                Collection<ObjectType> mergedTypes = new ArrayList<>();
				traverseAndUpdateType(t.getType(), HashSetFactory.make(), (type, context) -> {
					if (type instanceof ObjectUnionType) {
						ObjectType merged = computeMergedType((ObjectUnionType) type, reason);
						for (ObjectType prev: mergedTypes) {
							if (Types.isEqual(merged, prev)) {
								return prev;
							}
						}
						mergedTypes.add(merged);
						return merged;
					} else {
						return type;
					}
				});
            } catch (PropertyNotFoundException e) {
                throw new CoreException(e.getMessage(), reason);
            }
        }
        // there should be no object union types left anywhere
//		for (ITypeTerm t : terms) {
//			if (t instanceof TypeConstantTerm) continue;
//			traverseAndUpdateType(t.getType(), HashSetFactory.make(), (type, context) -> {
//				if (type instanceof ObjectUnionType) {
//					System.out.println("FOUND OBJECT UNION TYPE FOR " + t.getClass() + " " + t + "\nLINE "
//						+ (t.getNode() != null ? t.getNode().getLineno() : -1) + "\nTYPE " + t.getType());
//					assert false; 
//				}
//				return type;
//			});
//		}
    }

    private boolean setTermTypes() {
        boolean flag = true;
        for (ITypeTerm t: terms) {
            if (shouldSetType(t) && !(t instanceof ProtoTerm)) {

                flag = setTypeForTerm(flag, t);
            }
        }
        // we need a separate loop over the ProtoTerms, since
        // they require the underlying term type to be set first
        for (ITypeTerm t: terms) {
            if (t instanceof ProtoTerm) {
                ProtoTerm pt = (ProtoTerm) t;
                ITypeTerm ptNested = pt.getTerm();
                if (Types.usableAsConstructor(ptNested.getType())) {
                    flag = setTypeForTerm(flag, t);
                } else {
                    throw new CoreException("reference to prototype of non-constructor " + ptNested, contrib(t));
                }
            }
        }
        // now that we've set types for all type variables for which we inferred something,
        // go through UnknownIndexableType upper bounds and set them to something reasonable
        for (ITypeTerm t: terms) {
            if (shouldSetType(t) && t.getType() instanceof AnyType) {
                Type lowerBound = lowerBounds.get(t).getType();
                Type upperBound = upperBounds.get(t).getType();
                if ((lowerBound instanceof BottomType) && !(upperBound instanceof TopType)) {
                    if (upperBound instanceof UnknownIndexableType) {
                        UnknownIndexableType uit = (UnknownIndexableType) upperBound;
                        Type elemType = getTypeForTypeVar(uit.elemType());
                        if (elemType instanceof AnyType) {
                            elemType = fallbackType();
                        }
                        Type keyType = getTypeForTypeVar(uit.keyType());
                        if (keyType.equals(StringType.make())) {
                            // must be a map
                            t.setType(new MapType(elemType));
                        } else {
                            t.setType(new ArrayType(elemType));
                        }
                    }
                }
            }
        }
        return flag;
    }

    private boolean setTypeForTerm(boolean flag, ITypeTerm t) {
        logger.debug("Finding type for : {}", t);
        Type lowerBound = lowerBounds.get(t).getType();
        Type upperBound = upperBounds.get(t).getType();
        logger.debug(" Low = {}", lowerBound);
        logger.debug(" Up = {}", upperBound);
        if ((lowerBound instanceof BottomType || lowerBound instanceof BottomReferenceType) && !(upperBound instanceof TopType)) {
            if (upperBound instanceof UnknownIndexableType) {
                // this is not a valid type for the final solution
                logger.debug("cannot use MapOrArrayType for term: {}", t);
                flag = false;
            } else if (upperBound instanceof TopReferenceType) {
                // just use object type
                t.setType(new ObjectType());
            } else {
                // rather than just assigning the upperBound as a type,
                // we use the lowest subtype of upperBound.  We do this
                // to avoid cases where this term gets assigned a supertype
                // of some term into which it is copied; see GitHub issue #114
                Type lowestSubtype = Types.lowestSubtype(upperBound);
                if (lowestSubtype instanceof TopReferenceType) {
                    // TODO support BottomReferenceType in backend
                    // NOTE: using ObjectType here could cause the problem outlined in
                    // #114
                    lowestSubtype = new ObjectType();
                }
                t.setType(lowestSubtype);
            }
        } else if (!(lowerBound instanceof BottomType)) {
            if (lowerBound instanceof BottomReferenceType) {
                // TODO support BottomReferenceType in backend
                t.setType(new ObjectType());
            } else {
                t.setType(lowerBound);
            }
        } else {
            logger.debug("No type for term: {}", t);
            if (t instanceof ATerm) {
                if (t.getNode() != null) {
                    logger.debug("line {}", t.getNode().getLineno());
                }
            }
            flag = false;
        }
        return flag;
    }

    private Type getTypeForTypeVar(TypeVar typeVar) {
        TypeVariableTerm tvarTerm = factory
                .findOrCreateTypeVariableTerm(typeVar);
        return tvarTerm.getType();
    }

    private ObjectType computeMergedType(ObjectUnionType type, Cause reason) {
        ObjectType result = null;
        for (ObjectType o : type) {
            if (result == null) {
                result = o;
            } else {
                // intersect the properties
                boolean changed = false;
                List<Property> newResultProps = new ArrayList<Property>();
                for (Property resultProp : result.properties()) {
                    String resultPropName = resultProp.getName();
                    boolean sameProp = false;
                    if (o.hasProperty(resultPropName)) {
                        Property oProp = o.getProperty(resultPropName);
                        Type resultPropType = resultProp.getType();
                        Type oPropType = oProp.getType();
                        if (!Types.isEqual(resultPropType, oPropType)) {
                            // don't keep it
                        } else {
                            if (resultProp.isRW() && oProp.isRO()) {
                                // we need the final prop to be RO
                                newResultProps.add(oProp);
                            } else {
                                newResultProps.add(resultProp);
                                sameProp = true;
                            }
                        }
                    }
                    changed = changed || !sameProp;
                }
                if (changed) {
                    ObjectType newResult = new ObjectType(newResultProps);
                    result = newResult;
                }
            }
        }
        return result;
    }

    private boolean shouldSetType(ITypeTerm t) {
        return !isTypeSource(t)
                && ! (t instanceof PropertyAccessTerm)
                && ! (t instanceof KeyTerm)
                && ! (t instanceof IndexedTerm)
                && ! (t instanceof FunctionReturnTerm)
                && ! (t instanceof FunctionParamTerm);
    }



    public TypeAssignment solve() throws CancelException {
        logger.info("Type Solve ...");
        super.solve(null);
        if (logger.isDebugEnabled()) {
            dumpBounds();
        }
        boolean typedAllVariables = findFinalSolutions();
        if (!typedAllVariables) {
            logger.debug("Cannot find types for all variables");
        }
        if (logger.isDebugEnabled()) {
            dumpFinalSolution();
        }
        return new TypeAssignment(
                terms.stream()
                        .filter(t -> !(t instanceof EnvironmentDeclarationTerm))
                        .collect(Collectors.toMap(
                            Function.identity(),
                            ITypeTerm::getType,
                            (ty1, ty2) -> { assert Types.isEqual(ty1, ty2); return ty1; },
                            LinkedHashMap::new)),
                this);
    }

    private void dumpBounds() {
        logger.debug("Terms in the constraint system:");
        for (ITypeTerm t : terms) {
            logger.debug("{} <= {} <= {}", lowerBounds.get(t), t,
                    upperBounds.get(t));
        }
    }

    private void dumpFinalSolution() {
        logger.debug("Solution from the fixed point solver:");
        for (ITypeTerm t : terms) {
            logger.debug("{} -> {}", t, t.getType());
        }
        for (ITypeTerm t: terms) {
            if (term2MROMRW.containsKey(t)) {
                MROMRWVariable mroMRWVar = term2MROMRW.get(t);
                if (mroMRWVar.nonEmpty()) {
                    logger.debug("MROMRW {} -> {}", t, mroMRWVar);
                }
            }
        }
    }

    /**
     * generate constraints that ensure the solution for the two type terms will be equal
     * @param lty
     * @param rty
     * @return true if any new constraint was added, false otherwise
     */
    boolean equateTypes(ITypeTerm lty, ITypeTerm rty, boolean toWorklist, Cause reason) {
        initBounds(lty, reason);
        initBounds(rty, reason);
        TypeConstraintSolverVariable leftLo = lowerBounds.get(lty);
        TypeConstraintSolverVariable leftHi = upperBounds.get(lty);
        TypeConstraintSolverVariable rightLo = lowerBounds.get(rty);
        TypeConstraintSolverVariable rightHi = upperBounds.get(rty);

        boolean tmp1 = false, tmp2 = false, tmp3 = false, tmp4 = false;
        if (!isTypeSource(lty)) {
            tmp1 = newStatement(leftLo, new TypeJoinOperator(this, lty, reason), rightLo, toWorklist, false);
            tmp2 = newStatement(leftHi, new TypeMeetOperator(this, lty, reason), rightHi, toWorklist, false);
        }
        if (!isTypeSource(rty)) {
            tmp3 = newStatement(rightLo, new TypeJoinOperator(this, rty, reason), leftLo, toWorklist, false);
            tmp4 = newStatement(rightHi, new TypeMeetOperator(this, rty, reason), leftHi, toWorklist, false);
        }

        equateMROMRW(lty, rty, toWorklist, reason);
        return tmp1 || tmp2 || tmp3 || tmp4;
    }

    boolean equateTypes(ITypeTerm lty, ITypeTerm rty, Cause reason) {
        return equateTypes(lty, rty, true, reason);
    }

    /**
     *
     * @param lty
     * @param rty
     * @return true if any new constraint was added, false otherwise
     */
    boolean equateTypes(Type lty, Type rty, Cause reason) {
        return equateTypes(factory.getTermForType(lty), factory.getTermForType(rty), reason);
    }

    /**
     * Generate constraints to equate function types lty and rty
     *
     * @return false if arities of functions don't match, true otherwise
     */
    public boolean equateFunctionTypes(CodeType lty, CodeType rty, Cause reason) {
        List<Type> subFunParamTypes = lty.paramTypes();
        int nrParams = subFunParamTypes.size();
        List<Type> superFunParamTypes = rty.paramTypes();
        if (nrParams != superFunParamTypes.size()) {
            return false;
        }
        for (int i = 0; i < nrParams; i++) {
            equateTypes(
                    factory.getTermForType(superFunParamTypes.get(i)),
                    factory.getTermForType(subFunParamTypes.get(i)),
                    reason);
        }
        equateTypes(
                factory.getTermForType(lty.returnType()),
                factory.getTermForType(rty.returnType()),
                reason);
        if (lty instanceof ConstructorType && rty instanceof ConstructorType) {
            // equate the prototype types
            ConstructorType lCons = (ConstructorType)lty;
            ConstructorType rCons = (ConstructorType)rty;
            Type lProto = lCons.getPrototype() != null ? lCons.getPrototype() : new ObjectType();
            Type rProto = rCons.getPrototype() != null ? rCons.getPrototype() : new ObjectType();
            equateTypes(lProto, rProto, reason);
        }
        return true;
    }

    /**
     * given an object and array type, ensures that the object properties are all
     * present on the array and that they have the same types
     * @param objType
     * @param arrType
     */
    void equateObjAndArrayOrStringType(PropertyContainer objType, PropertyContainer stringOrArrayType, Cause reason) {
        for (Property p : objType.properties()) {
            String name = p.getName();
            Type arrayPropType;
            try {
                arrayPropType = stringOrArrayType.getTypeForProperty(name);
            } catch (PropertyNotFoundException e) {
                throw new CoreException(e.getMessage(), reason);
            }
            Type objPropType = p.getType();
            equateTypes(arrayPropType, objPropType, reason);
        }
    }

    void equateObjAndPrimType(ObjectType objType, PrimitiveType primType, Cause reason) {
        for (Property p : objType.properties()) {
            String name = p.getName();
            Type primPropType;
            try {
                primPropType = primType.getTypeForProperty(name);
            } catch (PropertyNotFoundException e) {
                throw new CoreException(e.getMessage(), reason);
            }
            Type objPropType = p.getType();
            equateTypes(primPropType, objPropType, reason);
        }
    }

    private final Map<ITypeTerm, MROMRWVariable> term2MROMRW = HashMapFactory.make();

    // MRO/MRW stuff
    protected MROMRWVariable getMROMRWVarForTerm(ITypeTerm term) {
        MROMRWVariable result = term2MROMRW.get(term);
        if (result == null) {
            result = new MROMRWVariable(term);
            term2MROMRW.put(term,result);
        }
        return result;
    }

    public Map<AstNode, MROMRWVariable> getExternalMROMRW() {
        Map<AstNode, MROMRWVariable> result = HashMapFactory.make();
        term2MROMRW
                .keySet()
                .stream()
                .filter((term) -> {
                    return !(term instanceof TypeConstantTerm)
                            && term.getNode() != null
                            && term2MROMRW.get(term).nonEmpty();
                })
                .forEach(
                        (t) -> {
                            AstNode node = t.getNode();
                            if (result.containsKey(node)) {
                                assert result.get(node).sameAs(
                                        term2MROMRW.get(t)) : "something weird happened";
                            } else {
                                result.put(node, term2MROMRW.get(t));
                            }
                        });
        return result;
    }
}
