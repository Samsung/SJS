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
 * Void type (for function and method returns)
 *
 * @author colin.gordon
 *
 */
public class VoidType implements Type {
	
	 @Override public boolean isPrimitive() { return true; }
     @Override public boolean isObject() { return false; }
     @Override public boolean isFunction() { return false; }
     @Override public boolean isConstructor() { return false; }
     @Override public boolean isAttachedMethod() { return false; }
     @Override public boolean isUnattachedMethod() { return false; }
     @Override public boolean isMap() { return false; }
     @Override public boolean isArray() { return false; }
     @Override public boolean isAny() { return false; }
     @Override public String toString() { return "void"; }
     @Override public boolean isVar() { return false; }
 	@Override public boolean isIntersectionType() { return false; }
    @Override
    public RepresentationSort rep() { return RepresentationSort.UNREPRESENTABLE; }
}
