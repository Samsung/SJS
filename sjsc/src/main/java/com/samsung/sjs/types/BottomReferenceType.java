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
 * Logically, represents a subtype of all reference types, {@link ObjectType},
 * {@link StringType}, {@link FunctionType}, {@link ArrayType}, and
 * {@link MapType}. For now, used solely to reason about the possible types for
 * <code>null</code>.
 *
 */
public class BottomReferenceType implements Type {

    private BottomReferenceType(){}

    private static final BottomReferenceType instance = new BottomReferenceType();

    public static BottomReferenceType make() {
        return instance;
    }

    @Override
    public boolean isPrimitive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isObject() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isFunction() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAttachedMethod() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isUnattachedMethod() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isMap() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isArray() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAny() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isVar() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isIntersectionType() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isConstructor() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public RepresentationSort rep() {
        return RepresentationSort.UNREPRESENTABLE;
    }

    @Override
    public String toString() {
        return "BottomReferenceType";
    }


}
