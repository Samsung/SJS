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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.PropertyGet;

import com.ibm.wala.util.collections.HashMapFactory;
import com.samsung.sjs.JSEnvironment;
import com.samsung.sjs.typeconstraints.ArrayLiteralTerm;
import com.samsung.sjs.typeconstraints.CheckArityConstraint;
import com.samsung.sjs.typeconstraints.ConcreteConstraint;
import com.samsung.sjs.typeconstraints.EnvironmentDeclarationTerm;
import com.samsung.sjs.typeconstraints.ExpressionTerm;
import com.samsung.sjs.typeconstraints.FunctionCallTerm;
import com.samsung.sjs.typeconstraints.FunctionParamTerm;
import com.samsung.sjs.typeconstraints.FunctionReturnTerm;
import com.samsung.sjs.typeconstraints.FunctionTerm;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.typeconstraints.IndexedTerm;
import com.samsung.sjs.typeconstraints.KeyTerm;
import com.samsung.sjs.typeconstraints.MapLiteralTerm;
import com.samsung.sjs.typeconstraints.MethodReceiverTerm;
import com.samsung.sjs.typeconstraints.NameDeclarationTerm;
import com.samsung.sjs.typeconstraints.ObjectLiteralTerm;
import com.samsung.sjs.typeconstraints.OperatorTerm;
import com.samsung.sjs.typeconstraints.PropertyAccessTerm;
import com.samsung.sjs.typeconstraints.ProtoParentTerm;
import com.samsung.sjs.typeconstraints.ProtoTerm;
import com.samsung.sjs.typeconstraints.SubTypeConstraint;
import com.samsung.sjs.typeconstraints.ThisTerm;
import com.samsung.sjs.typeconstraints.TypeConstantTerm;
import com.samsung.sjs.typeconstraints.TypeEqualityConstraint;
import com.samsung.sjs.typeconstraints.TypeVariableTerm;
import com.samsung.sjs.typeconstraints.UnaryOperatorTerm;
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.BottomReferenceType;
import com.samsung.sjs.types.CodeType;
import com.samsung.sjs.types.IndexableType;
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.MapType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.PrimitiveType;
import com.samsung.sjs.types.TopReferenceType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.TypeVar;
import com.samsung.sjs.types.VoidType;


/**
 * Factory for generating constraint variables and type constraints.
 *
 * @author ftip
 *
 */
public class ConstraintFactory {

    /**
     * Find or create a term representing an expression consisting of a variable name
     */
    public NameDeclarationTerm findOrCreateNameDeclarationTerm(final Name name){
        Name name2 = ConstraintGenUtil.findDecl(name);
        if (name2 == null) {
            throw new Error("no declaration found for " + name.getIdentifier());
        }
        if (!nameTerms.containsKey(name2)){
            nameTerms.put(name2, new NameDeclarationTerm(name2));
        }
        return nameTerms.get(name2);
    }

    /**
     * Find or create a term representing the name, without first mapping the name to its
     * declaration
     */
    public NameDeclarationTerm findOrCreateNameDeclTermNoLookup(final Name name) {
        if (!nameTerms.containsKey(name)){
            nameTerms.put(name, new NameDeclarationTerm(name));
        }
        return nameTerms.get(name);

    }
    /**
     * Find or create a term representing an expression consisting of a global variable name
     */
    public EnvironmentDeclarationTerm findOrCreateGlobalDeclarationTerm(final Name name, JSEnvironment env){
        if (!globalVarTerms.containsKey(name)){
            globalVarTerms.put(name, new EnvironmentDeclarationTerm(name, env));
        }
        return globalVarTerms.get(name);
    }

    /**
     * Find or create a term representing the return type of a function
     */
    public FunctionReturnTerm findOrCreateFunctionReturnTerm(ITypeTerm term, int nrParams){
        if (!functionReturnTerms.containsKey(term)){
            functionReturnTerms.put(term, new LinkedHashMap<Integer,FunctionReturnTerm>());
        }
        Map<Integer,FunctionReturnTerm> map = functionReturnTerms.get(term);
        if (!map.containsKey(nrParams)){
            map.put(nrParams, new FunctionReturnTerm(term, nrParams));
        }
        return map.get(nrParams);
    }

