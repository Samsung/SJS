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
/**
 *
 */
package com.samsung.sjs.types;

import java.util.List;

/**
 * Represents a type that is indexable (i.e., a Map, String, or Array),
 * for the purposes of bottom-up type inference.
 *
 * This is just a placeholder, and should never appear in the final inferred
 * types for expressions.
 *
 *
 * @author m.sridharan
 *
 */
public class UnknownIndexableType extends PropertyContainer implements IndexableType {


    private final TypeVar keyType;

    private final TypeVar elemType;


    public UnknownIndexableType(TypeVar keyType, TypeVar elemType) {
        super();
        this.keyType = keyType;
        this.elemType = elemType;
    }

    public UnknownIndexableType(TypeVar keyType, TypeVar elemType, List<Property> props) {
        super(props);
        this.keyType = keyType;
        this.elemType = elemType;
    }


    public TypeVar keyType() {
        return keyType;
    }


    public TypeVar elemType() {
        return elemType;
    }


    /* (non-Javadoc)
     * @see com.samsung.sjs.types.Type#isPrimitive()
     */
    @Override
    public boolean isPrimitive() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.samsung.sjs.types.Type#isObject()
     */
    @Override
    public boolean isObject() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.samsung.sjs.types.Type#isFunction()
     */
    @Override
    public boolean isFunction() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.samsung.sjs.types.Type#isAttachedMethod()
     */
    @Override
    public boolean isAttachedMethod() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.samsung.sjs.types.Type#isUnattachedMethod()
     */
    @Override
    public boolean isUnattachedMethod() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.samsung.sjs.types.Type#isMap()
     */
    @Override
    public boolean isMap() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.samsung.sjs.types.Type#isArray()
     */
    @Override
    public boolean isArray() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.samsung.sjs.types.Type#isAny()
     */
    @Override
    public boolean isAny() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.samsung.sjs.types.Type#isVar()
     */
    @Override
    public boolean isVar() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.samsung.sjs.types.Type#isIntersectionType()
     */
    @Override
    public boolean isIntersectionType() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see com.samsung.sjs.types.Type#isConstructor()
     */
    @Override
    public boolean isConstructor() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String toString(){
        return "Indexable<" + keyType.toString() + "," + elemType.toString() + ">";
    }


    @Override
    public void setProperty(String propertyName, Type type) {
        throw new RuntimeException("no mutation of this type");
    }


    @Override
    public void setProperty(String propertyName, Type type, boolean readOnly) {
        throw new RuntimeException("no mutation of this type");
    }


}
