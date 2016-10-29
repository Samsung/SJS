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
package com.samsung.sjs.constraintgenerator;

import com.samsung.sjs.JSEnvironment;
import com.samsung.sjs.backend.RhinoToIR;
import com.samsung.sjs.typeconstraints.CheckArityConstraint;
import com.samsung.sjs.typeconstraints.ConcreteConstraint;
import com.samsung.sjs.typeconstraints.EnvironmentDeclarationTerm;
import com.samsung.sjs.typeconstraints.FunctionCallTerm;
import com.samsung.sjs.typeconstraints.FunctionParamTerm;
import com.samsung.sjs.typeconstraints.FunctionReturnTerm;
import com.samsung.sjs.typeconstraints.FunctionTerm;
import com.samsung.sjs.typeconstraints.ITypeConstraint;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.typeconstraints.IndexedTerm;
import com.samsung.sjs.typeconstraints.KeyTerm;
import com.samsung.sjs.typeconstraints.MethodReceiverTerm;
import com.samsung.sjs.typeconstraints.NameDeclarationTerm;
import com.samsung.sjs.typeconstraints.OperatorTerm;
import com.samsung.sjs.typeconstraints.PropertyAccessTerm;
import com.samsung.sjs.typeconstraints.ProtoParentTerm;
import com.samsung.sjs.typeconstraints.ProtoTerm;
import com.samsung.sjs.typeconstraints.SubTypeConstraint;
import com.samsung.sjs.typeconstraints.ThisTerm;
import com.samsung.sjs.typeconstraints.TypeConstantTerm;
import com.samsung.sjs.typeconstraints.TypeEqualityConstraint;
import com.samsung.sjs.typeconstraints.TypeParamTerm;
import com.samsung.sjs.typeconstraints.UnaryOperatorTerm;
import com.samsung.sjs.typeerrors.Explainer;
import com.samsung.sjs.types.AnyType;
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.AttachedMethodType;
import com.samsung.sjs.types.BooleanType;
import com.samsung.sjs.types.CodeType;
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.FloatType;
import com.samsung.sjs.types.FunctionType;
import com.samsung.sjs.types.IntegerType;
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.MapType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.StringType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.VoidType;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.BreakStatement;
import org.mozilla.javascript.ast.ConditionalExpression;
import org.mozilla.javascript.ast.ContinueStatement;
import org.mozilla.javascript.ast.DoLoop;
import org.mozilla.javascript.ast.ElementGet;
import org.mozilla.javascript.ast.EmptyExpression;
import org.mozilla.javascript.ast.EmptyStatement;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.IfStatement;
import org.mozilla.javascript.ast.InfixExpression;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NewExpression;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.NumberLiteral;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.ParenthesizedExpression;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.SwitchCase;
import org.mozilla.javascript.ast.SwitchStatement;
import org.mozilla.javascript.ast.UnaryExpression;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;
import org.mozilla.javascript.ast.WhileLoop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.samsung.sjs.typeerrors.TypeErrorMessage.badPropertyRead;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.badPropertyWrite;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.binaryOperatorMisuse;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.concretenessError;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.describeNode;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.describeType;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.describeTypeOf;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.genericTypeError;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.hasType;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.locationOf;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.shortSrc;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.subtypeError;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.typeEqualityError;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.unaryOperatorMisuse;
import static com.samsung.sjs.typeerrors.TypeErrorMessage.wrongNumberOfParametersAtCallSite;

public class ConstraintVisitor implements NodeVisitor {

	/**
	 * The factory used for creating constraint variables (ITerms) and constraints.
	 */
	private ConstraintFactory factory;

	/**
	 * The sets of constraints produced so far.
	 */
	public Set<ITypeConstraint> typeConstraints;

	/**
	 * We use a map to avoid the repeated processing of the same expression.
	 * This map associates a constraint variable with each AST node. If a map
	 * entry exists for a given node, it means that constraints have already been
	 * created for that node and its subexpressions.
	 */
	private Map<AstNode,ITypeTerm> theMap = new LinkedHashMap<AstNode, ITypeTerm>();

	private JSEnvironment jsEnv;

	private ConstraintGenerator generator;

	private final List<SyntaxError> errors;

	public ConstraintVisitor(ConstraintFactory factory, JSEnvironment jsEnv, ConstraintGenerator gen) {
		this.factory = factory;
		this.typeConstraints = new LinkedHashSet<ITypeConstraint>();
		this.jsEnv = jsEnv;
		this.generator = gen;
		this.errors = new ArrayList<>();
	}

	private void error(String message, AstNode node) {
		errors.add(new SyntaxError(message, node));
	}

	private ITypeTerm expError(String message, AstNode node) {
		error(message, node);
		return factory.findOrCreateTypeVariableTerm(factory.freshTypeVar());
	}

	public List<SyntaxError> getErrors() {
		return errors;
	}

	/**
	 * Retrieve the computed type constraints. To be invoked after passing the visitor to an AST.
	 */
	public Set<ITypeConstraint> getTypeConstraints(){
		return typeConstraints;
	}

	/**
	 * This method generates constraints for all relevant AstNodes. It delegates its work to various
	 * processXXX() methods that handle AstNodes of type XXX.
	 */
	@Override
	public boolean visit(AstNode node) {
		if (node instanceof VariableInitializer){
			processVariableInitializer(node);
		} else if (node instanceof ReturnStatement){
			processReturnStatement((ReturnStatement)node);
		} else if (node instanceof ExpressionStatement){
			processExpressionStatement((ExpressionStatement)node);
		} else if (node instanceof ForLoop){
			processForLoop((ForLoop)node);
		} else if (node instanceof ForInLoop){
			processForInLoop((ForInLoop)node);
		}else if (node instanceof WhileLoop){
			processWhileLoop((WhileLoop)node);
		} else if (node instanceof DoLoop){
			processDoLoop((DoLoop)node);
		} else if (node instanceof NewExpression){
			processNewExpression((NewExpression)node);
		} else if (node instanceof FunctionCall){
			processFunctionCall((FunctionCall)node);
		} else if (node instanceof ElementGet){
			processElementGet((ElementGet)node);
		} else if (node instanceof FunctionNode){
			processFunctionNode((FunctionNode)node);
		} else if (node instanceof IfStatement){
			processIfStatement((IfStatement)node);
		} else if (node instanceof KeywordLiteral){
			processKeywordLiteral((KeywordLiteral)node);
		} else if (node instanceof SwitchStatement){
			processSwitchStatement((SwitchStatement)node);
		} else if (node instanceof SwitchCase){
			processSwitchCase((SwitchCase)node);
		} else if ((node instanceof AstRoot) || //AstRoot: no constraints need to be generated
			(node instanceof BreakStatement) || //BreakStatement: no constraints need to be generated
			(node instanceof VariableDeclaration) || //VariableDeclaration: we generate constraints for its constituent VariableInitializer nodes
			(node instanceof Name) || //Name: generate constraints for complex expressions that refer to names
			(node instanceof NumberLiteral) || //NumberLiteral: generate constraints for complex expressions that refer to names
			(node instanceof StringLiteral) || //StringLiteral: generate constraints for complex expressions that refer to names
			(node instanceof Assignment) || // Assignment is a special case of InfixExpression
			(node instanceof ArrayLiteral) ||
			(node instanceof UnaryExpression) ||
			(node instanceof InfixExpression) ||
			(node instanceof ConditionalExpression) ||
			(node instanceof ParenthesizedExpression) ||
			(node instanceof EmptyExpression) ||
			(node instanceof ObjectLiteral) ||
			(node instanceof EmptyStatement) ||
			(node instanceof ContinueStatement) ||
			(node instanceof Scope) ||
			(node instanceof Block)){ // // occurs in programs with for loops -- nothing to be done here?
			/* nothing */
		} else {
			error("unsupported node " + node.toSource().trim() + " of type: " + node.getClass().getName(), node);
		}
		return true;
	}

	/**
	 * constructor call
	 */
	private void processNewExpression(NewExpression ne) {
		processFunctionCall(ne);

		// in `new X(...)`, the target X must be a constructor
		ITypeTerm target = processExpression(ne.getTarget());
		int nrParams = ne.getArguments().size();
		List<Type> paramTypes = new ArrayList<>(nrParams);
		List<String> paramNames = new ArrayList<>(nrParams);
		for (int i = 0; i < nrParams; i++) {
			paramTypes.add(factory.freshTypeVar());
			paramNames.add("arg" + i);
		}
		Type returnType = factory.freshTypeVar();
		Type protoType = factory.freshTypeVar();
		Type ctorType = new ConstructorType(paramTypes, paramNames, returnType, protoType);
		addSubTypeConstraint(target, factory.getTermForType(ctorType), ne.getLineno(), null);
	}


