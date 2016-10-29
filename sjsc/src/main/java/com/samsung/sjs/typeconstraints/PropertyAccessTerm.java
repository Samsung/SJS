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

import org.mozilla.javascript.ast.PropertyGet;

import com.samsung.sjs.types.AnyType;
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.FloatType;
import com.samsung.sjs.types.IntegerType;
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.PropertyContainer;
import com.samsung.sjs.types.StringType;
import com.samsung.sjs.types.Type;

/**
 *  A term prop(|o|, "p") represents the type of property "p"
 *  in the object type |o|.
 *
 * @author ftip
 */
public class PropertyAccessTerm extends ATerm {

	public PropertyAccessTerm(ITypeTerm base, String property, PropertyGet pgNode){
		super(pgNode);
		this.base = base;
		this.property = property;
	}

	@Override
	public String stringRepresentation() {
		return "prop(" + base.stringRepresentation() + "," + property  + ")";
	}

	public ITypeTerm getBase(){
		return base;
	}

	public String getPropertyName(){
		return property;
	}

	@Override
	public Type getType(){
		Type baseType = base.getType();
		String propertyName = this.getPropertyName();
		if (baseType instanceof ObjectType){
			return ((PropertyContainer)baseType).getTypeForProperty(propertyName);
		} else if (baseType instanceof FloatType){
		  return ((FloatType)baseType).getTypeForProperty(propertyName);
		} else if (baseType instanceof IntegerType){
			return ((IntegerType)baseType).getTypeForProperty(propertyName);
		} else if (baseType instanceof StringType){
			return ((StringType)baseType).getTypeForProperty(propertyName);
		} else if (baseType instanceof ArrayType){
			return ((ArrayType)baseType).getTypeForProperty(propertyName);
		} else if (baseType instanceof IntersectionType){
			IntersectionType iType = (IntersectionType)baseType;
			for (Type componentType : iType.getTypes()){
				if (componentType instanceof ObjectType){
					return  ((PropertyContainer)componentType).getTypeForProperty(propertyName);
				}
			}
			throw new Error("unsupported case in PropertyAccessTerm.getType()");
		}
		return new AnyType();
	}

	@Override
	public void setType(Type type){
		ITypeTerm baseTerm = this.getBase();
		Type baseType = baseTerm.getType();
		if  (baseType instanceof ObjectType){
			String propertyName = this.getPropertyName();
			PropertyContainer oType = (PropertyContainer)baseType;
			if (oType.hasProperty(propertyName)){
				oType.setProperty(propertyName, type, oType.getProperty(propertyName).isRO());
			}
		} else {
			throw new Error("should not happen");
		}
	}

	private ITypeTerm base;
	private String property;
}
