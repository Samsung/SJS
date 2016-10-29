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
/**
 * Lexical scope.  This class is used to track sets of variables, as well as nesting of lexical
 * scopes.
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

import com.samsung.sjs.types.*;
import java.util.*;

public final class Scope implements Iterable<Var> {

    Scope parent;
    Set<Var> vars;
    Set<String> names;
    // We map strings to types instead of variables so map lookup will work,
    // and hashing will be more efficient
    Map<String,Type> types;

    public Iterator<Var> iterator() { return vars.iterator(); }

    public Scope(Scope parent) {
        vars = new HashSet<Var>();
        names = new HashSet<String>();
        types = new HashMap<String,Type>();
        this.parent = parent;
    }

    public Scope getParentScope() {
        return parent;
    }

    public boolean isLocallyBound(Var x) {
        return names.contains(x.getIdentifier());
    }

    public boolean isBound(Var x) {
        Scope cur = this;
        while (cur != null) {
            if (cur.isLocallyBound(x)) {
                return true;
            }
            cur = cur.getParentScope();
        }
        return false;
    }

    public Scope getContainingScope(Var x) {
        Scope cur = this;
        while (cur != null) {
            if (cur.isLocallyBound(x)) {
                return cur;
            }
            cur = cur.getParentScope();
        }
        return null;
    }

    /**
     * Declare a variable in this scope with some type.  Returns false if no further action is
     * needed by the caller.  Returns true if the declaration should be changed to an assignment due
     * to a previous declaration of the same variable with the same type.
     *
     * @return Whether or not the current declaration should be changed to an assignment.
     */
    public boolean declareVariable(Var v, Type t) {
        if (!names.contains(v.getIdentifier())) {
            names.add(v.getIdentifier());
            vars.add(v);
            types.put(v.getIdentifier(), t);
            return false;
        }
        // This isn't the most stable check
        if (!types.get(v.getIdentifier()).toString().equals(t.toString())) {
            dumpScopeChain();
            throw new IllegalArgumentException("Scope already contains variable ["+v.getIdentifier()+"] at type "+types.get(v.getIdentifier())+", but attempting to declare at type "+t);
        }
        return true;
    }

    public Type lookupType(Var x) {
        Scope cur = this;
        while (cur != null) {
            if (cur.isLocallyBound(x)) {
                return cur.types.get(x.getIdentifier());
            }
            cur = cur.getParentScope();
        }
        return null;
    }

    public void dumpScopeChain() {
        System.err.println("Dumping scope chain:");
        Scope cur = this;
        while (cur != null) {
            System.err.println("Scope "+cur+": ");
            for (Map.Entry<String,Type> kv : cur.types.entrySet()) {
                System.err.println("\t"+kv.getKey()+" : "+kv.getValue());
            }
            cur = cur.getParentScope();
        }
    }
}