	/**
	 * For a FunctionNode, determine if it needs to be processed as a function
	 * or a method.  See: processFunctionNodeForFunction() and  processFunctionNodeForMethod()
	 */
	private ITypeTerm processFunctionNode(FunctionNode node) {
        ITypeTerm funTerm = ConstraintGenUtil.isConstructor(node) ? findOrCreateFunctionTerm(
                node, FunctionTerm.FunctionKind.Constructor)
                : ((ConstraintGenUtil.isMethod(node)) ? findOrCreateFunctionTerm(
                        node, FunctionTerm.FunctionKind.Method)
                        : findOrCreateFunctionTerm(node,
                                FunctionTerm.FunctionKind.Function));
        if (ConstraintGenUtil.isConstructor(node)) {
            Set<String> writtenPrototypeProps = ConstraintGenUtil.getWrittenPrototypeProps(node);
            if (!writtenPrototypeProps.isEmpty()) {
                ConstructorType cType = (ConstructorType) funTerm.getType();
                ObjectType protoType = new ObjectType(writtenPrototypeProps
                        .stream()
                        .map(s -> new Property(s, new AnyType(), false))
                        .collect(Collectors.toList()));
                cType.setPrototype(protoType);
            }
        }
		createFunctionNodeConstraints(node, funTerm);

		// for functions and methods, generate a constraint to equate the return type to void, if needed
		if (!ConstraintGenUtil.returnsValue(node) && !ConstraintGenUtil.isConstructor(node)){
			ITypeTerm voidTerm = new TypeConstantTerm(new VoidType());
			ITypeTerm funReturnTerm = findOrCreateFunctionReturnTerm(funTerm, node.getParamCount(), node.getLineno(), null);
			addTypeEqualityConstraint(funReturnTerm, voidTerm, node.getLineno(),
					(solution) -> genericTypeError("function does not return a value", locationOf(node))
							.withNote(describeTypeOf(funReturnTerm, solution) + " was expected"));
		}

		if (ConstraintGenUtil.isConstructor(node)){
			ITypeTerm ctorProtoTerm = findOrCreateProtoTerm(funTerm, node.getLineno());
			ITypeTerm funReturnTerm = findOrCreateFunctionReturnTerm(funTerm, node.getParamCount(), node.getLineno(), null);
			ITypeTerm objectProtoTerm = findOrCreateProtoParentTerm(funReturnTerm, node.getLineno());
			addTypeEqualityConstraint(ctorProtoTerm, objectProtoTerm, node.getLineno(), null);
		}
		return funTerm;
	}

	/**
	 * For a function definition (FunctionNode), create constraints equating its
	 * parameters to param(.) variables, and equate the type of the function name
	 * to the type of the entire function definition.
	 */
	private void createFunctionNodeConstraints(FunctionNode node, ITypeTerm funTerm){
		// if the function has a name, equate the types of the function node and the function name
		Name funName = node.getFunctionName();
		if (funName != null){
			ITypeTerm nameTerm = findOrCreateNameDeclarationTerm(funName);
			addSubTypeConstraint(funTerm, nameTerm, node.getLineno(), null);
		}

		// for a function f with params v1, v2, ... generate param(|f|,i) = |v_i|
		for (int i=0; i < node.getParamCount(); i++){
			AstNode param = node.getParams().get(i);
			if (param instanceof Name){
				Name name = (Name)param;
				ITypeTerm nameVar = findOrCreateNameDeclarationTerm(name);
				ITypeTerm paramVar = findOrCreateFunctionParamTerm(funTerm, i, node.getParamCount(), node.getLineno());
				addTypeEqualityConstraint(paramVar, nameVar, param.getLineno(), null);
			} else {
				error("createFunctionNodeConstraints: unexpected parameter type", node);
			}
		}

	}

	/**
	 * generate constraints that relate the types of corresponding actual parameters
	 * and formal parameters. Multiple cases exist because the call may be through
	 * a closure, and it may be to an external function.
	 */
	private void processFunctionCall(FunctionCall fc) {
		AstNode target = fc.getTarget();
		if (target instanceof PropertyGet){
			PropertyGet pg = (PropertyGet)target;
			Name property = pg.getProperty();
			AstNode receiver = pg.getTarget();
			ITypeTerm receiverTerm = processExpression(receiver);

			// for a property access p.e(..) equate expression |p.e| with prop(|p|,e)
			ITypeTerm expTerm = findOrCreateExpressionTerm(pg);
			ITypeTerm propTerm = findOrCreatePropertyAccessTerm(receiverTerm, property.getIdentifier(), pg);
			addTypeEqualityConstraint(expTerm, propTerm, fc.getLineno(),
					(solution) -> typeEqualityError("cannot call " + shortSrc(receiver) + " has no method " + property.getIdentifier(),
							solution.typeOfTerm(expTerm), solution.typeOfTerm(propTerm), locationOf(fc)));

			// NOTE we don't call processCopy here, since our prohibition of detaching methods
			// ensures the receiver type is compatible.  we must still ensure the receiver
			// is concrete
			addConcreteConstraint(receiverTerm, fc.getLineno(),
					(solution) -> concretenessError(receiver, receiverTerm, solution));
		}
		processClosureCall(fc);
	}

	/**
	 * call a function through a closure
	 */
	private void processClosureCall(FunctionCall fc) {

		AstNode target = fc.getTarget();
		ITypeTerm funVar = processExpression(target);

		// for call foo(E_1,...,E_n), equate the type of the call to ret(foo)
		FunctionCallTerm callTerm = findOrCreateFunctionCallTerm(fc);
		callTerm.setTarget(funVar);
		ITypeTerm retTerm = findOrCreateFunctionReturnTerm(funVar, fc.getArguments().size(), fc.getLineno(), fc);
		addTypeEqualityConstraint(callTerm, retTerm, fc.getLineno(), null);

		// for call foo(E_1,...,E_n), generate constraints |E_i| <: Param(foo,i)
		for (int i=0; i < fc.getArguments().size(); i++){
			AstNode arg = fc.getArguments().get(i);
			ITypeTerm argExp = processExpression(arg);
			ITypeTerm paramExp = findOrCreateFunctionParamTerm(funVar, i, fc.getArguments().size(), fc.getLineno());
			processCopy(arg, argExp, paramExp,
					fc.getLineno(), (solution) ->
							subtypeError("bad argument " + shortSrc(arg) + " passed to function",
									solution.typeOfTerm(argExp), solution.typeOfTerm(paramExp), locationOf(arg)));
		}
	}

	/**
	 * For an ElementGet a[x], generate constraint |a[x]| = Elem(a), and return Elem(a).
	 * Also require the index to be an integer
	 */
	private ITypeTerm processElementGet(ElementGet eg) {
		AstNode target = eg.getTarget();
		AstNode element = eg.getElement();
		ITypeTerm egTerm = findOrCreateExpressionTerm(eg);
		ITypeTerm targetExp = processExpression(target);

		// for an expression e1[e2], generate constraint Elem(|e1|) = |e1[e2]|
		ITypeTerm elementVariable = findOrCreateIndexedTerm(targetExp, eg.getLineno());
		addTypeEqualityConstraint(elementVariable, egTerm, eg.getLineno(), null);

		// for arrays we want to restrict the index expression to be an integer, and
		// for maps, we want to restrict it to be a string. Since we don't know at
		// constraint generation time whether an array or map is being indexed, we
		// generate a constraint that requires the index expression to be equal to
		// the "key type" of the base expression's type. The key type for an ArrayType
		// is integer, and the key type for a MapType is (for now) string.
		ITypeTerm indexTerm = processExpression(element);
		ITypeTerm keyTerm = findOrCreateKeyTerm(targetExp, eg.getLineno());
		addTypeEqualityConstraint(indexTerm, keyTerm, eg.getLineno(),
				(solution) -> typeEqualityError("wrong key type used on " + describeNode(target, targetExp, solution),
						solution.typeOfTerm(indexTerm), solution.typeOfTerm(keyTerm), locationOf(eg)));

		return egTerm;
	}

	/**
	 * generate constraints for a for-loop
	 */
	private void processForLoop(ForLoop loop) {
		AstNode initializer = loop.getInitializer();
		if (initializer instanceof Assignment){
			processAssignment((Assignment)initializer);
		}
		AstNode condition = loop.getCondition();
		processExpression(condition);
		AstNode incrementExp = loop.getIncrement();
		processExpression(incrementExp);
	}

	/**
	 * generate constraints for a for-in loop
	 */
	private void processForInLoop(ForInLoop loop) {
		AstNode it = loop.getIterator();
		AstNode obj = loop.getIteratedObject();
		ITypeTerm objTerm = processExpression(obj);
		MapType mapType = new MapType(factory.freshTypeVar());
        addSubTypeConstraint(
                objTerm,
                findOrCreateTypeTerm(mapType, loop.getLineno()),
                loop.getLineno(),
                (solution) -> typeEqualityError("for-in loop can only iterate over map objects; " + obj.toSource() + " is not a map",
                        solution.typeOfTerm(objTerm), mapType, locationOf(obj)));
		if (it instanceof VariableDeclaration){
			VariableDeclaration vd = (VariableDeclaration)it;
			for (VariableInitializer vi : vd.getVariables()){
				AstNode target = vi.getTarget();
				if (target instanceof Name){
					ITypeTerm leftTerm = findOrCreateNameDeclarationTerm((Name)target);
                    addSubTypeConstraint(
                            leftTerm,
                            findOrCreateTypeTerm(StringType.make(),
                                    loop.getLineno()),
                            loop.getLineno(),
                            (solution) -> typeEqualityError(
                                    "loop variable " + it.toSource() + " of for-in loop must have string type",
                                    solution.typeOfTerm(leftTerm),
                                    StringType.make(), locationOf(it)));
				}
			}
		} else {
			error("unhandled type of iterator in for-in loop: " + it.getClass().getName(), it);
		}
	}

