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
import com.samsung.sjs.types.Type;

public class UnaryOperatorTerm extends ATerm  {

	public UnaryOperatorTerm(String operator, ITypeTerm operand, boolean isPrefix){
		super(null);
		this.operand = operand;
		this.operator = operator;
		this.type = new AnyType();
		this.isPrefix = isPrefix;
	}

	@Override
	public String stringRepresentation() {
		return "op(" + (isPrefix ? operator : "") + operand.stringRepresentation() + (!isPrefix ? operator : "") + ")";
	}

	public ITypeTerm getOperand(){
		return operand;
	}

	public String getOperator(){
		return operator;
	}

	@Override
	public Type getType(){
		return this.type;
	}

	@Override
	public void setType(Type type){
		this.type=type;
	}


	public boolean isPrefix(){
		return isPrefix;
	}

	private ITypeTerm operand;
	private String operator;
	private Type type;
	private boolean isPrefix;
}