    /**
     * Find or create a term representing the receiver type of a method
     */
    public MethodReceiverTerm findOrCreateMethodReceiverTerm(ITypeTerm term, int nrParams){
        if (!methodReceiverTerms.containsKey(term)){
            methodReceiverTerms.put(term, new LinkedHashMap<Integer,MethodReceiverTerm>());
        }
        Map<Integer,MethodReceiverTerm> map = methodReceiverTerms.get(term);
        if (!map.containsKey(nrParams)){
            map.put(nrParams, new MethodReceiverTerm(term, nrParams));
        }
        return map.get(nrParams);
    }

    /**
     * Find or create a term representing the type of a parameter of a function
     */
    public FunctionParamTerm findOrCreateFunctionParamTerm(ITypeTerm term, int param, int nrParams){
        if (!functionParamTerms.containsKey(term)){
             functionParamTerms.put(term, new LinkedHashMap<Integer,Map<Integer,FunctionParamTerm>>());
        }
        Map<Integer,Map<Integer,FunctionParamTerm>> map1 = functionParamTerms.get(term);
        if (!map1.containsKey(param)){
            map1.put(param, new LinkedHashMap<Integer,FunctionParamTerm>());
        }
        Map<Integer,FunctionParamTerm> map2 = map1.get(param);
        if (!map2.containsKey(nrParams)){
            map2.put(nrParams, new FunctionParamTerm(term, param, nrParams));
        }
        return map2.get(nrParams);
    }

    /**
     * Find or create a term representing the type of an expression
     */
    public ITypeTerm findOrCreateExpressionTerm(AstNode n) {
        if (!expressionTerms.containsKey(n)){
            expressionTerms.put(n, new ExpressionTerm(n));
        }
        return expressionTerms.get(n);
    }

    public FunctionCallTerm findOrCreateFunctionCallTerm(FunctionCall n) {
        if (!functionCallTerms.containsKey(n)){
            functionCallTerms.put(n, new FunctionCallTerm(n));
        }
        return functionCallTerms.get(n);
    }

    /**
     * Find or create a term representing the type of "this"
     */
    public ThisTerm findOrCreateThisTerm(AstNode n) {
        if (!thisTerms.containsKey(n)){
            thisTerms.put(n, new ThisTerm(n));
        }
        return thisTerms.get(n);
    }

    /**
     * Find or create a term representing the type of an object literal
     */
    public ITypeTerm findOrCreateObjectLiteralTerm(ObjectLiteral n) {
        if (!objectLiteralTerms.containsKey(n)){
            objectLiteralTerms.put(n, new ObjectLiteralTerm(n));
        }
        return objectLiteralTerms.get(n);
    }

    /**
     * Find or create a term representing the type of a map literal
     */
    public ITypeTerm findOrCreateMapLiteralTerm(ObjectLiteral n) {
        if (!mapLiteralTerms.containsKey(n)){
            mapLiteralTerms.put(n, new MapLiteralTerm(n));
        }
        return mapLiteralTerms.get(n);
    }

    /**
     * Find or create a term representing the type of an array literal
     */
    public ITypeTerm findOrCreateArrayLiteralTerm(ArrayLiteral n) {
        if (!arrayLiteralTerms.containsKey(n)){
            arrayLiteralTerms.put(n, new ArrayLiteralTerm(n));
        }
        return arrayLiteralTerms.get(n);
    }

    /**
     * Find or create a term representing a property access
     */
    public PropertyAccessTerm findOrCreatePropertyAccessTerm(ITypeTerm base, String property, PropertyGet pgNode){
        if (!propertyAccessTerms.containsKey(base)){
            propertyAccessTerms.put(base, new LinkedHashMap<String,PropertyAccessTerm>());
        }
        Map<String,PropertyAccessTerm> map = propertyAccessTerms.get(base);
        if (!map.containsKey(property)){
            map.put(property, new PropertyAccessTerm(base, property, pgNode));
        }
        return map.get(property);
    }