	/**
	 * generate constraints for an if-statement
	 */
	private void processIfStatement(IfStatement ifs) {
		AstNode condition = ifs.getCondition();
		processExpression(condition);
	}

	/**
	 * generate constraints for a switch-statement
	 */
	private void processSwitchStatement(SwitchStatement ss) {
		AstNode exp = ss.getExpression();
		processExpression(exp);
	}

	/**
	 * generate constraints for a switch-statement
	 */
	private void processSwitchCase(SwitchCase sc) {
		SwitchStatement ss = (SwitchStatement)sc.getParent();
		if (sc.getExpression() != null){ // default case has no expression
			ITypeTerm switchExpTerm = findOrCreateExpressionTerm(ss.getExpression());
			ITypeTerm switchCaseTerm = processExpression(sc.getExpression());
			addTypeEqualityConstraint(switchExpTerm, switchCaseTerm, sc.getLineno(),
					(solution) -> genericTypeError("switch expression must have the same type as its cases", locationOf(sc)));
		}
	}

	/**
	 * generate constraints for a while-statement
	 */
	private void processWhileLoop(WhileLoop w) {
		AstNode condition = w.getCondition();
		processExpression(condition);
	}

	/**
	 * generate constraints for a do-while-statement
	 */
	private void processDoLoop(DoLoop w) {
		AstNode condition = w.getCondition();
		processExpression(condition);
	}

	/**
	 * generate constraints for expression statements.
	 */
	private void processExpressionStatement(ExpressionStatement node) throws Error {
		processExpression(node.getExpression());
	}

	/**
	 * generate constraints for return statements
	 */
	private void processReturnStatement(ReturnStatement rs) throws Error {
		FunctionNode fun = ConstraintGenUtil.findEnclosingFunction(rs);
		FunctionTerm.FunctionKind funType =
				ConstraintGenUtil.isConstructor(fun) ? FunctionTerm.FunctionKind.Constructor :
				(ConstraintGenUtil.isMethod(fun) ? FunctionTerm.FunctionKind.Method : FunctionTerm.FunctionKind.Function);

		ITypeTerm funTerm = findOrCreateFunctionTerm(fun, funType);
		ITypeTerm returnTerm = findOrCreateFunctionReturnTerm(funTerm, fun.getParamCount(), rs.getLineno(), null);
		AstNode exp = rs.getReturnValue();
		if (exp != null){
			ITypeTerm expTerm = processExpression(exp);
			addSubTypeConstraint(expTerm, returnTerm, rs.getLineno(),
					(solution) -> subtypeError("bad return value " + shortSrc(exp),
							solution.typeOfTerm(expTerm), solution.typeOfTerm(returnTerm), locationOf(rs)));
		} else {
			ITypeTerm voidTerm = findOrCreateTypeTerm(new VoidType(), rs.getLineno());
			addTypeEqualityConstraint(voidTerm, returnTerm, rs.getLineno(),
					(solution) -> genericTypeError("missing return value", locationOf(rs))
							.withNote("expected " + describeTypeOf(returnTerm, solution)));
		}
	}


	/**
	 * generate constraints for initializers
	 */
	private void processVariableInitializer(AstNode node) throws Error {
		VariableInitializer vi = (VariableInitializer)node;
		AstNode varname = vi.getTarget();
		AstNode initializer = vi.getInitializer();
		if (varname instanceof Name){
            ITypeTerm leftTerm = findOrCreateNameDeclarationTerm((Name)varname); // declaration of variable, so no need to create ITerm for expression
			if (initializer != null){
				ITypeTerm rightTerm = processExpression(initializer);
				processCopy(initializer, rightTerm, leftTerm,
						node.getLineno(), (solution) -> subtypeError("bad initialization of " + varname.toSource(),
								solution.typeOfTerm(rightTerm), solution.typeOfTerm(leftTerm), locationOf(node)));
			}
			// in case of multiple declarations of same identifier,
			// equate the type of this particular Name to the type of
			// the canonical Name
            ITypeTerm localTerm = factory.findOrCreateNameDeclTermNoLookup((Name) varname);
            if (localTerm != leftTerm) {
                addTypeEqualityConstraint(localTerm, leftTerm, node.getLineno(),
                        (solution) -> typeEqualityError("different declarations within the same scope must have the same type",
                                solution.typeOfTerm(localTerm), solution.typeOfTerm(leftTerm), locationOf(node)));

            }
		} else {
			error("unsupported type of VariableInitializer", varname);
		}
	}

	/**
	 * generate constraints for assignments
	 */
	private void processAssignment(Assignment a) throws Error {
		AstNode left = a.getLeft();
		AstNode right = a.getRight();
		ITypeTerm expTerm = findOrCreateExpressionTerm(a);
		if (left instanceof Name){
			processAssignToVariable(a, left, right, expTerm);
		} else if (left instanceof PropertyGet) {
			PropertyGet pg = (PropertyGet)left;
			if (pg.getProperty().getIdentifier().equals("prototype")){
				processAssignToPrototype(a, left, right, expTerm);
			} else {
				processAssignToProperty(a, left, right, expTerm);
			}
			processExpression(pg.getTarget()); // TEST
		} else if (left instanceof ElementGet){
			processIndexedAssignment(a, left, right, expTerm);
		} else {
			error("unsupported LHS type in Assignment: " + left.getClass().getName(), left);
		}
	}

	/**
	 * assignment to an array element or map element: v[e] = ...
	 */
	private void processIndexedAssignment(Assignment a, AstNode left, AstNode right, ITypeTerm expTerm) throws Error {
		ElementGet eg = (ElementGet)left;
		AstNode base = eg.getTarget();
		AstNode element = eg.getElement();

		// generate the appropriate constraints for the expression being indexed
		processExpression(base);

		// an assignment expression has the same type as its left-hand side
		ITypeTerm leftTerm = findOrCreateExpressionTerm(eg);
		ITypeTerm rightTerm = processExpression(right);
		addTypeEqualityConstraint(expTerm, leftTerm, a.getLineno(), null);

		// require index to be of the appropriate type
		ITypeTerm elementTerm = processExpression(element);
		ITypeTerm baseTerm = findOrCreateExpressionTerm(base);
		addTypeEqualityConstraint(elementTerm, findOrCreateKeyTerm(baseTerm, eg.getLineno()), a.getLineno(),
				(solution) -> genericTypeError("indexes must be strings or ints", locationOf(element)));
		processCopy(right, rightTerm, leftTerm,
				a.getLineno(), (solution) -> subtypeError("bad assignment of " + shortSrc(right) + " to " + shortSrc(left),
						solution.typeOfTerm(rightTerm), solution.typeOfTerm(leftTerm), locationOf(a)));
	}

	/**
	 * assignment to the "prototype" property
	 */
	private void processAssignToPrototype(Assignment a, AstNode left, AstNode right, ITypeTerm expTerm) throws Error {
		PropertyGet pg = (PropertyGet)left;
		AstNode base = pg.getTarget();
		ITypeTerm pgTerm = findOrCreateExpressionTerm(pg);
		if (base instanceof Name){
			Name name = (Name)base;
			if (!validRHSForAssignToPrototype(right)) {
				error(
						"expression "
								+ right.toSource()
								+ " cannot be assigned to a constructor prototype (line "
								+ right.getLineno() + ")", a);
			}
			// can only write to prototype immediately after declaration of
			// constructor of the same name
			AstNode parent = a.getParent();
			if (!(parent instanceof ExpressionStatement)) {
				error(
						"assignment to prototype property not allowed here (line "
								+ a.getLineno() + ")", a);
				return;
			}
			Node prev = getPredecessorNode(parent);
			if (!(prev instanceof FunctionNode)) {
				error(
						"assignment to prototype property only allowed after constructor declaration (line "
								+ a.getLineno() + ")", a);
				return;
			}
			FunctionNode fn = (FunctionNode) prev;
			String functionName = fn.getName();
			String identifier = name.getIdentifier();
			if (!functionName.equals(identifier)) {
				error(
						"can only assign to prototype of function "
								+ functionName + " here (line " + a.getLineno()
								+ ")", a);
				return;
			}
			ITypeTerm baseTerm = findOrCreateExpressionTerm(base); // make term for expression
			ITypeTerm nameTerm = findOrCreateNameDeclarationTerm(name); // find unique representative for referenced Name
			addTypeEqualityConstraint(baseTerm, nameTerm, a.getLineno(), null); // equate them
			ITypeTerm protoTerm = findOrCreateProtoTerm(baseTerm, pg.getLineno());
			ITypeTerm rightTerm = processExpression(right);
			addTypeEqualityConstraint(pgTerm, protoTerm, a.getLineno(), null);
			addTypeEqualityConstraint(rightTerm, protoTerm, a.getLineno(), null);
			addTypeEqualityConstraint(expTerm, protoTerm, a.getLineno(), null);
		} else {
			error("processAssignToPrototype: unsupported case for receiver expression: " + base.getClass().getName(), base);
		}
	}

