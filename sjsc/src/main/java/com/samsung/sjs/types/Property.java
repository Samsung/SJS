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

import com.samsung.sjs.SourceLocation;

/**
 * Represents a property in an object. Each property has a name and a type
 * and an identifier that indicates if it belongs to the RO/RW/MRO/MRW set.
 * Each Property object belongs to exactly one such set.
 *
 * @author ftip
 */
public class Property implements Comparable<Property> {

	public Property(String name, Type type, boolean readOnly){
	    this(name,type,readOnly,null);
	}

    public Property(String name, Type type, boolean readOnly, SourceLocation sourceLoc) {
        this.name = name;
        this.type = type;
        this.readOnly = readOnly;
        this.sourceLoc = sourceLoc;
    }

	public String getName() {
		return name;
	}



	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (readOnly ? 1231 : 1237);
        result = prime * result
                + ((sourceLoc == null) ? 0 : sourceLoc.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Property other = (Property) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (readOnly != other.readOnly)
            return false;
        if (sourceLoc == null) {
            if (other.sourceLoc != null)
                return false;
        } else if (!sourceLoc.equals(other.sourceLoc))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    public Type getType() {
		return type;
	}

	@Override
	public String toString(){
		return name + ": " + type.toString();
	}

	@Override
	public int compareTo(Property o) {
		return this.name.compareTo(((Property)o).name);
	}

	public boolean isRO() {
	    return readOnly;
	}

	public boolean isRW() {
	    return !readOnly;
	}

	private final String name;
	private final Type type;
	private final boolean readOnly;

	/**
	 * for cases where there is a read or write operation that can
	 * be associated with the property, the source location of the
	 * operation.  otherwise, null
	 */
	private final SourceLocation sourceLoc;

    public SourceLocation getSourceLoc() {
        return sourceLoc;
    }
}

