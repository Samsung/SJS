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
 * Represents the type of an unattached method: {@code [t](t, ..., t) -> t}. Not to be
 * confused with the types of functions, which have no receiver component, or
 * with the types of attached methods, whose receiver type is specified
 * implicitly by their associated object.
 *
 * @author ftip
 *
 */

public class UnattachedMethodType extends AbstractMethodType {

	public UnattachedMethodType(List<Type> paramTypes, List<String> paramNames, Type returnType, Type receiverType) {
		this(paramTypes, paramNames, returnType, receiverType, 0);
	}

	public UnattachedMethodType(List<Type> paramTypes, List<String> paramNames, Type returnType, Type receiverType, int nrTypeVars) {
		super(paramTypes, paramNames, returnType);
		this.receiverType = receiverType;
		this.nrTypeVars = nrTypeVars;
	}

	public int getNrTypeVars() {
		return nrTypeVars;
	}

	@Override
	public boolean isAttachedMethod() {
		return false;
	}

	@Override
	public boolean isUnattachedMethod() {
		return true;
	}

	@Override
	public String toString() {
		String result = "[" + receiverType() + "](";
		for (int i = 0; i < paramTypes().size(); i++) {
			result += paramTypes().get(i);
			if (i + 1 < paramTypes().size())
				result += ",";
		}
		result += ")" + " -> " + returnType();
		return result;
	}

	public Type receiverType() {
		return receiverType;
	}

	public void setReceiverType(Type receiverType) {
		this.receiverType = receiverType;
	}

	private Type receiverType;
	private int nrTypeVars;
    @Override
    public String generateTag(TypeTagSerializer tts) {
        return tts.memoizeClosure(TypeTagSerializer.ClosureType.METHOD,
                                  returnType(),
                                  receiverType(),
                                  paramTypes());
    }
}
