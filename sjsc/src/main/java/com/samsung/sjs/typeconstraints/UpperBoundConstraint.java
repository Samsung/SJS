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
package com.samsung.sjs.typeconstraints;

import com.samsung.sjs.constraintsolver.Cause;
import com.samsung.sjs.types.UnattachedMethodType;

/**
 *
 * A constraint relating a possible method term
 * to the MRO and MRW of the containing object type.
 * When processed, if the possible method term is in
 * fact of {@link UnattachedMethodType}, then relate
 * the upper bound of the method's receiver to the MRO/MRW
 * of the containing object type.
 *
 * @author m.sridharan
 *
 */
public class UpperBoundConstraint implements MROMRWConstraint {

    private final ITypeTerm possibleMethodTerm;

    private final ITypeTerm containingObjectTerm;
    private final Cause reason;

    public ITypeTerm getPossibleMethodTerm() {
        return possibleMethodTerm;
    }

    public ITypeTerm getContainingObjectTerm() {
        return containingObjectTerm;
    }

    public UpperBoundConstraint(ITypeTerm possibleMethodTerm,
                                ITypeTerm containingObjectTerm, Cause reason) {
        this.possibleMethodTerm = possibleMethodTerm;
        this.containingObjectTerm = containingObjectTerm;
        this.reason = reason;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((containingObjectTerm == null) ? 0 : containingObjectTerm
                        .hashCode());
        result = prime
                * result
                + ((possibleMethodTerm == null) ? 0 : possibleMethodTerm
                        .hashCode());
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
        UpperBoundConstraint other = (UpperBoundConstraint) obj;
        if (containingObjectTerm == null) {
            if (other.containingObjectTerm != null)
                return false;
        } else if (!containingObjectTerm.equals(other.containingObjectTerm))
            return false;
        if (possibleMethodTerm == null) {
            if (other.possibleMethodTerm != null)
                return false;
        } else if (!possibleMethodTerm.equals(other.possibleMethodTerm))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "UpperBoundConstraint [methodReceiverTerm=" + possibleMethodTerm
                + ", containingObjectTerm=" + containingObjectTerm + "]";
    }


    @Override
    public Cause getReason() {
        return reason;
    }
}
