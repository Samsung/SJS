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

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This is a tiny max-theory solver. See
 * {@link #solve(Theory, FixingSetFinder, List, List)}
 * for extended details.
 */
public class TheorySolver {

    /** True to shuffle constraints before sending them to the theory solver */
    private static final boolean SHUFFLE = false;

    /** True to enable verbose progress messages */
    private static final boolean NOISY = true;

    private static <T> T timed(Supplier<T> f, String msg) {
        if (NOISY) {
            System.out.print(msg + "... ");
            System.out.flush();
            long start = System.currentTimeMillis();
            T result = f.get();
            long duration = System.currentTimeMillis() - start;
            System.out.println(duration + "ms");
            return result;
        } else {
            return f.get();
        }
    }

    static int[] extendClause(int[] clause, int i) {
        int[] result = new int[clause.length + 1];
        System.arraycopy(clause, 0, result, 0, clause.length);
        result[clause.length] = i;
        return result;
    }

    public static <Constraint, Model> void enumerateFixingSets(
            FixingSetFinder<Constraint> fixingSetFinder,
            Theory<Constraint, Model> theorySolver,
            Collection<Constraint> hardConstraints,
            Collection<Constraint> softConstraints,
            FixingSetListener<Constraint, Model> listener) {

        Collection<Constraint> constraints = new ArrayList<>();
        Collection<Constraint> core        = new ArrayList<>();
        Collection<Constraint> fixingSet   = new LinkedHashSet<>();
        for (;;) {

            if (fixingSetFinder.currentFixingSet(fixingSet, listener) == FixingSetListener.Action.STOP) {
                return;
            }

            constraints.addAll(hardConstraints);
            softConstraints.stream().filter(c -> !fixingSet.contains(c)).forEach(constraints::add);
            Either<Model, Collection<Constraint>> result = theorySolver.check(constraints);
            if (result.left != null) {
                if (listener.onFixingSet(result.left, fixingSet) == FixingSetListener.Action.STOP) {
                    return;
                }
                fixingSetFinder.addCore(constraints.stream()
                        .filter(softConstraints::contains)
                        .collect(Collectors.toList()));
            } else {
                result.right.stream().filter(softConstraints::contains).forEach(core::add);
                if (listener.onCore(core) == FixingSetListener.Action.STOP) {
                    return;
                }
                assert core.stream().allMatch(c -> !fixingSet.contains(c));
                fixingSetFinder.addCore(core);
            }
            core.clear();
            constraints.clear();
            fixingSet.clear();

        }

    }

    /**
     * Given a fixing set finder, a solver for some particular theory, and some constraints,
     * this procedure either finds a model or it finds a minimum set of constraints
     * which, if broken, make the system satisfiable (a "fixing set").
     *
     * <p>A <em>theory</em> is a solver that solves simple conjunctions of constraints.
     * The performance of this algorithm highly depends on the ability of the theory to
     * produce small unsatisfiable cores.
     *
     * @param <Constraint>    the type of constraint solved by the theory
     * @param <Model>         the type of model produced by the theory
     * @param theorySolver    a solver for some theory
     * @param fixingSetFinder a strategy for finding a fixing set
     * @param hardConstraints the hard constraints which MUST be satisfied
     * @param softConstraints the labeled soft constraints from which the fixing set is drawn
     * @return A pair (m,l) where l is the fixing set (a minimum set of constraints which had
     *   to be weakened to satisfy the system), and m is the resulting model after weakening.
     *   Note that <code>l.isEmpty()</code> means the entire set of constraints is satisfiable.
     * @see SatSolver
     * @see Theory
     */
    public static <Constraint, Model> Pair<Model, Collection<Constraint>> solve(
            Theory<Constraint, Model> theorySolver,
            FixingSetFinder<Constraint> fixingSetFinder,
            List<Constraint> hardConstraints,
            List<Constraint> softConstraints) {

        FixingSetListener<Constraint, Model> listener = NOISY ?
            loggingListener() :
            FixingSetListener.dummyListener();

        fixingSetFinder.setup(softConstraints);

        // These two are complements of each other:
        //    - fixingSet is the constraints we are going to remove
        //    - positive  is the constraints we are going to keep
        //      (i.e. all those not in the fixing set)
        Collection<Constraint> fixingSet = new ArrayList<>();
        Collection<Constraint> positive = new LinkedHashSet<>();
        for (;;) {

            // Be polite---interruptions mean that someone else wants
            // this thread to stop what it's doing.
            if (Thread.currentThread().isInterrupted()) {
                throw new RuntimeException("thread was interrupted");
            }

            positive.addAll(hardConstraints);
            positive.addAll(softConstraints);
            positive.removeAll(fixingSet);

            // Check the proposed fixing set against the theory.
            Either<Model, Collection<Constraint>> result =
                timed(() -> theorySolver.check(positive), "CHECKING THEORY");

            if (result.right == null) {
                // The proposed fixing set works! We are done!
                if (NOISY) System.out.println("VALID MODEL");
                listener.onFixingSet(result.left, fixingSet);
                return Pair.of(result.left, fixingSet);
            } else {
                // The proposed fixing set didn't work. We will use the core
                // to adjust what fixing set gets returned next.

                // Inform the listener
                listener.onCore(result.right);

                // The fixing set finder shouldn't care about hard constraints
                Collection<Constraint> softCore = result.right.stream()
                    .filter(c -> !hardConstraints.contains(c))
                    .collect(Collectors.toList());

                if (softCore.isEmpty()) {
                    throw new IllegalStateException("Hard clauses are unsat!");
                }

                // Push the core to the fixing set finder
                fixingSet.clear();
                fixingSetFinder.addCore(softCore);
                timed(() -> fixingSetFinder.currentFixingSet(fixingSet, listener),
                    "FINDING FIXING SET");
                System.out.println("  --> PROPOSAL SIZE = " + fixingSet.size());
            }

        }

    }

    private static <T> Collection<T> without(Collection<T> collection, T element) {
        return collection.stream().filter(e -> !element.equals(e)).collect(Collectors.toList());
    }

    public static <Constraint, Model> Pair<Model, Collection<Constraint>> minimizeFixingSet(
        Theory<Constraint, Model> theorySolver,
        List<Constraint> hardConstraints,
        List<Constraint> softConstraints,
        Model model,
        Collection<Constraint> fixingSet) {

        System.out.println("MINIMIZING FIXING SET [initial size=" + fixingSet.size() + ']');

        int iter = 0;
        Collection<Constraint> positive = new LinkedHashSet<>();
        boolean changed;
        do {

            ++iter;
            System.out.println("  --> iteration " + iter + "...");

            changed = false;
            for (Constraint c : fixingSet) {
                Collection<Constraint> candidate = without(fixingSet, c);
                positive.addAll(hardConstraints);
                positive.addAll(softConstraints);
                positive.removeAll(candidate);

                Either<Model, Collection<Constraint>> result = theorySolver.check(positive);
                if (result.left != null) {
                    // it's still a fixing set!
                    changed = true;
                    fixingSet = candidate;
                    model = result.left;
                    break;
                }
            }

        } while (changed);

        System.out.println("FINISHED MINIMIZING [final size=" + fixingSet.size() + ']');
        return Pair.of(model, fixingSet);
    }

    private static <Constraint, Model> FixingSetListener<Constraint, Model> loggingListener() {
        return new FixingSetListener<Constraint, Model>() {
            @Override
            public Action onCore(Collection<Constraint> unsatCore) {
                System.out.println("INVALID MODEL [core_size=" + unsatCore.size() + ']');
                return Action.CONTINUE;
            }

            @Override
            public Action onFixingSet(Model model, Collection<Constraint> fixingSet) {
                System.out.println("VALID MODEL");
                return Action.CONTINUE;
            }
        };
    }

}
