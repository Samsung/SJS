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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Represents the type of integer expressions. We will probably need to
 * refine this later to accommodate int-ish expressions.
 *
 * @author ftip
 *
 */
public class IntegerType implements PrimitiveType {

	// singleton pattern - hide the constructor so external clients cannot create new objects
	protected IntegerType(){}

	public static IntegerType make(){
		if (integerType == null){
			integerType = new IntegerType();

			String toFixed = "toFixed";
			List<Type> paramTypes = new ArrayList<Type>(); // integer.toFixed has type (integer)->string
			paramTypes.add(new IntegerType());
			Property property = new Property(toFixed, new AttachedMethodType(paramTypes, null, StringType.make()), true);
			properties.put(toFixed, property);

		    String toString = "toString";
		    AttachedMethodType t1 = new AttachedMethodType(new ArrayList<Type>(), null, StringType.make()); // () -> string
			paramTypes = new ArrayList<Type>();
			paramTypes.add(new IntegerType());
			AttachedMethodType t2 = new AttachedMethodType(paramTypes,null, StringType.make()); // (int) -> string
			IntersectionType iType = new IntersectionType(t1, t2);

			property = new Property(toString, iType, true);
			properties.put(toString, property);
		}
		return integerType;
	}

	private static IntegerType integerType = null;

	public Type getTypeForProperty(String propertyName) {
		if (!properties.containsKey(propertyName)) {
			throw new PropertyNotFoundException(propertyName);
		}
		Type type = properties.get(propertyName).getType();
		return type;
	}

	@Override
	public boolean isPrimitive() {
		return true;
	}

	@Override
	public boolean isObject() {
		return false;
	}

	@Override
	public boolean isFunction() {
		return false;
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
	public String toString() {
		return "integer";
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

	private static Map<String,Property> properties = new LinkedHashMap<String,Property>();

    @Override
    public RepresentationSort rep() { return RepresentationSort.INT; }
    
    @Override
    public String generateTag(TypeTagSerializer tts) {
        return tts.memoizeInt();
    }

	@Override
	public boolean equals(Object obj) {
		return obj instanceof IntegerType;
	}

	@Override
	public int hashCode() {
		return 213;
	}
}
