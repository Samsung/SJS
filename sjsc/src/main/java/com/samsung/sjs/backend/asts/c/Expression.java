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
 * Parent class for expressions in C
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import com.samsung.sjs.backend.asts.c.types.*;
import com.samsung.sjs.types.*;
public abstract class Expression extends CNode {
    @Override
    public boolean isExpression() { return true; }
    @Override
    public Expression asExpression() { return this; }

    // TODO: float shifting in interop mode
    public Expression asValue(Type t) {
        return new ValueCoercion(t, this, false);
    }

    /*
     * asValue() and inType() not only implement coercions, but we rely on subclass overrides to
     * effectively implement peephole optimizations.  Casting a value coercion to a value is
     * non-sense (it can return itself), and coercing a value coercion to another type (i.e., the
     * underlying value's original type) is as simple as removing the value coercion, rather than
     * doing more wrapping.
     */

    public Expression inType(Type t) {
        // TODO: float shifting in interop mode
        //if (t instanceof FloatType) {
        //    return new FloatUnshift(this);
        //}
        return new ValueAs(this, t);
    }
}
