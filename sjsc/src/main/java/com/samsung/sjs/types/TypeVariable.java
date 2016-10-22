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

/**
 * Represents a type variable. 
 * 
 * @author ftip
 *
 */
public final class TypeVariable implements Type {
	
	public TypeVariable(int nr){
		this.nr = nr;
	}
	
	@Override
	public boolean equals(Object o){
		return (o instanceof TypeVariable);
	}
	
	@Override
	public int hashCode(){
		return 17;
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
	public String toString() {
		return "T" + nr;
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
        return true;
    }
	
	@Override 
	public boolean isIntersectionType() { 
		return false; 
	}
	
	public int getNr(){
		return nr;
	}
	
	private final int nr;
	
    @Override
    public RepresentationSort rep() { return RepresentationSort.UNREPRESENTABLE; }
}
