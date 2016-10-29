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

import com.samsung.sjs.typeconstraints.ITypeConstraint;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Causes represent a derivation tree: why is some fact true? When an error
 * arises in the {@link TypeConstraintFixedPointSolver}, Causes can be used
 * to reconstruct why that error happened.
 *
 * <p>A cause is either a trivial truth ({@link #noReason()}), as a direct
 * result of a constraint ({@link #src(ITypeConstraint)}), or by derivation
 * using other facts ({@link #derived(Cause...)}).
 */
public interface Cause {

    Cause NO_REASON = new Cause() {
        @Override
        public <T extends Collection<ITypeConstraint>> T gatherCore(T target, Set<Cause> seen) {
            return target;
        }

        @Override
        public ITypeConstraint asSingleton() {
            return null;
        }

        @Override
        public Cause[] predecessors() {
            return new Cause[0];
        }
    };

    /**
     * @return a cause representing a trivial truth
     */
    static Cause noReason() {
        return NO_REASON;
    }

    /**
     * @param srcConstraint a constraint
     * @return a cause representing "follows immediately from srcConstraint"
     */
    static Cause src(ITypeConstraint srcConstraint) {
        Objects.requireNonNull(srcConstraint);
        return new Cause() {
            @Override
            public <T extends Collection<ITypeConstraint>> T gatherCore(T target, Set<Cause> seen) {
                target.add(srcConstraint);
                return target;
            }

            @Override
            public ITypeConstraint asSingleton() {
                return srcConstraint;
            }

            @Override
            public Cause[] predecessors() {
                return new Cause[0];
            }
        };
    }

    /**
     * @param causes a list of causes
     * @return a cause representing "follows from these other causes"
     */
    static Cause derived(Cause... causes) {
        for (Cause c : causes) Objects.requireNonNull(c);
        return new Cause() {
            @Override
            public <T extends Collection<ITypeConstraint>> T gatherCore(T target, Set<Cause> seen) {
                if (seen.contains(this)) {
                    return target;
                }
                seen.add(this);
                for (Cause c : causes) {
                    c.gatherCore(target, seen);
                }
                return target;
            }

            @Override
            public ITypeConstraint asSingleton() {
                return null;
            }

            @Override
            public Cause[] predecessors() {
                return causes;
            }
        };
    }

    /**
     * @return the set of constraints contributing to this cause
     */
    default Set<ITypeConstraint> core() {
        return gatherCore(new HashSet<>(), new HashSet<>());
    }

    /**
     * Efficiently gather the constraints contributing to this cause
     * @param target a set to put the core into
     * @param seen causes which have already been visited
     * @return target
     */
    <T extends Collection<ITypeConstraint>> T gatherCore(T target, Set<Cause> seen);

    /**
     * @return if this cause was directly derived from a single
     *   constraint, then that constraint, otherwise null
     */
    ITypeConstraint asSingleton();

    /**
     * @return if this cause was derived from other causes, then
     *   those causes, otherwise an empty array
     */
    Cause[] predecessors();

}
