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
 * Coercion from value_t to floating point
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import com.samsung.sjs.backend.asts.c.types.*;
import com.samsung.sjs.types.*;

public class FloatUnshift extends Expression {
    private Expression subject;
    public FloatUnshift(Expression e) {
        subject = e;
    }
    @Override
    public String toSource(int x) {
        return "(unshift_double("+subject.toSource(0)+"))";
    }
    @Override
    public Expression inType(Type t) {
        assert (t instanceof FloatType);
        return this;
    }
}