    /**
     * Find or create a term Elem(.) that represents the element type of
     * some array or map.
     */
    public IndexedTerm findOrCreateIndexedTerm(ITypeTerm cv){
        if (!indexedTerms.containsKey(cv)){
            indexedTerms.put(cv, new IndexedTerm(cv));
        }
        return indexedTerms.get(cv);
    }

    /**
     * Find or create a term KeyType(.) that represents the key type of
     * some array or map.
     */
    public KeyTerm findOrCreateKeyTerm(ITypeTerm cv){
        if (!keyTerms.containsKey(cv)){
            keyTerms.put(cv, new KeyTerm(cv));
        }
        return keyTerms.get(cv);
    }

    /**
     * Find or create a term representing a FunctionNode (function definition). The
     * created FunctionTerm provides methods for accessing the terms corresponding to the
     * function's parameters and return type. The boolean flag isMethod indicates whether
     * the function is a method.
     */
    public FunctionTerm findOrCreateFunctionTerm(FunctionNode fun, FunctionTerm.FunctionKind funType) {
        if (!functionTerms.containsKey(fun)){
            FunctionTerm var = new FunctionTerm(fun, funType);
            var.setReturnVariable(this.findOrCreateFunctionReturnTerm(var, fun.getParamCount()));
            List<AstNode> params = fun.getParams();
            List<NameDeclarationTerm> paramVars = new ArrayList<NameDeclarationTerm>();
            for (int i=0; i < params.size(); i++){
                AstNode param = params.get(i);
                if (param instanceof Name){
                    NameDeclarationTerm paramCV = this.findOrCreateNameDeclarationTerm((Name)param);
                    paramVars.add(paramCV);
                } else {
                    throw new Error("unimplemented case in findOrCreateFunctionVariable");
                }
            }
            var.setParamVariables(paramVars);
            functionTerms.put(fun, var);
        }
        return functionTerms.get(fun);
    }

//    public ITerm findOrCreateFunctionTerm(FunctionNode fun) {
//        return findOrCreateFunctionTerm(fun, false);
//    }

    public ProtoTerm findOrCreateProtoTerm(ITypeTerm term){
        if (!protoTerms.containsKey(term)){
            protoTerms.put(term, new ProtoTerm(term));
        }
        return protoTerms.get(term);
    }

    public ProtoParentTerm findOrCreateProtoParentTerm(ITypeTerm term) {
        return findOrCreate(protoParentTerms, term, ProtoParentTerm::new);
    }

    /**
     * Find or create a term  representing the type of an infixexpression, e.g. x + y
     */
    public OperatorTerm findOrCreateOperatorTerm(String operator, ITypeTerm left, ITypeTerm right){
        if (!operatorTerms.containsKey(operator)){
            operatorTerms.put(operator, new LinkedHashMap<ITypeTerm,Map<ITypeTerm,OperatorTerm>>());
        }
        Map<ITypeTerm,Map<ITypeTerm,OperatorTerm>> map = operatorTerms.get(operator);
        if (!map.containsKey(left)){
            map.put(left, new LinkedHashMap<ITypeTerm,OperatorTerm>());
        }
        Map<ITypeTerm,OperatorTerm> map2 = map.get(left);
        if (!map2.containsKey(right)){
            map2.put(right, new OperatorTerm(operator,left, right));
        }
        return map2.get(right);
    }

