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
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.Type;


/**
 * Represents the prototype of a ConstructorType.
 *
 * @author ftip
 */
public class ProtoTerm extends ATerm {

	public ProtoTerm(ITypeTerm term) {
		super(null);
		this.term = term;
 	}

	@Override
	public Type getType() {
		if (term.getType().isConstructor()){
			ConstructorType cType = (ConstructorType)term.getType();
			if (cType.getPrototype() == null){
				return new AnyType();
			} else {
				return cType.getPrototype();
			}
		}
		return new AnyType();
	}

	@Override
	public void setType(Type type) {
		if (term.getType().isConstructor()){
			ConstructorType cType = (ConstructorType)term.getType();
			cType.setPrototype(type);
		} else {
			throw new Error("ProtoTerm.setType(): unexpected type: " + term.getType());
		}
	}


	@Override
	public String stringRepresentation() {
		return "prototype(" + term.stringRepresentation() + ")";
	}

	public ITypeTerm getTerm() {
	    return term;
	}

	private ITypeTerm term;
}