    /**
     * is right a valid expression to assign into the prototype field of a
     * constructor?  currently we allow object literals, constructor calls, and
     * expressions of the form B.prototype
     *
     * @param right
     * @return
     */
    private boolean validRHSForAssignToPrototype(AstNode right) {
        if (right instanceof ObjectLiteral || right instanceof NewExpression) {
            return true;
        }
        if (right instanceof PropertyGet) {
            PropertyGet pg = (PropertyGet) right;
            if (pg.getProperty().getIdentifier().equals("prototype")) {
                return true;
            }
        }
        return false;
    }

	/**
	 * Get the predecessor sibling of n, ignoring {@link EmptyStatement}s.
	 *
	 * This code is not that clean, due to how Rhino's APIs work.
	 * @param n
	 * @return
	 */
	private Node getPredecessorNode(AstNode n) {
		AstNode parent = n.getParent();
		Node prev = null;
		boolean found = false;
		for (Node child: parent) {
			if (child instanceof EmptyStatement) {
				continue;
			}
			if (child.equals(n)) {
				found = true;
				break;
			} else {
				prev = child;
			}
		}
		assert found;
		return prev;
	}

	/**
	 * assignment to an object property
	 */
	private void processAssignToProperty(Assignment a, AstNode left, AstNode right, ITypeTerm expTerm) throws Error {

		PropertyGet pg = (PropertyGet)left;
		AstNode base = pg.getTarget();
		Name prop = pg.getProperty();
		ITypeTerm pgTerm = findOrCreateExpressionTerm(pg);

		ITypeTerm baseTerm;
		if (base instanceof KeywordLiteral && ConstraintGenUtil.isThis(base)){
			baseTerm = findOrCreateThisTerm(base);
		} else {
			baseTerm = generateReceiverConstraints(pg);
		}

		int assignLineNo = a.getLineno();
		// detect assignments of the form C.prototype.foo = ...
		if (base instanceof PropertyGet) {
			PropertyGet basePG = (PropertyGet) base;
			String baseProp = basePG.getProperty().getIdentifier();
			if (baseProp.equals("prototype")) {
				checkForValidProtoPropAssign(a, pg, assignLineNo, basePG);
			}
		}
		ITypeTerm leftTerm = findOrCreatePropertyAccessTerm(baseTerm, prop.getIdentifier(), null);
		ITypeTerm rightTerm = processExpression(right);
		addTypeEqualityConstraint(pgTerm, leftTerm, assignLineNo, (solution) ->
				typeEqualityError("incompatible types",
						solution.typeOfTerm(leftTerm), solution.typeOfTerm(pgTerm), locationOf(pg)));
		addTypeEqualityConstraint(expTerm, leftTerm, assignLineNo, (solution) ->
				typeEqualityError("incompatible types",
						solution.typeOfTerm(leftTerm), solution.typeOfTerm(expTerm), locationOf(a)));
		processCopy(right, rightTerm, leftTerm, assignLineNo,
				(solution) -> badPropertyWrite(pg, solution.typeOfTerm(baseTerm), hasType(leftTerm, solution) ? solution.typeOfTerm(leftTerm) : null, solution.typeOfTerm(rightTerm)));
	}

	private void checkForValidProtoPropAssign(Assignment a, PropertyGet pg,
			int assignLineNo, PropertyGet basePG) {
		AstNode baseTarget = basePG.getTarget();
		if (!(baseTarget instanceof Name)) {
			error("assignment to property of prototype not valid (line " + assignLineNo + ")", a);
			return;
		}
		Name baseName = (Name) baseTarget;
		AstNode parent = a.getParent();
		if (!(parent instanceof ExpressionStatement)) {
			error("assignment to property of prototype not valid (line " + assignLineNo + ")", a);
			return;
		}
		Node prev = getPredecessorNode(parent);
		if (prev instanceof FunctionNode) {
			FunctionNode fn = (FunctionNode) prev;
			String functionName = fn.getName();
			String identifier = baseName.getIdentifier();
			if (!functionName.equals(identifier)) {
				error("can only assign to prototype of function " + functionName + " here (line " + assignLineNo + ")", a);
				return;
			}

		} else if (prev instanceof ExpressionStatement) {
			// it needs to be an assignment either to C.prototype or C.prototype.foo
			// TODO clean up this gross code
			AstNode expression = ((ExpressionStatement)prev).getExpression();
			if (!(expression instanceof Assignment)) {
				error("assignment to property of prototype not valid (line " + assignLineNo + ")", a);
				return;
			}
			Assignment prevAssign = (Assignment) expression;
			AstNode prevLeft = prevAssign.getLeft();
			if (!(prevLeft instanceof PropertyGet)) {
				error("assignment to property of prototype not valid (line " + assignLineNo + ")", a);
				return;
			}
			PropertyGet prevPG = (PropertyGet) prevLeft;
			AstNode prevPGTarget = prevPG.getTarget();
			if (prevPG.getProperty().getIdentifier().equals("prototype")) {
				checkForSameName(assignLineNo, baseName, prevPGTarget);
			} else if (prevPGTarget instanceof PropertyGet) {
				PropertyGet prevPGBasePG = (PropertyGet) prevPGTarget;
				if (!prevPGBasePG.getProperty().getIdentifier().equals("prototype")) {
					error("assignment to property of prototype not valid (line " + assignLineNo + ")", a);
					return;
				}
				checkForSameName(assignLineNo, baseName, prevPGBasePG.getTarget());
			} else {
				error("assignment to property of prototype not valid (line " + assignLineNo + ")", a);
				return;
			}
		} else {
			error("assignment to property of prototype not valid (line " + assignLineNo + ")", a);
			return;
		}
	}

	private void checkForSameName(int assignLineNo, Name baseName,
			AstNode other) {
		if (!(other instanceof Name)) {
			error("assignment to property of prototype not valid (line " + assignLineNo + ")", baseName);
			return;
		}
		if (!baseName.getIdentifier().equals(((Name)other).getIdentifier())) {
			error("assignment to property of prototype not valid (line " + assignLineNo + ")", baseName);
			return;
		}
	}

	/**
	 * Generate constraints for the (possibly nested) receiver of a property-get expression.
	 */
	private ITypeTerm generateReceiverConstraints(PropertyGet pg) throws Error {
		AstNode base = pg.getTarget();
		ITypeTerm baseTerm = findOrCreateExpressionTerm(base); // make term for expression
		if (base instanceof Name) {
			Name name = (Name)base;
			ITypeTerm nameTerm = findOrCreateNameDeclarationTerm(name); // find unique representative for referenced Name
			addTypeEqualityConstraint(baseTerm, nameTerm, base.getLineno(), null);
		} else if (base instanceof PropertyGet) {
			PropertyGet basePG = (PropertyGet)base;
			ITypeTerm bbaseTerm = generateReceiverConstraints(basePG);
			String baseProperty = basePG.getProperty().getIdentifier();
			ITypeTerm basePATerm;
			if (basePG.getProperty().getIdentifier().equals("prototype")){
				basePATerm = findOrCreateProtoTerm(bbaseTerm, pg.getLineno());
			} else {
				basePATerm = findOrCreatePropertyAccessTerm(bbaseTerm, baseProperty, basePG);
			}
			addTypeEqualityConstraint(baseTerm, basePATerm, basePG.getLineno(), null);
		} else if (base instanceof KeywordLiteral && ConstraintGenUtil.isThis(base)){
			processExpression(base);
			//error("unsupported property get with base this: "+pg.toSource());
		} else if (base instanceof ElementGet) {
		    ElementGet baseEGet = (ElementGet) base;
		    processElementGet(baseEGet);
		} else {
			System.err.println("base = " + base.toSource() + ", type = " + base.getClass().getName() );
			error("unsupported property get: " + pg.toSource(), pg);
		}
		return baseTerm;
	}



	/**
	 * assignment to a variable
	 */
	private void processAssignToVariable(Assignment a, AstNode left, AstNode right, ITypeTerm expTerm) {
		ITypeTerm leftTerm = findOrCreateExpressionTerm(left);
		ITypeTerm nameTerm = findOrCreateNameDeclarationTerm((Name) left); // find unique representative for the Name
		addTypeEqualityConstraint(leftTerm, nameTerm, a.getLineno(), null); // equate to the LHS expression
		ITypeTerm rightTerm = processExpression(right);
		addTypeEqualityConstraint(expTerm, leftTerm, a.getLineno(),
				(solution) -> genericTypeError("assignment " + shortSrc(a), locationOf(a)));
		processCopy(right, rightTerm, leftTerm,
				a.getLineno(),
				(solution) -> subtypeError(shortSrc(right) + " assigned to " + shortSrc(left),
						solution.typeOfTerm(rightTerm), solution.typeOfTerm(leftTerm), locationOf(a)));
	}