    /**
     * Find or create a term  representing the type of a unary expression, e.g. ~x
     */
    public UnaryOperatorTerm findOrCreateUnaryOperatorTerm(String operator, ITypeTerm operand, boolean isPrefix){
        if (!unaryOperatorTerms.containsKey(operator)){
            unaryOperatorTerms.put(operator, new LinkedHashMap<ITypeTerm, Map<Boolean,UnaryOperatorTerm>>());
        }
        Map<ITypeTerm,Map<Boolean,UnaryOperatorTerm>> map = unaryOperatorTerms.get(operator);
        if (!map.containsKey(operand)){
            map.put(operand, new LinkedHashMap<Boolean,UnaryOperatorTerm>());

        }
        Map<Boolean,UnaryOperatorTerm> map2 = map.get(operand);
        if (!map2.containsKey(isPrefix)){
            map2.put(isPrefix, new UnaryOperatorTerm(operator, operand, isPrefix));
        }
        return map2.get(isPrefix);
    }

    /**
     * Find or create a term  representing a specific type.
     */
    public TypeConstantTerm findOrCreateTypeTerm(Type type){
        if (!typeTerms.containsKey(type)){
            typeTerms.put(type, new TypeConstantTerm(type));
        }
        return typeTerms.get(type);
    }


    public TypeVariableTerm findOrCreateTypeVariableTerm(TypeVar tyVar) {
        return findOrCreate(typeVarTerms, tyVar, TypeVariableTerm::new);
    }

    private static <T,U> U findOrCreate(Map<T,U> map, T key, Function<T,U> creator) {
        if (!map.containsKey(key)) {
            map.put(key, creator.apply(key));
        }
        return map.get(key);
    }

    /**
     * Return the term corresponding to the type, if such a term exists.
     * Otherwise, throw an exception.
     */
    public ITypeTerm getTermForType(Type t) {
        if (t instanceof TypeVar) {
            return findOrCreateTypeVariableTerm((TypeVar) t);
        } else if (t instanceof PrimitiveType || t instanceof VoidType) {
            return findOrCreateTypeTerm(t);
        } else if (t instanceof BottomReferenceType
                || t instanceof TopReferenceType || t instanceof MapType
                || t instanceof ArrayType || t instanceof CodeType
                || t instanceof ObjectType || t instanceof IntersectionType
                || t instanceof IndexableType) {
            return findOrCreateTypeTerm(t);
        }
        throw new RuntimeException("need to handle type " + t);
    }

    public TypeVar freshTypeVar() {
        return freshTypeVar("X");
    }

    public TypeVar freshTypeVar(String base) {
        int id = gensym++;
        return new TypeVar(base + id);
    }

    /**
     * Find or create a type constraint expressing that the types of two program entities
     * should be equal.
     */
    public TypeEqualityConstraint findOrCreateTypeEqualityConstraint(ITypeTerm left, ITypeTerm right){
        if (!typeEqualityConstraints.containsKey(left)){
            typeEqualityConstraints.put(left, new LinkedHashMap<ITypeTerm,TypeEqualityConstraint>());
        }
        Map<ITypeTerm,TypeEqualityConstraint> map = typeEqualityConstraints.get(left);
        if (!map.containsKey(right)){
            map.put(right, new TypeEqualityConstraint(left, right));
        }
        return map.get(right);
    }

    /**
     * Find or create a type constraint expressing that the type of
     * program entity left should be a subtype of the type of program entity right.
     */
    public SubTypeConstraint findOrCreateSubTypeConstraint(ITypeTerm sub, ITypeTerm sup) {
        if (!subTypeConstraints.containsKey(sub)){
            subTypeConstraints.put(sub, new LinkedHashMap<ITypeTerm,SubTypeConstraint>());
        }
        Map<ITypeTerm,SubTypeConstraint> map = subTypeConstraints.get(sub);
        if (!map.containsKey(sup)){
            map.put(sup, new SubTypeConstraint(sub, sup));
        }
        return map.get(sup);
    }

    public ConcreteConstraint findOrCreateConcreteConstraint(ITypeTerm term) {
        return findOrCreate(concreteConstraints, term, ConcreteConstraint::new);
    }

    public CheckArityConstraint findOrCreateCheckArityConstraint(FunctionReturnTerm term) {
        return findOrCreate(arityConstraints, term, CheckArityConstraint::new);
    }



    // maps used to implement the various findOrCreateXXX() methods

