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
import java.util.List;


/**
 * Represents the type of string expressions.
 *
 * @author colin.gordon
 *
 */
public final class StringType extends PropertyContainer implements PrimitiveType, IndexableType {

	// singleton pattern - hide the constructor so external clients cannot create new objects

    private boolean initDone = false;

	private StringType(){}

	public static StringType make(){
		if (stringType == null){
			stringType = new StringType();

			String length = "length";
			stringType.setProperty(length, IntegerType.make(), true);

			String join = "join";
			stringType.setProperty(join, IntegerType.make(), true);

			String substring = "substring"; // TODO: need to support substring with one argument as well..
			List<Type> paramTypes = new ArrayList<Type>(); // String.substring has type (integer,integer)->string
			paramTypes.add(IntegerType.make());
			paramTypes.add(IntegerType.make());
			stringType.setProperty(substring, new AttachedMethodType(paramTypes, null, stringType), true);

			String charCodeAt = "charCodeAt";
			paramTypes = new ArrayList<Type>();
			paramTypes.add(IntegerType.make());
			stringType.setProperty(charCodeAt, new AttachedMethodType(paramTypes, null, IntegerType.make()), true);

			String charAt = "charAt";
			paramTypes = new ArrayList<Type>();
			paramTypes.add(IntegerType.make());
			stringType.setProperty(charAt, new AttachedMethodType(paramTypes, null, StringType.make()), true);

			String indexOf = "indexOf";
			paramTypes = new ArrayList<Type>();
			paramTypes.add(StringType.make());
			stringType.setProperty(indexOf, new AttachedMethodType(paramTypes, null, IntegerType.make()), true);

			String localeCompare = "localeCompare";
			paramTypes = new ArrayList<Type>();
			paramTypes.add(StringType.make());
			stringType.setProperty(localeCompare, new AttachedMethodType(paramTypes, null, IntegerType.make()), true);

			String concat = "concat";
			paramTypes = new ArrayList<Type>();
			paramTypes.add(StringType.make());
			stringType.setProperty(concat, new AttachedMethodType(paramTypes, null, StringType.make()), true);

			stringType.initDone = true;

		}
		return stringType;
	}

	private static StringType stringType = null;

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
		return "string";
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

    @Override
    public Type keyType() {
        return IntegerType.make();
    }

    @Override
    public Type elemType() {
        return StringType.make();
    }
    @Override
    public RepresentationSort rep() { return RepresentationSort.STRING; }
    
    @Override
    public String generateTag(TypeTagSerializer tts) {
        return tts.memoizeString();
    }

    @Override
    public void setProperty(String propertyName, Type type) {
        if (initDone) {
            throw new RuntimeException("no mutation of this type");
        }
        super.setProperty(propertyName, type);
    }


    @Override
    public void setProperty(String propertyName, Type type, boolean readOnly) {
        if (initDone) {
            throw new RuntimeException("no mutation of this type");
        }
        super.setProperty(propertyName, type, readOnly);
    }
}
