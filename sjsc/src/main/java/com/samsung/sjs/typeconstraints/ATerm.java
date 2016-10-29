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


import org.mozilla.javascript.ast.AstNode;


/**
 * root of the hierarchy of classes that implement constraint terms
 *
 * @author ftip
 *
 */
public abstract class ATerm implements ITypeTerm {

	ATerm(AstNode node){
		this.node = node;
	}

	@Override
	public AstNode getNode() {
		return node;
	}

	public abstract String stringRepresentation();

	@Override
	public String toString(){
		try {
		String stringRepresentation = stringRepresentation();
		return stringRepresentation;
		} catch (IllegalArgumentException e){
			return "???";
		}
	}

	protected final AstNode node;
}
