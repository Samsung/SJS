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

import com.samsung.sjs.JSEnvironment;
import com.samsung.sjs.ModuleSystem;
import com.samsung.sjs.constraintgenerator.ConstraintFactory;
import com.samsung.sjs.constraintgenerator.ConstraintGenerator;
import com.samsung.sjs.constraintsolver.Cause;
import com.samsung.sjs.constraintsolver.CoreException;
import com.samsung.sjs.constraintsolver.DirectionalConstraintSolver;
import com.samsung.sjs.constraintsolver.TypeAssignment;
import com.samsung.sjs.typeconstraints.ITypeConstraint;
import org.mozilla.javascript.ast.AstRoot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Solver class for SJS types.
 *
 * <p>Note that there is a lot of weirdness here. This is a Theory over
 * integers, not {@link ITypeConstraint}s! Since the constraint solving
 * API exposed by {@link DirectionalConstraintSolver} is stateful, we
 * need to regenerate the constraints from source every time the solver
 * is invoked. We can't keep the constraints around! Instead, the
 * integers represent indexes into the list of constraints generated
 * from the source tree. Mercifully, this list has a consistent order.
 * Someday, someone should clean all that up.
 */
public class SJSTypeTheory implements Theory<Integer, TypeAssignment> {

    private static final boolean DUMP_DOT = false;

    private final List<ITypeConstraint> constraints;
    private final JSEnvironment env;
    private final AstRoot root;
    private ConstraintFactory factory;
    private ConstraintGenerator generator;
    private ModuleSystem modsys;

    public SJSTypeTheory(JSEnvironment env, ModuleSystem modsys, AstRoot root) {
        constraints = new ArrayList<>();
        this.env = env;
        this.root = root;
        this.modsys = modsys;
    }

    public List<ITypeConstraint> getConstraints() {
        constraints.clear();
        factory = new ConstraintFactory();
        generator = new ConstraintGenerator(factory, env, modsys);
        generator.generateConstraints(root);
        constraints.addAll(generator.getTypeConstraints());
        return new ArrayList<>(constraints);
    }

    public List<ITypeConstraint> hackyConstraintAccess() {
        return constraints;
    }

    public Integer hackySrcLoc(ITypeConstraint c) {
        Set<Integer> locs = generator.getSourceMapping().get(c);
        return locs != null && !locs.isEmpty() ?
                locs.iterator().next() :
                null;
    }

    public ConstraintGenerator hackyGenerator() {
        return generator;
    }

    @Override
    public Either<TypeAssignment, Collection<Integer>> check(Collection<Integer> pos) {
        // Grumble grumble stateful APIs... we need to reconstruct all of this...
        getConstraints();

        Set<ITypeConstraint> cs = pos.stream()
            .sorted()
            .map(constraints::get)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        DirectionalConstraintSolver solver = new DirectionalConstraintSolver(cs, factory, generator);
        TypeAssignment solution;
        try {
            solution = solver.solve();
        } catch (CoreException e) {
            Collection<Integer> core = new HashSet<>();
            for (ITypeConstraint c : e.cause.core()) {
                int idx = constraints.indexOf(c);
                assert idx >= 0 : "constraint not found!";
                core.add(idx);
            }
            if (DUMP_DOT) {
                dumpDot(e.cause);
            }
            return Either.right(core);
        }

        return Either.left(solution);
    }

    private static void dumpDot(Cause core) {
        try {
            File f = Files.createTempFile("boo", ".dot").toFile();
            System.out.println("dumping dot: " + f.getAbsolutePath());
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "ascii"))) {
                writer.write("digraph G {\n");
                Map<Cause, Integer> ids = new HashMap<>();
                Collection<Cause> visited = new HashSet<>();
                Queue<Cause> q = new LinkedList<>();
                q.add(core);
                int counter = 0;
                while (!q.isEmpty()) {
                    Cause c = q.remove();
                    if (visited.contains(c)) {
                        continue;
                    }
                    visited.add(c);
                    Integer id = ids.get(c);
                    if (id == null) {
                        id = (counter++);
                        ids.put(c, id);
                    }
                    writer.write(Integer.toString(id));
                    ITypeConstraint root = c.asSingleton();
                    if (root != null) {
                        writer.write(" [label=\"");
                        String s = root.toString();
                        int idx = s.indexOf('\n');
                        if (idx >= 0) {
                            s = s.substring(0, idx) + "...";
                        }
                        writer.write(escape(s));
                        writer.write("\"]");
                    } else if (c.predecessors().length == 0) {
                        writer.write(" [label=\"_\"]\n");
                    }
                    writer.write('\n');
                    for (Cause p : c.predecessors()) {
                        if (p.core().isEmpty()) {
                            continue;
                        }
                        Integer id2 = ids.get(p);
                        if (id2 == null) {
                            id2 = (counter++);
                            ids.put(p, id2);
                        }
                        writer.write(Integer.toString(id2));
                        writer.write(" -> ");
                        writer.write(Integer.toString(id));
                        writer.write('\n');
                        q.add(p);
                    }
                }
                writer.write("}\n");
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static String escape(String s) {
        return s.replaceAll(Pattern.quote("\\"), Matcher.quoteReplacement("\\\\"))
                .replaceAll(Pattern.quote("\""), Matcher.quoteReplacement("\\\""));
    }

}