    private Map<Name, NameDeclarationTerm> nameTerms = new LinkedHashMap<Name, NameDeclarationTerm>();
    private Map<Name, EnvironmentDeclarationTerm> globalVarTerms = new LinkedHashMap<Name, EnvironmentDeclarationTerm>();
    private Map<FunctionNode,FunctionTerm> functionTerms = new LinkedHashMap<FunctionNode,FunctionTerm>();
    private Map<ITypeTerm,Map<Integer,Map<Integer,FunctionParamTerm>>> functionParamTerms = new LinkedHashMap<ITypeTerm,Map<Integer,Map<Integer,FunctionParamTerm>>>();
    private Map<ITypeTerm,Map<Integer,FunctionReturnTerm>> functionReturnTerms = new LinkedHashMap<ITypeTerm,Map<Integer,FunctionReturnTerm>>();
    private Map<ITypeTerm,Map<Integer,MethodReceiverTerm>> methodReceiverTerms = new LinkedHashMap<ITypeTerm,Map<Integer,MethodReceiverTerm>>();

    private Map<AstNode,ExpressionTerm> expressionTerms = new LinkedHashMap<AstNode,ExpressionTerm>();
    private Map<AstNode,FunctionCallTerm> functionCallTerms = new LinkedHashMap<AstNode,FunctionCallTerm>();
    private Map<AstNode,ThisTerm> thisTerms = new LinkedHashMap<AstNode,ThisTerm>();
    private Map<AstNode,ObjectLiteralTerm> objectLiteralTerms = new LinkedHashMap<AstNode,ObjectLiteralTerm>();
    private Map<AstNode,MapLiteralTerm> mapLiteralTerms = new LinkedHashMap<AstNode,MapLiteralTerm>();
    private Map<AstNode,ArrayLiteralTerm> arrayLiteralTerms = new LinkedHashMap<AstNode,ArrayLiteralTerm>();
    private Map<ITypeTerm,IndexedTerm> indexedTerms = new LinkedHashMap<ITypeTerm,IndexedTerm>();
    private Map<ITypeTerm,KeyTerm> keyTerms = new LinkedHashMap<ITypeTerm,KeyTerm>();
    private Map<ITypeTerm,Map<String,PropertyAccessTerm>> propertyAccessTerms = new LinkedHashMap<ITypeTerm,Map<String,PropertyAccessTerm>>();
    private Map<String,Map<ITypeTerm,Map<ITypeTerm,OperatorTerm>>> operatorTerms = new LinkedHashMap<String,Map<ITypeTerm,Map<ITypeTerm,OperatorTerm>>>();
    private Map<String,Map<ITypeTerm,Map<Boolean,UnaryOperatorTerm>>> unaryOperatorTerms = new LinkedHashMap<String,Map<ITypeTerm,Map<Boolean,UnaryOperatorTerm>>>();
    private Map<Type,TypeConstantTerm> typeTerms = new LinkedHashMap<Type,TypeConstantTerm>();
    private Map<TypeVar,TypeVariableTerm> typeVarTerms = new LinkedHashMap<>();

    private Map<ITypeTerm,ProtoTerm> protoTerms = new LinkedHashMap<ITypeTerm,ProtoTerm>();
    private Map<ITypeTerm,ProtoParentTerm> protoParentTerms = HashMapFactory.make();

    private Map<ITypeTerm,Map<ITypeTerm,TypeEqualityConstraint>> typeEqualityConstraints = new LinkedHashMap<ITypeTerm,Map<ITypeTerm,TypeEqualityConstraint>>();
    private Map<ITypeTerm,Map<ITypeTerm,SubTypeConstraint>> subTypeConstraints = new LinkedHashMap<ITypeTerm,Map<ITypeTerm,SubTypeConstraint>>();
    private Map<ITypeTerm,ConcreteConstraint> concreteConstraints = HashMapFactory.make();
    private Map<FunctionReturnTerm,CheckArityConstraint> arityConstraints = HashMapFactory.make();

    // for generating fresh type variables
    private int gensym = 0;
}
