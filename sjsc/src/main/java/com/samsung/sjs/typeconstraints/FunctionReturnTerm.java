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
import java.util.LinkedHashSet;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;

import com.samsung.sjs.types.AnyType;
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.FunctionType;
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.AttachedMethodType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.UnattachedMethodType;

public final class FunctionReturnTerm extends ATerm implements ITypeTerm {

	public FunctionReturnTerm(ITypeTerm term, int nrParams) {
		super(null);
		this.term = term;
		this.nrParams = nrParams;
	}

	public ITypeTerm getFunctionTerm(){
		return term;
	}

	public int getNrParams(){
		return nrParams;
	}

	@Override
	public AstNode getNode() {
		return null;
	}

	@Override
	public String stringRepresentation() {
		 return "ret(" + term.stringRepresentation() + ")";
	}

	@Override
	public Type getType(){
		Type type = this.getFunctionTerm().getType();
		if (type instanceof FunctionType){
			FunctionType funType = (FunctionType)type;
			return funType.returnType();
		} else if (type instanceof AttachedMethodType){
			AttachedMethodType methType = (AttachedMethodType)type;
			return methType.returnType();
		} else if (type instanceof UnattachedMethodType){
			UnattachedMethodType methType = (UnattachedMethodType)type;
			return methType.returnType();
		} else if (type instanceof IntersectionType){
			IntersectionType iType = (IntersectionType)type;
			Set<Type> returnTypes = new LinkedHashSet<Type>();
			for (Type componentType : iType.getTypes()){
				if (componentType instanceof FunctionType){
					FunctionType funType = (FunctionType)componentType;
					if (funType.nrParams() == this.nrParams){
						returnTypes.add(funType.returnType());
					}
				} else if (componentType instanceof AttachedMethodType){
					AttachedMethodType methType = (AttachedMethodType)componentType;
					if (methType.nrParams() == this.nrParams){
						returnTypes.add(methType.returnType());
					}
				}
			}
			if (returnTypes.size() == 1){
				return returnTypes.iterator().next();
			} else {
				return new IntersectionType(new ArrayList<Type>(returnTypes));
			}
		} else if (type instanceof ConstructorType){
			ConstructorType ctorType = (ConstructorType)type;
			return ctorType.returnType();
		}
		return new AnyType();
	}

	@Override
	public void setType(Type type){
		Type fType = this.getFunctionTerm().getType();
		if (fType instanceof FunctionType){
			FunctionType funType = (FunctionType)fType;
			funType.setReturnType(type);
		} else if (fType instanceof AttachedMethodType){
			AttachedMethodType methType = (AttachedMethodType)fType;
			methType.setReturnType(type);
		} else if (fType instanceof UnattachedMethodType){
			UnattachedMethodType methType = (UnattachedMethodType)fType;
			methType.setReturnType(type);
		} else if (fType instanceof ConstructorType){
			ConstructorType ctorType = (ConstructorType)fType;
			ctorType.setReturnType(type);
		} else if (fType instanceof IntersectionType){
			IntersectionType iType = (IntersectionType)fType;
			for (Type componentType : iType.getTypes()){
				if (componentType instanceof FunctionType){
					FunctionType funType = (FunctionType)componentType;
					funType.setReturnType(type);
				} else if (componentType instanceof AttachedMethodType){
					AttachedMethodType methType = (AttachedMethodType)componentType;
					methType.setReturnType(type);
				}
			}
		} else {
			throw new Error("should not happen");
		}
	}


	private final ITypeTerm term;
	private final int nrParams;
}