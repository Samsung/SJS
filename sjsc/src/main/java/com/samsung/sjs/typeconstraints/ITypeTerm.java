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

import com.samsung.sjs.types.Type;

/**
 * Represents a term in the system of type constraints.
 *
 * @author ftip
 *
 */
public interface ITypeTerm {

	/**
	 * the Rhino AST node corresponding to this constraint variable
	 */
	AstNode getNode();

	/**
	 * printable representation
	 */
	String stringRepresentation();

	/**
	 * retrieve the type inferred for this term by the solver
	 */
	Type getType();

	/**
	 * set the type inferred for this term
	 * @param type
	 */
	void setType(Type type);

}