	/**
	 * Creates constraints for the subtree rooted at a designated expression node,
	 * and returns a constraint variable corresponding to the root of the tree.
	 */
	private ITypeTerm processExpression(AstNode n){

		ITypeTerm cached = theMap.get(n);
		if (cached != null) return cached;

		if (n instanceof Name){
			return processVariableReference((Name)n);
		} else if (n instanceof NumberLiteral){
			return processNumericConstant((NumberLiteral)n);
		} else if (n instanceof StringLiteral){
			return processStringLiteral((StringLiteral)n);
		} else if (ConstraintGenUtil.isBooleanConstant(n)){
			return processBooleanConstant(n);
		} else if (n instanceof UnaryExpression){
			return processUnaryExpression((UnaryExpression)n);
		} else if (n instanceof InfixExpression){
			return processInfixExpression((InfixExpression)n);
		} else if (n instanceof FunctionCall){
			return processFunctionCallExpression((FunctionCall)n);
		} else if (n instanceof ArrayLiteral){
			return processArrayLiteral((ArrayLiteral)n);
		} else if (n instanceof ElementGet){
			return processElementGet((ElementGet)n);
		} else if (n instanceof ParenthesizedExpression) {
			return processParenthesizedExpression((ParenthesizedExpression)n);
		} else if (n instanceof ConditionalExpression) {
			return processConditionalExpression((ConditionalExpression)n);
		} else if (n instanceof ObjectLiteral) {
			return processObjectLiteral((ObjectLiteral)n);
		} else if (n instanceof KeywordLiteral){
			return processKeywordLiteral((KeywordLiteral)n);
		} else if (n instanceof FunctionNode){
			return processFunctionNode((FunctionNode)n);
		} else if (n instanceof EmptyExpression){
			return processEmptyExpression((EmptyExpression)n);
		} else {
			System.err.println(n.toSource());
			return expError("unimplemented case in findOrCreateExpressionVariable: " + n.getClass().getName(), n);
		}
	}

	/**
	 * handle empty expressions (used e.g. in for loops).
	 */
	private ITypeTerm processEmptyExpression(EmptyExpression n) {
		return  findOrCreateExpressionTerm(n);
	}

	/**
	 * generate constraints for object literals.
	 */
	private ITypeTerm processObjectLiteral(ObjectLiteral n) {

		if (ConstraintGenUtil.isObject(n)){
			return processObjectLiteralForObject(n);
		} else if (ConstraintGenUtil.isMap(n)){
			return processObjectLiteralForMap(n);
		} else {
			return expError("inconsistent use of quotes in object literal: " + n.toSource(), n);
		}
	}

	/**
	 * Create constraints for an object literal.
	 */
	private ITypeTerm processObjectLiteralForObject(ObjectLiteral n) {
		ITypeTerm expTerm = findOrCreateObjectLiteralTerm(n);
		ObjectLiteral o = (ObjectLiteral)n;
		for (ObjectProperty prop : o.getElements()){
			AstNode left = prop.getLeft();
			AstNode right = prop.getRight();
			if (left instanceof Name){
				String identifier = ((Name)left).getIdentifier();

				// for object literal o = { name_1 : exp_1, ..., name_k : exp_k } generate
				// a constraint |exp_i| <: prop(|o|, name_i)

				ITypeTerm propTerm = findOrCreatePropertyAccessTerm(expTerm, identifier, null);
				ITypeTerm valTerm = processExpression(right);
				processCopy(right, valTerm, propTerm, n.getLineno(), null);
			}
		}
		return expTerm;
	}

	/**
	 * Create constraints for a map literal.
	 */
	private ITypeTerm processObjectLiteralForMap(ObjectLiteral o) {
		ITypeTerm expTerm = findOrCreateMapLiteralTerm(o);
		for (ObjectProperty prop : o.getElements()){
			AstNode left = prop.getLeft();
			AstNode right = prop.getRight();
			if (left instanceof StringLiteral){

				// for map literal o = { name_1 : exp_1, ..., name_k : exp_k } generate
				// a constraint |exp_i| <: MapElem(|o|)

				ITypeTerm mapAccessTerm = findOrCreateIndexedTerm(expTerm, o.getLineno());
				ITypeTerm valTerm = processExpression(right);
				processCopy(right, valTerm, mapAccessTerm,
						o.getLineno(), (solution) ->
								genericTypeError("map does not have a homogenous value type", locationOf(prop))
										.withNote("map value type is " + describeTypeOf(mapAccessTerm, solution))
										.withNote("key " + left.toSource() + " has type " + describeTypeOf(valTerm, solution)));
			}
		}
		return expTerm;
	}

	/**
	 * process KeywordLiteral. This includes "null", "this".
	 */
	private ITypeTerm processKeywordLiteral(KeywordLiteral kw){
		ITypeTerm expTerm = findOrCreateExpressionTerm(kw);

		// for references to "this", equate the type to the object literal containing
		// the surrounding function, or to the return type of the enclosing constructor.
		if (ConstraintGenUtil.isThis(kw)){
			FunctionNode fun = ConstraintGenUtil.findEnclosingFunction(kw);
			if (fun == null) {
				error("'this' may not be used outside a function (at line " + kw.getLineno() + ')', kw);
			} else if (ConstraintGenUtil.isConstructor(fun)) {
				ThisTerm thisTerm = findOrCreateThisTerm(kw);
				ITypeTerm ctorTerm = findOrCreateFunctionTerm(fun, FunctionTerm.FunctionKind.Constructor);
				ITypeTerm constructorReturnTerm = findOrCreateFunctionReturnTerm(ctorTerm, fun.getParamCount(), kw.getLineno(), null);
				addTypeEqualityConstraint(thisTerm, constructorReturnTerm, kw.getLineno(), null);
				addTypeEqualityConstraint(thisTerm, expTerm, kw.getLineno(), null);
			} else if (ConstraintGenUtil.isMethod(fun)){
				ThisTerm thisTerm = findOrCreateThisTerm(kw);
				// equate the type of 'this' to the type of the receiver term
				ITypeTerm methodTerm = findOrCreateFunctionTerm(fun,
						FunctionTerm.FunctionKind.Method);
				ITypeTerm receiverTerm = findOrCreateMethodReceiverTerm(
						methodTerm, fun.getParamCount(), kw.getLineno());
				addTypeEqualityConstraint(thisTerm, receiverTerm,
						kw.getLineno(), null);
				addTypeEqualityConstraint(thisTerm, expTerm, kw.getLineno(), null);
			} else {
				error("this accessed in Function not recognized as method: " + fun.toSource(), fun);
			}
		}

		return expTerm;
	}

	/**
	 * for an expression that consists of a simple reference to a variable, we create an ITerm
	 * for that expression, find the unique representative for the referenced variable, and
	 * generate a constraint that equates these.
	 */
	private ITypeTerm processVariableReference(Name name) {
		ITypeTerm expTerm = findOrCreateExpressionTerm(name);
		if (!ConstraintGenUtil.isGlobal(name)){ // no need for NameDeclarationTerm for global variables
			Name declaration = ConstraintGenUtil.findDecl(name);
			ITypeTerm nameTerm = findOrCreateNameDeclarationTerm(declaration);
			addTypeEqualityConstraint(expTerm, nameTerm, name.getLineno(), null);
		} else {
			String identifier = name.getIdentifier();
			if (identifier.equals("undefined")){
				addTypeEqualityConstraint(new TypeParamTerm(name), findOrCreateExpressionTerm(name), name.getLineno(), null);
			} else {
				ITypeTerm globalTerm = findOrCreateGlobalDeclarationTerm(name, jsEnv);
				addTypeEqualityConstraint(globalTerm, expTerm, name.getLineno(), null);
				createConstraintsForGlobalFunction(name);
			}
		}
		return expTerm;
	}

	/**
	 * Create constraints for parameterized external functions such as Array<T>
	 */
	private void createConstraintsForGlobalFunction(Name name) {
		ITypeTerm term = findOrCreateExpressionTerm(name);
		String functionName = name.getIdentifier();
		Type type = jsEnv.get(functionName);
		if (type == null){
			error("reference to unknown global: " + name.getIdentifier(), name);
			return;
		}
		ITypeTerm typeParamTerm = new TypeParamTerm(name);
		if (ConstraintGenUtil.isParameterized(type)){
			generateConstraintsForType(name, typeParamTerm, term, type);
		}
	}

