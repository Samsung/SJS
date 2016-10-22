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
import org.sat4j.maxsat.SolverFactory;
import org.sat4j.maxsat.WeightedMaxSatDecorator;
import org.sat4j.pb.OptToPBSATAdapter;
import org.sat4j.pb.PseudoOptDecorator;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class MaxSatFixingSetFinder<T> implements FixingSetFinder<T> {

    /*
    WeightedMaxSatDecorator solver;
    Map<T, Integer> varsByConstraint = new HashMap<>();
    Map<Integer, T> constraintsByVar = new HashMap<>();

    @Override
    public void setup(Collection<T> allConstraints) {
        solver = new WeightedMaxSatDecorator(SolverFactory.newDefault());
        varsByConstraint.clear();
        constraintsByVar.clear();
    }

    @Override
    public void addCore(Collection<T> core) {
        VecInt clause = new VecInt();

        for (T c : core) {
            Integer var = varsByConstraint.get(c);
            if (var == null) {
                var = solver.nextFreeVarId(true);
                varsByConstraint.put(c, var);
                constraintsByVar.put(var, c);
            }
            clause.push(-var);
            try {
                solver.addSoftClause(new VecInt().push(var));
            } catch (ContradictionException e) {
                throw new RuntimeException("insanity; this should never happen", e);
            }
        }

        try {
            solver.addHardClause(clause);
        } catch (ContradictionException e) {
            throw new RuntimeException("Hard clauses are unsat!", e);
        }
    }

    @Override
    public FixingSetListener.Action currentFixingSet(Collection<T> out, FixingSetListener<T, ?> listener) {
        IProblem problemSolver = new OptToPBSATAdapter(new PseudoOptDecorator(solver));

        try {
            if (problemSolver.isSatisfiable()) {
                int[] model = problemSolver.model();
                if (model == null) {
                    throw new RuntimeException("Hard clauses are unsat!");
                }
//                System.out.println("Got model: " + Arrays.toString(model));
                for (int lit : model) {
                    if (lit < 0) {
                        out.add(constraintsByVar.get(-lit));
                    }
                }
            }
        } catch (TimeoutException e) {
            throw new RuntimeException("timeout", e);
        }

        return FixingSetListener.Action.CONTINUE;
    }
    */

    Collection<Collection<T>> cores = new ArrayList<>();

    @Override
    public boolean isOptimal() {
        return true;
    }

    @Override
    public void setup(Collection<T> allConstraints) {
        cores.clear();
    }

    @Override
    public void addCore(Collection<T> core) {
//        System.out.println("got core " + core);
        cores.add(new ArrayList<>(core));
    }

    @Override
    public FixingSetListener.Action currentFixingSet(Collection<T> out, FixingSetListener<T, ?> listener) {
        // I think Sat4J might have an incremental API, but it's probably safer to just reconstruct
        // the solver every time.
        WeightedMaxSatDecorator solver = new WeightedMaxSatDecorator(SolverFactory.newDefault());
        Map<T, Integer> varsByConstraint = new HashMap<>();
        Map<Integer, T> constraintsByVar = new HashMap<>();

        Collection<T> allConstraints = new LinkedHashSet<>();
        for (Collection<T> core : cores) {
            allConstraints.addAll(core);
        }

//        System.out.print("Allocating " + allConstraints.size() + " vars... ");
//        int var = solver.newVar(allConstraints.size());
//        solver.nextFreeVarId()
//        System.out.println("got block " + var + "..." + (var + allConstraints.size()));
        solver.setExpectedNumberOfClauses(allConstraints.size() + cores.size());
//        System.out.println("predicting " + (allConstraints.size() + cores.size()) + " clauses");

        for (T c : allConstraints) {
            int var = solver.nextFreeVarId(true);
            constraintsByVar.put(var, c);
            varsByConstraint.put(c, var);
            ++var;
        }

        for (Collection<T> core : cores) {
            VecInt clause = new VecInt();
            for (T c : core) {
                clause.push(-varsByConstraint.get(c));
            }
            try {
//                System.out.println("adding hard clause " + clause);
                solver.addHardClause(clause);
            } catch (ContradictionException e) {
                throw new RuntimeException("Hard clauses are unsat!", e);
            }
        }

        for (int v : constraintsByVar.keySet()) {
            try {
                solver.addSoftClause(new VecInt().push(v));
//                System.out.println("adding soft clause [" + v + ']');
            } catch (ContradictionException e) {
                throw new RuntimeException("insanity, this should never happen", e);
            }
        }

        solver.setTimeout(100000);
        IProblem problemSolver = new OptToPBSATAdapter(new PseudoOptDecorator(solver));

        try {
            if (problemSolver.isSatisfiable()) {
                int[] model = problemSolver.model();
                if (model == null) {
                    throw new RuntimeException("Hard clauses are unsat!");
                }
//                System.out.println("Got model: " + Arrays.toString(model));
                for (int lit : model) {
                    if (lit < 0) {
                        out.add(constraintsByVar.get(-lit));
                    }
                }
                if (!cores.stream().allMatch(c -> c.stream().anyMatch(out::contains))) {
//                    System.err.println("cores = " + cores);
//                    System.err.println("out = " + out);
//                    System.err.println("model = " + Arrays.toString(model));
                    throw new RuntimeException("bug!");
                }
            }
        } catch (TimeoutException e) {
            throw new RuntimeException("timeout", e);
        }

        return FixingSetListener.Action.CONTINUE;
    }


}
