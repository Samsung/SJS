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
 * Base class for expressions
 * 
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.ir;

public abstract class Expression extends IRNode {
    protected Expression(Tag t) {
        super(t);
    }
    @Override
    public final boolean isExpression() { return true; }
    @Override
    public final Expression asExpression() { return this; }

    public boolean isVar() { return false; }
    public Var asVar() { throw new IllegalArgumentException(); }

    /**
     * Check whether the expression is (extensionally) pure
     */
    public boolean isPure() { return false; }

    /**
     * Check whether the expression is constant (or, folds to the same constant)
     */
    public boolean isConst() { return false; }

    /**
     * Check if the expression may trip the dirty flag in interop mode.
     *
     * This check returns true if the expression may trip the dirty flag, coarsely overapproximating
     * whether the expression must be SSA transformed so intermediate results can be transferred to
     * a dynamic type-checking path.  It is always sound to return true.
     *
     * Returning false can improve performance, since it avoids value packing/unpacking that the C
     * compiler sometimes can't reason about well (i.e., cancellativity of X_as_val and val_as_X),
     * but doing so when some subexpression may observe the dirty flag trip (i.e., a function call)
     * is unsound, because we won't save the intermediate results for the path switch correctly!
     *
     * TODO: Think about whether this, or a value numbering approach are the way to go
     */
    public boolean mustSaveIntermediates() { return true; }

    public boolean isCall() { return false; }
    public Call asCall() { throw new IllegalArgumentException(); }
}