	/**
	 * Analyze the signature of a generic type and generate constraints that equate
	 * a variable to terms corresponding to all occurrences of type parameters
	 */
	private void generateConstraintsForType(Name name, ITypeTerm TP, ITypeTerm term, Type type) {

		if (type instanceof IntersectionType){
			IntersectionType iType = (IntersectionType)type;
			for (Type t : iType.getTypes()){
				generateConstraintsForType(name, TP, term, t);
			}
		} else if (type instanceof ArrayType && ((ArrayType)type).elemType().isVar()){
		// Array<T> : TP = Elem(term)
			addTypeEqualityConstraint(TP, findOrCreateIndexedTerm(term, name.getLineno()), name.getLineno(), null);
		} else if (type instanceof FunctionType){
			FunctionType fType = (FunctionType)type;
			Type returnType = fType.returnType();
			// (...) -> Array<T> : TP = Elem(ret(term))
			if (returnType instanceof ArrayType && ((ArrayType)returnType).elemType().isVar()){
				ITypeTerm retTerm = findOrCreateFunctionReturnTerm(term, fType.nrParams(), name.getLineno(), null);
				addTypeEqualityConstraint(TP, findOrCreateIndexedTerm(retTerm, name.getLineno()), name.getLineno(), null);
				createArrayConstraints(retTerm, name);
			}
			for (int i=0; i < fType.nrParams(); i++){
				Type paramType = fType.paramTypes().get(i);

				// (..., T, ...) -> Array<T> : TP = param(term, i)
				if (paramType.isVar()){
					ITypeTerm paramTerm = findOrCreateFunctionParamTerm(term, i, fType.nrParams(), name.getLineno());
					addTypeEqualityConstraint(TP, paramTerm, name.getLineno(), null);
				}
			}
		}
	}

	/**
	 * For a conditional expression e1 ? e2 : e3, create an ITerm representing the
	 * type of the entire expression and equate that term to the ITerms representing
	 * the type of e2 and e3.
	 *
	 * We do not require the test expression to be a boolean; we can handle the implicit
	 * conversions for this case.
	 */
	private ITypeTerm processConditionalExpression(ConditionalExpression ce) {
		ITypeTerm condTerm = findOrCreateExpressionTerm(ce);
		// still process the test expression to generate any appropriate
		// nested constraints
		processExpression(ce.getTestExpression());
		ITypeTerm trueTerm = processExpression(ce.getTrueExpression());
		ITypeTerm falseTerm = processExpression(ce.getFalseExpression());
		addSubTypeConstraint(trueTerm, condTerm, ce.getLineno(),
				(solution) -> subtypeError("conditional expression branches are incompatible",
						solution.typeOfTerm(trueTerm), solution.typeOfTerm(condTerm), locationOf(ce)));
		addSubTypeConstraint(falseTerm, condTerm, ce.getLineno(),
				(solution) -> subtypeError("conditional expression branches are incompatible",
						solution.typeOfTerm(falseTerm), solution.typeOfTerm(condTerm), locationOf(ce)));
		return condTerm;
	}

	/**
	 * For parenthesized expressions return a term representing the expression. generate
	 * an equality constraint that equates this term to the subexpression being parenthesized
	 */
	private ITypeTerm processParenthesizedExpression(ParenthesizedExpression pe) {
		ITypeTerm parenTerm = findOrCreateExpressionTerm(pe);
		ITypeTerm subExpTerm = processExpression(pe.getExpression());
		addTypeEqualityConstraint(parenTerm, subExpTerm, pe.getLineno(), null);
		return parenTerm;
	}

	/**
	 * create constraint variable for the array literal. create subtype constraints between expressions
	 * in the literal and the array's element type
	 */
	private ITypeTerm processArrayLiteral(ArrayLiteral lit) {
		ITypeTerm arrayTerm = findOrCreateArrayLiteralTerm(lit);

		ITypeTerm elemTerm = findOrCreateIndexedTerm(arrayTerm, lit.getLineno());
		List<AstNode> elements = lit.getElements();
		for (AstNode litElem : elements){
			ITypeTerm litElemTerm = processExpression(litElem);
			processCopy(litElem, litElemTerm, elemTerm,
					lit.getLineno(), (solution) -> subtypeError("array cannot contain " + shortSrc(litElem),
							solution.typeOfTerm(litElemTerm), solution.typeOfTerm(elemTerm), locationOf(litElem)));
		}

		createArrayConstraints(arrayTerm, lit);

		return arrayTerm;
	}

	/**
	 * generate constraints reflecting a copy from rhs to lhs, e.g.,
	 * an assignment or initialization of a literal field.
	 * @param rhsNode the AST node associated with the right-hand side
	 * @param rhs
	 * @param lhs
	 * @param lineNumber
	 */
	private void processCopy(AstNode rhsNode, ITypeTerm rhs, ITypeTerm lhs, int lineNumber, Explainer explainer) {
		if (ConstraintGenUtil.isNullUndefinedLitOrVoidOp(rhs)) {
			addTypeEqualityConstraint(rhs, lhs, lineNumber, explainer);
		} else {
			addSubTypeConstraint(rhs, lhs, lineNumber, explainer);
		}
		// for now, we do not allow copies of values with abstract type
		addConcreteConstraint(rhs, lineNumber,
				(solution) -> concretenessError(rhsNode, rhs, solution));
	}

	/**
	 * generate constraints for the properties of a built-in array<T>.  Initially, all parameters
	 * of array methods are unconstrained. Then, we create a fresh variable TP and equate it to
	 * the array's element type, and to all positions in method signatures whose type is the
	 * array's type parameter.
	 */
	private void createArrayConstraints(ITypeTerm arrayTerm, AstNode node) {
		ITypeTerm typeParamTerm = new TypeParamTerm(node);
		for (Property prop : ArrayType.getParameterizedProperties().values()){
			String name = prop.getName();
			Type type = prop.getType();

			if (type instanceof AttachedMethodType){
				AttachedMethodType mType = (AttachedMethodType)type;
				ITypeTerm term = findOrCreatePropertyAccessTerm(arrayTerm, name, null);

				Type returnType = mType.returnType();
				if (returnType instanceof ArrayType && ((ArrayType)returnType).elemType().isVar()){
					// for call of the form (...) -> Array<T>, generate constraint TP = elem(ret(term))
					ITypeTerm returnTerm = findOrCreateIndexedTerm(findOrCreateFunctionReturnTerm(term, 0, node.getLineno(), null), node.getLineno());
					addTypeEqualityConstraint(typeParamTerm, returnTerm, node.getLineno(), null);
				} else if (returnType.isVar()){
					// for call of the form foo(...) -> T, generate constraint TP = ret(term)
					ITypeTerm returnTerm = findOrCreateFunctionReturnTerm(term, mType.nrParams(), node.getLineno(), null);
					addTypeEqualityConstraint(typeParamTerm, returnTerm, node.getLineno(), null);
				}

				for (int i=0; i < mType.nrParams(); i++){
					Type paramType = mType.paramTypes().get(i);
					if (paramType.isVar()){

						// for call of the form foo(...T...), generate constraint TP = param(term, i)
						ITypeTerm paramTerm = findOrCreateFunctionParamTerm(term, i, mType.nrParams(), node.getLineno());
						addTypeEqualityConstraint(typeParamTerm, paramTerm, node.getLineno(), null);
					}
				}
			}
		}

		// TP(e) = Elem(|Array|)
		ITypeTerm elemTerm = findOrCreateIndexedTerm(arrayTerm, node.getLineno());
		addTypeEqualityConstraint(typeParamTerm, elemTerm, node.getLineno(), null);
	}

	/**
	 * for function calls, return a constraint variable that corresponds to the
	 * callee's return type. The generation of constraints that require the type of each actual
	 *  parameter to be a subtype of the type of the corresponding formal parameter happens
	 *  in processFunctionCallParams
	 */
	private ITypeTerm processFunctionCallExpression(FunctionCall fc) {
		return findOrCreateFunctionCallTerm(fc);
	}

