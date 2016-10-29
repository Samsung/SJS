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

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Sat4J implements SatSolver {
    private int nvars = 0;
    private final ISolver solver = SolverFactory.newDefault();
    private final Map<Integer, int[]> clausesByKey = new LinkedHashMap<>();

    @Override
    public int allocVars(int count) {
        int v = nvars + 1;
        nvars += count;
        solver.newVar(nvars);
        return v;
    }

    @Override
    public void addClause(int... literals) {
        int key = allocVars(1);
        try {
            int[] c2 = new int[literals.length + 1];
            System.arraycopy(literals, 0, c2, 1, literals.length);
            c2[0] = -key;
            solver.addClause(new VecInt(c2));
            clausesByKey.put(key, literals);
        } catch (ContradictionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearClauses() {
        solver.reset();
        solver.newVar(nvars);
        clausesByKey.clear();
    }

    @Override
    public Either<BitSet, Collection<int[]>> getModel() {
        int[] assumps = new int[clausesByKey.size()];
        int i = 0;
        for (Integer k : clausesByKey.keySet()) {
            assumps[i++] = k;
        }
        try {
            if (solver.isSatisfiable(new VecInt(assumps))) {
                BitSet model = new BitSet(nvars + 1);
                for (int lit : solver.model()) {
                    if (lit > 0) {
                        model.set(lit);
                    }
                }
                return Either.left(model);
            } else {
                IVecInt coreLits = solver.unsatExplanation();
                Collection<int[]> core = new ArrayList<>();
                for (i = 0; i < coreLits.size(); ++i) {
                    int lit = coreLits.get(i);
                    core.add(clausesByKey.get(lit));
                }
                return Either.right(core);
            }
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
