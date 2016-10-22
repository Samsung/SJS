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

import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.StringLiteral;

import com.samsung.sjs.constraintgenerator.ConstraintGenUtil;
import com.samsung.sjs.types.AnyType;
import com.samsung.sjs.types.MapType;
import com.samsung.sjs.types.Type;

/**
 * Used for representing the type of map literals.
 *
 * @author ftip
 *
 */
public class MapLiteralTerm extends ATerm {

	public MapLiteralTerm(ObjectLiteral n){
		super(n);
		this.type = new MapType(new AnyType());
	}

	public List<String> getPropertyNames(){
		List<String> propNames = new ArrayList<String>();
		ObjectLiteral ol = (ObjectLiteral)this.getNode();
		for (ObjectProperty op : ol.getElements()){
			AstNode left = op.getLeft();
			if (left instanceof Name){
				propNames.add(((Name)left).getIdentifier());
			} else if (left instanceof StringLiteral){
				String identifier = ConstraintGenUtil.removeQuotes(((StringLiteral)left).toSource());
				propNames.add(identifier);
			} else {
				System.err.println(left.getClass().getName() + " " + left.toSource());
				throw new Error("unsupported case in getPropertyNames()");
			}
		}
		return propNames;
	}

	@Override
	public String stringRepresentation(){
        return "|" + getNode().toSource() + "|";
	}

	@Override
	public Type getType(){
		return this.type;
	}

	@Override
	public void setType(Type type){
		this.type = type;
	}

	private Type type;
}
