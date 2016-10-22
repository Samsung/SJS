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

import java.util.Collection;

/**
 * A theory which solves constraints of type C.
 * @param <C> the type of constraints
 * @param <M> the type of model produced
 */
public interface Theory<C, M> {

    /**
     * Check a conjunction of constraints.
     * @param constraints the constraints to check
     * @return either a model or an unsatisfiable core (a small
     *   set of constraints which are collectively unsatisfiable).
     *   A degenerate (but legal) unsatisfiable core is simply the
     *   collection of all the input constraints.
     */
    Either<M, Collection<C>> check(Collection<C> constraints);

}
