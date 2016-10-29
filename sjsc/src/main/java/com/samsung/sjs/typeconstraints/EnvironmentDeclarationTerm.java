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

import org.mozilla.javascript.ast.Name;

import com.samsung.sjs.JSEnvironment;
import com.samsung.sjs.types.AnyType;
import com.samsung.sjs.types.ArrayType;
import com.samsung.sjs.types.AttachedMethodType;
import com.samsung.sjs.types.BooleanType;
import com.samsung.sjs.types.FloatType;
import com.samsung.sjs.types.FunctionType;
import com.samsung.sjs.types.ConstructorType;
import com.samsung.sjs.types.IntegerType;
import com.samsung.sjs.types.IntersectionType;
import com.samsung.sjs.types.ObjectType;
import com.samsung.sjs.types.NamedObjectType;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.StringType;
import com.samsung.sjs.types.Type;
import com.samsung.sjs.types.TypeVariable;
import com.samsung.sjs.types.VoidType;

/**
 * Represents a variable or function declared outside the program.
 *
 * @author ftip
 */
public class EnvironmentDeclarationTerm extends ATerm {

	public EnvironmentDeclarationTerm(Name name, JSEnvironment env){
		super(null);
		this.name = name;
		this.type = null;
		this.env = env;
	}

	public static Type subst(Type type, Type paramType){
		if (type instanceof IntegerType || type instanceof FloatType ||
			type instanceof BooleanType || type instanceof StringType ||
			type instanceof AnyType     || type instanceof VoidType ||
                        type instanceof NamedObjectType){
			return type;
		} else if (type instanceof TypeVariable){
			return paramType;
		} else if (type instanceof FunctionType){
			List<Type> resultParamTypes = new ArrayList<Type>();
			FunctionType fType = (FunctionType)type;
			for (Type t : fType.paramTypes()){
				resultParamTypes.add(subst(t, paramType));
			}
			Type resultReturnType = subst(fType.returnType(), paramType);
			return new FunctionType(resultParamTypes, fType.paramNames(), resultReturnType, 0);
		} else if (type instanceof ConstructorType){
			List<Type> resultParamTypes = new ArrayList<Type>();
			ConstructorType fType = (ConstructorType)type;
			for (Type t : fType.paramTypes()){
				resultParamTypes.add(subst(t, paramType));
			}
			Type resultReturnType = subst(fType.returnType(), paramType);
			return new ConstructorType(resultParamTypes, fType.paramNames(), resultReturnType, null);
		} else if (type instanceof AttachedMethodType){
			List<Type> resultParamTypes = new ArrayList<Type>();
			AttachedMethodType mType = (AttachedMethodType)type;
			for (Type t : mType.paramTypes()){
				resultParamTypes.add(subst(t, paramType));
			}
			Type resultReturnType = subst(mType.returnType(), paramType);
			return new AttachedMethodType(resultParamTypes, mType.paramNames(), resultReturnType, 0);
		} else if (type instanceof ArrayType){
			ArrayType aType = (ArrayType)type;
			Type resultElemType = subst(aType.elemType(), paramType);
			return new ArrayType(resultElemType);
		} else if (type instanceof IntersectionType){
			IntersectionType iType = (IntersectionType)type;
			List<Type> compTypes = new ArrayList<Type>();
			for (Type t : iType.getTypes()){
				compTypes.add(subst(t, paramType));
			}
			return new IntersectionType(compTypes);
		} else if (type instanceof ObjectType){
			ObjectType oType = (ObjectType)type;
			ObjectType result = new ObjectType();
			for (Property prop : oType.properties()){
				result.setProperty(prop.getName(), subst(prop.getType(), paramType), prop.isRO());
			}
			return result;
		} else {
			throw new Error("subst: unimplemented case : " + type.getClass().getName());
		}
	}

	// --------------------------------------------------------------------------------------------------------------- //

	@Override
	public Type getType() {
		if (type == null){
			type = subst(env.get(name.getIdentifier()), new AnyType());
		}
		return type;
	}

	@Override
	public void setType(Type type) {
		throw new Error("cannot change the type of global variable");
	}

	@Override
	public String stringRepresentation(){
			return "|" + name.getIdentifier() + "|";
	}

	private JSEnvironment env;
	private Type type;
	private final Name name;
}
