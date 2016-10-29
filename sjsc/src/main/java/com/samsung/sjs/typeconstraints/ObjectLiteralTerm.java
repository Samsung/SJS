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
import java.util.stream.Collectors;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.StringLiteral;

import com.samsung.sjs.constraintgenerator.ConstraintGenUtil;
import com.samsung.sjs.types.AnyType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.Type;

/**
 * Used for representing the type of object literals.
 *
 * @author ftip
 *
 */
public class ObjectLiteralTerm extends ATerm {

	public ObjectLiteralTerm(ObjectLiteral n){
		super(n);
        ObjectType oType = new ObjectType(this.getPropertyNames().stream()
                .map((s) -> new Property(s, new AnyType(), false))
                .collect(Collectors.toList()));
		this.type = oType;
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
	public Type getType(){
		return this.type;
	}

	@Override
	public void setType(Type type){
		this.type = type;
	}

	@Override
	public String stringRepresentation(){
        return "|" + getNode().toSource() + "|";
	}

	private Type type;
}
