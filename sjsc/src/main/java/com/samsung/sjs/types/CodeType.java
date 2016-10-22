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
/**
 * Supertype for all "code types:" functions, and both variants of methods.
 *
 * @author colin.gordon
 */
package com.samsung.sjs.types;

import java.util.List;
public abstract class CodeType implements Type {
	
	protected CodeType(List<Type> paramTypes, List<String> paramNames, Type returnType){
		this.paramTypes = paramTypes;
		this.paramNames = paramNames;
		this.returnType = returnType;
	}
	
	public Type returnType() { 
		return returnType;
	}

	public List<Type> paramTypes() { 
		return paramTypes;
	}

	public List<String> paramNames() { 
		return paramNames;
	}
	
	public int nrParams(){
		return paramTypes.size();
	}
	
	public void setReturnType(Type returnType) {
		this.returnType = returnType;
	}
	
	public void setParamType(Type paramType, int nr){
		paramTypes.set(nr, paramType);
	}
	
	private List<Type> paramTypes;
	private List<String> paramNames;
	private Type returnType;
    @Override
    public RepresentationSort rep() { return RepresentationSort.CODE; }
}
