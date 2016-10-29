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
 * Callback interface for {@link TheorySolver#enumerateFixingSets(FixingSetFinder, Theory, Collection, Collection, FixingSetListener)}.
 */
public interface FixingSetListener<Constraint, Model> {

    enum Action { STOP, CONTINUE }

    default Action onFixingSet(Model model, Collection<Constraint> fixingSet) { return Action.CONTINUE; }
    default Action onCore(Collection<Constraint> unsatCore) { return Action.CONTINUE; }
    default Action onWeakening(int newFixingSetSize) { return Action.CONTINUE; }

    // ------------------------------------------------------------------------

    FixingSetListener DUMMY_LISTENER = new FixingSetListener() { };

    @SuppressWarnings("unchecked")
    static <Constraint, Model> FixingSetListener<Constraint, Model> dummyListener() {
        return (FixingSetListener<Constraint, Model>)DUMMY_LISTENER;
    }

}
