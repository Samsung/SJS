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
package com.samsung.sjs.types;

import java.util.List;

/**
 * Common supertype of AttachedMethodType and UnattachedMethodType
 * 
 * @author ftip
 */
public abstract class AbstractMethodType extends CodeType {

	protected AbstractMethodType(List<Type> paramTypes, List<String> paramNames, Type returnType){
		super(paramTypes, paramNames, returnType);
	}
	
	@Override
	public final boolean isPrimitive() {
		return false;
	}

	@Override
	public final boolean isObject() {
		return false;
	}

	@Override
	public final boolean isFunction() {
		return false;
	}

	@Override
	public final boolean isConstructor() {
		return false;
	}
	
	@Override
	public final boolean isMap() {
		return false;
	}

	@Override
	public final boolean isArray() {
		return false;
	}

	@Override
	public final boolean isAny() {
		return false;
	}

	@Override
	public final boolean isVar() {
		return false;
	}

	@Override
	public final boolean isIntersectionType() {
		return false;
	}
}
