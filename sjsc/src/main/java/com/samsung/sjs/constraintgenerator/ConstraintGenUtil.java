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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.Assignment;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.EmptyStatement;
import org.mozilla.javascript.ast.ExpressionStatement;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.KeywordLiteral;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NodeVisitor;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.ReturnStatement;
import org.mozilla.javascript.ast.Scope;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.UnaryExpression;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.types.AnyType;
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.AttachedMethodType;
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.FunctionType;
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.MapType;
import com.samsung.sjs.types.NamedObjectType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.TypeVariable;

/**
 * Utility functions used to implement constraint generation.
 *
 * @author ftip
 *
 */
public class ConstraintGenUtil {

	/**
	 * Check if an AstNode corresponds to a boolean constant.
	 */
	static boolean isBooleanConstant(AstNode node){
		if (node instanceof KeywordLiteral){
			KeywordLiteral kw = (KeywordLiteral)node;
			if (kw.toSource().equals("true") || kw.toSource().equals("false")){
				return true;
			}
		}
		return false;
	}

	/**
	 * Find return statements within a FunctionNode and checks if any of
	 * them returns a value.
	 *
	 */
	private static class ReturnStatementVisitor implements NodeVisitor {

		public ReturnStatementVisitor(FunctionNode fun){
			this.fun = fun;
		}

		@Override
		public boolean visit(AstNode node) {
			if (node instanceof ReturnStatement){
				ReturnStatement rs = (ReturnStatement)node;
				if (rs.getReturnValue() != null){
					if (ConstraintGenUtil.findEnclosingFunction(node) == fun){
						returnsValue = true;
					}
				}
			}
			return true;
		}

		public boolean returnsValue(){
			return this.returnsValue;
		}

		private FunctionNode fun;
		private boolean returnsValue = false;
	};

	/**
	 * Indicates if the function returns a value.
	 */
	static boolean returnsValue(FunctionNode node){
		ReturnStatementVisitor visitor = new ReturnStatementVisitor(node);
		node.visit(visitor);
		return visitor.returnsValue();
	}


