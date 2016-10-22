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
 * Represents object types.  Logically, an object has a set
 * of properties, each consisting of a name and a type.
 * Properties may be present on the object type itself, or be
 * inherited via the prototype chain.  Own vs. inherited properties
 * are represented separately.
 *
 * @author ftip
 * @author m.sridharan
 *
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class ObjectType extends PropertyContainer implements Type {

	public ObjectType(){
	    this(null, Collections.emptyList(), Collections.emptyList());
	}

    public ObjectType(Type prototypeParent, List<Property> ownProperties, List<Property> inheritedProperties) {
        this.prototypeParent = prototypeParent;
        for (Property p: ownProperties) {
            assert p.isRW();
            this.properties.put(p.getName(), p);
        }
        for (Property p : inheritedProperties) {
            assert p.isRO();
            // a read-write property takes precedence over an inherited one
            if (!this.hasProperty(p.getName())) {
                this.properties.put(p.getName(), p);
            }
        }
    }

    public ObjectType(Type prototypeParent, List<Property> props) {
        super(props);
        this.prototypeParent = prototypeParent;
    }

    public ObjectType(List<Property> props) {
        this(null, props);
    }

    @Override
	public boolean isPrimitive() {
		return false;
	}

	@Override
	public boolean isObject() {
		return true;
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
	public boolean isIntersectionType() {
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

	/**
	 * parent type of this in the prototype chain.  if <code>null</code>,
	 * this type cannot have any inherited properties
	 */
	private final Type prototypeParent;

//	/**
//	 * Set the prototype parent for this type.  This should only be called once (not enforced).
//	 */
//	public void setPrototypeParent(Type prototypeParent) {
//	    this.prototypeParent = prototypeParent;
//	    this.inheritedProperties = HashMapFactory.make();
//	}

	public Type getPrototypeParent() {
	    return this.prototypeParent;
	}

	/**
	 * get the currently-known inherited properties for this.
	 * @return
	 */
	public Set<Property> inheritedProperties() {
        return properties().stream().filter((p) -> {
            return p.isRO();
        }).collect(Collectors.toSet());
	}

	public Set<Property> getROProperties() {
	    return inheritedProperties();
	}

	public List<Property> ownProperties() {
        return properties.values().stream().filter((p) -> {
            return p.isRW();
        }).collect(Collectors.toList());
	}

	public List<Property> getRWProperties() {
	    return ownProperties();
	}

    public boolean hasOwnProperty(String propertyName) {
        return hasProperty(propertyName) && properties.get(propertyName).isRW();
    }


    public boolean hasInheritedProperty(String propName) {
        return properties.containsKey(propName) &&
                properties.get(propName).isRO();
	}


    protected static Stack<ObjectType> objects_being_serialized = new Stack<>();

	@Override
	public String toString(){
            if (objects_being_serialized.search(this) != -1) {
                return "<<recursive>>";
            }
            objects_being_serialized.push(this);
            List<Property> roProps = new ArrayList<Property>();
            List<Property> rwProps = new ArrayList<Property>();
            for (Iterator<Property> it = properties.values().iterator(); it.hasNext(); ){
                    Property prop = it.next();
                    if (prop.isRO()) {
                        roProps.add(prop);
                    } else {
                        rwProps.add(prop);
                    }
            }

            StringBuilder result = new StringBuilder();
            result.append("{ ");
            for (Iterator<Property> it = roProps.iterator(); it.hasNext(); ){
                Property prop = it.next();
                String propType = prop.getType().toString();
                result.append(prop.getName() + ": " + propType);
                if (it.hasNext()){
                        result.append(", ");
                }
            }
            result.append(" | ");
            for (Iterator<Property> it = rwProps.iterator(); it.hasNext(); ){
                Property prop = it.next();
                String propType = prop.getType().toString();
                result.append(prop.getName() + ": " + propType);
                if (it.hasNext()){
                        result.append(", ");
                }
            }
            result.append(" }");

            objects_being_serialized.pop();

            return result.toString();
	}

    @Override
    public RepresentationSort rep() { return RepresentationSort.OBJECT; }

    @Override public String generateTag(TypeTagSerializer tts) {
        return tts.memoizeObject(this);
    }
}