	/**
	 * for unary expressions, we ignore implicit coercions for now, and simply
	 * force the type of the operand and the type of the entire expression to be the same
	 */
	private ITypeTerm processUnaryExpression(UnaryExpression u) {
		int operator = u.getOperator();
		AstNode operand = u.getOperand();
		ITypeTerm uTerm = findOrCreateExpressionTerm(u);
		if (operator == Token.VOID){ // void operator is handled specially
			/*ITypeTerm operandTerm =*/ processExpression(operand);
			TypeParamTerm typeParamTerm = new TypeParamTerm(u);
			addTypeEqualityConstraint(typeParamTerm, uTerm, u.getLineno(), null);
			return uTerm;
		} else if (operator == Token.BITNOT){
			ITypeTerm operandTerm = processExpression(operand);
			UnaryOperatorTerm opTerm = findOrCreateUnaryOperatorTerm("~", operandTerm, u.isPrefix(), u.getLineno());
			addTypeEqualityConstraint(uTerm, opTerm, u.getLineno(), (solution) -> unaryOperatorMisuse(u, solution.typeOfTerm(operandTerm), solution.typeOfTerm(opTerm)));
			theMap.put(u, opTerm);
			return opTerm;
		} else if (operator == Token.TYPEOF){
			ITypeTerm operandTerm = processExpression(operand);
			UnaryOperatorTerm opTerm = findOrCreateUnaryOperatorTerm("typeof", operandTerm, u.isPrefix(), u.getLineno());
			addTypeEqualityConstraint(uTerm, opTerm, u.getLineno(), (solution) -> unaryOperatorMisuse(u, solution.typeOfTerm(operandTerm), solution.typeOfTerm(opTerm)));
			theMap.put(u, opTerm);
			return opTerm;
		}  else if (operator == Token.INC){
			ITypeTerm operandTerm = processExpression(operand);
			UnaryOperatorTerm opTerm = findOrCreateUnaryOperatorTerm("++", operandTerm, u.isPrefix(), u.getLineno());
			addTypeEqualityConstraint(uTerm, opTerm, u.getLineno(), (solution) -> unaryOperatorMisuse(u, solution.typeOfTerm(operandTerm), solution.typeOfTerm(opTerm)));
			theMap.put(u, opTerm);
			return opTerm;

		} else if (operator == Token.DEC){
			ITypeTerm operandTerm = processExpression(operand);
			UnaryOperatorTerm opTerm = findOrCreateUnaryOperatorTerm("--", operandTerm, u.isPrefix(), u.getLineno());
			addTypeEqualityConstraint(uTerm, opTerm, u.getLineno(), (solution) -> unaryOperatorMisuse(u, solution.typeOfTerm(operandTerm), solution.typeOfTerm(opTerm)));
			theMap.put(u, opTerm);
			return opTerm;
		}  else if (operator == Token.NEG){
			ITypeTerm operandTerm = processExpression(operand);
			UnaryOperatorTerm opTerm = findOrCreateUnaryOperatorTerm("-", operandTerm, u.isPrefix(), u.getLineno());
			addTypeEqualityConstraint(uTerm, opTerm, u.getLineno(), (solution) -> unaryOperatorMisuse(u, solution.typeOfTerm(operandTerm), solution.typeOfTerm(opTerm)));
			theMap.put(u, opTerm);
			return opTerm;
		} else if (operator == Token.NOT){
			ITypeTerm operandTerm = processExpression(operand);
			UnaryOperatorTerm opTerm = findOrCreateUnaryOperatorTerm("!", operandTerm, u.isPrefix(), u.getLineno());
			addTypeEqualityConstraint(uTerm, opTerm, u.getLineno(), (solution) -> unaryOperatorMisuse(u, solution.typeOfTerm(operandTerm), solution.typeOfTerm(opTerm)));
			theMap.put(u, opTerm);
			return opTerm;
		} else if (operator == Token.POS){
			ITypeTerm operandTerm = processExpression(operand);
			UnaryOperatorTerm opTerm = findOrCreateUnaryOperatorTerm("+", operandTerm, u.isPrefix(), u.getLineno());
			addTypeEqualityConstraint(uTerm, opTerm, u.getLineno(), (solution) -> unaryOperatorMisuse(u, solution.typeOfTerm(operandTerm), solution.typeOfTerm(opTerm)));
			theMap.put(u, opTerm);
			return opTerm;
		}  else {
			return expError("unsupported operator " + operator + " in expression " + u.toSource(), u);
		}
	}

	/**
	 *  for infix expressions, we ignore implicit coercions for now. For arithmetic
	 *  operators, we assume the type of the entire expression to be the same as that
	 *  of either operand. For comparison operators, we require operands to have
	 *  the same type, and assume that the result is a boolean. Note that Assignments
	 *  are also InfixExpressions and that some property-get operations show up as
	 *  InfixExpressions for which the operator is GETPROP.
	 */
	private ITypeTerm processInfixExpression(InfixExpression i) throws Error {
		int operator = i.getOperator();
		AstNode leftOperand = i.getLeft();
		AstNode rightOperand = i.getRight();
		ITypeTerm iTerm = findOrCreateExpressionTerm(i);
		switch (operator){
			case Token.GETPROP:
				return processPropertyGet(i, leftOperand, rightOperand);
			case Token.ASSIGN:
			case Token.ASSIGN_ADD:
			case Token.ASSIGN_SUB:
			case Token.ASSIGN_MUL:
			case Token.ASSIGN_DIV:
			case Token.ASSIGN_BITAND:
			case Token.ASSIGN_BITOR:
			case Token.ASSIGN_BITXOR:
			case Token.ASSIGN_RSH:
				processAssignment((Assignment)i);
			return iTerm;
			case Token.ADD:
            case Token.SUB:
            case Token.MUL:
            case Token.DIV:
            case Token.MOD:
            case Token.BITOR:
            case Token.EQ:
            case Token.LE:
            case Token.LT:
            case Token.NE:
            case Token.GT:
            case Token.GE:
            case Token.SHNE:
            case Token.SHEQ:
            case Token.AND:
            case Token.OR:
            case Token.BITAND:
            case Token.BITXOR:
            case Token.LSH:
            case Token.RSH:
            case Token.URSH:
            case Token.IN:
				ITypeTerm leftTerm = processExpression(leftOperand);
				ITypeTerm rightTerm = processExpression(rightOperand);
				OperatorTerm opTerm = findOrCreateOperatorTerm(RhinoToIR.decodeRhinoOperator(operator), leftTerm, rightTerm, i.getLineno());
				addTypeEqualityConstraint(iTerm, opTerm, i.getLineno(), (solution) -> binaryOperatorMisuse(i, solution.typeOfTerm(leftTerm), solution.typeOfTerm(rightTerm), solution.typeOfTerm(opTerm)));
				break;
			default:
				error("unexpected infix operator: " + operator + "(" + RhinoToIR.decodeRhinoOperator(operator) + ") in " + i.toSource(), i);
		}
		theMap.put(i, iTerm);
		return iTerm;
	}

	/**
	 * generate constraints for PropertyGet operations
	 *
	 */
	private ITypeTerm processPropertyGet(InfixExpression i, AstNode leftOperand, AstNode rightOperand) throws Error {
		assert (rightOperand instanceof Name);
		Name name = (Name)rightOperand;

		ITypeTerm expTerm = findOrCreateExpressionTerm(i);
		if (name.getIdentifier().equals("prototype")){
			ITypeTerm leftOperandTerm = processExpression(leftOperand);
			ITypeTerm protoTerm = findOrCreateProtoTerm(leftOperandTerm, i.getLineno());
			addTypeEqualityConstraint(expTerm, protoTerm, i.getLineno(), (solution) -> {
				if (solution.typeOfTerm(leftOperandTerm) instanceof CodeType && solution.typeOfTerm(leftOperandTerm).isConstructor()) {
					return subtypeError("prototype has unexpected type", solution.typeOfTerm(protoTerm), solution.typeOfTerm(expTerm), locationOf(i));
				} else {
					return genericTypeError(".prototype is only legal on constructors", locationOf(i))
							.withNote("type of " + shortSrc(leftOperand) + " is " + describeType(solution.typeOfTerm(leftOperandTerm)));
				}
			});
		} else {
			ITypeTerm leftOperandTerm = processExpression(leftOperand);
			ITypeTerm iTerm = findOrCreatePropertyAccessTerm(leftOperandTerm, name.getIdentifier(), null);

			// for property-get a.x, generate constraint |a.x| = prop(|a|, x)
//			ITypeTerm expTerm = findOrCreateExpressionTerm(i);
			addTypeEqualityConstraint(expTerm, iTerm, i.getLineno(),
					(solution) -> badPropertyRead(i, solution.typeOfTerm(leftOperandTerm), hasType(iTerm, solution) ? solution.typeOfTerm(iTerm) : null, solution.typeOfTerm(expTerm)));

		}
		theMap.put(i, expTerm);
		return expTerm;
	}

	/**
	 * for boolean constants, returns an ITerm representing the expression. A separate
	 * equality constraint is generated that equates that term to boolean
	 */
	private ITypeTerm processBooleanConstant(AstNode n) {
		ITypeTerm expTerm = findOrCreateExpressionTerm(n);
		ITypeTerm boolConst = findOrCreateTypeTerm(BooleanType.make(), n.getLineno());
		addTypeEqualityConstraint(expTerm, boolConst, n.getLineno(), null);
		return expTerm;
	}

	/**
	 * for string constants, returns an ITerm representing the expression. A separate
	 * equality constraint is generated that equates that term to string
	 */
	private ITypeTerm processStringLiteral(StringLiteral n) {
		ITypeTerm expTerm = findOrCreateExpressionTerm(n);
		ITypeTerm stringConst = findOrCreateTypeTerm(StringType.make(), n.getLineno());
		addTypeEqualityConstraint(expTerm, stringConst, n.getLineno(), null);
		return expTerm;
	}

