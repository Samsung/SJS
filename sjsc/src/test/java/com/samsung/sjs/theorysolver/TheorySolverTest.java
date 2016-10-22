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
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class TheorySolverTest {

    private static final long SEED = 1000L;

    /**
     * This tests the {@link TheorySolver} using a theory which has a random set of
     * blacklisted objects. We verify that the TheorySolver always finds the entire
     * set of non-blacklisted objects.
     */
    @Test
    public void testBasics() {

        List<Object> all = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h");

        for (int i = 0; i < 100; ++i) {
            Random r = new Random(SEED + i);

            Collection<Object> truthy = all.stream()
                    .filter(x -> r.nextBoolean())
                    .collect(Collectors.toSet());

            Theory<Object, Void> theory = positive -> {
                Collection<Object> bad = positive.stream()
                        .filter(x -> !truthy.contains(x))
                        .collect(Collectors.toSet());
                if (bad.size() > 0) {
                    // Construct a random, nonempty unsat core.
                    Collection<Object> unsat = new HashSet<>();
                    unsat.add(bad.iterator().next());
                    bad.stream().filter(x -> r.nextBoolean()).forEach(unsat::add);
                    return Either.right(unsat);
                } else {
                    return Either.left(null);
                }
            };

            Pair<Void, Collection<Object>> result = TheorySolver.solve(
                theory, new SatFixingSetFinder<>(new Sat4J()),
                Collections.emptyList(), all);

            Assert.assertEquals(all.size() - truthy.size(), result.getRight().size());
            Assert.assertEquals(truthy, all.stream().filter(x -> !result.getRight().contains(x)).collect(Collectors.toSet()));
        }

    }

}
