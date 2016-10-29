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

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.wala.util.collections.HashSetFactory;
import com.samsung.sjs.typeconstraints.ITypeTerm;
import com.samsung.sjs.types.Property;

public class MROMRWVariable extends TypeInfSolverVariable {

    private Set<Property> mroProps = HashSetFactory.make();

    private Set<Property> mrwProps = HashSetFactory.make();

    private final ITypeTerm term;

    public MROMRWVariable(ITypeTerm term) {
        this.term = term;
    }

    @Override
    public void copyState(TypeInfSolverVariable v) {
        MROMRWVariable other = (MROMRWVariable) v;
        this.mroProps = HashSetFactory.make(other.mroProps);
        this.mrwProps = HashSetFactory.make(other.mrwProps);
        reasonForCurrentValue = v.reasonForCurrentValue;
    }

    /**
     * returns an unmodifiable view of the MRO set, which *will* reflect
     * changes to the underlying set
     * @return
     */
    public Set<Property> getMRO() {
        return Collections.unmodifiableSet(mroProps);
    }

    /**
     * returns an unmodifiable view of the MRW set, which *will* reflect
     * changes to the underlying set
     * @return
     */
    public Set<Property> getMRW() {
        return Collections.unmodifiableSet(mrwProps);
    }

    public void removeMRO(Property prop, Cause cause) {
        boolean removed = mroProps.remove(prop);
        assert removed : "tried to remove non-existent prop " + prop;
        reasonForCurrentValue = Cause.derived(reasonForCurrentValue, cause);
    }

    public void addMRO(Property prop, Cause cause) {
        assert prop.isRO();
        mroProps.add(prop);
        reasonForCurrentValue = Cause.derived(reasonForCurrentValue, cause);
    }

    public void addMRW(Property prop, Cause cause) {
        assert prop.isRW();
        mrwProps.add(prop);
        reasonForCurrentValue = Cause.derived(reasonForCurrentValue, cause);
    }

    public boolean nonEmpty() {
        return !mroProps.isEmpty() || !mrwProps.isEmpty();
    }

    public boolean sameAs(MROMRWVariable other) {
        return mroProps.equals(other.mroProps) && mrwProps.equals(other.mrwProps);
    }

    @Override
    public String toString() {
        return sortedString();
    }

    public String sortedString() {
        return "[MRO=" + sortedPropStr(mroProps) + ", MRW=" + sortedPropStr(mrwProps) + "]";
    }

    private String sortedPropStr(Set<Property> props) {
        return props.stream().map(p -> p.getName().toString()).sorted().collect(Collectors.joining(", ", "[", "]"));
    }

    public ITypeTerm getTerm() {
        return term;
    }

}
