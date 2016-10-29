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
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.MapType;
import com.samsung.sjs.types.StringType;
import com.samsung.sjs.types.Type;

/**
 * A IndexedTerm Elem(|o|) represents the element type of the map type
 * or array type bound to |o|. Note that we cannot differentiate a
 * map-access from an array-access syntactically. In the solver,
 * the type of this term is not resolved until a map type or array
 * type has been inferred for the base expression.
 *
 * @author ftip
 */
public class IndexedTerm extends ATerm {

	public IndexedTerm(ITypeTerm base){
		super(null);
		this.base = base;
	}

	@Override
	public String stringRepresentation() {
		return "Elem(" + base.stringRepresentation() + ")";
	}

	public ITypeTerm getBase(){
		return base;
	}

	@Override
	public Type getType(){
		ITypeTerm baseTerm = this.getBase();
		Type baseType = baseTerm.getType();
		if (baseType instanceof ArrayType){
			ArrayType arrayType = (ArrayType)baseType;
			return arrayType.elemType();
		} else if (baseType instanceof MapType){
			MapType mapType = (MapType)baseType;
			return mapType.elemType();
		} else if (baseType instanceof StringType){
			return StringType.make();
		} else if (baseType instanceof IntersectionType){
			IntersectionType iType = (IntersectionType)baseType;
			for (Type t : iType.getTypes()){
				if (t instanceof ArrayType){
					return ((ArrayType)t).elemType();
				}
			}
			throw new Error("unsupported case in IndexedTerm.getType()");
		} else {
			return new AnyType();
		}
	}

	@Override
	public void setType(Type type){
		Type baseType = this.getBase().getType();
		if (baseType instanceof ArrayType){
			ArrayType arrayType = (ArrayType)baseType;
			arrayType.setElemType(type);
		} else if (baseType instanceof MapType){
			MapType mapType = (MapType)baseType;
			mapType.setElemType(type);
		} else if (baseType instanceof IntersectionType){
			IntersectionType iType = (IntersectionType)baseType;
			for (Type t : iType.getTypes()){
				if (t instanceof ArrayType){
					((ArrayType)t).setElemType(type);
//					return;
				}
			}
//			System.err.println("this = " + this);
//			throw new Error("unsupported case in IndexedTerm.setType(): type = " + type);
		} else {
			throw new Error("should not happen");
		}
	}

	private ITypeTerm base;
}
