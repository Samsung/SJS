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
package com.samsung.sjs.typeconstraints;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mozilla.javascript.ast.FunctionNode;

import com.samsung.sjs.constraintgenerator.ConstraintGenUtil;
import com.samsung.sjs.types.AnyType;
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.FunctionType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.UnattachedMethodType;

/**
 * Represents the type of a function declaration (FunctionNode).
 *
 * @author ftip
 *
 */
public class FunctionTerm extends ATerm {

	public FunctionTerm(FunctionNode fun, FunctionKind funType) {
		super(fun);
		this.funType = funType;
		init();
 	}

	public enum FunctionKind {
		Function,
		Method,
		Constructor
	};

	private void init(){
        switch (this.funType){
		case Function:
			Type returnType = new AnyType();
			List<Type> paramTypes = new ArrayList<Type>();
			for (int i=0; i < getFunction().getParamCount(); i++){
				paramTypes.add(new AnyType());
			}
			this.type = new FunctionType(paramTypes, null, returnType);
            break;
		case Method:
			Type receiverType = new AnyType();
			returnType = new AnyType();
			paramTypes = new ArrayList<Type>();
			for (int i=0; i < getFunction().getParamCount(); i++){
				paramTypes.add(new AnyType());
			}
			this.type = new UnattachedMethodType(paramTypes, null, returnType, receiverType);
			break;
		case Constructor:
			Type protoType = null;
            List<Property> writtenProperties = ConstraintGenUtil
                    .getWrittenProperties(getFunction()).stream()
                    .map((s) -> new Property(s, new AnyType(), false))
                    .collect(Collectors.toList());
			ObjectType ctorType = new ObjectType(writtenProperties);
			paramTypes = new ArrayList<Type>();
			for (int i=0; i < getFunction().getParamCount(); i++){
				paramTypes.add(new AnyType());
			}
			this.type = new ConstructorType(paramTypes, null, ctorType, protoType);
            break;
		}
	}

	public FunctionNode getFunction(){
		return (FunctionNode)getNode();
	}

	public void setReturnVariable(FunctionReturnTerm returnVar){
		this.returnVar = returnVar;
	}

	public FunctionReturnTerm getReturnVariable(){
		return returnVar;
	}

	public List<NameDeclarationTerm> getParamVariables(){
		return paramVars;
	}

	public void setParamVariables(List<NameDeclarationTerm> paramVars){
		this.paramVars = paramVars;
	}

	@Override
	public String stringRepresentation(){
		String name = (getFunction().getName().equals("")) ? "<anonymous>" : getFunction().getName();
		switch (funType){
			case Function:
				return "|function " + name + "|";
			case Method:
				return "|method " + name + "|";
			case Constructor:
				return "|constructor " + name + "|";
			default:
				throw new Error("unsupported function type");
		}
	}

	public FunctionKind funType(){
		return this.funType;
	}

	@Override
	public Type getType(){
		return this.type;
	}

	@Override
	public void setType(Type type){
		this.type = type;
	}

	private List<NameDeclarationTerm> paramVars;
	private FunctionReturnTerm returnVar;
	private FunctionKind funType;
	private Type type;
}