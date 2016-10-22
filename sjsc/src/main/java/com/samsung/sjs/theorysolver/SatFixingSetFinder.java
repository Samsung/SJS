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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optimal fixing set finder backed by a SAT solver.
 *
 * <p>The SAT solver is used to reason about disjunctions and to find minimum sets of
 * constraints to break.
 *
 * <p>Technical note: since the SAT solver is a black box, this algorithm doesn't do
 * <a href="https://www.cs.upc.edu/~oliveras/TDV/intro-SMT.pdf">theory propagation</a>.
 * This makes for a less efficient SMT solver, but it greatly eases development of
 * theories.
 *
 * @param <T>     the type of constraints
 */
public class SatFixingSetFinder<T> implements FixingSetFinder<T> {

    private final SatSolver solver;
    int fixingSetSize = 0;
    List<int[]> clauses, hard;
    Map<T, Integer> varsByConstraint;
    Map<Integer, T> constraintsByVar;

    public SatFixingSetFinder(SatSolver solver) {
        this.solver = solver;
    }

    @Override
    public boolean isOptimal() {
        return true;
    }

    @Override
    public void setup(Collection<T> allConstraints) {
        clauses = new ArrayList<>(allConstraints.size());
        hard    = new ArrayList<>();
        varsByConstraint = new LinkedHashMap<>();
        constraintsByVar = new LinkedHashMap<>();

        int offset = solver.allocVars(allConstraints.size());
        int i = 0;
        for (T c : allConstraints) {
            int var = offset + i;
            varsByConstraint.put(c, var);
            constraintsByVar.put(var, c);
            clauses.add(new int[]{var});
            ++i;
        }
        clauses.forEach(solver::addClause);
    }

    @Override
    public void addCore(Collection<T> core) {
        int[] literals = new int[core.size()];
        int i = 0;
        for (T c : core) {
            Integer var = varsByConstraint.get(c);
            assert var != null : "theory solver returned constraint (" + c + ") not in the system!";
            literals[i++] = -var;
        }
        clauses.add(literals);
        solver.addClause(literals);
    }

    @Override
    public FixingSetListener.Action currentFixingSet(Collection<T> out, FixingSetListener<T, ?> listener) {

        boolean found = false;
        do {
            Either<BitSet, Collection<int[]>> satResult = solver.getModel();
            BitSet satModel = satResult.left;
            if (satModel != null) {
                found = true;
                for (Map.Entry<Integer, T> e : constraintsByVar.entrySet()) {
                    int v = e.getKey();
                    if (!satModel.get(v)) {
                        out.add(e.getValue());
                    }
                }
            } else {
                ++fixingSetSize;
                if (listener.onWeakening(fixingSetSize) == FixingSetListener.Action.STOP) {
                    return FixingSetListener.Action.STOP;
                }
                solver.clearClauses();
                Collection<int[]> core = satResult.right;
                Collection<Integer> clausesToWeaken = new ArrayList<>(core.size());
                for (int[] clause : core) {
                    int idx = clauses.indexOf(clause);
                    if (idx >= 0) {
                        clausesToWeaken.add(idx);
                    }
                }

                assert clausesToWeaken.size() > 0 && fixingSetSize <= clauses.size() : "hard clauses are unsat";

                int offset = solver.allocVars(clausesToWeaken.size());
                int i = 0;
                for (int clauseIdx : clausesToWeaken) {
                    clauses.set(clauseIdx, TheorySolver.extendClause(clauses.get(clauseIdx), offset + i));
                    ++i;
                }
                for (i = 0; i < clausesToWeaken.size(); ++i) {
                    for (int j = i + 1; j < clausesToWeaken.size(); ++j) {
                        hard.add(new int[] { -(offset + i), -(offset + j) });
                    }
                }

                clauses.forEach(solver::addClause);
                hard.forEach(solver::addClause);
            }
        } while (!found);
        return FixingSetListener.Action.CONTINUE;
    }
}
