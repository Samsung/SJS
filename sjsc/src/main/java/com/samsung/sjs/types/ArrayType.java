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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Represents the type of an array: an array is homogeneous in the sense that
 * all of its elements have the same type.
 *
 * @author ftip
 *
 */
public class ArrayType extends PropertyContainer implements IndexableType {

	public ArrayType(Type elemType){
		this.elemType = elemType;
	}

	public static Map<String,Property> getParameterizedProperties() {
		Map<String,Property> props = new LinkedHashMap<String,Property>();

		// Array<T>.length() -> int
		props.put("length", new Property("length", IntegerType.make(), true));

		// Array<T>.concat() -> Array<T>
       	props.put("concat", new Property("concat", new AttachedMethodType(new ArrayList<Type>(), null, new ArrayType(new TypeVariable(1))), true));

        // Array<T>.push(T) -> int
		props.put("push", new Property("push", new AttachedMethodType(Arrays.asList(new TypeVariable(1)), null, IntegerType.make()), true));

		// Array<T>.shift() -> T
    	props.put("shift", new Property("shift", new AttachedMethodType(new ArrayList<Type>(), null, new TypeVariable(1)), true));

    	// Array<T>.pop() -> T
    	props.put("pop", new Property("pop", new AttachedMethodType(new ArrayList<Type>(), null, new TypeVariable(1)), true));

    	// Array<T>.reverse() -> Array<T>
    	props.put("reverse", new Property("reverse", new AttachedMethodType(new ArrayList<Type>(), null, new ArrayType(new TypeVariable(1))), true));

    	// Array<T>.join() -> String
    	List<Type> paramTypes = new ArrayList<Type>();
    	paramTypes.add(StringType.make());
    	props.put("join", new Property("join", new AttachedMethodType(paramTypes, null, StringType.make()), true));

		return props;
	}

	/**
	 * NOTE: this method side-effects the ArrayType in the case that
	 * a parameterized property name is passed in
	 */
	public Type getTypeForProperty(String propertyName) {
		Map<String,Property> props = getParameterizedProperties();
		if (!properties.containsKey(propertyName)){
			Property prop = props.get(propertyName);
			if (prop != null){
				properties.put(propertyName, new Property(propertyName, instantiate(prop.getType()), true));
			} else {
				throw new PropertyNotFoundException(propertyName);
			}
		}
		return properties.get(propertyName).getType();
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
	public boolean isArray() {
		return true;
	}

	public Type elemType() {
		return elemType;
	}

	public void setElemType(Type type) {
		this.elemType = type;
	}

	@Override
	public String toString(){
		return "Array<" + elemType.toString() + ">";
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

	private Type instantiate(Type type){
		if (type.isPrimitive()){
			return type;
		} else if (type.isVar()){
			return elemType;
		} else if (type instanceof AttachedMethodType){
			List<Type> resultParamTypes = new ArrayList<Type>();
			AttachedMethodType mType = (AttachedMethodType)type;
			for (Type paramType : mType.paramTypes()){
				resultParamTypes.add(instantiate(paramType));
			}
			Type resultReturnType = instantiate(mType.returnType());
//			Type resultReceiverType = instantiate(mType.receiverType());
			return new AttachedMethodType(resultParamTypes, mType.paramNames(), resultReturnType, 0);
		} else if (type.isArray()){
			ArrayType aType = (ArrayType)type;
			Type resultElemType = instantiate(aType.elemType());
			return new ArrayType(resultElemType);
		} else {
			throw new Error("unimplemented case in instantiate: " + type.getClass().getName());
		}
	}

	private Type elemType;

    @Override
    public Type keyType() {
        return IntegerType.make();
    }
    @Override
    public RepresentationSort rep() { return RepresentationSort.OBJECT; }

    private String tagstr = null;
    @Override
    public String generateTag(TypeTagSerializer tts) {
        if (tagstr == null) {
            tagstr = tts.memoizeArray(this);
        }
        return tagstr;
    }

    @Override
    public void setProperty(String propertyName, Type type) {
        throw new RuntimeException("no mutation of this type");
    }


    @Override
    public void setProperty(String propertyName, Type type, boolean readOnly) {
        throw new RuntimeException("no mutation of this type");
    }
}

