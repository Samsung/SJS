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
package com.samsung.sjs.constraintsolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.types.Type;

/**
 * Created by schandra on 3/11/15.
 */

public class TypeConstraintSolverVariable extends TypeInfSolverVariable {

    private static Logger logger = LoggerFactory.getLogger(TypeConstraintSolverVariable.class);

    /**
     * current type solution for this variable
     */
    private Type type;

    /**
     * a term that "explains" the current solution {@link #type}.  If this
     * is a lower bound variable, the explanation would be a corresponding type
     * source.  If this is an upper bound variable, the explanation would be a
     * "sink", i.e., some expression requiring a value of type {@link #type}
     */
    private ITypeTerm termForType;
    /**
     * the original {@link ITypeTerm} associated with this variable
     */
    private final ITypeTerm orig;

    /**
     * whether this variable is a lower or upper bound for term {@link #orig}
     */
    private final String hilo; /* for debugging only */

    public TypeConstraintSolverVariable(ITypeTerm t, String hilo, Type type) {
        this.type = type;
        this.orig = t;
        this.hilo = hilo;
    }

    public Type getType() { return type; }

    public ITypeTerm getTermForType() {
        return termForType;
    }

    public ITypeTerm getOrigTerm() {
        return orig;
    }

    public void setType(Type type, Cause cause) {
        setTypeAndTerm(type, null, cause);
    }

    public void setTypeAndTerm(Type type, ITypeTerm term, Cause cause) {
        this.type = type;
        this.termForType = term;
        reasonForCurrentValue = Cause.derived(reasonForCurrentValue, cause);
    }

    @Override
    public void copyState(TypeInfSolverVariable other) {
        type = ((TypeConstraintSolverVariable)other).type;
        reasonForCurrentValue = other.reasonForCurrentValue;
    }

    public String toString() {

        if (logger.isDebugEnabled())
            return orig + ":" + hilo + ":" + getType().toString() + "(" + termForType + ")";
        else
            return getType().toString();
    }

}
