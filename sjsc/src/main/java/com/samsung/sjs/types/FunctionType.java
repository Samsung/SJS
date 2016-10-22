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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Represents the type of a function: {@code (t, ..., t) -> t}. Not to be confused with
 * the types of methods, which have an additional receiver component.
 *
 * @author ftip
 *
 */
public class FunctionType extends CodeType {

	private int nrTypeVars;

	public FunctionType(List<Type> paramTypes, List<String> paramNames, Type returnType){
		this(paramTypes, paramNames, returnType, 0);
	}

	public FunctionType(List<Type> paramTypes, List<String> paramNames, Type returnType, int nrTypeVars){
		super(paramTypes, paramNames, returnType);
		this.nrTypeVars = nrTypeVars;
	}

	public int getNrTypeVars(){
		return nrTypeVars;
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}

	@Override
	public boolean isObject() {
		return false;
	}

	@Override
	public boolean isFunction() {
		return true;
	}

	@Override
	public boolean isConstructor() {
		return false;
	}

	@Override
	public boolean isAttachedMethod() {
		return false;
	}

	@Override
	public boolean isUnattachedMethod() {
		return false;
	}

	@Override
	public boolean isMap() {
		return false;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
    public boolean isAny() {
        return false;
    }

	@Override
    public boolean isVar() {
        return false;
    }

	@Override
	public boolean isIntersectionType() {
		return false;
	}

	private static final Collection<Type> PRINTING = new HashSet<>();

	@Override
	public String toString() {
		String result = "(";
		synchronized (PRINTING) {
			if (PRINTING.contains(this)) {
				return "<<recursive>>";
			}
			PRINTING.add(this);

			for (int i = 0; i < paramTypes().size(); i++) {
				result += paramTypes().get(i);
				if (i + 1 < paramTypes().size()) result += ",";
			}
			result += ")" + " -> " + returnType();

			PRINTING.remove(this);
		}
		return result;
	}
    @Override
    public String generateTag(TypeTagSerializer tts) {
        return tts.memoizeClosure(TypeTagSerializer.ClosureType.FUNCTION,
                                  returnType(),
                                  null,
                                  paramTypes());
    }

}