	/**
	 * Check if an AstNode corresponds to "null"
	 */
	static boolean isNullConstant(AstNode node){
		if (node instanceof KeywordLiteral){
			KeywordLiteral kw = (KeywordLiteral)node;
			if (kw.toSource().equals("null")){
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if an AstNode corresponds to the "this" reference.
	 *
	 * @param node
	 * @return
	 */
	public static boolean isThis(AstNode node){
		if (node instanceof KeywordLiteral){
			KeywordLiteral kw = (KeywordLiteral)node;
			if (kw.toSource().equals("this")){
				return true;
			}
		}
		return false;
	}

	/**
	 * find the function definition that immediately surrounds a given AST node.
	 * Returns null if there is no surrounding function.
	 *
	 */
	public static FunctionNode findEnclosingFunction(AstNode node) {
		AstNode current = node;
		while (current != null && !(current instanceof FunctionNode)){
			current = current.getParent();
		}
		FunctionNode fun = (FunctionNode)current;
		return fun;
	}

	public static Assignment findEnclosingAssignment(AstNode node) {
		AstNode current = node;
		while (current != null && !(current instanceof Assignment)){
			current = current.getParent();
		}
		Assignment a = (Assignment)current;
		return a;
	}

	/**
	 * Determine if a variable is declared outside the program.
	 * @param n
	 * @return
	 */
	static boolean isExternal(Name n){
		return findDecl(n) == null;
	}

	/**
	 * Finds the declaration of the called function. Assumes that the call's target
	 * is a Name, which is passed as the second parameter.
	 *
	 */
	static FunctionNode findFunDecl(FunctionCall fc, Name funName){
		List<FunctionNode> funsFound = findFunDecl2(fc, new ArrayList<FunctionNode>());
		for (int i=0; i < funsFound.size(); i++){
			FunctionNode fun = funsFound.get(i);
			if (funName.getIdentifier().equals(fun.getName())){
				return fun;
			}
		}
		return null;
	}

	/**
	 * Helper function for implementing findFunDecl(). Recursively walks up the AST
	 * to find functions available in enclosing scopes. Implemented because Rhinos's
	 * built-in support for finding enclosing scopes is highly erratic.
	 */
	private static List<FunctionNode> findFunDecl2(FunctionCall fc, ArrayList<FunctionNode> foundSoFar) {
		AstNode node = fc.getParent();
		while (node != null){
			if (node instanceof FunctionNode){
				FunctionNode fn = (FunctionNode)node;
				foundSoFar.add(fn);
			} else if (node instanceof AstRoot || node instanceof Block || node instanceof Scope){
				for (Iterator<Node> it = node.iterator(); it.hasNext(); ){
					Node n = it.next();
					if (n instanceof FunctionNode){
						FunctionNode fn = (FunctionNode)n;
							foundSoFar.add(fn);
					}
				}
			}
			node = node.getParent();
		}
		return foundSoFar;
	}

	/**
	 * Indicates if a given Name is a global variable
	 */
	static boolean isGlobal(Name name){
		return findDecl(name) == null;
	}

	/**
	 * Finds the declaration of the referenced Name.
	 */
	static Name findDecl(Name name){
		List<Name> declsFound = findDecl2(name);
		for (int i=0; i < declsFound.size(); i++){
			Name name2 = declsFound.get(i);
			if (name.getIdentifier().equals(name2.getIdentifier())){
				return name2;
			}
		}
		return null;
	}

	/**
	 * Helper function for implementing findDecl. Iteratively walks up the AST
	 * to find variables available in enclosing scopes. Implemented because Rhinos's
	 * built-in support for finding enclosing scopes is highly erratic.
	 */
	private static List<Name> findDecl2(Name name){
	    List<Name> foundSoFar = new ArrayList<>();
	    // maps an identifier to its canonical Name declaration in the current scope
	    Map<String,Name> foundInCurrentScope = HashMapFactory.make();
	    Consumer<Name> addToScope = (Name newName) -> {
	        String identifier = newName.getIdentifier();
            Name curName = foundInCurrentScope.get(identifier);
	        if (curName == null || newName.getAbsolutePosition() < curName.getAbsolutePosition()) {
	            foundInCurrentScope.put(identifier, newName);
	        }
	    };
		AstNode node = name.getParent();
		while (node != null){
			if (node instanceof FunctionNode){
				FunctionNode fn = (FunctionNode)node;
				for (AstNode param : fn.getParams()){
					if (param instanceof Name){
						Name paramName = (Name)param;
						addToScope.accept(paramName);
					}
				}
				Name funName = fn.getFunctionName();
				if (funName != null){
					addToScope.accept(funName);
				}
				// clear out current scope before going to parent
				foundSoFar.addAll(foundInCurrentScope.values());
				foundInCurrentScope.clear();
			}  else if (node instanceof AstRoot || node instanceof Block || node instanceof Scope){
				for (Iterator<Node> it = node.iterator(); it.hasNext(); ){
					Node n = it.next();
					if (n instanceof VariableDeclaration){
						VariableDeclaration vd = (VariableDeclaration)n;
						for (VariableInitializer vi : vd.getVariables()){
							AstNode target = vi.getTarget();
							if (target instanceof Name){
								Name targetName = (Name)target;
								addToScope.accept(targetName);
							}
						}
					} else if (n instanceof ForLoop){
						ForLoop fl = (ForLoop)n;
						AstNode init = fl.getInitializer();
						if (init instanceof VariableDeclaration){
							VariableDeclaration vd = (VariableDeclaration)init;
							for (VariableInitializer vi : vd.getVariables()){
								AstNode target = vi.getTarget();
								if (target instanceof Name){
									Name targetName = (Name)target;
									addToScope.accept(targetName);
								}
							}
						}
						AstNode body = fl.getBody();
						if (body instanceof Scope){
							Scope scope = (Scope)body;
							List<AstNode> stmts = scope.getStatements();
							for (AstNode stmt : stmts){
								if (stmt instanceof VariableDeclaration){
									VariableDeclaration vd = (VariableDeclaration)stmt;
									for (VariableInitializer vi : vd.getVariables()){
										AstNode target = vi.getTarget();
										if (target instanceof Name){
											Name targetName = (Name)target;
											addToScope.accept(targetName);
										}
									}
								}
							}
						}

					} else if (n instanceof ForInLoop){
						ForInLoop fl = (ForInLoop)n;
						AstNode iterator = fl.getIterator();
						if (iterator instanceof VariableDeclaration){
							VariableDeclaration vd = (VariableDeclaration)iterator;
							for (VariableInitializer vi : vd.getVariables()){
								AstNode target = vi.getTarget();
								if (target instanceof Name){
									Name targetName = (Name)target;
									addToScope.accept(targetName);
								}
							}
						}
					}else if (n instanceof FunctionNode){
						FunctionNode fn = (FunctionNode)n;
						Name funName = fn.getFunctionName();
						addToScope.accept(funName);
					}
				}
			}
			node = node.getParent();
		}
		// clear out top-level scope
		foundSoFar.addAll(foundInCurrentScope.values());
		return foundSoFar;
	}

	/**
	 * Remove start and end quotes from a string literal. Assumes the string is quoted.
	 */
	public static String removeQuotes(String s){
		assert(s.charAt(0) == '\'' || s.charAt(0) == '\"');
		assert(s.charAt(s.length()-1) == '\'' || s.charAt(s.length()-1) == '\"');
		return s.substring(1, s.length() - 1);
	}

	public static String quote(String s){
		return "\"" + s + "\"";
	}

	/**
	 * Tests if an object literal is an object by checking that all
	 * properties are unquoted.
	 */
	static boolean isObject(ObjectLiteral o){
		boolean result = (o.getElements().size() > 0);
		for (ObjectProperty prop : o.getElements()){
			AstNode left = prop.getLeft();
			result = result && (left instanceof Name);
		}
		return result;
	}

	/**
	 * Tests if an object literal is a map by checking that
	 * all properties are quoted.
	 * In JavaScript, both double quotes and single quotes are
	 * supported but for now we assume double quotes are used.
	 *
	 * Empty object literals are assumed to be maps.
	 */
	static boolean isMap(ObjectLiteral o){
		boolean result = true;
		for (ObjectProperty prop : o.getElements()){
			AstNode left = prop.getLeft();
			result = result && (left instanceof StringLiteral);
		}
		return result;
	}

	static class ThisVisitor implements NodeVisitor {
		public ThisVisitor(FunctionNode fun) {
			this.refersToThis = false;
			this.fun = fun;
		}

		public boolean refersToThis(){
			return refersToThis;
		}

		private boolean refersToThis;
		private FunctionNode fun;

		@Override
		public boolean visit(AstNode node) {
			if (isThis(node)){
				AstNode current = node;
				while (!(current instanceof FunctionNode)){
					current = current.getParent();
				}
				if (current == this.fun){
					refersToThis = true;
				}
			}
			return true;
		}
	}


	/**
	 * Determines if a function definition occurs in a position where it is
	 * assumed to be a method. For now, we assume this is only the case if
	 * the function refers to "this". This heuristic will need some further
	 * thinking/refinement
	 */
	public static boolean isMethod(FunctionNode fun){
		ThisVisitor tv = new ThisVisitor(fun);
		fun.visit(tv);
		return tv.refersToThis();
	}

	/**
	 * Determines if a FunctionNode is a constructor. This is done simply by
	 * checking if its name starts with a capital letter.
	 */
	public static boolean isConstructor(FunctionNode fun){
		String funName = fun.getName();
		return (funName != null && funName.length() > 0 && funName.charAt(0) >= 'A' && funName.charAt(0) <= 'Z');
	}

	/**
	 * Find the properties of "this" that a method/constructor reads/writes.
	 */
	private static class PropertyVisitor implements NodeVisitor {

		PropertyVisitor(){
			readProperties = new LinkedHashSet<String>();
			writtenProperties = new LinkedHashSet<String>();
		}

		@Override
		public boolean visit(AstNode node) {
			if (node instanceof PropertyGet){
				PropertyGet pg = (PropertyGet)node;
				AstNode target = pg.getTarget();
				String propName = pg.getProperty().getIdentifier();
				if (target instanceof KeywordLiteral && ConstraintGenUtil.isThis(target)){
					if (node.getParent() instanceof Assignment){
						Assignment a = (Assignment)node.getParent();
						if (a.getLeft() == node){
							writtenProperties.add(propName);
						} else {
							readProperties.add(propName);
						}
					} else {
						readProperties.add(propName);
					}
					readProperties.removeAll(writtenProperties); // if something is read and written, it should only be in the written set
				}
			} else if (node instanceof FunctionNode) {
			    // don't recurse into nested function body
			    return false;
			}
			return true;
		}

		public Set<String> getWrittenProperties(){
			return writtenProperties;
		}

		@SuppressWarnings("unused")
        public Set<String> getReadProperties(){
			return readProperties;
		}

		private Set<String> readProperties;
		private Set<String> writtenProperties;
	}

	/**
	 * For a given FunctionNode, return a set of names of properties
	 * that are written locally using assignments of the
	 * form "this.x = ..."
	 */
	public static Set<String> getWrittenProperties(FunctionNode fun){
		PropertyVisitor pv = new PropertyVisitor();
		fun.getBody().visit(pv);
		return pv.getWrittenProperties();
	}

	public static List<String> getPropertyNames(ObjectLiteral ol){
		List<String> propNames = new ArrayList<String>();
		for (ObjectProperty op : ol.getElements()){
			AstNode left = op.getLeft();
			if (left instanceof Name){
				propNames.add(((Name)left).getIdentifier());
			} else if (left instanceof StringLiteral){
				String identifier = ConstraintGenUtil.removeQuotes(((StringLiteral)left).toSource());
				propNames.add(identifier);
			} else {
				System.err.println(left.getClass().getName() + " " + left.toSource());
				throw new Error("unsupported case in getPropertyNames()");
			}
		}
		return propNames;
	}

	public static final Type OBJECT_TYPE = new ObjectType();
	public static final Type ARRAY_TYPE = new ArrayType(new AnyType());
    public static final Type MAP_TYPE = new MapType(new AnyType());
	public static final Type FUNCTION_TYPE = new FunctionType(new ArrayList<Type>(), new ArrayList<String>(), new AnyType());

	/**
	 * Determines if a type is parameterized by checking if it refers to any TypeVariables.
	 * Used by the constraint generator to determine what constraints to generate.
	 */
	public static boolean isParameterized(Type type){
		if (type instanceof TypeVariable){
			return true;
		} else if (type.isPrimitive() || type.isAny()){
			return false;
                } else if (type instanceof NamedObjectType) {
                        // By fiat...
                        return false;
		} else if (type instanceof ArrayType){
			return isParameterized(((ArrayType)type).elemType());
		} else if (type instanceof FunctionType){
			FunctionType fType = (FunctionType)type;
			boolean result = isParameterized(fType.returnType());
			for (Type t : fType.paramTypes()){
				result = result || isParameterized(t);
			}
			return result;
		} else if (type instanceof AttachedMethodType){
			AttachedMethodType mType = (AttachedMethodType)type;
			boolean result = isParameterized(mType.returnType());
			for (Type t : mType.paramTypes()){
				result = result || isParameterized(t);
			}
			return result;
                } else if (type instanceof ConstructorType) {
                        ConstructorType ct = (ConstructorType)type;
                        boolean result = isParameterized(ct.returnType());
			for (Type t : ct.paramTypes()){
				result = result || isParameterized(t);
			}
			return result;
		} else if (type instanceof ObjectType){
			ObjectType oType = (ObjectType)type;
			boolean result = false;
			for (Property prop : oType.properties()){
				result = result || isParameterized(prop.getType());
			}
			return result;
		} else if (type instanceof IntersectionType){
			IntersectionType iType = (IntersectionType)type;
			boolean result = false;
			for (Type t : iType.getTypes()){
				result = result || isParameterized(t);
			}
			return result;
		} else if (type instanceof MapType){
			MapType mType = (MapType)type;
			return isParameterized(mType.elemType());
		} else {
			throw new Error("missing case: " + type.getClass().getName());
		}
	}

	public static boolean isMethodAssignedInObjectLiteral(FunctionNode n){
		return (n.getParent() instanceof ObjectProperty && n.getParent().getParent() instanceof ObjectLiteral);
	}


    public static boolean isNullUndefinedLitOrVoidOp(ITypeTerm term) {
        AstNode node = term.getNode();
        return isNullConstant(node)
                || (node instanceof Name && ((Name)node).getIdentifier().equals("undefined"))
                || (node instanceof UnaryExpression && ((UnaryExpression)node).getOperator() == Token.VOID);
    }

    public static boolean isNullConstant(ITypeTerm term) {
        return isNullConstant(term.getNode());
    }

    /**
     * For a {@link FunctionNode} representing a constructor, if the constructor C is
     * followed by a sequence of assignments of the form C.prototype.a = ...;, return
     * a set of all the properties written on the prototype.  If the assignments do not
     * fit that form, return the empty set.
     * @param consNode
     * @return
     */
    public static Set<String> getWrittenPrototypeProps(FunctionNode consNode) {
        Set<String> result = HashSetFactory.make();
        AstNode parent = consNode.getParent();
        boolean found = false;
        for (Node child: parent) {
            if (child instanceof EmptyStatement) {
                continue;
            }
            if (child.equals(consNode)) {
                found = true;
            } else if (found) {
                // looking for a statement of the form C.prototype.a = ...;
                boolean foundAssign = false;
                if (child instanceof ExpressionStatement) {
                    AstNode expression = ((ExpressionStatement)child).getExpression();
                    if (expression instanceof Assignment) {
                        Assignment assign = (Assignment) expression;
                        AstNode lhs = assign.getLeft();
                        if (lhs instanceof PropertyGet) {
                            PropertyGet pg = (PropertyGet) lhs;
                            AstNode pgTarget = pg.getTarget();
                            if (pgTarget instanceof PropertyGet) {
                                PropertyGet basePG = (PropertyGet) pgTarget;
                                if (basePG.getProperty().getIdentifier().equals("prototype")) {
                                    // BINGO
                                    result.add(pg.getProperty().getIdentifier());
                                    foundAssign = true;
                                }
                            }
                        }
                    }
                }
                if (!foundAssign) {
                    // stop looking for more assignments
                    break;
                }
            }
        }
        return result;
    }
}
