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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.samsung.sjs.SourceLocation;
import com.samsung.sjs.types.Property;
import com.samsung.sjs.types.Type;

/**
 * Abstract supertype of ObjectType and ArrayType that stores the
 * set of properties  as a list list of (name,type) pairs.
 *
 * @author ftip
 *
 */
public abstract class PropertyContainer implements Type {

	protected Map<String,Property> properties = new LinkedHashMap<String,Property>();

	public void setProperty(String propertyName, Type type) {
	    setProperty(propertyName, type, false);
	}

	public void setProperty(String propertyName, Type type, boolean readOnly) {
	    this.properties.put(propertyName, new Property(propertyName, type, readOnly));
	}

    public void setProperty(String propertyName, Type type, boolean readOnly,
            SourceLocation sourceLoc) {
        this.properties.put(propertyName, new Property(propertyName, type,
                readOnly, sourceLoc));
    }

	public boolean hasProperty(String propertyName) {
		return properties.containsKey(propertyName);
	}

	public Property getProperty(String propertyName) {
		return properties.get(propertyName);
	}

	public Type findMemberType(String f) {
                Property p = getProperty(f);
                if (p != null) {
                    return p.getType();
                }
                System.err.println("BAD: Lookup of ["+f+"] in "+this.toString()+" returned null, but we don't believe it");
                System.err.println("Map size: "+properties.size());
		for (Property prop : properties.values()){
                    if (prop.getName().equals(f)) {
                        System.err.println("PANIC: Lookup by string name failed, but found by iteration!");
                        return prop.getType();
                    } else {
                        System.err.println("Skipping ["+prop.getName()+"]");
                    }
		}
                assert (hasProperty(f));
                assert(false);
		return null;
	}

	public List<com.samsung.sjs.types.Property> properties() {
		return new ArrayList<com.samsung.sjs.types.Property>(properties.values());
	}

	public List<String> propertyNames() {
		List<String> propNames = new ArrayList<String>();
		for (Property prop : properties.values()){
			propNames.add(prop.getName());
		}
		return propNames;
	}

	/**
	 * Property lookup. Only looks in this PropertyContainer. Does not follow
	 * the prototype chain.
	 * @throws PropertyNotFoundException if not found
	 */
	public Type getTypeForProperty(String propertyName) {
		Property property = properties.get(propertyName);
		if (property == null){
		    throw new PropertyNotFoundException(propertyName);
		}
		return property.getType();
	}

	public PropertyContainer() {
		super();
	}

	public PropertyContainer(List<Property> props) {
        for (Property p: props) {
            this.properties.put(p.getName(), p);
        }
	}

	@Override public abstract boolean isPrimitive();
	@Override public abstract boolean isObject();
	@Override public abstract boolean isFunction();
	@Override public abstract boolean isAttachedMethod();
	@Override public abstract boolean isUnattachedMethod();
	@Override public abstract boolean isMap();
	@Override public abstract boolean isArray();
	@Override public abstract boolean isAny();
	@Override public abstract boolean isVar();

    @Override
    public RepresentationSort rep() { return RepresentationSort.UNREPRESENTABLE; /* This class is reused many ways; any real subtype should override this. */}
}
