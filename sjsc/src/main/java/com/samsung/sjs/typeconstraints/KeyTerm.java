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
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.IntegerType;
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.MapType;
import com.samsung.sjs.types.StringType;
import com.samsung.sjs.types.Type;

/**
 * A KeyTerm Key(|o|) represents the key type of the map type
 * or array type bound to |o|. For an ArrayType, the key type is
 * integer, and for a MapType, the key type is string.
 *
 * @author ftip
 */
public class KeyTerm extends ATerm {

	public KeyTerm(ITypeTerm base){
		super(null);
		this.base = base;
	}

	@Override
	public String stringRepresentation() {
		return "Key(" + base.stringRepresentation() + ")";
	}

	public ITypeTerm getBase(){
		return base;
	}

	@Override
	public Type getType(){
		ITypeTerm baseTerm = this.getBase();
		Type baseType = baseTerm.getType();
		if (baseType instanceof ArrayType){
			return IntegerType.make();
		} else if (baseType instanceof MapType){
			return StringType.make();
		} else if (baseType instanceof StringType){
			return IntegerType.make();
		} else if (baseType instanceof IntersectionType){
			for (Type t : ((IntersectionType)baseType).getTypes()){
				if (t.isArray()){
					return IntegerType.make();
				}
			}
			throw new Error("unsupported case in KeyTerm.getType()");
		} else {
			return new AnyType();
		}
	}

	@Override
	public void setType(Type type){
		throw new Error("setType() not supported on KeyType");
	}

	private ITypeTerm base;
}
