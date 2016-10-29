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

import com.samsung.sjs.types.AnyType;
import com.samsung.sjs.types.AttachedMethodType;
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.FunctionType;
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.UnattachedMethodType;
import org.mozilla.javascript.ast.AstNode;

import java.util.List;

public final class FunctionParamTerm extends ATerm implements ITypeTerm {

	public FunctionParamTerm(ITypeTerm term, int param, int nrParams) {
		super(null);
		 this.term = term;
		 this.param = param;
		 this.nrParams = nrParams;
	}

	public ITypeTerm getFunctionTerm(){
		return term;
	}

	public int getParam(){
		return param;
	}

	public int getNrParams(){
		return nrParams;
	}

	@Override
	public String stringRepresentation(){
		 return "param(" + term.stringRepresentation() + "," + param + ")";
	}

	@Override
	public AstNode getNode() {
		return null;
	}

	private static Type softGet(List<Type> l, int i) {
		return i < l.size() ? l.get(i) : new AnyType();
	}

	private static <T> void softSet(List<T> l, int i, T t) {
		if (i < l.size()) l.set(i, t);
	}

	@Override
	public Type getType(){
		Type type = this.getFunctionTerm().getType();
		if (type instanceof FunctionType){
			FunctionType funType = (FunctionType)type;
			return softGet(funType.paramTypes(), getParam());
		} else if (type instanceof AttachedMethodType){
			AttachedMethodType methType = (AttachedMethodType)type;
			return softGet(methType.paramTypes(), getParam());
		} else if (type instanceof UnattachedMethodType){
			UnattachedMethodType methType = (UnattachedMethodType)type;
			return softGet(methType.paramTypes(), getParam());
		} else if (type instanceof IntersectionType){
			IntersectionType iType = (IntersectionType)type;
			for (Type componentType : iType.getTypes()){
				if (componentType instanceof FunctionType){
					FunctionType funType = (FunctionType)componentType;
					if (funType.nrParams() == this.nrParams){
						return softGet(funType.paramTypes(), getParam());
					}
				} else if (componentType instanceof AttachedMethodType){
					AttachedMethodType methType = (AttachedMethodType)componentType;
					if (methType.nrParams() == this.nrParams){
						return softGet(methType.paramTypes(), getParam());
					}
				}
			}
		} else if (type instanceof ConstructorType){
			ConstructorType ctorType = (ConstructorType)type;
			return softGet(ctorType.paramTypes(), getParam());
		}
		return new AnyType();
	}

	@Override
	public void setType(Type type){
		Type fType = this.getFunctionTerm().getType();
		if (fType instanceof FunctionType){
			FunctionType funType = (FunctionType)fType;
			softSet(funType.paramTypes(), getParam(), type);
		} else if (fType instanceof AttachedMethodType){
			AttachedMethodType methType = (AttachedMethodType)fType;
			softSet(methType.paramTypes(), getParam(), type);
		} else if (fType instanceof UnattachedMethodType){
			UnattachedMethodType methType = (UnattachedMethodType)fType;
			softSet(methType.paramTypes(), getParam(), type);
		} else if (fType instanceof ConstructorType){
			ConstructorType ctorType = (ConstructorType)fType;
			softSet(ctorType.paramTypes(), getParam(), type);
		} else if (fType instanceof IntersectionType){
			IntersectionType iType = (IntersectionType)fType;
			for (Type componentType : iType.getTypes()){
				if (componentType instanceof FunctionType){
					FunctionType funType = (FunctionType)componentType;
					if (funType.nrParams() == this.nrParams){
						softSet(funType.paramTypes(), getParam(), type);
					}
				} else if (componentType instanceof AttachedMethodType){
					AttachedMethodType methType = (AttachedMethodType)componentType;
					if (methType.nrParams() == this.nrParams){
						softSet(methType.paramTypes(), getParam(), type);
					}
				}
			}
		} else {
			throw new Error("should not happen");
		}
	}



	private final ITypeTerm term;
	private final int param;
	private final int nrParams;
}
