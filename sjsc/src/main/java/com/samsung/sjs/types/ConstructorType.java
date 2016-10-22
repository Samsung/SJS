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

import java.util.List;

/**
 * Represents a constructor with prototype information.  The prototype is used for checking access
 * to C.prototype, which is used for inheritance.  The function part of the type signature reflects
 * the arguments and result of construction.  The result type is also the type of the receiver in
 * the constructor body.
 *  
 * @author colin.gordon
 *
 */
public final class ConstructorType extends CodeType {

    private Type proto;
    private int nrTyVars;

    public ConstructorType(List<Type> paramTypes, List<String> paramNames, Type returnType, Type proto) {
        this(paramTypes, paramNames, returnType, proto, 0);
    }

    public ConstructorType(List<Type> paramTypes, List<String> paramNames, Type returnType, Type proto, int tyvars) {
        super(paramTypes, paramNames, returnType);
        this.proto = proto;
        this.nrTyVars = 0;
    }
    
    public int getNrTypeVars(){
        return nrTyVars;
    }

    public Type getPrototype() {
        return proto;
    }
    public void setPrototype(Type p) {
        proto = p;
    }

    @Override
    public String toString() {
        String result =  "ctor<"+nrTyVars+">[" + proto + "](";
        for (int i=0; i < paramTypes().size(); i++){
            result += paramTypes().get(i);
            if (i+1 < paramTypes().size()) result += ",";
        }
        result += ")" + " -> " + returnType();
        return result;
    }

    // Remaining type interface members
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
		return true;
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
	
	@Override
	public boolean isIntersectionType() { 
		return false;
	}
    @Override
    public String generateTag(TypeTagSerializer tts) {
        return tts.memoizeClosure(TypeTagSerializer.ClosureType.CTOR,
                                  returnType(),
                                  null,
                                  paramTypes());
    }
}

