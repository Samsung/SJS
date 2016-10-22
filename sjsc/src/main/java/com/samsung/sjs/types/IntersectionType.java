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
package com.samsung.sjs.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Represents an intersection type.
 *
 * @author ftip
 *
 */

public final class IntersectionType implements Type  {

	public IntersectionType(List<Type> types){
		this.types = new ArrayList<Type>(types);
	}

	public IntersectionType(Type... types){
		this.types = new ArrayList<Type>(Arrays.asList(types));
	}

	public List<Type> getTypes(){
		return types;
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}

	@Override
	public boolean isObject() {
		return false;
	}

	@Override
	public boolean isFunction() {
		return false;
	}

	@Override
	public boolean isConstructor() {
		return false;
	}

	@Override
	public boolean isAttachedMethod() {
		return false;
	}

	@Override
	public boolean isUnattachedMethod() {
		return false;
	}

	@Override
	public boolean isMap() {
		return false;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public boolean isAny() {
		return false;
	}

	@Override
	public boolean isVar() {
		return false;
	}

	@Override
	public boolean isIntersectionType() {
		return true;
	}

	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		for (Iterator<Type> it = types.iterator(); it.hasNext(); ){
			Type type = it.next();
			result.append(type.toString());
			if (it.hasNext()) result.append(" INTERSECT ");
		}
		return result.toString();
	}

	private List<Type> types;

        public FunctionType findFunctionType(int arity) {
            for (Type t : types) {
                if (t.isFunction() && ((FunctionType)t).nrParams() == arity) {
                    return (FunctionType)t;
                }
            }
            return null;
        }

        public CodeType findMethodType(int arity) {
            for (Type t : types) {
                if ((t.isAttachedMethod() || t.isUnattachedMethod()) && ((CodeType)t).nrParams() == arity) {
                    return (CodeType)t;
                }
            }
            return null;
        }

        public ConstructorType findConstructorType(int arity) {
            for (Type t : types) {
                if (t.isConstructor() && ((ConstructorType)t).nrParams() == arity) {
                    return (ConstructorType)t;
                }
            }
            return null;
        }

        public ObjectType findObjectType() {
            for (Type t : types) {
                if (t.isObject()) {
                    return (ObjectType)t;
                }
            }
            return null;
        }
    @Override
    public RepresentationSort rep() {
        // TODO: Technically we should be representable; there's an open bug for this
        // To make forward progress, we leverage the fact that the cases we use right now for
        // intersection are intersections of the same representation sort, which means the low-level
        // tagging and untagging operations would be the same.
        boolean init = false;
        RepresentationSort s = RepresentationSort.UNREPRESENTABLE;
        for (Type t : types) {
            if (!init) {
                s = t.rep();
                init = true;
                System.err.println("First representation sort of the intersection is: "+s);
            } {
                if (s != t.rep()) {
                    System.err.println("Found differing representation sort: "+s);
                    return RepresentationSort.UNREPRESENTABLE;
                }
            }
        }
        if (init) {
            return s;
        }
        return RepresentationSort.UNREPRESENTABLE;
    }
}
