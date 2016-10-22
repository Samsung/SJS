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
 * Represents named object type which is lazily resolved since the JSEnv system may observe a name
 * before a definition for named object types
 *
 * @author colin.gordon
 */
package com.samsung.sjs.types;

import java.util.List;
import java.util.Set;

import com.samsung.sjs.JSEnvironment;

public class NamedObjectType extends ObjectType {

    private String name;
    private JSEnvironment env;

    private ObjectType resolved;

    public NamedObjectType(String name, JSEnvironment env) {
        this(name, env, null);
    }

    private NamedObjectType(String name, JSEnvironment env, ObjectType resolved) {
        this.name = name;
        this.env = env;
        this.resolved = resolved;
    }

    private void resolve() {
        if (resolved == null) {
            resolved = env.namedTypes.get(name);
            if (resolved == null) {
                System.err.println("ERROR: Failed to force lazy resolution of named type ["+name+"]");
                System.err.println("named types are: "+env.namedTypes.keySet());
            }
            assert (resolved != null);
        }
    }

    public void setProperty(String propertyName, Type type) {
        resolve();
        resolved.setProperty(propertyName, type);
    }

    public void setProperty(String propertyName, Type type, boolean readOnly) {
        resolve();
        resolved.setProperty(propertyName, type, readOnly);
    }

    public boolean hasProperty(String propertyName) {
        resolve();
        return resolved.hasProperty(propertyName);
    }

    public Property getProperty(String propertyName) {
        resolve();
        return resolved.getProperty(propertyName);
    }

    public Type findMemberType(String f) {
        resolve();
        return resolved.findMemberType(f);
    }

    public List<com.samsung.sjs.types.Property> properties() {
        resolve();
        return resolved.properties();
    }

    public List<String> propertyNames() {
        resolve();
        return resolved.propertyNames();
    }

    public Type getTypeForProperty(String propertyName) {
        resolve();
        return resolved.getTypeForProperty(propertyName);
    }

    @Override
    public Set<Property> inheritedProperties() {
        resolve();
        return resolved.inheritedProperties();
    }

    @Override
    public java.util.List<Property> ownProperties() {
        resolve();
        return resolved.ownProperties();
    };

    @Override
    public boolean hasOwnProperty(String propertyName) {
        resolve();
        return resolved.hasOwnProperty(propertyName);
    };


    @Override
    public boolean hasInheritedProperty(String propName) {
        resolve();
        return resolved.hasInheritedProperty(propName);
    }

    @Override
    public String toString() {
        resolve();
        return resolved.toString();
    }

}
