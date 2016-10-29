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
 * The root of the SJS type hierarchy.
 *
 * @author ftip
 *
 */
public interface Type {
	boolean isPrimitive();
	boolean isObject();
	boolean isFunction();
	boolean isAttachedMethod();
	boolean isUnattachedMethod();
	boolean isMap();
	boolean isArray();
	boolean isAny();
	boolean isVar();
	boolean isIntersectionType();
	boolean isConstructor();
	RepresentationSort rep();
}
