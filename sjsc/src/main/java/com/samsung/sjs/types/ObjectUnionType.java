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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * For handling meets over different object types that have prototypes.  For such
 * types, we cannot eagerly compute the meet, as new properties may emerge from
 * the prototype chain during constraint solving.
 *
 * @author m.sridharan
 *
 */
public class ObjectUnionType implements Type, Iterable<ObjectType> {

    private final List<ObjectType> objTypes;



    public ObjectUnionType(List<ObjectType> objTypes) {
        super();
        this.objTypes = Collections.unmodifiableList(objTypes);
    }

    public ObjectUnionType(ObjectType... objTypes) {
        this(Arrays.asList(objTypes));
    }


    public List<ObjectType> getObjTypes() {
        return objTypes;
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
    public String toString() {
        return "ObjectUnionType " + objTypes;
    }

    @Override
    public Iterator<ObjectType> iterator() {
        return objTypes.iterator();
    }


    @Override
    public RepresentationSort rep() { return RepresentationSort.UNREPRESENTABLE; }
}
