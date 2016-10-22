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
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class GreedyFixingSetFinder<T> implements FixingSetFinder<T> {

    Collection<Collection<T>> cores = new ArrayList<>();
    Collection<T> allConstraints;

    @Override
    public void setup(Collection<T> allConstraints) {
        cores.clear();
        this.allConstraints = new ArrayList<>(allConstraints);
    }

    @Override
    public void addCore(Collection<T> core) {
        cores.add(core);
    }

    @Override
    public FixingSetListener.Action currentFixingSet(Collection<T> out, FixingSetListener<T, ?> listener) {

        Collection<Collection<T>> cores = new LinkedList<>(this.cores);
        while (!cores.isEmpty()) {
            // At each iteration, pick the constraint that appears in the
            // greatest number of uncovered cores.

            Map<T, Integer> counts = cores.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(
                    c -> c,
                    c -> 1,
                    (v1, v2) -> v1 + v2));

            T constraint = allConstraints.stream()
                .filter(counts::containsKey)
                .max((c1, c2) -> counts.get(c1) - counts.get(c2))
                .orElseThrow(NoSuchElementException::new);
            out.add(constraint);
            cores.removeIf(core -> core.contains(constraint));
        }

        return FixingSetListener.Action.CONTINUE;
    }

}