	/**
	 * for numeric constants, returns an ITerm representing the expression. A separate
	 * equality constraint is generated that equates that term to a constant of the
	 * appropriate type
	 */
	private ITypeTerm processNumericConstant(NumberLiteral lit) {
	    double number = lit.getNumber();
	    // check if it's a mathematical integer that fits in the int32 range,
	    // and that it wasn't explicitly written with a decimal point
		if (!lit.getValue().contains(".") && number == (int) number){
			ITypeTerm expTerm = findOrCreateExpressionTerm(lit);
			ITypeTerm intConst = findOrCreateTypeTerm(IntegerType.make(), lit.getLineno());
			addTypeEqualityConstraint(expTerm, intConst, lit.getLineno(), null);
			return expTerm;
		} else {
			ITypeTerm expTerm = findOrCreateExpressionTerm(lit);
			ITypeTerm floatConst = findOrCreateTypeTerm(FloatType.make(), lit.getLineno());
			addTypeEqualityConstraint(expTerm, floatConst, lit.getLineno(), null);
			return expTerm;
		}
	}

	// --------------------------------------------------------------------------------------
	// Methods for creating terms. These forward to factory methods and handle
	// tracking of line numbers

	private ITypeTerm findOrCreateExpressionTerm(AstNode exp){
		ITypeTerm term = factory.findOrCreateExpressionTerm(exp);
		generator.addTermLineNumber(term, exp.getLineno());
		return term;
	}

	private ITypeTerm findOrCreateTypeTerm(Type type, int lineno){
		TypeConstantTerm term = factory.findOrCreateTypeTerm(type);
		generator.addTermLineNumber(term, lineno);
		return term;
	}

	private ITypeTerm findOrCreatePropertyAccessTerm(ITypeTerm term, String name, PropertyGet pgNode){
		PropertyAccessTerm t = factory.findOrCreatePropertyAccessTerm(term, name, pgNode);
		generator.addTermLineNumber(t, pgNode != null ? pgNode.getLineno() : -1);
		return t;
	}

	private ITypeTerm findOrCreateProtoTerm(ITypeTerm term, int lineno){
		ProtoTerm t = factory.findOrCreateProtoTerm(term);
		generator.addTermLineNumber(t, lineno);
		return t;
	}

	private ITypeTerm findOrCreateProtoParentTerm(ITypeTerm term, int lineno) {
		ProtoParentTerm t = factory.findOrCreateProtoParentTerm(term);
		generator.addTermLineNumber(t, lineno);
		return t;
	}

	private ITypeTerm findOrCreateNameDeclarationTerm(Name name) {
		NameDeclarationTerm t;
		try {
			t = factory.findOrCreateNameDeclarationTerm(name);
		} catch (Error e) { // TODO: factory method should throw something meaningful or return null
			return expError("unknown name '" + name.getIdentifier() + '\'', name);
		}
		generator.addTermLineNumber(t, name.getLineno());
		return t;
	}

	private ITypeTerm findOrCreateFunctionTerm(FunctionNode fun, FunctionTerm.FunctionKind funType){
		FunctionTerm t = factory.findOrCreateFunctionTerm(fun, funType);
		generator.addTermLineNumber(t, fun.getLineno());
		return t;
	}

	private ITypeTerm findOrCreateFunctionParamTerm(ITypeTerm term, int param, int nrParams, int lineno){
		FunctionParamTerm t = factory.findOrCreateFunctionParamTerm(term, param, nrParams);
		generator.addTermLineNumber(t, lineno);
		return t;
	}

	private ITypeTerm findOrCreateFunctionReturnTerm(ITypeTerm term, int nrParams, int lineno, AstNode callSite){
		FunctionReturnTerm t = factory.findOrCreateFunctionReturnTerm(term, nrParams);
		generator.addTermLineNumber(t, lineno);
		if (callSite != null) {
			addCheckArityConstraint(t, lineno, (solution) ->
					wrongNumberOfParametersAtCallSite(
							((CodeType) t.getFunctionTerm().getType()).nrParams(),
							nrParams,
							callSite));
		}
		return t;
	}

	private ITypeTerm findOrCreateMethodReceiverTerm(ITypeTerm term, int nrParams, int lineno){
		MethodReceiverTerm t = factory.findOrCreateMethodReceiverTerm(term, nrParams);
		generator.addTermLineNumber(t, lineno);
		return t;
	}

	private FunctionCallTerm findOrCreateFunctionCallTerm(FunctionCall fc){
		FunctionCallTerm t = factory.findOrCreateFunctionCallTerm(fc);
		generator.addTermLineNumber(t, fc.getLineno());
		return t;
	}

	private ITypeTerm findOrCreateIndexedTerm(ITypeTerm term, int lineno){
		IndexedTerm t = factory.findOrCreateIndexedTerm(term);
		generator.addTermLineNumber(t, lineno);
		return t;
	}

	private ITypeTerm findOrCreateKeyTerm(ITypeTerm term, int lineno){
		KeyTerm t = factory.findOrCreateKeyTerm(term);
		generator.addTermLineNumber(t, lineno);
		return t;
	}

	private ThisTerm findOrCreateThisTerm(AstNode node){
		ThisTerm t = factory.findOrCreateThisTerm(node);
		generator.addTermLineNumber(t, node.getLineno());
		return t;
	}

	private ITypeTerm findOrCreateObjectLiteralTerm(ObjectLiteral lit){
		ITypeTerm t = factory.findOrCreateObjectLiteralTerm(lit);
		generator.addTermLineNumber(t, lit.getLineno());
		return t;
	}

	private ITypeTerm findOrCreateMapLiteralTerm(ObjectLiteral lit){
		ITypeTerm t = factory.findOrCreateMapLiteralTerm(lit);
		generator.addTermLineNumber(t, lit.getLineno());
		return t;
	}

	private OperatorTerm findOrCreateOperatorTerm(String op, ITypeTerm left, ITypeTerm right, int lineno){
		OperatorTerm t = factory.findOrCreateOperatorTerm(op, left, right);
		generator.addTermLineNumber(t, lineno);
		return t;
	}

	private UnaryOperatorTerm findOrCreateUnaryOperatorTerm(String op, ITypeTerm operand, boolean isPrefix, int lineno){
		UnaryOperatorTerm t = factory.findOrCreateUnaryOperatorTerm(op, operand, isPrefix);
		generator.addTermLineNumber(t, lineno);
		return t;
	}


	private ITypeTerm findOrCreateGlobalDeclarationTerm(Name name, JSEnvironment env){
		EnvironmentDeclarationTerm t = factory.findOrCreateGlobalDeclarationTerm(name, env);
		generator.addTermLineNumber(t, name.getLineno());
		return t;
	}

	private ITypeTerm findOrCreateArrayLiteralTerm(ArrayLiteral lit){
		ITypeTerm t = factory.findOrCreateArrayLiteralTerm(lit);
		generator.addTermLineNumber(t, lit.getLineno());
		return t;
	}

	/**
	 * Add an equality constraint. Update linenumber information as needed.
	 * @param left one term
	 * @param right another term
	 * @param lineno the line related to this constraint
	 * @param exp the explainer to use if this constraint needs to be broken.
	 *			A null value indicates that this constraint may never be broken.
	 * @return true iff this constraint did not already exist
	 */
	private boolean addTypeEqualityConstraint(ITypeTerm left, ITypeTerm right, int lineno, Explainer exp) {
		TypeEqualityConstraint constraint = factory.findOrCreateTypeEqualityConstraint(left, right);
		generator.addSourceLineNumber(constraint, lineno);
		generator.mapExplanation(constraint, exp);
		return typeConstraints.add(constraint);
	}

	/**
	 * Add a subtype constraint. Update linenumber information as needed.
	 * @param left the smaller term
	 * @param right the larger term
	 * @param lineno the line related to this constraint
	 * @param exp the explainer to use if this constraint needs to be broken.
	 *			A null value indicates that this constraint may never be broken.
	 * @return true iff this constraint did not already exist
	 */
	private boolean addSubTypeConstraint(ITypeTerm left, ITypeTerm right, int lineno, Explainer exp) {
		SubTypeConstraint constraint = factory.findOrCreateSubTypeConstraint(left, right);
		generator.addSourceLineNumber(constraint, lineno);
		generator.mapExplanation(constraint, exp);
		return typeConstraints.add(constraint);
	}

	/**
	 * Add a constraint that the given term be concrete.
	 * @param term the term to constrain
	 * @param lineno the line related to this constraint
	 * @param exp the explainer to use if this constraint needs to be broken.
	 *			A null value indicates that this constraint may never be broken.
	 * @return true iff this constraint did not already exist
	 */
	private boolean addConcreteConstraint(ITypeTerm term, int lineno, Explainer exp) {
		ConcreteConstraint constraint = factory.findOrCreateConcreteConstraint(term);
		generator.addSourceLineNumber(constraint, lineno);
		generator.mapExplanation(constraint, exp);
		return typeConstraints.add(constraint);
	}

	private boolean addCheckArityConstraint(FunctionReturnTerm term, int lineno, Explainer explainer) {
		CheckArityConstraint constraint = factory.findOrCreateCheckArityConstraint(term);
		generator.addSourceLineNumber(constraint, lineno);
		generator.mapExplanation(constraint, explainer);
		return typeConstraints.add(constraint);
	}

}
