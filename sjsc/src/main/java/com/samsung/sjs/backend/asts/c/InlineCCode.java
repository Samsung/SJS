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
 * Represent a chunk of explicit C source code text, usable in any expresion context
 *
 * Really, this just blasts a string literal into the C output.
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import com.samsung.sjs.types.*;
public class InlineCCode extends Expression {
    private String source;
    public InlineCCode(String s) {
        source = s;
    }
    @Override
    public String toSource(int x) { return source; }
    @Override
    public Expression asValue(Type t) {
        return this;
    }
}
