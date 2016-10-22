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
package com.samsung.sjs.theorysolver;

import java.util.BitSet;
import java.util.Collection;

/**
 * An incremental boolean satisfiability solver. NOTE: to make
 * implementation easier, {@link #allocVars(int)} is only allowed
 * to be called <em>before any other method in this interface</em>
 * or <em>after a call to {@link #clearClauses()}</em>.
 */
public interface SatSolver {

    /**
     * Allocate variables.
     * @param count the number of variables to allocate
     * @return an integer offset such that [offset, offset+count)
     *   are now all legal variable names.
     */
    int allocVars(int count);

    /**
     * Add a clause.
     * @param literals the clause literals (v or -v for each v
     *                 returned by {@link #allocVars(int)})
     */
    void addClause(int... literals);

    /**
     * Clear all the added clauses.
     */
    void clearClauses();

    /**
     * Check satisfiability.
     * @return either a model (a BitSet s where s.get(v) indicates
     *   whether v was set to true) or an unsatisfiable core (a
     *   collection of clauses which are, by themselves,
     *   unsatisfiable).
     */
    Either<BitSet, Collection<int[]>> getModel();

}
