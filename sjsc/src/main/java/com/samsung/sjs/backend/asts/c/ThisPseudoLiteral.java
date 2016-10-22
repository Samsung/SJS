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
 * Representation of the C backend encoding of 'this'
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import com.samsung.sjs.types.*;
public class ThisPseudoLiteral extends Expression {
    public ThisPseudoLiteral() {}
    @Override
    public String toSource(int x) { return "__this"; }
    @Override
    public Expression asValue(Type t) {
        // the __this variables is always bound as a value_t, no need to rewrap (which then cancels
        // later coercions and fails to produce a C type
        return this;
    }
}
